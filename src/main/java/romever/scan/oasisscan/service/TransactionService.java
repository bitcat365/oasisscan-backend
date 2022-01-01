package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.ApplicationConfig;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Runtime;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.MethodEnum;
import romever.scan.oasisscan.vo.RuntimeTransactionType;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.Transaction;
import romever.scan.oasisscan.vo.chain.runtime.AbstractRuntimeTransaction;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeTransaction;
import romever.scan.oasisscan.vo.chain.runtime.emerald.EmeraldTransaction;
import romever.scan.oasisscan.vo.response.AccountSimple;
import romever.scan.oasisscan.vo.response.HistogramResponse;
import romever.scan.oasisscan.vo.response.ListTransactionResponse;
import romever.scan.oasisscan.vo.response.TransactionDetailResponse;
import romever.scan.oasisscan.vo.response.runtime.ListRuntimeTransactionResponse;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static romever.scan.oasisscan.common.ESFields.*;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;
    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private RuntimeService runtimeService;

    private static final int HISTORY_DAY_SIZE = 30;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public long totalTransactions() {
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            CountResponse countResponse = JestDao.count(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), boolQueryBuilder);
            return countResponse.getCount();
        } catch (IOException e) {
            log.error("error", e);
        }
        return 0;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult transactions(
            int size, int page, Long height, String address, String method, boolean runtime) {
        long total = 0;
        List<Object> responses = Lists.newArrayList();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (height != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(TRANSACTION_HEIGHT, height));
        }
        if (Texts.isNotBlank(address)) {
            BoolQueryBuilder addressBoolQuery = QueryBuilders.boolQuery();
            boolQueryBuilder.filter(
                    addressBoolQuery
                            .should(QueryBuilders.termQuery(TRANSACTION_FROM, address))
                            .should(QueryBuilders.termQuery(TRANSACTION_ESCROW_ACCOUNT, address))
                            .should(QueryBuilders.termQuery(TRANSACTION_BODY_SIG_PUBLIC_KEY, address))
                            .should(QueryBuilders.termQuery(TRANSACTION_TRANSFER_TO, address))
            );

            //runtime transactions
            if (runtime) {
                boolQueryBuilder.filter(
                        addressBoolQuery
                                .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_SIG_ADDRESS, address))
                                .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_TO, address))
                                .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_EVENT_FROM, address))
                                .should(QueryBuilders.termQuery(RUNTIME_TRANSACTION_EVENT_TO, address))
                );
            }
        }
        if (Texts.isNotBlank(method)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(TRANSACTION_METHOD, method));
        } else {
            //remove registry.RegisterNode transactions
            boolQueryBuilder.mustNot(QueryBuilders.termQuery(TRANSACTION_METHOD, MethodEnum.RegistryRegisterNode.getName()));
        }
        if (boolQueryBuilder.hasClauses()) {
            searchSourceBuilder.query(boolQueryBuilder);
        }

        searchSourceBuilder.sort(TRANSACTION_TIMESTAMP, SortOrder.DESC);
        searchSourceBuilder.from(size * (page - 1));
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse;
            if (runtime) {
                String[] indices = new String[]{elasticsearchConfig.getTransactionIndex(), elasticsearchConfig.getRuntimeTransactionIndex()};
                searchResponse = JestDao.searchFromIndices(elasticsearchClient, indices, searchSourceBuilder);
            } else {
                searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), searchSourceBuilder);
            }
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                total = hits.getTotalHits().value;
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    JsonNode txJson = Mappers.parseJson(hit.getSourceAsString());
                    if (txJson.has(RUNTIME_TRANSACTION_ROUND)) {
                        //runtime transactions
                        String type = txJson.path("type").asText();
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

                    } else {
                        //consensus transactions
                        Transaction tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<Transaction>() {
                        });
                        if (tx != null) {
                            //calculate reclaim amount
                            double escrowBalance = 0;
                            double totalShares = 0;
                            String txMethod = tx.getMethod();
                            MethodEnum methodEnum = MethodEnum.getEnumByName(txMethod);
                            if (methodEnum == MethodEnum.StakingReclaimEscrow) {
                                Transaction.Body body = tx.getBody();
                                String validator = body.getAccount();

                                AccountSimple accountSimple = accountInfo(validator, tx.getHeight() - 1);
                                if (accountSimple != null) {
                                    escrowBalance = Double.parseDouble(accountSimple.getEscrow());
                                    totalShares = Double.parseDouble(accountSimple.getTotalShares());
                                }
                            }

                            responses.add(ListTransactionResponse.of(tx, escrowBalance, totalShares));
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(responses, page, size, total);
    }

    public List<Transaction> getEsTransactionsByHeight(Long height) throws IOException {
        List<Transaction> transactions = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (height != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(TRANSACTION_HEIGHT, height));
        }
        if (boolQueryBuilder.hasClauses()) {
            searchSourceBuilder.query(boolQueryBuilder);
        }

        searchSourceBuilder.size(10000);
        searchSourceBuilder.trackTotalHits(true);

        SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), searchSourceBuilder);
        if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Transaction tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<Transaction>() {
                });
                transactions.add(tx);
            }
        }
        return transactions;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public TransactionDetailResponse detail(String hash) {
        TransactionDetailResponse response = null;
        if (Texts.isNotBlank(hash)) {
            try {
                GetResponse getResponse = JestDao.get(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), hash);
                if (getResponse.isExists()) {
                    Transaction transaction = Mappers.parseJson(getResponse.getSourceAsString(), new TypeReference<Transaction>() {
                    });
                    if (transaction != null) {

                        //calculate reclaim amount
                        double escrowBalance = 0;
                        double totalShares = 0;
                        String method = transaction.getMethod();
                        MethodEnum methodEnum = MethodEnum.getEnumByName(method);
                        if (methodEnum == MethodEnum.StakingReclaimEscrow) {
                            Transaction.Body body = transaction.getBody();
                            String validator = body.getAccount();

                            AccountSimple accountSimple = accountInfo(validator, transaction.getHeight() - 1);
                            if (accountSimple != null) {
                                escrowBalance = Double.parseDouble(accountSimple.getEscrow());
                                totalShares = Double.parseDouble(accountSimple.getTotalShares());
                            }
                        }

                        response = TransactionDetailResponse.of(transaction, escrowBalance, totalShares);
                    }
                }
            } catch (ElasticsearchException e) {
                if (e.status() == RestStatus.NOT_FOUND) {
                    log.warn("transaction {} not found.", hash);
                }
            } catch (IOException e) {
                log.error("error", e);
            }
        }
        return response;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<String> methods() {
        List<String> methodList = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermsAggregationBuilder methodAggBuilder = AggregationBuilders.terms(TRANSACTION_METHOD).field(TRANSACTION_METHOD).size(100);
        searchSourceBuilder.aggregation(methodAggBuilder);
        searchSourceBuilder.size(0);
        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                Aggregations aggregations = searchResponse.getAggregations();
                Terms methodAgg = aggregations.get(TRANSACTION_METHOD);
                List<? extends Terms.Bucket> buckets = methodAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    for (Terms.Bucket bucket : buckets) {
                        methodList.add(bucket.getKeyAsString());
                    }
                }

            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return methodList;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<HistogramResponse> transactionHistory() {
        List<HistogramResponse> list = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.rangeQuery(TRANSACTION_TIME).from(OffsetDateTime.now().minusDays(HISTORY_DAY_SIZE), true));
        DateHistogramAggregationBuilder dateHistogramAggregationBuilder =
                AggregationBuilders.dateHistogram(TRANSACTION_TIME)
                        .field(TRANSACTION_TIME)
                        .calendarInterval(DateHistogramInterval.DAY)
                        .format("yyyy-MM-dd");
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.aggregation(dateHistogramAggregationBuilder);
        searchSourceBuilder.size(0);
        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                Aggregations aggregations = searchResponse.getAggregations();
                Histogram histogram = aggregations.get(TRANSACTION_TIME);
                List<? extends Histogram.Bucket> buckets = histogram.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    for (Histogram.Bucket bucket : buckets) {
                        HistogramResponse response = new HistogramResponse();
                        bucket.getKey();
                        ZonedDateTime zonedDateTime = (ZonedDateTime) bucket.getKey();
                        response.setKey(String.valueOf(zonedDateTime.toEpochSecond()));
                        response.setValue(bucket.getDocCount());
                        list.add(response);
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return list;
    }


    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult powerEvent(int size, int page, String address) {
        long total = 0;
        List<ListTransactionResponse> responses = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(TRANSACTION_ESCROW_ACCOUNT, address));
        boolQueryBuilder.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.termQuery(TRANSACTION_METHOD, MethodEnum.StakingAddEscrow.getName()))
                .should(QueryBuilders.termQuery(TRANSACTION_METHOD, MethodEnum.StakingReclaimEscrow.getName()))
        );
        boolQueryBuilder.mustNot(QueryBuilders.existsQuery(TRANSACTION_ERROR_MESSAGE));
        boolQueryBuilder.filter(QueryBuilders.rangeQuery(TRANSACTION_HEIGHT).from(applicationConfig.getUpgradeStartHeight(), true));
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort(TRANSACTION_TIMESTAMP, SortOrder.DESC);
        searchSourceBuilder.from(size * (page - 1));
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                total = hits.getTotalHits().value;
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    Transaction tx = Mappers.parseJson(hit.getSourceAsString(), new TypeReference<Transaction>() {
                    });

                    if (tx == null) {
                        continue;
                    }

                    AccountSimple accountSimple = accountInfo(address, tx.getHeight() - 1);
                    if (accountSimple == null) {
                        continue;
                    }
                    responses.add(ListTransactionResponse.of(tx, Double.parseDouble(accountSimple.getEscrow()), Double.parseDouble(accountSimple.getTotalShares())));
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return ApiResult.page(responses, page, size, total);
    }

    public AccountSimple accountInfo(String address, Long height) {
        AccountSimple response = null;
        try {
            AccountInfo accountInfo = apiClient.accountInfo(address, height);
            if (accountInfo != null) {
                response = AccountSimple.of(accountInfo);
                response.setAddress(address);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return response;
    }
}
