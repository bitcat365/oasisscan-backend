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
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApplicationConfig;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Account;
import romever.scan.oasisscan.entity.SystemProperty;
import romever.scan.oasisscan.repository.AccountRepository;
import romever.scan.oasisscan.repository.SystemPropertyRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.StakingEvent;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeState;

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
    private AccountRepository accountRepository;
    @Autowired
    private ScanValidatorService scanValidatorService;

    @Data
    public static class DirtyList {
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

            for (String address : dl.getAccounts()) {
                updateAccountInfo(address);
            }

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
                dl.getAccounts().add(ev.getEscrow().getAdd().getOwner());
                dl.getAccounts().add(ev.getEscrow().getAdd().getEscrow());
            }
            if (ev.getEscrow().getTake() != null) {
                dl.getAccounts().add(ev.getEscrow().getTake().getOwner());
            }
            if (ev.getEscrow().getDebonding_start() != null) {
                dl.getAccounts().add(ev.getEscrow().getDebonding_start().getOwner());
                dl.getAccounts().add(ev.getEscrow().getDebonding_start().getEscrow());
            }
            if (ev.getEscrow().getReclaim() != null) {
                dl.getAccounts().add(ev.getEscrow().getReclaim().getOwner());
                dl.getAccounts().add(ev.getEscrow().getReclaim().getEscrow());
            }
        }
        if (ev.getAllowance_change() != null) {
            dl.getAccounts().add(ev.getAllowance_change().getOwner());
        }
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
