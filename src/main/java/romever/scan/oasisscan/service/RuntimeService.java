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
import romever.scan.oasisscan.common.ESFields;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Runtime;
import romever.scan.oasisscan.entity.RuntimeStatsCount;
import romever.scan.oasisscan.entity.RuntimeStatsType;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.RuntimeRepository;
import romever.scan.oasisscan.repository.RuntimeStatsRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.vo.RuntimeHeaderTypeEnum;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.Block;
import romever.scan.oasisscan.vo.chain.RuntimeRound;
import romever.scan.oasisscan.vo.response.BlockDetailResponse;
import romever.scan.oasisscan.vo.response.RuntimeResponse;
import romever.scan.oasisscan.vo.response.RuntimeRoundResponse;
import romever.scan.oasisscan.vo.response.RuntimeStatsResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RuntimeService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;
    @Autowired
    private RuntimeRepository runtimeRepository;
    @Autowired
    private RuntimeStatsRepository runtimeStatsRepository;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult roundList(String runtimeId, int size, int page) {
        long total = 0;
        List<RuntimeRoundResponse> roundList = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(ESFields.RUNTIME_ROUND_NAMESPACE, runtimeId));
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort(ESFields.RUNTIME_ROUND_ROUND, SortOrder.DESC);
        searchSourceBuilder.from(size * (page - 1));
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeRoundIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                total = hits.getTotalHits().value;
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    RuntimeRound.Header round = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<RuntimeRound.Header>() {
                    });
                    if (round != null) {
                        roundList.add(RuntimeRoundResponse.of(round));
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(roundList, page, size, total);
    }

    public RuntimeRoundResponse roundInfo(String runtimeId, long round) {
        RuntimeRoundResponse response = null;
        String esId = runtimeId + "_" + round;
        try {
            GetResponse getResponse = JestDao.get(elasticsearchClient, elasticsearchConfig.getRuntimeRoundIndex(), String.valueOf(esId));
            if (getResponse.isExists()) {
                RuntimeRound.Header header = Mappers.parseJson(getResponse.getSourceAsString(), new TypeReference<RuntimeRound.Header>() {
                });
                if (header != null) {
                    response = RuntimeRoundResponse.of(header);
                }
            }
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.warn("runtime round {} not found.", esId);
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return response;
    }

    public List<RuntimeResponse> runtimeList() {
        List<RuntimeResponse> list = Lists.newArrayList();
        List<Runtime> runtimeList = runtimeRepository.findAll();
        if (!CollectionUtils.isEmpty(runtimeList)) {
            for (Runtime runtime : runtimeList) {
                RuntimeResponse response = new RuntimeResponse();
                response.setName(runtime.getName());
                response.setRuntimeId(runtime.getRuntimeId());
                list.add(response);
            }
        }
        return list;
    }

    @Cached(expire = 60, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    @CacheRefresh(refresh = 30, timeUnit = TimeUnit.SECONDS)
    public List<RuntimeStatsResponse> runtimeStats(String runtimeId) {
        List<RuntimeStatsResponse> responses = Lists.newArrayList();
        List<String> entities = runtimeStatsRepository.entities(runtimeId);
        if (CollectionUtils.isEmpty(entities)) {
            return responses;
        }
        RuntimeStatsType[] types = RuntimeStatsType.class.getEnumConstants();
        for (String entity : entities) {
            List statsList = runtimeStatsRepository.statsByRuntimeId(runtimeId, entity);
            if (CollectionUtils.isEmpty(statsList)) {
                continue;
            }
            RuntimeStatsResponse response = new RuntimeStatsResponse();
            response.setEntityId(entity);
            Map<String, Long> statsMap = Maps.newLinkedHashMap();
            for (RuntimeStatsType type : types) {
                statsMap.put(type.name().toLowerCase(), 0L);
            }
            for (Object row : statsList) {
                Object[] cells = (Object[]) row;
                for (int i = 0; i < types.length; i++) {
                    RuntimeStatsType type = types[i];
                    if (i == (Integer) cells[0]) {
                        long count = ((BigInteger) cells[1]).longValue();
                        statsMap.put(type.name().toLowerCase(), count);
                        break;
                    }
                }
            }
            response.setStats(statsMap);
            responses.add(response);
        }
        responses.sort((r1, r2) -> {
            Map<String, Long> map1 = r1.getStats();
            Map<String, Long> map2 = r2.getStats();
            long count1 = map1.get(types[0].name().toLowerCase());
            long count2 = map2.get(types[0].name().toLowerCase());
            if (count1 == count2) {
                return r1.getEntityId().compareTo(r2.getEntityId());
            }
            return (int) (map2.get(types[0].name().toLowerCase()) - map1.get(types[0].name().toLowerCase()));
        });
        return responses;
    }

}
