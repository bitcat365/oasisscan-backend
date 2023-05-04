package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ESFields;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Runtime;
import romever.scan.oasisscan.entity.RuntimeStatsInfo;
import romever.scan.oasisscan.entity.RuntimeStatsType;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.RuntimeNodeRepository;
import romever.scan.oasisscan.repository.RuntimeRepository;
import romever.scan.oasisscan.repository.RuntimeStatsInfoRepository;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.MethodEnum;
import romever.scan.oasisscan.vo.RuntimeTransactionType;
import romever.scan.oasisscan.vo.chain.Node;
import romever.scan.oasisscan.vo.chain.runtime.*;
import romever.scan.oasisscan.vo.chain.runtime.emerald.EmeraldTransaction;
import romever.scan.oasisscan.vo.response.ListRuntimeStatsResponse;
import romever.scan.oasisscan.vo.response.RuntimeResponse;
import romever.scan.oasisscan.vo.response.RuntimeRoundResponse;
import romever.scan.oasisscan.vo.response.RuntimeStatsResponse;
import romever.scan.oasisscan.vo.response.runtime.ListRuntimeTransactionResponse;
import romever.scan.oasisscan.vo.response.runtime.RuntimeTransactionResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static romever.scan.oasisscan.common.ESFields.*;

@Slf4j
@Service
public class RuntimeService {

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;
    @Autowired
    private RuntimeRepository runtimeRepository;
    @Autowired
    private RuntimeStatsInfoRepository runtimeStatsInfoRepository;
    @Autowired
    private RuntimeNodeRepository runtimeNodeRepository;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private RuntimeService runtimeService;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult roundList(String runtimeId, int size, int page) {
        //get runtime info
        String runtimeName = "unknown";
        Runtime runtimeInfo = runtimeService.getRuntimeInfo(runtimeId);
        if (runtimeInfo != null) {
            runtimeName = runtimeInfo.getName();
        }

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
                        RuntimeRoundResponse response = RuntimeRoundResponse.of(round);
                        response.setRuntimeName(runtimeName);
                        roundList.add(response);
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(roundList, page, size, total);
    }

