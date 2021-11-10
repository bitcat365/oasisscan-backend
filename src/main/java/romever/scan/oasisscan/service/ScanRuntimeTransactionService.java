package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
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
import org.springframework.util.CollectionUtils;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionDecoder;
import romever.scan.oasisscan.common.ApplicationConfig;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.runtime.*;
import romever.scan.oasisscan.vo.chain.runtime.emerald.EmeraldTransaction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static romever.scan.oasisscan.common.ESFields.RUNTIME_TRANSACTION_ID;
import static romever.scan.oasisscan.common.ESFields.RUNTIME_TRANSACTION_ROUND;


@Slf4j
@Service
public class ScanRuntimeTransactionService {
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    /**
     * Currently only scan emerald transactions
     */
    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 10 * 1000)
    public void scanTransaction() {
        String emerald = "00000000000000000000000000000000000000000000000072c8215e60d5bca7";
        String runtimeId = emerald;
        RuntimeState runtimeState = apiClient.roothashRuntimeState(runtimeId, null);
        if (runtimeState == null) {
            return;
        }

        long currentRound = runtimeState.getLast_normal_round();
        Long scanRound = getEsRound(runtimeId);
        if (scanRound == null) {
            return;
        }
        if (scanRound != 0) {
            scanRound++;
        }
        for (; scanRound <= currentRound; scanRound++) {
            List<RuntimeTransactionWithResult> list = apiClient.runtimeTransactionsWithResults(runtimeId, scanRound);
            if (CollectionUtils.isEmpty(list)) {
                log.info(String.format("runtime transaction %s, round: %s, count: %s", emerald, scanRound, 0));
                continue;
            }

            RuntimeRound runtimeRound = apiClient.runtimeRound(runtimeId, scanRound);
            Map<String, Map<String, Object>> txMap = Maps.newHashMap();
            for (RuntimeTransactionWithResult r : list) {
                try {
                    JsonNode rawJson = Mappers.parseCborFromBase64(r.getTx(), new TypeReference<JsonNode>() {
                    });
                    if (!rawJson.isArray()) {
                        continue;
                    }

                    String raw = rawJson.get(0).asText();
                    String type = rawJson.get(1).toString();
                    AbstractRuntimeTransaction transaction;
                    String txHash;
                    if (type.contains("evm")) {
                        String hex = Texts.base64ToHex(raw);
                        txHash = Hash.sha3(hex);
                        EmeraldTransaction emeraldTransaction = new EmeraldTransaction();
                        RawTransaction rawTransaction = TransactionDecoder.decode(hex);
                        if (rawTransaction instanceof SignedRawTransaction) {
                            SignedRawTransaction signedResult = (SignedRawTransaction) rawTransaction;
                            emeraldTransaction.setFrom(signedResult.getFrom());
                            emeraldTransaction.setTo(signedResult.getTo());
                            emeraldTransaction.setNonce(signedResult.getNonce().longValue());
                            emeraldTransaction.setGasLimit(signedResult.getGasLimit().longValue());
                            emeraldTransaction.setGasPrice(signedResult.getGasPrice().longValue());
                            emeraldTransaction.setData(signedResult.getData());
                            emeraldTransaction.setValue(signedResult.getValue().toString());
                        }
                        transaction = emeraldTransaction;
                        transaction.setType("evm");
                    } else {
                        transaction = Mappers.parseCborFromBase64(raw, new TypeReference<RuntimeTransaction>() {
                        });
                        txHash = Texts.sha512_256(Texts.base64Decode(raw));

                        RuntimeTransaction runtimeTransaction = (RuntimeTransaction) transaction;
                        RuntimeTransaction.Body body = runtimeTransaction.getCall().getBody();
                        String address = apiClient.base64ToBech32Address(body.getTo());
                        if (Texts.isBlank(address)) {
                            throw new RuntimeException(String.format("address parse failed, %s", body.getTo()));
                        }
                        body.setTo(address);
                        transaction.setType("consensus");
                    }
                    transaction.setRuntime_id(runtimeId);
                    transaction.setTx_hash(txHash);
                    transaction.setRound(scanRound);
                    transaction.setTimestamp(runtimeRound.getHeader().getTimestamp().toEpochSecond());

                    JsonNode resultJson = Mappers.parseCborFromBase64(r.getResult(), new TypeReference<JsonNode>() {
                    });
                    String result = resultJson.fieldNames().next();
                    transaction.setResult("ok".equalsIgnoreCase(result));
                    transaction.setMessage(resultJson.path(result).asText());

                    String esId = runtimeId + "_" + txHash;
                    txMap.put(esId, Mappers.map(transaction));
                    if (!CollectionUtils.isEmpty(txMap)) {
                        BulkResponse bulkResponse = JestDao.indexBulk(elasticsearchClient, elasticsearchConfig.getRuntimeTransactionIndex(), txMap);
                        for (BulkItemResponse bulkItemResponse : bulkResponse) {
                            if (bulkItemResponse.isFailed()) {
                                BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                                log.error(failure.getMessage());
                                throw failure.getCause();
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("error", e);
                    return;
                }
            }
            log.info(String.format("runtime transaction %s, round: %s, count: %s", emerald, scanRound, list.size()));
        }
    }

    private Long getEsRound(String runtimeId) {
        Long storeHeight = null;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(RUNTIME_TRANSACTION_ID, runtimeId));
        searchSourceBuilder.sort(RUNTIME_TRANSACTION_ROUND, SortOrder.DESC);
        searchSourceBuilder.fetchSource(RUNTIME_TRANSACTION_ROUND, null);
        searchSourceBuilder.size(1);
        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getRuntimeTransactionIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                if (hits.getTotalHits().value == 0) {
                    return 0L;
                }
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    RuntimeTransaction tx = Mappers.parseJson(hit.getSourceAsString(), RuntimeTransaction.class).orElse(null);
                    if (tx != null) {
                        storeHeight = tx.getRound();
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return storeHeight;


    }
}