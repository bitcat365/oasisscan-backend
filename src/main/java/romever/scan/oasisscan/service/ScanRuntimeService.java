package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import romever.scan.oasisscan.entity.RuntimeNode;
import romever.scan.oasisscan.entity.RuntimeStats;
import romever.scan.oasisscan.entity.RuntimeStatsInfo;
import romever.scan.oasisscan.entity.RuntimeStatsType;
import romever.scan.oasisscan.repository.RuntimeNodeRepository;
import romever.scan.oasisscan.repository.RuntimeRepository;
import romever.scan.oasisscan.repository.RuntimeStatsInfoRepository;
import romever.scan.oasisscan.repository.RuntimeStatsRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.CommitteeRoleEnum;
import romever.scan.oasisscan.vo.MethodEnum;
import romever.scan.oasisscan.vo.RuntimeHeaderTypeEnum;
import romever.scan.oasisscan.vo.chain.runtime.Runtime;
import romever.scan.oasisscan.vo.chain.*;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeRound;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeState;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    @Autowired
    private RuntimeStatsRepository runtimeStatsRepository;
    @Autowired
    private RuntimeStatsInfoRepository runtimeStatsInfoRepository;
    @Autowired
    private RuntimeNodeRepository runtimeNodeRepository;

    @Autowired
    private TransactionService transactionService;

    /**
     * Get all runtimes info and save in elasticsearch
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 5 * 1000)
    public void scanRuntime() throws IOException {
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
    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 10 * 1000)
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
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
                long genesisTime = runtimeState.getGenesis_block().getHeader().getTimestamp().toEpochSecond();
                Long genesisHeight = getGenesisRoundHeight(genesisTime);
                if (genesisHeight == null) {
                    throw new RuntimeException(String.format("Genesis Height can not found. %s", runtimeId));
                }
                scanHeight = genesisHeight;

                Optional<romever.scan.oasisscan.entity.Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
                if (!optionalRuntime.isPresent()) {
                    throw new RuntimeException("Runtime db read error.");
                }
                romever.scan.oasisscan.entity.Runtime _runtime = optionalRuntime.get();
                _runtime.setStartRoundHeight(genesisHeight);
                runtimeRepository.saveAndFlush(_runtime);
            }
            while (scanHeight < currentChainHeight) {
                RuntimeRound runtimeRound = apiClient.roothashLatestblock(runtimeId, scanHeight);
                if (runtimeRound == null) {
                    throw new RuntimeException(String.format("Runtime round api error. %s, %s", runtimeId, scanHeight));
                }
                RuntimeRound.Header header = runtimeRound.getHeader();
                String id = header.getNamespace() + "_" + header.getRound();
                JestDao.index(elasticsearchClient, elasticsearchConfig.getRuntimeRoundIndex(), Mappers.map(header), id);
                log.info("Runtime round sync done. {} [{}]", scanHeight, id);

                //save scan height
                Optional<romever.scan.oasisscan.entity.Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
                if (!optionalRuntime.isPresent()) {
                    throw new RuntimeException("Runtime db read error.");
                }
                romever.scan.oasisscan.entity.Runtime _runtime = optionalRuntime.get();
                _runtime.setScanRoundHeight(scanHeight);
                runtimeRepository.saveAndFlush(_runtime);
                scanHeight++;
            }
        }
    }

    /**
     * See https://github.com/oasisprotocol/tools/tree/main/runtime-stats
     */
    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 20 * 1000)
    public void scanRuntimeStats() throws IOException {
        if (applicationConfig.isLocal()) {
            return;
        }

        List<romever.scan.oasisscan.entity.Runtime> runtimes = runtimeRepository.findAllByOrderByStartRoundHeightAsc();
        if (CollectionUtils.isEmpty(runtimes)) {
            return;
        }
        long currentChainHeight = apiClient.getCurHeight();
        for (romever.scan.oasisscan.entity.Runtime runtime : runtimes) {
            String runtimeId = runtime.getRuntimeId();
            long scanHeight = runtime.getStatsHeight();
            if (scanHeight == 0) {
                scanHeight = runtime.getStartRoundHeight();
            }
            long curRound = 0;
            boolean roundDiscrepancy = false;
            RuntimeState.Committee committee = null;
            RuntimeState.Member currentScheduler = null;
            while (scanHeight < currentChainHeight) {
                Optional<romever.scan.oasisscan.entity.Runtime> optionalRuntime = runtimeRepository.findByRuntimeId(runtimeId);
                if (!optionalRuntime.isPresent()) {
                    throw new RuntimeException("Runtime db read error.");
                }
                romever.scan.oasisscan.entity.Runtime _runtime = optionalRuntime.get();
                _runtime.setStatsHeight(scanHeight);
                runtimeRepository.saveAndFlush(_runtime);

                List<Node> nodes = apiClient.registryNodes(scanHeight);
                if (nodes == null) {
                    throw new RuntimeException(String.format("Registry nodes api error. %s, %s", runtimeId, scanHeight));
                }
                //save node entity map
                Map<String, String> nodeToEntity = Maps.newHashMap();
                for (Node node : nodes) {
                    nodeToEntity.put(node.getId(), node.getEntity_id());
                    List<Node.Runtime> runtimeList = node.getRuntimes();
                    if (CollectionUtils.isEmpty(runtimeList)) {
                        continue;
                    }
                    for (Node.Runtime r : runtimeList) {
                        if (r.getId().equalsIgnoreCase(runtimeId)) {
                            //save in db
                            Optional<RuntimeNode> optionalRuntimeNode = runtimeNodeRepository.findByRuntimeIdAndNodeId(runtimeId, node.getId());
                            if (!optionalRuntimeNode.isPresent()) {
                                RuntimeNode runtimeNode = new RuntimeNode();
                                runtimeNode.setRuntimeId(runtimeId);
                                runtimeNode.setNodeId(node.getId());
                                runtimeNode.setEntityId(node.getEntity_id());
                                runtimeNodeRepository.save(runtimeNode);
                            }
                        }
                    }
                }

                RuntimeRound runtimeRound = apiClient.roothashLatestblock(runtimeId, scanHeight);
                if (runtimeRound == null) {
                    throw new RuntimeException(String.format("Runtime round api error. %s", scanHeight));
                }
                // If new round, check for proposer timeout.
                // Need to look at submitted transactions if round failure was caused by a proposer timeout.
                List<Transaction> txs = null;
                try {
                    txs = transactionService.getEsTransactionsByHeight(scanHeight);
                } catch (IOException e) {
                    log.error("error", e);
                    break;
                }
                boolean proposerTimeout = false;
                if (!CollectionUtils.isEmpty(txs) && curRound != runtimeRound.getHeader().getRound() && committee != null) {
                    for (Transaction tx : txs) {
                        TransactionResult.Error error = tx.getError();
                        if (error != null) {
                            if (Texts.isNotBlank(error.getMessage())) {
                                continue;
                            }
                        }
                        if (!tx.getMethod().equalsIgnoreCase(MethodEnum.RoothashExecutorProposerTimeout.getName())) {
                            continue;
                        }
                        if (!runtimeId.equalsIgnoreCase(tx.getBody().getId())) {
                            continue;
                        }
                        saveRuntimeStats(runtimeId, scanHeight, tx.getBody().getRound(), nodeToEntity.get(tx.getSignature().getPublic_key()), RuntimeStatsType.PROPOSED_TIMEOUT);
                        if (currentScheduler != null) {
                            saveRuntimeStats(runtimeId, scanHeight, tx.getBody().getRound(), nodeToEntity.get(currentScheduler.getPublic_key()), RuntimeStatsType.PROPOSER_MISSED);
                        }
                        proposerTimeout = true;
                        break;
                    }
                }

                long headerType = runtimeRound.getHeader().getHeader_type();
                // Go over events before updating potential new round committee info.
                // Even if round transition happened at this height, all events emitted
                // at this height belong to the previous round.
                List<RoothashEvent> events = apiClient.roothashEvents(scanHeight);
                if (events != null && curRound != 0) {
                    for (RoothashEvent ev : events) {
                        if (!runtimeId.equalsIgnoreCase(ev.getRuntime_id())) {
                            continue;
                        }
                        if (ev.getExecution_discrepancy() != null) {
                            roundDiscrepancy = true;
                        }
                        if (ev.getFinalized() != null) {
                            RoothashEvent.Finalized _ev = ev.getFinalized();
                            // Skip the empty finalized event that is triggered on initial round.
                            if (CollectionUtils.isEmpty(_ev.getGood_compute_nodes()) && CollectionUtils.isEmpty(_ev.getBad_compute_nodes()) && committee == null) {
                                continue;
                            }
                            // Skip if epoch transition or suspended blocks.
                            if (headerType == RuntimeHeaderTypeEnum.EpochTransition.getCode() || headerType == RuntimeHeaderTypeEnum.Suspended.getCode()) {
                                continue;
                            }
                            if (proposerTimeout) {
                                continue;
                            }
                            // Update stats.
                            OUTER:
                            for (RuntimeState.Member member : committee.getMembers()) {
                                String entityId = nodeToEntity.get(member.getPublic_key());
                                // Primary workers are always required.
                                if (member.getRole().equalsIgnoreCase(CommitteeRoleEnum.WORKER.getName())) {
                                    saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.PRIMARY_INVOKED);
                                }
                                // In case of discrepancies backup workers were invoked as well.
                                if (roundDiscrepancy && member.getRole().equalsIgnoreCase(CommitteeRoleEnum.BACKUPWORKER.getName())) {
                                    saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.BCKP_INVOKED);
                                }
                                // Go over good commitments.
                                if (_ev.getGood_compute_nodes() != null) {
                                    for (String g : _ev.getGood_compute_nodes()) {
                                        if (member.getPublic_key().equalsIgnoreCase(g) && member.getRole().equalsIgnoreCase(CommitteeRoleEnum.WORKER.getName())) {
                                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.PRIMARY_GOOD_COMMIT);
                                            continue OUTER;
                                        }
                                        if (member.getPublic_key().equalsIgnoreCase(g) && roundDiscrepancy && member.getRole().equalsIgnoreCase(CommitteeRoleEnum.BACKUPWORKER.getName())) {
                                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.BCKP_GOOD_COMMIT);
                                            continue OUTER;
                                        }
                                    }
                                }
                                // Go over bad commitments.
                                if (_ev.getBad_compute_nodes() != null) {
                                    for (String g : _ev.getBad_compute_nodes()) {
                                        if (member.getPublic_key().equalsIgnoreCase(g) && member.getRole().equalsIgnoreCase(CommitteeRoleEnum.WORKER.getName())) {
                                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.PRIM_BAD_COMMMIT);
                                            continue OUTER;
                                        }
                                        if (member.getPublic_key().equalsIgnoreCase(g) && roundDiscrepancy && member.getRole().equalsIgnoreCase(CommitteeRoleEnum.BACKUPWORKER.getName())) {
                                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.BCKP_BAD_COMMIT);
                                            continue OUTER;
                                        }
                                    }
                                }
                                // Neither good nor bad - missed commitment.
                                if (member.getRole().equalsIgnoreCase(CommitteeRoleEnum.WORKER.getName())) {
                                    saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.PRIMARY_MISSED);
                                }
                                if (roundDiscrepancy && member.getRole().equalsIgnoreCase(CommitteeRoleEnum.BACKUPWORKER.getName())) {
                                    saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.BCKP_MISSED);
                                }
                            }
                        }
                    }
                }

                if (headerType == RuntimeHeaderTypeEnum.Suspended.getCode()) {
                    log.info(String.format("runtime stats: %s, %s", runtimeId, scanHeight));
                    scanHeight++;
                    continue;
                }

                if (curRound != runtimeRound.getHeader().getRound()) {
                    curRound = runtimeRound.getHeader().getRound();

                    RuntimeState runtimeState = apiClient.roothashRuntimeState(runtimeId, scanHeight);
                    if (runtimeState == null) {
                        throw new RuntimeException(String.format("Runtime state api error. %s", scanHeight));
                    }
                    RuntimeState.ExecutorPool executorPool = runtimeState.getExecutor_pool();
                    if (executorPool == null) {
                        log.info(String.format("runtime stats: %s, %s", runtimeId, scanHeight));
                        scanHeight++;
                        continue;
                    }

                    committee = executorPool.getCommittee();
                    currentScheduler = getTransactionScheduler(committee.getMembers(), curRound);
                    roundDiscrepancy = false;
                }

                // Update election stats.
                Set<String> seen = Sets.newHashSet();
                if (committee != null) {
                    for (RuntimeState.Member member : committee.getMembers()) {
                        String pubKey = member.getPublic_key();
                        String entityId = nodeToEntity.get(pubKey);
                        if (Texts.isBlank(entityId)) {
                            throw new RuntimeException(String.format("Entity id not found. %s, %s", pubKey, scanHeight));
                        }

                        if (!seen.contains(pubKey)) {
                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.ELECTED);
                        }
                        seen.add(pubKey);

                        if (member.getRole().equalsIgnoreCase(CommitteeRoleEnum.WORKER.getName())) {
                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.PRIMARY);
                        }
                        if (member.getRole().equalsIgnoreCase(CommitteeRoleEnum.BACKUPWORKER.getName())) {
                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.BACKUP);
                        }
                        if (currentScheduler != null && pubKey.equalsIgnoreCase(currentScheduler.getPublic_key())) {
                            saveRuntimeStats(runtimeId, scanHeight, curRound, entityId, RuntimeStatsType.PROPOSER);
                        }
                    }
                }

                log.info(String.format("runtime stats: %s, %s", runtimeId, scanHeight));
                scanHeight++;
            }
        }
    }

    @Scheduled(fixedDelay = 30 * 1000, initialDelay = 30 * 1000)
    public void scanRuntimeStatsInfo() {
        if (applicationConfig.isLocal()) {
            return;
        }

        List<romever.scan.oasisscan.entity.Runtime> runtimes = runtimeRepository.findAllByOrderByStartRoundHeightAsc();
        if (CollectionUtils.isEmpty(runtimes)) {
            return;
        }

        for (romever.scan.oasisscan.entity.Runtime runtime : runtimes) {
            List<String> entities = runtimeStatsRepository.entities(runtime.getRuntimeId());
            if (CollectionUtils.isEmpty(entities)) {
                continue;
            }
            RuntimeStatsType[] types = RuntimeStatsType.class.getEnumConstants();
            for (String entity : entities) {
                List statsList = runtimeStatsRepository.statsByRuntimeId(runtime.getRuntimeId(), entity);
                if (CollectionUtils.isEmpty(statsList)) {
                    continue;
                }
                for (Object row : statsList) {
                    Object[] cells = (Object[]) row;
                    for (int i = 0; i < types.length; i++) {
                        RuntimeStatsType type = types[i];
                        if (i == (Integer) cells[0]) {
                            long count = ((BigInteger) cells[1]).longValue();

                            RuntimeStatsInfo statsInfo;
                            Optional<RuntimeStatsInfo> optional = runtimeStatsInfoRepository.findByRuntimeIdAndEntityIdAndStatsType(runtime.getRuntimeId(), entity, type);
                            if (optional.isPresent()) {
                                statsInfo = optional.get();
                            } else {
                                statsInfo = new RuntimeStatsInfo();
                                statsInfo.setRuntimeId(runtime.getRuntimeId());
                                statsInfo.setEntityId(entity);
                            }
                            statsInfo.setStatsType(type);
                            statsInfo.setCount(count);
                            runtimeStatsInfoRepository.save(statsInfo);

                            break;
                        }
                    }
                }
                log.info(String.format("runtime stats info: %s,%s", runtime.getRuntimeId(), entity));
            }
        }
    }

    private RuntimeState.Member getTransactionScheduler(List<RuntimeState.Member> members, long round) {
        List<RuntimeState.Member> workers = workers(members);
        int numNodes = workers.size();
        if (numNodes == 0) {
            return null;
        }
        long schedulerIdx = round % numNodes;
        return workers.get((int) schedulerIdx);
    }

    private List<RuntimeState.Member> workers(List<RuntimeState.Member> members) {
        List<RuntimeState.Member> wokers = Lists.newArrayList();
        for (RuntimeState.Member member : members) {
            String role = member.getRole();
            if (role.equalsIgnoreCase(CommitteeRoleEnum.WORKER.getName())) {
                wokers.add(member);
            }
        }
        return wokers;
    }

    private void saveRuntimeStats(String runtimeId, long scanHeight, long round, String entityId, RuntimeStatsType statsType) {
        Optional<RuntimeStats> optional = runtimeStatsRepository.findByRuntimeIdAndEntityIdAndRoundAndStatsType(runtimeId, entityId, round, statsType);
        RuntimeStats stats;
        if (optional.isPresent()) {
            stats = optional.get();
            stats.setHeight(scanHeight);
        } else {
            stats = new RuntimeStats();
            stats.setRuntimeId(runtimeId);
            stats.setEntityId(entityId);
            stats.setHeight(scanHeight);
            stats.setRound(round);
            stats.setStatsType(statsType);
        }
        runtimeStatsRepository.save(stats);
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
