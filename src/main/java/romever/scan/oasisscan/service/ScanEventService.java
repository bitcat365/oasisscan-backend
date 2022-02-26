package romever.scan.oasisscan.service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApplicationConfig;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Account;
import romever.scan.oasisscan.entity.Debonding;
import romever.scan.oasisscan.entity.Delegator;
import romever.scan.oasisscan.entity.SystemProperty;
import romever.scan.oasisscan.repository.AccountRepository;
import romever.scan.oasisscan.repository.DebondingRepository;
import romever.scan.oasisscan.repository.DelegatorRepository;
import romever.scan.oasisscan.repository.SystemPropertyRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.Delegations;
import romever.scan.oasisscan.vo.chain.StakingEvent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class ScanEventService {

    @Autowired
    private ApplicationConfig applicationConfig;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    @Autowired
    private SystemPropertyRepository systemPropertyRepository;
    @Autowired
    private DelegatorRepository delegatorRepository;
    @Autowired
    private DebondingRepository debondingRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ScanValidatorService scanValidatorService;

    @Data
    public static class DirtyList {
        /** Escrow addresses where delegations to that account have changed */
        private Set<String> delegations = Sets.newHashSet();
        /** Delegator addresses where debonding delegations made by that account have changed */
        private Set<String> debondingDelegations = Sets.newHashSet();
        /** Escrow addresses where the active and/or debonding pool token prices have changed */
        private Set<String> escrowPools = Sets.newHashSet();
        /** Addresses of accounts that have changed */
        private Set<String> accounts = Sets.newHashSet();
    }

    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 15 * 1000)
    public void scanEvent() throws Exception {
        if (applicationConfig.isLocal()) {
            return;
        }

        Long curHeight = apiClient.getCurHeight();
        Long scanHeight = getScanHeight();
        if (scanHeight == null) {
            return;
        }
        if (scanHeight != 0) {
            scanHeight++;
        }

        for (; scanHeight <= curHeight; scanHeight++) {
            List<StakingEvent> stakingEvents = apiClient.stakingEvents(scanHeight);
            if (CollectionUtils.isEmpty(stakingEvents)) {
                saveScanHeight(scanHeight);
                log.info(String.format("staking events: height: %s, count: %s", scanHeight, 0));
                continue;
            }

            Map<String, Map<String, Object>> txMap = Maps.newHashMap();
            DirtyList dl = new DirtyList();
            for (int i = 0; i < stakingEvents.size(); i++) {
                StakingEvent event = stakingEvents.get(i);
                String esId = scanHeight + "_" + i;
                txMap.put(esId, Mappers.map(event));

                //update account info
                dirty(dl, event);
            }

            if (!CollectionUtils.isEmpty(txMap)) {
                BulkResponse bulkResponse = JestDao.indexBulk(elasticsearchClient, elasticsearchConfig.getStakingEventIndex(), txMap);
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        log.error(failure.getMessage());
                        throw failure.getCause();
                    }
                }
            }

            updateDirty(dl);

            saveScanHeight(scanHeight);
            log.info(String.format("staking events: height: %s, count: %s", scanHeight, 0));
        }
    }

    private void dirty(DirtyList dl, StakingEvent ev) {
        if (ev.getTransfer() != null) {
            dl.getAccounts().add(ev.getTransfer().getFrom());
            dl.getAccounts().add(ev.getTransfer().getTo());
        }
        if (ev.getBurn() != null) {
            dl.getAccounts().add(ev.getBurn().getOwner());
        }
        if (ev.getEscrow() != null) {
            if (ev.getEscrow().getAdd() != null) {
                dl.getDelegations().add(ev.getEscrow().getAdd().getEscrow());
                dl.getAccounts().add(ev.getEscrow().getAdd().getOwner());
                dl.getAccounts().add(ev.getEscrow().getAdd().getEscrow());
                if ("0".equals(ev.getEscrow().getAdd().getNew_shares())) {
                    // https://github.com/oasisprotocol/oasis-core/blob/v21.3.10/go/consensus/tendermint/apps/staking/state/state.go#L850
                    // Rewards are entered as add escrow events where no new shares are created.
                    dl.getEscrowPools().add(ev.getEscrow().getAdd().getEscrow());
                }
            }
            if (ev.getEscrow().getTake() != null) {
                // Note: Shares don't change in take escrow events.
                dl.getEscrowPools().add(ev.getEscrow().getTake().getOwner());
                dl.getAccounts().add(ev.getEscrow().getTake().getOwner());
            }
            if (ev.getEscrow().getDebonding_start() != null) {
                dl.getDelegations().add(ev.getEscrow().getDebonding_start().getEscrow());
                dl.getDebondingDelegations().add(ev.getEscrow().getDebonding_start().getOwner());
                dl.getAccounts().add(ev.getEscrow().getDebonding_start().getOwner());
                dl.getAccounts().add(ev.getEscrow().getDebonding_start().getEscrow());
            }
            if (ev.getEscrow().getReclaim() != null) {
                dl.getDelegations().add(ev.getEscrow().getDebonding_start().getEscrow());
                dl.getAccounts().add(ev.getEscrow().getReclaim().getOwner());
                dl.getAccounts().add(ev.getEscrow().getReclaim().getEscrow());
            }
        }
        if (ev.getAllowance_change() != null) {
            dl.getAccounts().add(ev.getAllowance_change().getOwner());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateDirty(DirtyList dl) throws IOException {
        for (String address : dl.getDelegations()) {
            updateDelegatorInfo(address);
        }
        for (String address : dl.getDebondingDelegations()) {
            updateDebondingInfo(address);
        }
        for (String address : dl.getEscrowPools()) {
            updatePoolInfo(address);
        }
        for (String address : dl.getAccounts()) {
            updateAccountInfo(address);
        }
    }

    private void updateDelegatorInfo(String validatorAccount) throws IOException {
        Map<String, Delegations> delegationInfo = apiClient.delegationsTo(validatorAccount, null);
        List<Delegator> saveList = scanValidatorService.getDelegatorsForValidator(validatorAccount, delegationInfo);
        delegatorRepository.deleteByValidator(validatorAccount);
        delegatorRepository.saveAll(saveList);
    }

    private void updateDebondingInfo(String delegatorAccount) throws IOException {
        Map<String, List<romever.scan.oasisscan.vo.chain.Debonding>> debondingDelegationInfo = apiClient.debondingdelegations(delegatorAccount, null);
        List<Debonding> saveList = scanValidatorService.getDebondingsForDelegator(delegatorAccount, debondingDelegationInfo);
        debondingRepository.deleteByDelegator(delegatorAccount);
        debondingRepository.saveAll(saveList);
    }

    private void updatePoolInfo(String address) throws IOException {
        AccountInfo accountInfo = apiClient.accountInfo(address, null);
        scanValidatorService.updateValidatorPools(address, accountInfo);
    }

    private void updateAccountInfo(String address) throws IOException {
        AccountInfo accountInfo = apiClient.accountInfo(address, null);
        Account account = scanValidatorService.getAccount(address, accountInfo);
        accountRepository.save(account);
    }

    private Long getScanHeight() throws IOException {
        Long storeHeight = 0L;
        String property = Constants.SCAN_STAKING_EVENT_HEIGHT_PROPERTY;
        Optional<SystemProperty> optionalSystemProperty = systemPropertyRepository.findByProperty(property);
        if (optionalSystemProperty.isPresent()) {
            storeHeight = Long.parseLong(optionalSystemProperty.get().getValue());
        }
        return storeHeight;
    }

    private void saveScanHeight(long height) {
        String property = Constants.SCAN_STAKING_EVENT_HEIGHT_PROPERTY;
        SystemProperty systemProperty = systemPropertyRepository.findByProperty(property).orElse(new SystemProperty());
        systemProperty.setProperty(property);
        systemProperty.setValue(String.valueOf(height));
        systemPropertyRepository.saveAndFlush(systemProperty);
    }
}
