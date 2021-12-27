package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.crypto.*;
import romever.scan.oasisscan.common.ApplicationConfig;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.Runtime;
import romever.scan.oasisscan.entity.SystemProperty;
import romever.scan.oasisscan.repository.RuntimeRepository;
import romever.scan.oasisscan.repository.SystemPropertyRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.RuntimeTransactionType;
import romever.scan.oasisscan.vo.chain.runtime.*;
import romever.scan.oasisscan.vo.chain.runtime.emerald.EmeraldTransaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static romever.scan.oasisscan.common.ESFields.RUNTIME_TRANSACTION_ID;
import static romever.scan.oasisscan.common.ESFields.RUNTIME_TRANSACTION_ROUND;


@Slf4j
@Service
public class ScanRuntimeTransactionService {
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
    @Autowired
    private SystemPropertyRepository systemPropertyRepository;

    /**
     * Currently only scan emerald transactions
     */
    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 10 * 1000)
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void scanTransaction() throws IOException {
        if (applicationConfig.isLocal()) {
            return;
        }

        String emerald = null;
        if ("prod".equalsIgnoreCase(applicationConfig.getEnv())) {
            emerald = "000000000000000000000000000000000000000000000000e2eaa99fc008f87f";
        } else if ("test".equalsIgnoreCase(applicationConfig.getEnv())) {
            emerald = "00000000000000000000000000000000000000000000000072c8215e60d5bca7";
        }
        if (Texts.isBlank(emerald)) {
            return;
        }

        String runtimeId = emerald;
        Long currentRound = getCurrentRound(runtimeId);
        if (currentRound == null) return;
        Long scanRound = getScanRound(runtimeId);
        if (scanRound == null) {
            return;
        }
        if (scanRound != 0) {
            scanRound++;
        }
        for (; scanRound <= currentRound; scanRound++) {
            List<RuntimeTransactionWithResult> list = apiClient.runtimeTransactionsWithResults(runtimeId, scanRound);
            if (CollectionUtils.isEmpty(list)) {
                //save scan height
                Optional<Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
                if (optionalRuntime.isPresent()) {
                    Runtime runtime = optionalRuntime.get();
                    runtime.setScanTxHeight(scanRound);
                    runtimeRepository.save(runtime);
                }
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
                    if (type.contains("evm.ethereum")) {
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
                        transaction.setType(RuntimeTransactionType.EVM.getType());
                    } else {
                        transaction = Mappers.parseCborFromBase64(raw, new TypeReference<RuntimeTransaction>() {
                        });
                        txHash = Texts.sha512_256(Texts.base64Decode(raw));

                        RuntimeTransaction runtimeTransaction = (RuntimeTransaction) transaction;
                        RuntimeTransaction.Body body = runtimeTransaction.getCall().getBody();
                        if (Texts.isNotBlank(body.getTo())) {
                            String address = apiClient.base64ToBech32Address(body.getTo());
                            if (Texts.isBlank(address)) {
                                throw new RuntimeException(String.format("address parse failed, %s", body.getTo()));
                            }
                            body.setTo(address);
                        }
                        transaction.setType(RuntimeTransactionType.CONSENSUS.getType());

                        List<RuntimeTransaction.Si> sis = runtimeTransaction.getAi().getSi();
                        if (!CollectionUtils.isEmpty(sis)) {
                            for (RuntimeTransaction.Si si : sis) {
                                RuntimeTransaction.Signature signature = si.getAddress_spec().getSignature();
                                if (Texts.isNotBlank(signature.getEd25519())) {
                                    String addressSi = apiClient.pubkeyToBech32Address(signature.getEd25519());
                                    if (Texts.isBlank(addressSi)) {
                                        throw new RuntimeException(String.format("address parse failed, %s", signature.getEd25519()));
                                    }
                                    signature.setAddress(addressSi);
                                }
                                if (Texts.isNotBlank(signature.getSecp256k1eth())) {
                                    String hexCompressed = Texts.base64ToHex(signature.getSecp256k1eth());
                                    byte[] c = Texts.hexStringToByteArray(hexCompressed);
                                    byte[] uc = Texts.compressedToUncompressed(c);
                                    String address = Keys.toChecksumAddress(Keys.getAddress(new BigInteger(Texts.toHex(uc), 16))).toLowerCase();
                                    signature.setAddress(address);
                                }
                            }
                        }
                    }
                    transaction.setRuntime_id(runtimeId);
                    transaction.setTx_hash(txHash);
                    transaction.setRound(scanRound);
                    transaction.setTimestamp(runtimeRound.getHeader().getTimestamp().toEpochSecond());

                    JsonNode resultJson = Mappers.parseCborFromBase64(r.getResult(), new TypeReference<JsonNode>() {
                    });
                    String result = resultJson.fieldNames().next();
                    transaction.setResult("ok".equalsIgnoreCase(result));
                    transaction.setMessage(resultJson.path(result).toString());

                    //events
                    List<RuntimeEvent> events = r.getEvents();
                    if (!CollectionUtils.isEmpty(events) && !type.contains("evm.ethereum")) {
                        List<AbstractRuntimeTransaction.Event> runtimeEvents = Lists.newArrayList();
                        for (RuntimeEvent event : events) {
                            AbstractRuntimeTransaction.Event runtimeEvent = new AbstractRuntimeTransaction.Event();
                            String key = event.getKey();
                            String keyHex = Texts.base64ToHex(key);
                            String eventType = "";
                            if (keyHex.contains(Constants.RUNTIME_TX_DEPOSIT_HEX)) {
                                eventType = "deposit";
                            } else if (keyHex.contains(Constants.RUNTIME_TX_WITHDRAW_HEX)) {
                                eventType = "withdraw";
                            }
                            runtimeEvent.setType(eventType);

                            String value = event.getValue();
                            List<EventLog> eventLogs = Lists.newArrayList();
                            JsonNode eventJson = Mappers.parseCborFromBase64(value, new TypeReference<JsonNode>() {
                            });
                            if (eventJson.isArray()) {
                                eventLogs = Mappers.parseCborFromBase64(value, new TypeReference<List<EventLog>>() {
                                });
                            } else {
                                eventLogs.add(Mappers.parseCborFromBase64(value, new TypeReference<EventLog>() {
                                }));
                            }
                            if (!CollectionUtils.isEmpty(eventLogs)) {
                                for (EventLog log : eventLogs) {
                                    String from = apiClient.base64ToBech32Address(log.getFrom());
                                    if (Texts.isBlank(from)) {
                                        throw new RuntimeException(String.format("address parse failed, %s", scanRound));
                                    }
                                    log.setFrom(from);

                                    String to = apiClient.base64ToBech32Address(log.getTo());
                                    if (Texts.isBlank(to)) {
                                        throw new RuntimeException(String.format("address parse failed, %s", scanRound));
                                    }
                                    log.setTo(to);
                                }
                                runtimeEvent.setLogs(eventLogs);
                            }
                            runtimeEvents.add(runtimeEvent);
                        }
                        transaction.setEvents(runtimeEvents);
                    }

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
                    log.error(String.format("error, %s, %s, %s", runtimeId, scanRound, r.getTx()), e);
                    return;
                }
            }

            //save scan height
            Optional<Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
            if (optionalRuntime.isPresent()) {
                Runtime runtime = optionalRuntime.get();
                runtime.setScanTxHeight(scanRound);
                runtimeRepository.save(runtime);
            }

            log.info(String.format("runtime transaction %s, round: %s, count: %s", emerald, scanRound, list.size()));
        }
    }

    private Long getCurrentRound(String runtimeId) throws IOException {
        RuntimeState runtimeState = apiClient.roothashRuntimeState(runtimeId, null);
        if (runtimeState == null) {
            return null;
        }

        return runtimeState.getLast_normal_round();
    }

    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 15 * 1000)
    public void scanEvents() throws Exception {
        if (applicationConfig.isLocal()) {
            return;
        }

        String emerald = null;
        if ("prod".equalsIgnoreCase(applicationConfig.getEnv())) {
            emerald = "000000000000000000000000000000000000000000000000e2eaa99fc008f87f";
        } else if ("test".equalsIgnoreCase(applicationConfig.getEnv())) {
            emerald = "00000000000000000000000000000000000000000000000072c8215e60d5bca7";
        } else if ("local".equalsIgnoreCase(applicationConfig.getEnv())) {
            emerald = "00000000000000000000000000000000000000000000000072c8215e60d5bca7";
        }
        if (Texts.isBlank(emerald)) {
            return;
        }

        String runtimeId = emerald;
        Long currentRound = getCurrentRound(runtimeId);
        if (currentRound == null) return;

        Long scanRound = getScanEventRound(runtimeId);
        if (scanRound == null) {
            return;
        }

        if (scanRound != 0) {
            scanRound++;
        }
        for (; scanRound <= currentRound; scanRound++) {
            List<RuntimeEvent> events = apiClient.runtimeEvent(runtimeId, scanRound);
            if (CollectionUtils.isEmpty(events)) {
                //save scan height
                saveScanEventRound(runtimeId, scanRound);
                log.info(String.format("runtime event %s, round: %s, count: %s", emerald, scanRound, 0));
                continue;
            }

            Map<String, Map<String, Object>> eventMap = Maps.newHashMap();
            for (int i = 0; i < events.size(); i++) {
                RuntimeEvent event = events.get(i);
                String key = event.getKey();
                String keyHex = Texts.base64ToHex(key);
                if (keyHex.equalsIgnoreCase(Constants.RUNTIME_EVENT_EVM_HEX)) {
                    continue;
                }
                event.setKey(keyHex);

                String value = event.getValue();
                List<EventLog> eventLogs = Lists.newArrayList();
                JsonNode eventJson = Mappers.parseCborFromBase64(value, new TypeReference<JsonNode>() {
                });
                if (eventJson.isArray()) {
                    eventLogs = Mappers.parseCborFromBase64(value, new TypeReference<List<EventLog>>() {
                    });
                } else {
                    eventLogs.add(Mappers.parseCborFromBase64(value, new TypeReference<EventLog>() {
                    }));
                }
                if (!CollectionUtils.isEmpty(eventLogs)) {
                    for (EventLog log : eventLogs) {
                        if (Texts.isNotBlank(log.getFrom())) {
                            String from = apiClient.base64ToBech32Address(log.getFrom());
                            if (Texts.isBlank(from)) {
                                throw new RuntimeException(String.format("address parse failed, %s", scanRound));
                            }
                            log.setFrom(from);
                        }
                        if (Texts.isNotBlank(log.getTo())) {
                            String to = apiClient.base64ToBech32Address(log.getTo());
                            if (Texts.isBlank(to)) {
                                throw new RuntimeException(String.format("address parse failed, %s", scanRound));
                            }
                            log.setTo(to);
                        }
                        if (Texts.isNotBlank(log.getOwner())) {
                            String owner = apiClient.base64ToBech32Address(log.getOwner());
                            if (Texts.isBlank(owner)) {
                                throw new RuntimeException(String.format("address parse failed, %s", scanRound));
                            }
                            log.setOwner(owner);
                        }
                    }
                }

                RuntimeEventES eventES = new RuntimeEventES();
                eventES.setType(event.getKey());
                eventES.setTx_hash(event.getTx_hash());
                eventES.setLogs(eventLogs);
                String esId = runtimeId + "_" + scanRound + "_" + i;
                eventMap.put(esId, Mappers.map(eventES));
            }

            if (!CollectionUtils.isEmpty(eventMap)) {
                BulkResponse bulkResponse = JestDao.indexBulk(elasticsearchClient, elasticsearchConfig.getRuntimeEventIndex(), eventMap);
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        log.error(failure.getMessage());
                        throw failure.getCause();
                    }
                }
            }
            //save scan height
            saveScanEventRound(runtimeId, scanRound);
            log.info(String.format("runtime event %s, round: %s, count: %s", emerald, scanRound, eventMap.size()));
        }
    }

    private Long getScanRound(String runtimeId) {
        Long storeHeight = null;
        Optional<Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
        if (optionalRuntime.isPresent()) {
            storeHeight = optionalRuntime.get().getScanTxHeight();
        }
        return storeHeight;
    }

    private Long getScanEventRound(String runtimeId) {
        Long storeHeight = null;
        String property = Constants.SYSTEM_RUNTIME_EVENT_ROUND_PREFIX + runtimeId;
        Optional<SystemProperty> optionalSystemProperty = systemPropertyRepository.findByProperty(property);
        if (optionalSystemProperty.isPresent()) {
            storeHeight = Long.parseLong(optionalSystemProperty.get().getValue());
        }
        return storeHeight;
    }

    private void saveScanEventRound(String runtimeId, long round) {
        String property = Constants.SYSTEM_RUNTIME_EVENT_ROUND_PREFIX + runtimeId;
        SystemProperty systemProperty = systemPropertyRepository.findByProperty(property).orElse(new SystemProperty());
        systemProperty.setProperty(Constants.SCAN_HEIGHT_PROPERTY);
        systemProperty.setValue(String.valueOf(round));
        systemPropertyRepository.saveAndFlush(systemProperty);
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
