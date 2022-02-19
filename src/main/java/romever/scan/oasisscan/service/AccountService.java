package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Account;
import romever.scan.oasisscan.entity.Debonding;
import romever.scan.oasisscan.entity.Delegator;
import romever.scan.oasisscan.entity.Runtime;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.AccountRepository;
import romever.scan.oasisscan.repository.DebondingRepository;
import romever.scan.oasisscan.repository.DelegatorRepository;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.MemoryPageUtil;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.RuntimeTransactionType;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.Delegations;
import romever.scan.oasisscan.vo.chain.runtime.AbstractRuntimeTransaction;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeTransaction;
import romever.scan.oasisscan.vo.chain.runtime.emerald.EmeraldTransaction;
import romever.scan.oasisscan.vo.response.AccountDebondingResponse;
import romever.scan.oasisscan.vo.response.AccountResponse;
import romever.scan.oasisscan.vo.response.AccountValidatorResponse;
import romever.scan.oasisscan.vo.response.runtime.ListRuntimeTransactionResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static romever.scan.oasisscan.common.ESFields.*;
import static romever.scan.oasisscan.common.ESFields.TRANSACTION_TRANSFER_TO;

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
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private ScanValidatorService scanValidatorService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

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
        try {
            AccountInfo accountInfo = apiClient.accountInfo(address, null);
            if (accountInfo != null) {
//            Optional<Account> optional = accountRepository.findByAddress(address);
//            if (optional.isPresent()) {
//                response = AccountResponse.of(optional.get(), Constants.DECIMALS);
//            }

                //escrow
                double totalEscrow = 0;
                Map<String, Delegations> delegationsMap = apiClient.delegations(address, null);
                if (!CollectionUtils.isEmpty(delegationsMap)) {
                    for (Map.Entry<String, Delegations> entry : delegationsMap.entrySet()) {
                        double shares = Double.parseDouble(Texts.formatDecimals(entry.getValue().getShares(), Constants.DECIMALS, 9));

                        String validator = entry.getKey();
                        AccountInfo validatorInfo = apiClient.accountInfo(validator, null);
                        //tokens = shares * balance / total_shares
                        double totalShares = Double.parseDouble(Texts.formatDecimals(validatorInfo.getEscrow().getActive().getTotal_shares(), Constants.DECIMALS, 9));
                        double escrow = Double.parseDouble(Texts.formatDecimals(validatorInfo.getEscrow().getActive().getBalance(), Constants.DECIMALS, 9));
                        double amount = Numeric.divide(Numeric.multiply(shares, escrow), totalShares, 9);
                        totalEscrow = Numeric.add(amount, totalEscrow);
                    }
                }
                long totalEscrowLong = (long) (totalEscrow * Math.pow(10, 9));

                //debonding
                long totalDebonding = 0;
                Map<String, romever.scan.oasisscan.vo.chain.Debonding> debondingMap = apiClient.debondingdelegations(address, null);
                if (!CollectionUtils.isEmpty(debondingMap)) {
                    for (Map.Entry<String, romever.scan.oasisscan.vo.chain.Debonding> entry : debondingMap.entrySet()) {
                        totalDebonding += Long.parseLong(entry.getValue().getShares());
                    }
                }

                long available = Long.parseLong(accountInfo.getGeneral().getBalance());
                response = AccountResponse.of(address, available, totalEscrowLong, totalDebonding, Constants.DECIMALS);


//                Account accountDb = scanValidatorService.getAccount(address, accountInfo);
//                if (accountDb != null) {
//                    response = AccountResponse.of(accountDb, Constants.DECIMALS);
//                }

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
        } catch (Exception e) {
            log.error("", e);
        }
        return response;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult debondings(String delegator, int page, int size) {
        List<AccountDebondingResponse> responses = Lists.newArrayList();
        long total = 0;
        try {
            Map<String, romever.scan.oasisscan.vo.chain.Debonding> debondingMap = apiClient.debondingdelegations(delegator, null);
            if (!CollectionUtils.isEmpty(debondingMap)) {
                total = debondingMap.size();
                long currentEpoch = apiClient.epoch(null);
                if (total > 0) {
                    List<Debonding> list = Lists.newArrayList();
                    for (Map.Entry<String, romever.scan.oasisscan.vo.chain.Debonding> entry : debondingMap.entrySet()) {
                        Debonding debonding = new Debonding();
                        debonding.setDelegator(delegator);
                        debonding.setValidator(entry.getKey());
                        debonding.setShares(entry.getValue().getShares());
                        debonding.setDebondEnd(entry.getValue().getDebond_end());
                        list.add(debonding);
                    }
                    list = MemoryPageUtil.pageLimit(list, page, size);

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
            }
        } catch (Exception e) {
            log.error("", e);
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

    public ApiResult runtimeTransactions(String address, String runtimeId, int page, int size) {
        long total = 0;
        List<ListRuntimeTransactionResponse> responses = Lists.newArrayList();

        if (Texts.isBlank(address)) {
            return ApiResult.page(responses, page, size, total);
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(
                QueryBuilders.boolQuery()
                        .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_SIG_ADDRESS, address))
                        .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_TO, address))
                        .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_EVENT_FROM, address))
                        .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_EVENT_TO, address))
        );
        if (Texts.isNotBlank(runtimeId)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_ID, runtimeId));
        }

        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort(RUNTIME_TRANSACTION_ROUND, SortOrder.DESC);
        searchSourceBuilder.from(size * (page - 1));
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                total = hits.getTotalHits().value;
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    JsonNode jsonHit = Mappers.parseJson(hit.getSourceAsString());
                    String type = jsonHit.path("type").asText();
                    AbstractRuntimeTransaction tx;
                    if (type.equalsIgnoreCase("evm")) {
                        tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<EmeraldTransaction>() {
                        });
                    } else {
                        tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<RuntimeTransaction>() {
                        });
                    }

                    if (tx != null) {
                        ListRuntimeTransactionResponse response = new ListRuntimeTransactionResponse();
                        BeanUtils.copyProperties(tx, response);
                        response.setRuntimeId(tx.getRuntime_id());
                        response.setTxHash(tx.getTx_hash());
                        response.setType(RuntimeTransactionType.getDisplayNameByType(response.getType()));
                        Runtime runtimeInfo = runtimeService.getRuntimeInfo(response.getRuntimeId());
                        if (runtimeInfo != null) {
                            response.setRuntimeName(runtimeInfo.getName());
                        }
                        responses.add(response);
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(responses, page, size, total);
    }
}