    public RuntimeRoundResponse roundInfo(String runtimeId, long round) {//get runtime info
        String runtimeName = "unknown";
        Runtime runtimeInfo = runtimeService.getRuntimeInfo(runtimeId);
        if (runtimeInfo != null) {
            runtimeName = runtimeInfo.getName();
        }

        RuntimeRoundResponse response = null;
        String esId = runtimeId + "_" + round;
        try {
            GetResponse getResponse = JestDao.get(elasticsearchClient, elasticsearchConfig.getRuntimeRoundIndex(), String.valueOf(esId));
            if (getResponse.isExists()) {
                RuntimeRound.Header header = Mappers.parseJson(getResponse.getSourceAsString(), new TypeReference<RuntimeRound.Header>() {
                });
                if (header != null) {
                    response = RuntimeRoundResponse.of(header);
                    long latestRound = latestRound(runtimeId);
                    response.setNext(round < latestRound);
                    response.setRuntimeName(runtimeName);
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

    private long latestRound(String runtimeId) {
        long roundHeight = 0;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(ESFields.RUNTIME_ROUND_NAMESPACE, runtimeId));
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort(ESFields.RUNTIME_ROUND_ROUND, SortOrder.DESC);
        searchSourceBuilder.size(1);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeRoundIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    RuntimeRound.Header round = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<RuntimeRound.Header>() {
                    });
                    if (round != null) {
                        roundHeight = round.getRound();
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return roundHeight;
    }

    @CacheRefresh(refresh = 60, timeUnit = TimeUnit.SECONDS)
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

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ListRuntimeStatsResponse runtimeStats(String runtimeId, int sort) {
        ListRuntimeStatsResponse listResponse = new ListRuntimeStatsResponse();
        List<RuntimeStatsResponse> responses = Lists.newArrayList();
        listResponse.setList(responses);
        try {
            List<String> entities = runtimeNodeRepository.entities(runtimeId);
            if (CollectionUtils.isEmpty(entities)) {
                return listResponse;
            }

            List<Node> nodes = apiClient.registryNodes(null);
            if (CollectionUtils.isEmpty(nodes)) {
                return listResponse;
            }
            Set<String> onlineNodeSet = Sets.newHashSet();
            for (Node node : nodes) {
                List<Node.Runtime> runtimeList = node.getRuntimes();
                if (CollectionUtils.isEmpty(runtimeList)) {
                    continue;
                }
                for (Node.Runtime r : runtimeList) {
                    if (r.getId().equalsIgnoreCase(runtimeId)) {
                        onlineNodeSet.add(node.getEntity_id());
                        break;
                    }
                }
            }

            RuntimeStatsType[] types = RuntimeStatsType.class.getEnumConstants();
            int online = 0;
            for (String entity : entities) {
                RuntimeStatsResponse response = new RuntimeStatsResponse();
                response.setEntityId(entity);
                //info
                Optional<ValidatorInfo> optionalValidatorInfo = validatorInfoRepository.findByEntityId(entity);
                String address = null;
                boolean validator = false;
                if (optionalValidatorInfo.isPresent()) {
                    ValidatorInfo info = optionalValidatorInfo.get();
                    response.setName(info.getName());
                    response.setIcon(info.getIcon());
                    address = info.getEntityAddress();
                    validator = true;
                }

                if (Texts.isBlank(address)) {
                    address = apiClient.pubkeyToBech32Address(entity);
                }
                response.setAddress(address);
                response.setValidator(validator);
                if (onlineNodeSet.contains(entity)) {
                    online++;
                    response.setStatus(onlineNodeSet.contains(entity));
                }

                //stats
                List<RuntimeStatsInfo> statsInfoList = runtimeStatsInfoRepository.findByRuntimeIdAndEntityId(runtimeId, entity);
                Map<String, Long> statsMap = Maps.newLinkedHashMap();
                for (RuntimeStatsType type : types) {
                    statsMap.put(type.name().toLowerCase(), 0L);
                }
                if (!CollectionUtils.isEmpty(statsInfoList)) {
                    for (RuntimeStatsInfo info : statsInfoList) {
                        for (RuntimeStatsType type : types) {
                            if (type == info.getStatsType()) {
                                statsMap.put(type.name().toLowerCase(), info.getCount());
                                break;
                            }
                        }
                    }
                }
                response.setStats(statsMap);
                responses.add(response);
            }
            responses.sort((r1, r2) -> {
                Map<String, Long> map1 = r1.getStats();
                Map<String, Long> map2 = r2.getStats();
                long count1 = map1.get(types[sort].name().toLowerCase());
                long count2 = map2.get(types[sort].name().toLowerCase());
                if (count1 == count2) {
                    return r1.getEntityId().compareTo(r2.getEntityId());
                }
                return (int) (map2.get(types[sort].name().toLowerCase()) - map1.get(types[sort].name().toLowerCase()));
            });
            listResponse.setList(responses);
            listResponse.setOnline(online);
            listResponse.setOffline(responses.size() - online);
        } catch (Exception e) {
            log.error("", e);
        }
        return listResponse;
    }

    /**
     * runtime transaction list
     *
     * @param size
     * @param page
     * @param runtimeId
     * @param round
     * @return
     */
    public ApiResult runtimeTransactions(int size, int page, String runtimeId, Long round) {
        long total = 0;
        List<ListRuntimeTransactionResponse> responses = Lists.newArrayList();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_ID, runtimeId));
        if (round != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_ROUND, round));
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
                        responses.add(response);
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(responses, page, size, total);
    }

