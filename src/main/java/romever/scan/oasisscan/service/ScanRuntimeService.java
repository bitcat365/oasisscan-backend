package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApplicationConfig;
import romever.scan.oasisscan.common.ESFields;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.repository.RuntimeRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.vo.chain.*;
import romever.scan.oasisscan.vo.chain.Runtime;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ScanRuntimeService {

    @Autowired
    private ApplicationConfig applicationConfig;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;
    @Autowired
    private RuntimeRepository runtimeRepository;

    /**
     * Get all runtimes info and save in elasticsearch
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 5 * 1000)
    public void scanRuntime() {
        if (applicationConfig.isLocal()) {
            return;
        }

        List<Runtime> runtimes = apiClient.runtimes(null);
        if (CollectionUtils.isEmpty(runtimes)) {
            return;
        }

        for (Runtime runtime : runtimes) {
            String runtimeId = runtime.getId();
            Optional<romever.scan.oasisscan.entity.Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
            romever.scan.oasisscan.entity.Runtime runtimeEntity;
            runtimeEntity = optionalRuntime.orElseGet(romever.scan.oasisscan.entity.Runtime::new);
            runtimeEntity.setEntityId(runtime.getEntity_id());
            runtimeEntity.setRuntimeId(runtimeId);
            runtimeRepository.save(runtimeEntity);
        }
        log.info("Runtime info sync done.");
    }

    /**
     * Scan runtimes round and save in elasticsearch
     */
    @Scheduled(fixedDelay = 30 * 1000, initialDelay = 10 * 1000)
//    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void scanRuntimeRound() throws IOException {
        if (applicationConfig.isLocal()) {
            return;
        }

        List<romever.scan.oasisscan.entity.Runtime> runtimes = runtimeRepository.findAll();
        if (CollectionUtils.isEmpty(runtimes)) {
            return;
        }

        long currentChainHeight = apiClient.getCurHeight();
        for (romever.scan.oasisscan.entity.Runtime runtime : runtimes) {
            String runtimeId = runtime.getRuntimeId();
            long scanHeight = runtime.getScanRoundHeight();
            if (scanHeight == 0) {
                RuntimeState runtimeState = apiClient.roothashRuntimeState(runtimeId, null);
                long genesisTime = runtimeState.getGenesis_block().getHeader().getTimestamp();
                Long genesisHeight = getGenesisRoundHeight(genesisTime);
                if (genesisHeight == null) {
                    throw new RuntimeException(String.format("Genesis Height can not found. %s", runtimeId));
                }
                scanHeight = genesisHeight;
            }
            while (scanHeight < currentChainHeight) {
                RuntimeRound runtimeRound = apiClient.roothashLatestblock(runtimeId, scanHeight);
                if (runtimeRound == null) {
                    throw new RuntimeException(String.format("Runtime round api error. %s", runtimeId));
                }
                RuntimeRound.Header header = runtimeRound.getHeader();
                String id = header.getNamespace() + "_" + header.getRound();
                JestDao.index(elasticsearchClient, elasticsearchConfig.getRuntimeRoundIndex(), Mappers.map(header), id);
                log.info("Runtime round sync done. {} [{}]", scanHeight, id);
                scanHeight++;
                Optional<romever.scan.oasisscan.entity.Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
                if (!optionalRuntime.isPresent()) {
                    throw new RuntimeException("Runtime db read error.");
                }
                romever.scan.oasisscan.entity.Runtime _runtime = optionalRuntime.get();
                _runtime.setScanRoundHeight(scanHeight);
                runtimeRepository.saveAndFlush(_runtime);
            }
        }
    }

    private Long getGenesisRoundHeight(long timestamp) {
        Long height = null;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.rangeQuery(ESFields.BLOCK_TIMESTAMP).from(timestamp, true));

        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort(ESFields.BLOCK_TIMESTAMP, SortOrder.ASC);
        searchSourceBuilder.size(1);
        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() != searchResponse.getSuccessfulShards()) {
                return height;
            }
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            if (searchHits.length != 1) {
                return height;
            }
            SearchHit hit = searchHits[0];
            Block block = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<Block>() {
            });
            if (block == null) {
                return height;
            }
            height = block.getHeight();
        } catch (Exception e) {
            log.error("error", e);
        }
        return height;
    }
}
