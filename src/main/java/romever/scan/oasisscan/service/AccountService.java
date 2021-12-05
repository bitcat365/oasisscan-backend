package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.entity.Account;
import romever.scan.oasisscan.entity.Debonding;
import romever.scan.oasisscan.entity.Delegator;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.AccountRepository;
import romever.scan.oasisscan.repository.DebondingRepository;
import romever.scan.oasisscan.repository.DelegatorRepository;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.response.AccountDebondingResponse;
import romever.scan.oasisscan.vo.response.AccountResponse;
import romever.scan.oasisscan.vo.response.AccountValidatorResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DebondingRepository debondingRepository;
    @Autowired
    private DelegatorRepository delegatorRepository;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;

    @Cached(expire = 60, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult accountList(int page, int size) {
        List<AccountResponse> responses = Lists.newArrayList();
        long total = accountRepository.countBy();
        if (total > 0) {
            PageRequest pageRequest = PageRequest.of(page - 1, size);
            List<Account> list = accountRepository.findByOrderByTotalDesc(pageRequest);
            for (int i = 0; i < list.size(); i++) {
                Account account = list.get(i);
                AccountResponse response = AccountResponse.of(account, 2);
                response.setRank(i + 1);
                responses.add(response);
            }
        }
        return ApiResult.page(responses, page, size, total);
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public AccountResponse accountInfo(String address) {
        AccountResponse response = new AccountResponse();
        response.setAddress(address);
        AccountInfo accountInfo = apiClient.accountInfo(address, null);
        if (accountInfo != null) {
            Optional<Account> optional = accountRepository.findByAddress(address);
            if (optional.isPresent()) {
                response = AccountResponse.of(optional.get(), Constants.DECIMALS);
            }
            AccountInfo.General general = accountInfo.getGeneral();
            if (general != null) {
                response.setNonce(general.getNonce());
                Map<String, String> allowanceMap = general.getAllowances();
                if (!CollectionUtils.isEmpty(allowanceMap)) {
                    List<AccountResponse.Allowance> allowanceList = Lists.newArrayList();
                    for (Map.Entry<String, String> entry : allowanceMap.entrySet()) {
                        AccountResponse.Allowance allowance = new AccountResponse.Allowance();
                        allowance.setAddress(entry.getKey());
                        allowance.setAmount(Texts.formatDecimals(String.valueOf(entry.getValue()), Constants.DECIMALS, Constants.DECIMALS));
                        allowanceList.add(allowance);
                    }
                    response.setAllowances(allowanceList);
                }
            }
        }
        return response;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult debondings(String delegator, int page, int size) {
        List<AccountDebondingResponse> responses = Lists.newArrayList();
        long total = debondingRepository.countByDelegator(delegator);
        long currentEpoch = apiClient.epoch(null);
        if (total > 0) {
            PageRequest pageRequest = PageRequest.of(page - 1, size);
            List<Debonding> list = debondingRepository.findByDelegatorOrderByDebondEndAsc(delegator, pageRequest);
            if (!CollectionUtils.isEmpty(list)) {
                for (Debonding debonding : list) {
                    String validatorAddress = debonding.getValidator();
                    Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityAddress(validatorAddress);
                    if (optional.isPresent()) {
                        ValidatorInfo validatorInfo = optional.get();
                        AccountDebondingResponse response = new AccountDebondingResponse();
                        response.setValidatorAddress(validatorAddress);
                        response.setValidatorName(validatorInfo.getName());
                        response.setIcon(validatorInfo.getIcon());
                        response.setDebondEnd(debonding.getDebondEnd());
                        response.setEpochLeft(debonding.getDebondEnd() - currentEpoch);
                        response.setShares(Texts.formatDecimals(String.valueOf(debonding.getShares()), Constants.DECIMALS, 2));
                        responses.add(response);
                    }
                }
            }
        }
        return ApiResult.page(responses, page, size, total);
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult delegations(String delegator, boolean all, int page, int size) {
        List<AccountValidatorResponse> responses = Lists.newArrayList();
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<Delegator> delegatorPage;
        if (all) {
            delegatorPage = delegatorRepository.findByDelegator(delegator, pageRequest);
        } else {
            delegatorPage = delegatorRepository.findByDelegatorAll(delegator, pageRequest);
        }
        List<Delegator> list = delegatorPage.toList();
        if (!CollectionUtils.isEmpty(list)) {
            for (Delegator d : list) {
                String validatorAddress = d.getValidator();
                Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityAddress(validatorAddress);
                AccountValidatorResponse response = new AccountValidatorResponse();
                double shares = Double.parseDouble(Texts.formatDecimals(d.getShares(), Constants.DECIMALS, Constants.DECIMALS));
                if (optional.isPresent()) {
                    ValidatorInfo validatorInfo = optional.get();
                    response.setValidatorAddress(validatorAddress);
                    response.setValidatorName(validatorInfo.getName());
                    response.setIcon(validatorInfo.getIcon());
                    response.setActive(validatorInfo.getNodes() == 1);
                    //tokens = shares * balance / total_shares
                    double totalShares = Double.parseDouble(Texts.formatDecimals(validatorInfo.getTotalShares(), Constants.DECIMALS, Constants.DECIMALS));
                    double escrow = Double.parseDouble(Texts.formatDecimals(validatorInfo.getEscrow(), Constants.DECIMALS, Constants.DECIMALS));
                    double amount = Numeric.divide(Numeric.multiply(shares, escrow), totalShares, Constants.DECIMALS);
                    response.setShares(Numeric.formatDouble(shares, Constants.DECIMALS));
                    response.setAmount(Numeric.formatDouble(amount, Constants.DECIMALS));
                } else {
                    response.setShares(Numeric.formatDouble(shares, Constants.DECIMALS));
                    response.setAmount(Numeric.formatDouble(shares, Constants.DECIMALS));
                    response.setEntityAddress(validatorAddress);
                }
                responses.add(response);
            }
        }
        return ApiResult.page(responses, page, size, delegatorPage.getTotalElements());
    }
}