    /**
     * transaction detail
     *
     * @param runtimeId
     * @param txHash
     * @return
     */
    public RuntimeTransactionResponse transactionInfo(String runtimeId, String txHash, Long round) {
        String runtimeName = "unknown";
        Runtime runtimeInfo = runtimeService.getRuntimeInfo(runtimeId);
        if (runtimeInfo != null) {
            runtimeName = runtimeInfo.getName();
        }

        RuntimeTransactionResponse response = null;
        String esId = runtimeId + "_" + txHash;
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_TX_HASH, txHash));
            if (round != null) {
                boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_ROUND, round));
            }
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.sort(RUNTIME_TRANSACTION_RESULT, SortOrder.DESC);
            searchSourceBuilder.sort(RUNTIME_TRANSACTION_ROUND, SortOrder.DESC);
            searchSourceBuilder.size(1);
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                SearchHit[] searchHits = hits.getHits();
                if (searchHits.length != 1) {
                    return response;
                }
                SearchHit hit = searchHits[0];
                JsonNode jsonHit = Mappers.parseJson(hit.getSourceAsString());
                String type = jsonHit.path("type").asText();
                if (type.equalsIgnoreCase("evm")) {
                    EmeraldTransaction tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<EmeraldTransaction>() {
                    });
                    if (tx != null) {
                        response = new RuntimeTransactionResponse();
                        BeanUtils.copyProperties(tx, response);
                        response.setRuntimeId(tx.getRuntime_id());
                        response.setTxHash(tx.getTx_hash());
                        RuntimeTransactionResponse.Ethereum etx = new RuntimeTransactionResponse.Ethereum();
                        BeanUtils.copyProperties(tx, etx);
                        response.setEtx(etx);
                    }
                } else {
                    RuntimeTransaction tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<RuntimeTransaction>() {
                    });
                    if (tx != null) {
                        response = new RuntimeTransactionResponse();
                        BeanUtils.copyProperties(tx, response);
                        response.setRuntimeId(tx.getRuntime_id());
                        response.setTxHash(tx.getTx_hash());
                        List<RuntimeTransaction.Si> sis = tx.getAi().getSi();
                        if (!CollectionUtils.isEmpty(sis)) {
                            RuntimeTransactionResponse.Consensus ctx = new RuntimeTransactionResponse.Consensus();
                            ctx.setNonce(sis.get(0).getNonce());
                            ctx.setFrom(sis.get(0).getAddress_spec().getSignature().getAddress());
                            ctx.setTo(tx.getCall().getBody().getTo());
                            ctx.setMethod(tx.getCall().getMethod());
                            List<String> amounts = tx.getCall().getBody().getAmount();
                            String amount = null;
                            if (!CollectionUtils.isEmpty(amounts)) {
                                amount = amounts.get(0);
                                if (Texts.isNotBlank(amount)) {
                                    ctx.setAmount(Texts.formatDecimals(String.valueOf(Texts.numberFromBase64(amount)), Constants.EMERALD_DECIMALS, Constants.EMERALD_DECIMALS));
                                }
                            }
                            response.setCtx(ctx);

                            //find events
                            String from = null;
                            String to = null;
                            if (ctx.getMethod().equalsIgnoreCase(MethodEnum.ConsensusWithdraw.getName())) {
                                List<AbstractRuntimeTransaction.Event> events = tx.getEvents();
                                if (!CollectionUtils.isEmpty(events)) {
                                    AbstractRuntimeTransaction.Event event = events.get(0);
                                    List<EventLog> logs = event.getLogs();
                                    if (!CollectionUtils.isEmpty(logs)) {
                                        EventLog eventLog = logs.get(0);
                                        from = eventLog.getFrom();
                                    }
                                }
                            } else {
                                from = ctx.getFrom();
                            }

                            if (Texts.isNotBlank(from)) {
                                RuntimeEventES eventES = findEvents(from, ctx.getNonce());
                                if (eventES != null) {
                                    response.setEvents(eventES);
                                }
                            }

//                            List<AbstractRuntimeTransaction.Event> events = tx.getEvents();
//                            if (!CollectionUtils.isEmpty(events)) {
//                                for (AbstractRuntimeTransaction.Event event : events) {
//                                    List<EventLog> logs = event.getLogs();
//                                    if (CollectionUtils.isEmpty(logs)) {
//                                        continue;
//                                    }
//                                    for (EventLog eventLog : logs) {
//                                        List<String> hexAmounts = eventLog.getAmount();
//                                        List<String> numberAmounts = Lists.newArrayList();
//                                        if (!CollectionUtils.isEmpty(hexAmounts)) {
//                                            String amount = hexAmounts.get(0);
//                                            if (Texts.isNotBlank(amount)) {
//                                                numberAmounts.add(Texts.formatDecimals(String.valueOf(Texts.numberFromBase64(amount)), Constants.EMERALD_DECIMALS, Constants.EMERALD_DECIMALS));
//                                            }
//                                        }
//                                        eventLog.setAmount(numberAmounts);
//                                    }
//                                }
//                                response.setEvents(tx.getEvents());
//                            }
                        }
                    }
                }
                if (response != null) {
                    response.setType(RuntimeTransactionType.getDisplayNameByType(response.getType()));
                    response.setRuntimeName(runtimeName);
                }
            }
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.warn("transaction {} not found.", esId);
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return response;
    }

    public RuntimeEventES findEvents(String from, long nonce) {
        RuntimeEventES eventES = null;
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_EVENT_FROM, from));
            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_EVENT_NONCE, nonce));
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.size(1);
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeEventIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    eventES = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<RuntimeEventES>() {
                    });
                    if (eventES != null) {
                        String typeHex = eventES.getType();
                        if (typeHex.contains(Constants.RUNTIME_TX_DEPOSIT_HEX)) {
                            eventES.setType("deposit");
                        } else if (typeHex.contains(Constants.RUNTIME_TX_WITHDRAW_HEX)) {
                            eventES.setType("withdraw");
                        } else {
                            return null;
                        }

                        List<String> hexAmounts = eventES.getAmount();
                        List<String> numberAmounts = Lists.newArrayList();
                        if (!CollectionUtils.isEmpty(hexAmounts)) {
                            String a = hexAmounts.get(0);
                            if (Texts.isNotBlank(a)) {
                                numberAmounts.add(Texts.formatDecimals(String.valueOf(Texts.numberFromBase64(a)), Constants.EMERALD_DECIMALS, Constants.EMERALD_DECIMALS));
                            }
                        }
                        eventES.setAmount(numberAmounts);
                    }
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        }
        return eventES;
    }

