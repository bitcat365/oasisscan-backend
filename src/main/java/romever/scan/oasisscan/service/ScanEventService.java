package romever.scan.oasisscan.service;

import com.google.common.collect.Maps;
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
import romever.scan.oasisscan.entity.SystemProperty;
import romever.scan.oasisscan.repository.SystemPropertyRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.vo.chain.StakingEvent;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeState;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            for (int i = 0; i < stakingEvents.size(); i++) {
                StakingEvent event = stakingEvents.get(i);
                String esId = scanHeight + "_" + i;
                txMap.put(esId, Mappers.map(event));
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

            saveScanHeight(scanHeight);
            log.info(String.format("staking events: height: %s, count: %s", scanHeight, 0));
        }
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
