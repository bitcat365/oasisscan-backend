package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.ValidatorConsensus;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.ValidatorConsensusRepository;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.Block;
import romever.scan.oasisscan.vo.response.BlockDetailResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static romever.scan.oasisscan.common.ESFields.*;

@Slf4j
@Service
public class BlockService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private ValidatorConsensusRepository validatorConsensusRepository;
    @Autowired
    private BlockService blockService;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult latestBlocks(int size, int page) {
        long total = 0;
        List<BlockDetailResponse> responses = Lists.newArrayList();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort(BLOCK_HEIGHT, SortOrder.DESC);
        searchSourceBuilder.from(size * (page - 1));
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                Map<String, ValidatorInfo> tmAddressMap = blockService.tmAddressNodeMap();

                SearchHits hits = searchResponse.getHits();
                total = hits.getTotalHits().value;
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    Block block = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<Block>() {
                    });
                    if (block != null) {
                        responses.add(BlockDetailResponse.of(block, tmAddressMap));
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(responses, page, size, total);
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public BlockDetailResponse detail(Long height) {
        BlockDetailResponse response = null;
        if (height != null) {
            try {
                GetResponse getResponse = JestDao.get(elasticsearchClient, elasticsearchConfig.getBlockIndex(), String.valueOf(height));
                if (getResponse.isExists()) {
                    Block block = Mappers.parseJson(getResponse.getSourceAsString(), new TypeReference<Block>() {
                    });
                    if (block != null) {
                        Map<String, ValidatorInfo> tmAddressMap = blockService.tmAddressNodeMap();
                        response = BlockDetailResponse.of(block, tmAddressMap);
                        response.setEpoch(apiClient.epoch(height));
                    }
                }
            } catch (ElasticsearchException e) {
                if (e.status() == RestStatus.NOT_FOUND) {
                    log.warn("block {} not found.", height);
                }
            } catch (IOException e) {
                log.error("error", e);
            }
        }
        return response;
    }

    public BlockDetailResponse detail(String hash) {
        BlockDetailResponse response = null;
        if (Texts.isNotBlank(hash)) {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.filter(QueryBuilders.termQuery(BLOCK_HASH, hash));
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.size(1);
            try {
                SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
                if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                    SearchHits hits = searchResponse.getHits();
                    SearchHit[] searchHits = hits.getHits();
                    for (SearchHit hit : searchHits) {
                        Block block = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<Block>() {
                        });
                        if (block != null) {
                            Map<String, ValidatorInfo> tmAddressMap = blockService.tmAddressNodeMap();
                            response = BlockDetailResponse.of(block, tmAddressMap);
                            response.setEpoch(apiClient.epoch(block.getHeight()));
                        }
                    }
                }
            } catch (IOException e) {
                log.error("error", e);
            }
        }
        return response;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult proposerBlocks(String entityId, String address, int size, int page) {
        long total = 0;
        List<BlockDetailResponse> responses = Lists.newArrayList();
        if (Texts.isBlank(address)) {
            address = apiClient.pubkeyToBech32Address(entityId);
            if (Texts.isBlank(address)) {
                throw new RuntimeException(String.format("address parse failed, %s", entityId));
            }
        }

        Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityAddress(address);
        if (optional.isPresent()) {
            List<ValidatorConsensus> consensusList = validatorConsensusRepository.findByEntityId(optional.get().getEntityId());
            if (!CollectionUtils.isEmpty(consensusList)) {
                List<String> tmAddressList = Lists.newArrayList();
                for (ValidatorConsensus consensus : consensusList) {
                    String tmAddress = Texts.hexToBase64(apiClient.pubkeyToTendermintAddress(consensus.getConsensusId()));
                    tmAddressList.add(tmAddress);
                }

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                boolQueryBuilder.filter(QueryBuilders.termsQuery(BLOCK_PROPOSER_ADDRESS, tmAddressList));

                searchSourceBuilder.query(boolQueryBuilder);
                searchSourceBuilder.from(size * (page - 1));
                searchSourceBuilder.size(size);
                searchSourceBuilder.sort(BLOCK_HEIGHT, SortOrder.DESC);
                searchSourceBuilder.trackTotalHits(true);

                try {
                    SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
                    if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                        Map<String, ValidatorInfo> tmAddressMap = blockService.tmAddressNodeMap();

                        SearchHits hits = searchResponse.getHits();
                        total = hits.getTotalHits().value;
                        SearchHit[] searchHits = hits.getHits();
                        for (SearchHit hit : searchHits) {
                            Mappers.parseJson(hit.getSourceAsString(), Block.class).ifPresent(block -> responses.add(BlockDetailResponse.of(block, tmAddressMap)));
                        }
                    }
                } catch (IOException e) {
                    log.error("error", e);
                }
            }
        }
        return ApiResult.page(responses, page, size, total);
    }

    @Cached(expire = 60 * 2, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    @CacheRefresh(refresh = 30, timeUnit = TimeUnit.SECONDS)
    public Map<String, ValidatorInfo> tmAddressNodeMap() {
        Map<String, ValidatorInfo> map = Maps.newHashMap();
        List<ValidatorConsensus> validatorConsensuses = validatorConsensusRepository.findAll();
        if (!CollectionUtils.isEmpty(validatorConsensuses)) {
            for (ValidatorConsensus consensus : validatorConsensuses) {
                Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityId(consensus.getEntityId());
                optional.ifPresent(validatorInfo -> map.put(consensus.getTmAddress(), validatorInfo));
            }
        }
        return map;
    }

}