//    public RuntimeEventES findEvents(String from, String to, long nonce, String amount) {
//        RuntimeEventES eventES = null;
//        try {
//            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
//            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_EVENT_FROM, from));
//            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_EVENT_TO, to));
//            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_EVENT_NONCE, nonce));
//            if (Texts.isNotBlank(amount)) {
//                boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_EVENT_AMOUNT, amount));
//            }
//            searchSourceBuilder.query(boolQueryBuilder);
//            searchSourceBuilder.size(1);
//            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeEventIndex(), searchSourceBuilder);
//            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
//                SearchHits hits = searchResponse.getHits();
//                SearchHit[] searchHits = hits.getHits();
//                for (SearchHit hit : searchHits) {
//                    eventES = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<RuntimeEventES>() {
//                    });
//                    if (eventES != null) {
//                        List<EventLog> logs = eventES.getLogs();
//                        if (CollectionUtils.isEmpty(logs)) {
//                            return null;
//                        }
//                        String typeHex = eventES.getType();
//                        if (typeHex.contains(Constants.RUNTIME_TX_DEPOSIT_HEX)) {
//                            eventES.setType("deposit");
//                        } else if (typeHex.contains(Constants.RUNTIME_TX_WITHDRAW_HEX)) {
//                            eventES.setType("withdraw");
//                        } else {
//                            return null;
//                        }
//                        List<EventLog> el = Lists.newArrayList();
//                        for (EventLog eventLog : logs) {
//                            if (!eventLog.getFrom().equals(from)) {
//                                continue;
//                            }
//                            List<String> hexAmounts = eventLog.getAmount();
//                            List<String> numberAmounts = Lists.newArrayList();
//                            if (!CollectionUtils.isEmpty(hexAmounts)) {
//                                String a = hexAmounts.get(0);
//                                if (Texts.isNotBlank(a)) {
//                                    numberAmounts.add(Texts.formatDecimals(String.valueOf(Texts.numberFromBase64(a)), Constants.EMERALD_DECIMALS, Constants.EMERALD_DECIMALS));
//                                }
//                            }
//                            eventLog.setAmount(numberAmounts);
//                            el.add(eventLog);
//                        }
//                        eventES.setLogs(el);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("error", e);
//        }
//        return eventES;
//    }

    @Cached(expire = 15, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public RuntimeTransactionResponse transactionInfo(String txHash) {
        RuntimeTransactionResponse response = null;
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_TX_HASH, txHash));
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.sort(RUNTIME_TRANSACTION_RESULT, SortOrder.DESC);
            searchSourceBuilder.sort(RUNTIME_TRANSACTION_ROUND, SortOrder.DESC);
            searchSourceBuilder.size(1);
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
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
                        response = new RuntimeTransactionResponse();
                        BeanUtils.copyProperties(tx, response);
                        response.setRuntimeId(tx.getRuntime_id());
                        response.setTxHash(tx.getTx_hash());
                        response.setType(RuntimeTransactionType.getDisplayNameByType(response.getType()));
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return response;
    }

    @Cached(expire = 60, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public Runtime getRuntimeInfo(String runtimeId) {
        Optional<Runtime> optional = runtimeRepository.findByRuntimeId(runtimeId);
        return optional.orElse(null);
    }

}
