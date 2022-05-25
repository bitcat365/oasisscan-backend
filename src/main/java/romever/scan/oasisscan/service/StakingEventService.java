package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
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
import org.springframework.stereotype.Service;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.vo.chain.StakingEvent;
import romever.scan.oasisscan.vo.response.BlockDetailResponse;
import romever.scan.oasisscan.vo.response.ListStakingEventResponse;

import java.io.IOException;
import java.util.List;

import static romever.scan.oasisscan.common.ESFields.*;

@Slf4j
@Service
public class StakingEventService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    @Autowired
    private BlockService blockService;

    public ApiResult stakingEvents(int size, int page, String address) {
        long total = 0;
        List<ListStakingEventResponse> responses = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.termQuery(STAKING_EVENT_TRANSFER_FROM, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_TRANSFER_TO, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_BURN_OWNER, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_ADD_OWNER, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_ADD_ESCROW, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_TAKE_OWNER, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_DEBONDING_START_OWNER, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_DEBONDING_START_ESCROW, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_RECLAIM_OWNER, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ESCROW_RECLAIM_ESCROW, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ALLOWANCE_CHANGE_OWNER, address))
                .should(QueryBuilders.termQuery(STAKING_EVENT_ALLOWANCE_CHANGE_BENEFICIARY, address))
        );
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort("height", SortOrder.DESC);
        searchSourceBuilder.from(size * (page - 1));
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getStakingEventIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                total = hits.getTotalHits().value;
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    StakingEvent se = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<StakingEvent>() {
                    });
                    ListStakingEventResponse response = new ListStakingEventResponse();
                    if (se == null) {
                        continue;
                    }
                    BeanUtils.copyProperties(se, response);
                    response.setId(hit.getId());
                    response.setType(getType(se));
                    responses.add(response);
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(responses, page, size, total);
    }

    public StakingEvent stakingEventInfo(String id) {
        StakingEvent stakingEvent = null;
        try {
            GetResponse getResponse = JestDao.get(elasticsearchClient, elasticsearchConfig.getStakingEventIndex(), id);
            if (getResponse.isExists()) {
                stakingEvent = Mappers.parseJson(getResponse.getSourceAsString(), new TypeReference<StakingEvent>() {
                });
                if (stakingEvent != null) {
                    stakingEvent.setType(getType(stakingEvent));

                    BlockDetailResponse block = blockService.detail(stakingEvent.getHeight());
                    stakingEvent.setTimestamp(block.getTimestamp());
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return stakingEvent;
    }

    private String getType(StakingEvent stakingEvent) {
        if (stakingEvent.getTransfer() != null) {
            return "transfer";
        } else if (stakingEvent.getEscrow() != null) {
            return "escrow";
        } else if (stakingEvent.getBurn() != null) {
            return "burn";
        } else if (stakingEvent.getAllowance_change() != null) {
            return "allowance change";
        }
        return "unknown";
    }

}
