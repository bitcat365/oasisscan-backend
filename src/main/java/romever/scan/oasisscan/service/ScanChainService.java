package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
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
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.MethodEnum;
import romever.scan.oasisscan.vo.chain.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static romever.scan.oasisscan.common.ESFields.BLOCK_HEIGHT;

@Slf4j
@Service
public class ScanChainService {

    @Autowired
    private ApplicationConfig applicationConfig;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    private Long scanHeight = null;

    @Autowired
    private SystemPropertyRepository systemPropertyRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ScanValidatorService scanValidatorService;

    @PostConstruct
    public void init() {
        Long storeHeight = getStoreHeight();
//        Long storeHeight = 86770L;
        if (storeHeight != null) {
            scanHeight = storeHeight;
        }

        log.info("Scan chain data start with {}...", storeHeight);
    }


    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 5 * 1000)
    public void scanBlock() throws IOException {
        if (applicationConfig.isLocal()) {
            return;
        }
        Long curHeight = apiClient.getCurHeight();
        log.info("Scan chain current chain height:{}, scan height:{}", curHeight, scanHeight);
        if (curHeight != null && scanHeight != null) {
            long start = scanHeight;
            long end = curHeight;
            scanHeight = syncBlock(start, end, false);
        } else {
            if (scanHeight == null) {
                scanHeight = getStoreHeight();
            }
        }
    }

    public long syncBlock(long start, long end, boolean fix) throws IOException {
        if (start > end) {
            return start;
        }
        while (start <= end) {
            Block block = apiClient.block(start);
            if (block != null) {
                String meta = block.getMeta();
                if (Texts.isNotBlank(meta)) {
                    try {
                        Block.MetaData metaData = Mappers.parseCborFromBase64(meta, new TypeReference<Block.MetaData>() {
                        });
                        if (metaData != null) {
                            String id = String.valueOf(block.getHeight());
                            block.setMetadata(metaData);
                            block.setMeta(null);

                            //transaction
                            TransactionWithResult transactionWithResult = apiClient.transactionswithresults(start);
                            List<String> txList = transactionWithResult.getTransactions();
                            List<TransactionResult> results = transactionWithResult.getResults();
                            Map<String, Map<String, Object>> txMap = Maps.newHashMap();
                            if (!CollectionUtils.isEmpty(txList)) {
                                for (int i = 0; i < txList.size(); i++) {
                                    String rawTx = txList.get(i);
                                    TransactionResult transactionResult = null;
                                    if (results.size() > i) {
                                        transactionResult = results.get(i);
                                    }
                                    String txHash = Texts.sha512_256(Texts.base64Decode(rawTx));
                                    RawTransaction rawTransaction = Mappers.parseCborFromBase64(rawTx, new TypeReference<RawTransaction>() {
                                    });
                                    if (rawTransaction != null) {
                                        Transaction tx = Mappers.parseCborFromBase64(rawTransaction.getUntrusted_raw_value(), new TypeReference<Transaction>() {
                                        });
                                        if (tx != null) {
                                            //status
                                            if (transactionResult != null) {
                                                TransactionResult.Error error = transactionResult.getError();
                                                tx.setError(error);
                                            }

                                            tx.setTx_hash(txHash);
                                            tx.setHeight(start);
                                            tx.setTime(block.getTime());
                                            tx.setTimestamp(block.getTime().toEpochSecond());
                                            tx.setSignature(rawTransaction.getSignature());
                                            String bech32Address = apiClient.pubkeyToBech32Address(tx.getSignature().getPublic_key());
                                            if (Texts.isBlank(bech32Address)) {
                                                throw new RuntimeException(String.format("address parse failed, %s", tx.getSignature().getPublic_key()));
                                            }
                                            tx.getSignature().setAddress(bech32Address);
                                            Transaction.Body body = tx.getBody();
                                            if (body != null) {
                                                String bodyRawData = body.getUntrusted_raw_value();
                                                if (Texts.isNotBlank(bodyRawData)) {
                                                    body.setUntrusted_raw_value(null);
                                                    Node node = Mappers.parseCborFromBase64(bodyRawData, new TypeReference<Node>() {
                                                    });
                                                    tx.setNode(node);
                                                }

                                                //address
                                                if (Texts.isNotBlank(body.getAccount())) {
                                                    String address = apiClient.base64ToBech32Address(body.getAccount());
                                                    if (Texts.isBlank(address)) {
                                                        throw new RuntimeException(String.format("address parse failed, %s", body.getAccount()));
                                                    }
                                                    body.setAccount(address);
                                                }
                                                if (Texts.isNotBlank(body.getBeneficiary())) {
                                                    String address = apiClient.base64ToBech32Address(body.getBeneficiary());
                                                    if (Texts.isBlank(address)) {
                                                        throw new RuntimeException(String.format("address parse failed, %s", body.getBeneficiary()));
                                                    }
                                                    body.setBeneficiary(address);
                                                }
                                                if (Texts.isNotBlank(body.getFrom())) {
                                                    String address = apiClient.base64ToBech32Address(body.getFrom());
                                                    if (Texts.isBlank(address)) {
                                                        throw new RuntimeException(String.format("address parse failed, %s", body.getFrom()));
                                                    }
                                                    body.setFrom(address);
                                                }
                                                if (Texts.isNotBlank(body.getTo())) {
                                                    String address = apiClient.base64ToBech32Address(body.getTo());
                                                    if (Texts.isBlank(address)) {
                                                        throw new RuntimeException(String.format("address parse failed, %s", body.getTo()));
                                                    }
                                                    body.setTo(address);
                                                }
                                            }
                                            txMap.put(tx.getTx_hash(), Mappers.map(tx));

                                            //update account info
                                            updateAccountInfo(tx);
                                        }
                                    }
                                }
                                block.setTxs(txMap.size());
                            }

                            //block
                            JestDao.index(elasticsearchClient, elasticsearchConfig.getBlockIndex(), Mappers.map(block), id);
                            log.info("block [{}] sync done.", start);
                            //transaction
                            if (!CollectionUtils.isEmpty(txMap)) {
                                BulkResponse bulkResponse = JestDao.indexBulk(elasticsearchClient, elasticsearchConfig.getTransactionIndex(), txMap);
                                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                                    if (bulkItemResponse.isFailed()) {
                                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                                        log.error(failure.getMessage());
                                        throw failure.getCause();
                                    }
                                }

                                if (!fix) {
                                    SystemProperty systemProperty = systemPropertyRepository.findByProperty(Constants.SCAN_HEIGHT_PROPERTY).orElse(new SystemProperty());
                                    systemProperty.setProperty(Constants.SCAN_HEIGHT_PROPERTY);
                                    systemProperty.setValue(String.valueOf(start));
                                    systemPropertyRepository.saveAndFlush(systemProperty);
                                    log.info("transaction [{}] sync done, count: {} ", start, txMap.size());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("error", e);
                        break;
                    }
                    start++;
                    continue;
                }
            }
            break;
        }
        return start;
    }

    private void updateAccountInfo(Transaction tx) throws IOException {
        String method = tx.getMethod();
        MethodEnum methodEnum = MethodEnum.getEnumByName(method);
        Transaction.Body body = tx.getBody();
        if (body == null) {
            return;
        }

        if (methodEnum == null) {
            return;
        }

        String from = null;
        String to = null;
        switch (methodEnum) {
            case StakingTransfer:
                to = body.getTo();
            case StakingAddEscrow:
            case StakingReclaimEscrow:
                from = tx.getSignature().getAddress();
                break;
            default:
                break;
        }

        if (Texts.isNotBlank(from)) {
            updateAccountInfo(from);
        }

        if (Texts.isNotBlank(to)) {
            updateAccountInfo(to);
        }
    }

    private void updateAccountInfo(String address) throws IOException {
        AccountInfo accountInfo = apiClient.accountInfo(address, null);
        Account account = scanValidatorService.getAccount(address, accountInfo);
        accountRepository.save(account);
    }

    public Long getEsHeight() {
        Long storeHeight = null;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort(BLOCK_HEIGHT, SortOrder.DESC);
        searchSourceBuilder.fetchSource(BLOCK_HEIGHT, null);
        searchSourceBuilder.size(1);
        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                if (hits.getTotalHits().value == 0) {
                    return 1L;
                }
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    Block block = Mappers.parseJson(hit.getSourceAsString(), Block.class).orElse(null);
                    if (block != null) {
                        storeHeight = block.getHeight();
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return storeHeight;
    }

    public Long getStoreHeight() {
        Long latestHeight = null;
        Optional<SystemProperty> optional = systemPropertyRepository.findByProperty(Constants.SCAN_HEIGHT_PROPERTY);
        if (optional.isPresent()) {
            SystemProperty property = optional.get();
            latestHeight = Long.parseLong(property.getValue());
        }
        return latestHeight;
    }
}
