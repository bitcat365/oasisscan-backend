package romever.scan.oasisscan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountResponse;
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
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.DataAccess;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.*;
import romever.scan.oasisscan.repository.*;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.utils.Times;
import romever.scan.oasisscan.utils.okhttp.OkHttp;
import romever.scan.oasisscan.vo.chain.*;
import romever.scan.oasisscan.vo.chain.Debonding;
import romever.scan.oasisscan.vo.git.Content;
import romever.scan.oasisscan.vo.git.EntityInfo;
import romever.scan.oasisscan.vo.git.EntityRawInfo;

import javax.xml.soap.Text;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static romever.scan.oasisscan.common.ESFields.*;

@Service
@Slf4j
public class ScanValidatorService {

    @Autowired
    private ApplicationConfig applicationConfig;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private DelegatorRepository delegatorRepository;
    @Autowired
    private ValidatorConsensusRepository validatorConsensusRepository;
    @Autowired
    private EscrowStatsRepository escrowStatsRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DebondingRepository debondingRepository;
    @Autowired
    private SystemPropertyRepository systemPropertyRepository;

    @Autowired
    private DataAccess dataAccess;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ScanChainService scanChainService;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    private static final int SIGN_SCORE = 1;
    private static final int PROPOSAL_SCORE = 2;

    /**
     * Sync validators chain state
     *
     * @throws IOException
     */
    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void syncValidators() throws IOException {
        if (applicationConfig.isLocal()) {
            return;
        }
        long storeHeight = scanChainService.getStoreHeight();
        List<ValidatorInfo> validatorInfos = validatorInfoRepository.findByOrderByEscrowDesc();
        if (!CollectionUtils.isEmpty(validatorInfos)) {
            long curHeight = apiClient.getCurHeight();
            long currentEpoch = apiClient.epoch(curHeight);
            RegistryGenesis registryGenesis = apiClient.registryGenesis(null);
            List<ValidatorInfo> list = Lists.newArrayList();

            Set<String> tmNodes = Sets.newHashSet();
            GetResponse getResponse = JestDao.get(elasticsearchClient, elasticsearchConfig.getBlockIndex(), String.valueOf(storeHeight));
            if (getResponse.isExists()) {
                Block block = Mappers.parseJson(getResponse.getSourceAsString(), new TypeReference<Block>() {
                });
                if (block != null) {
                    List<Block.MetaData.Signature> signatures = block.getMetadata().getLast_commit().getSignatures();
                    if (!CollectionUtils.isEmpty(signatures)) {
                        for (Block.MetaData.Signature signature : signatures) {
                            if (Texts.isNotBlank(signature.getValidator_address())) {
                                tmNodes.add(signature.getValidator_address());
                            }
                        }
                    }
                }
            }


            for (ValidatorInfo info : validatorInfos) {
                //init
                info.setNodes(0);
                info.setSigns(0);
                info.setProposals(0);
                info.setSignsUptime(0);
                info.setEscrow_24h("0");

                if (registryGenesis != null) {
                    Map<String, NodeStatus> nodeStatusMap = registryGenesis.getNode_statuses();
                    if (!CollectionUtils.isEmpty(nodeStatusMap)) {
                        NodeStatus nodeStatus = nodeStatusMap.get(info.getNodeId());
                        if (nodeStatus != null) {
                            info.setStatus(!nodeStatus.isExpiration_processed());
                        }
                    }
                }

                String entityId = info.getEntityId();
                String entityAddress = apiClient.pubkeyToBech32Address(entityId);
                if (Texts.isBlank(entityAddress)) {
                    throw new RuntimeException(String.format("address parse failed, %s", entityId));
                }
                AccountInfo v = apiClient.accountInfo(entityAddress, null);
                if (v != null) {
                    AccountInfo.Escrow vEscrow = v.getEscrow();
                    if (vEscrow != null) {
                        AccountInfo.Active vActive = vEscrow.getActive();
                        if (vActive != null) {
                            info.setEscrow(vActive.getBalance());
                            info.setTotalShares(vActive.getTotal_shares());
                        }

                        AccountInfo.CommissionSchedule commissionSchedule = vEscrow.getCommission_schedule();
                        if (commissionSchedule != null) {
                            //rate
                            List<AccountInfo.Rate> rates = commissionSchedule.getRates();
                            if (!CollectionUtils.isEmpty(rates)) {
                                Comparator<AccountInfo.Rate> comparator = (c1, c2) -> Long.compare(c2.getStart(), c1.getStart());
                                rates.sort(comparator);
                                for (AccountInfo.Rate rate : rates) {
                                    if (currentEpoch >= rate.getStart()) {
                                        info.setCommission(Long.parseLong(rate.getRate()));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    info.setBalance(v.getGeneral().getBalance());
                }

                List<String> tmAddressList = Lists.newArrayList();
                List<ValidatorConsensus> consensusList = validatorConsensusRepository.findByEntityId(entityId);
                if (!CollectionUtils.isEmpty(consensusList)) {
                    for (ValidatorConsensus consensus : consensusList) {
                        String tmAddress = Texts.hexToBase64(apiClient.pubkeyToTendermintAddress(consensus.getConsensusId()));
                        tmAddressList.add(tmAddress);

//                        if (tmNodes.contains(tmAddress)) {
//                            info.setNodes(1);
//                        }
                    }
                    info.setProposals(statsCount(tmAddressList, true));
                    info.setSigns(statsCount(tmAddressList, false));
                    long statsCount = statsCount(tmAddressList, false, storeHeight - Constants.UPTIME_HEIGHT);
                    info.setSignsUptime(statsCount);
                    if (statsCount > 0) {
                        info.setNodes(1);
                    }
                }

                info.setScore(info.getSigns() * SIGN_SCORE + info.getProposals() * PROPOSAL_SCORE);

                AccountInfo accountInfo = apiClient.accountInfo(entityAddress, curHeight - Constants.ONE_DAY_HEIGHT);
                if (accountInfo != null) {
                    AccountInfo.Escrow escrow = accountInfo.getEscrow();
                    if (escrow != null) {
                        AccountInfo.Active active = escrow.getActive();
                        if (active != null) {
                            long lastEscrow = Long.parseLong(active.getBalance());
                            long change = Long.parseLong(info.getEscrow()) - lastEscrow;
                            info.setEscrow_24h(String.valueOf(change));
                        }
                    }
                }

                list.add(info);
            }
            if (!CollectionUtils.isEmpty(list)) {
                validatorInfoRepository.saveAll(list);
            }
            log.info("sync validators done, height: [{}]", storeHeight);
        }
    }

    /**
     * Sync entity and node relation from oasis api
     */
    @Scheduled(fixedDelay = 15 * 1000, initialDelay = 10 * 1000)
    public void syncEntityNode() {
        if (applicationConfig.isLocal()) {
            return;
        }
        log.info("sync entity node start...");
//        long dbHeight = dataAccess.getDbHeight();
        long dbHeight = getDbHeight();
        long curHeight = apiClient.getCurHeight();
        RegistryGenesis registryGenesis = apiClient.registryGenesis(dbHeight);
        while (dbHeight <= curHeight) {
            List<Node> nodes = apiClient.registryNodes(dbHeight);
            if (!CollectionUtils.isEmpty(nodes)) {
                try {
                    saveValidator(dbHeight, nodes, registryGenesis);
                } catch (Exception e) {
                    break;
                }
            }

            SystemProperty systemProperty = systemPropertyRepository.findByProperty(Constants.DB_HEIGHT_PROPERTY).orElse(new SystemProperty());
            systemProperty.setProperty(Constants.DB_HEIGHT_PROPERTY);
            systemProperty.setValue(String.valueOf(dbHeight));
            systemPropertyRepository.saveAndFlush(systemProperty);
            dbHeight++;
        }
    }

    /**
     * Sync validator base info from github and keybase
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncValidatorInfo() {
        if (applicationConfig.isLocal()) {
            return;
        }
        //from github
        String gitUrl = applicationConfig.getValidator().getGitInfo();
        List<Content> gitContents = OkHttp.of(gitUrl).exec(new TypeReference<List<Content>>() {
        });
        if (CollectionUtils.isEmpty(gitContents)) {
            return;
        }

        for (Content gitContent : gitContents) {
            String name = gitContent.getName();
            if (!name.endsWith(".json")) {
                continue;
            }

            String validatorHex = name.replace(".json", "");
            String entityId = Texts.hexToBase64(validatorHex);
            if (Texts.isBlank(entityId)) {
                continue;
            }

            Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityId(entityId);
            if (!optional.isPresent()) {
                continue;
            }

            EntityRawInfo rawInfo = OkHttp.of(gitContent.getDownload_url()).exec(new TypeReference<EntityRawInfo>() {
            });
            if (rawInfo == null) {
                continue;
            }

            EntityInfo entityInfo = Mappers.parseCborFromBase64(rawInfo.getUntrusted_raw_value(), new TypeReference<EntityInfo>() {
            });
            if (entityInfo == null) {
                return;
            }

            //get icon from keybase
            String keybaseId = entityInfo.getKeybase();
            String icon = "";
            try {
                String keybaseUrl = applicationConfig.getValidator().getKeybaseJson();
                String keybaseResult = OkHttp.of(keybaseUrl).query("usernames", keybaseId).exec();
                if (Texts.isNotBlank(keybaseResult)) {
                    JsonNode json = Mappers.parseJson(keybaseResult);
                    icon = json.path("them").path(0).path("pictures").path("primary").path("url").asText();
                }
            } catch (Exception e) {
                log.error("", e);
            }


            ValidatorInfo validatorInfo = optional.get();
            validatorInfo.setName(entityInfo.getName());
            validatorInfo.setEmail(entityInfo.getEmail());
            validatorInfo.setKeybase(keybaseId);
            validatorInfo.setWebsite(entityInfo.getUrl());
            validatorInfo.setTwitter(entityInfo.getTwitter());
            if (Texts.isNotBlank(icon)) {
                validatorInfo.setIcon(icon);
            }
            validatorInfoRepository.save(validatorInfo);
        }
        log.info("validator base info sync done.");
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveValidator(long dbHeight, List<Node> nodes, RegistryGenesis registryGenesis) {
        List<ValidatorConsensus> consensusList = Lists.newArrayList();
        List<ValidatorInfo> validatorInfoList = Lists.newArrayList();
        Map<String, NodeStatus> nodeStatus = registryGenesis.getNode_statuses();
        for (Node node : nodes) {
            String entityId = node.getEntity_id();
            String nodeId = node.getId();
            String consensusId = node.getConsensus().getId();
            if (!nodeStatus.containsKey(nodeId)) {
                continue;
            }

            Optional<ValidatorConsensus> consensusOptional =
                    validatorConsensusRepository.findByEntityIdAndNodeIdAndConsensusId(entityId, nodeId, consensusId);
            ValidatorConsensus validatorConsensus = consensusOptional.orElse(new ValidatorConsensus());
            if (!consensusOptional.isPresent()) {
                validatorConsensus.setEntityId(entityId);
                validatorConsensus.setNodeId(nodeId);
                validatorConsensus.setConsensusId(consensusId);
                String tmAddress = Texts.hexToBase64(apiClient.pubkeyToTendermintAddress(consensusId));
                validatorConsensus.setTmAddress(tmAddress);
            }
            validatorConsensus.setHeight(dbHeight);
            consensusList.add(validatorConsensus);

            //validator info
            Optional<ValidatorInfo> optionalValidatorInfo = validatorInfoRepository.findByEntityId(entityId);
            ValidatorInfo validatorInfo;
            if (optionalValidatorInfo.isPresent()) {
                validatorInfo = optionalValidatorInfo.get();
                if (!nodeId.equals(validatorInfo.getNodeId())) {
                    validatorInfo.setNodeId(nodeId);
                    validatorInfo.setNodeAddress(apiClient.pubkeyToBech32Address(nodeId));
                    validatorInfo.setConsensusId(consensusId);
                    String tmAddress = Texts.hexToBase64(apiClient.pubkeyToTendermintAddress(consensusId));
                    validatorInfo.setTmAddress(tmAddress);

                    validatorInfoList.add(validatorInfo);
                }
            } else {
                validatorInfo = new ValidatorInfo();
                validatorInfo.setEntityId(entityId);
                validatorInfo.setEntityAddress(apiClient.pubkeyToBech32Address(entityId));
                validatorInfo.setNodeId(nodeId);
                validatorInfo.setNodeAddress(apiClient.pubkeyToBech32Address(nodeId));
                validatorInfo.setConsensusId(consensusId);
                String tmAddress = Texts.hexToBase64(apiClient.pubkeyToTendermintAddress(consensusId));
                validatorInfo.setTmAddress(tmAddress);

                validatorInfoList.add(validatorInfo);
            }
        }
        if (!CollectionUtils.isEmpty(consensusList)) {
            validatorConsensusRepository.saveAll(consensusList);
        }
        if (!CollectionUtils.isEmpty(validatorInfoList)) {
            validatorInfoRepository.saveAll(validatorInfoList);
        }
        log.info("sync entity node done, height: [{}]", dbHeight);
    }

    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 15 * 1000)
    public void syncGenesis() {
        if (applicationConfig.isLocal()) {
            return;
        }
        log.info("genesis sync start...");
        StakingGenesis stakingGenesis = apiClient.stakingGenesis(null);
        if (stakingGenesis != null) {

            syncDelegator(stakingGenesis);
            syncDebonding(stakingGenesis);

            syncAccounts(stakingGenesis);
        }
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 5 * 1000)
    public void syncEscrowStats() {
        if (applicationConfig.isLocal()) {
            return;
        }
        LocalDateTime nowTime = Times.nowUtcDateTime();
        String nowStr = Times.format(nowTime, Times.DATE_FORMATTER);
        String dateSave = null;
        Optional<EscrowStats> optional = escrowStatsRepository.findFirstByOrderByHeightDesc();
        if (optional.isPresent()) {
            EscrowStats escrowStats = optional.get();
            String dateStr = escrowStats.getDate();
            LocalDateTime date = Times.parseDay(dateStr);
            dateSave = Times.format(date.plusDays(1), Times.DATE_FORMATTER);
        } else {
            dateSave = Times.format(nowTime.minusDays(Constants.ESCROW_STATS_LIMIT), Times.DATE_FORMATTER);
        }

        if (Texts.isNotBlank(dateSave)) {
            while (dateSave.compareTo(nowStr) <= 0) {
                List<EscrowStats> saveList = Lists.newArrayList();
                LocalDateTime date = Times.parseDay(dateSave);
                Long height = getHeightByTime(Times.toEpochSecond(date));
                if (height != null) {
                    List<ValidatorInfo> validatorInfos = validatorInfoRepository.findByOrderByEscrowDesc();
                    for (ValidatorInfo validatorInfo : validatorInfos) {
                        String address = validatorInfo.getEntityAddress();
                        String escrow = getEscrowByHeight(address, height);
                        if (Texts.isNotBlank(escrow)) {
                            EscrowStats stats = escrowStatsRepository.findByEntityAddressAndHeight(address, height).orElse(new EscrowStats());
                            stats.setEntityAddress(address);
                            stats.setHeight(height);
                            stats.setDate(dateSave);
                            stats.setEscrow(escrow);
                            saveList.add(stats);
                        }
                    }
                }
                if (!CollectionUtils.isEmpty(saveList)) {
                    escrowStatsRepository.saveAll(saveList);
                    log.info("escrow stats save done. {}", dateSave);
                }
                date = date.plusDays(1);
                dateSave = Times.format(date, Times.DATE_FORMATTER);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncDelegator(StakingGenesis stakingGenesis) {
        Map<String, Map<String, Delegations>> delegations = stakingGenesis.getDelegations();
        if (!CollectionUtils.isEmpty(delegations)) {
            List<Delegator> saveList = Lists.newArrayList();
            for (Map.Entry<String, Map<String, Delegations>> entry : delegations.entrySet()) {
                String validator = entry.getKey();
                Map<String, Delegations> delegationsMap = entry.getValue();
                if (!CollectionUtils.isEmpty(delegationsMap)) {
                    for (Map.Entry<String, Delegations> delegationsEntry : delegationsMap.entrySet()) {
                        String delegatorId = delegationsEntry.getKey();
                        Delegator delegator = delegatorRepository.findByValidatorAndDelegator(validator, delegatorId).orElse(new Delegator());
                        String shares = delegationsEntry.getValue().getShares();
                        delegator.setValidator(validator);
                        delegator.setDelegator(delegatorId);
                        delegator.setShares(shares);
                        saveList.add(delegator);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(saveList)) {
                delegatorRepository.deleteAll();
                delegatorRepository.saveAll(saveList);
                log.info("delegators sync done, size: {}", saveList.size());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncDebonding(StakingGenesis stakingGenesis) {
        Map<String, Map<String, List<Debonding>>> debondings = stakingGenesis.getDebonding_delegations();
        if (!CollectionUtils.isEmpty(debondings)) {
            List<romever.scan.oasisscan.entity.Debonding> saveList = Lists.newArrayList();
            for (Map.Entry<String, Map<String, List<Debonding>>> entry : debondings.entrySet()) {
                String validator = entry.getKey();
                Map<String, List<Debonding>> debondingMap = entry.getValue();
                if (!CollectionUtils.isEmpty(debondingMap)) {
                    for (Map.Entry<String, List<Debonding>> debondingEntry : debondingMap.entrySet()) {
                        String delegator = debondingEntry.getKey();
                        List<Debonding> debondingList = debondingEntry.getValue();
                        if (!CollectionUtils.isEmpty(debondingMap)) {
                            for (Debonding debonding : debondingList) {
                                romever.scan.oasisscan.entity.Debonding s = new romever.scan.oasisscan.entity.Debonding();
                                s.setValidator(validator);
                                s.setDelegator(delegator);
                                s.setShares(debonding.getShares());
                                s.setDebondEnd(debonding.getDebond_end());
                                saveList.add(s);
                            }
                        }
                    }
                }
            }
            if (!CollectionUtils.isEmpty(saveList)) {
                debondingRepository.deleteAll();
                debondingRepository.saveAll(saveList);
                log.info("debonding sync done, size: {}", saveList.size());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncAccounts(StakingGenesis stakingGenesis) {
        Map<String, AccountInfo> accountInfoMap = stakingGenesis.getLedger();
        if (!CollectionUtils.isEmpty(accountInfoMap)) {
            List<Account> saveList = Lists.newArrayList();
            for (Map.Entry<String, AccountInfo> entry : accountInfoMap.entrySet()) {
                String address = entry.getKey();
                AccountInfo accountInfo = entry.getValue();

                Account account = accountRepository.findByAddress(address).orElse(new Account());
                account.setAddress(address);
                account.setAvailable(Long.parseLong(accountInfo.getGeneral().getBalance()));
                List<Delegator> delegations = delegatorRepository.findByDelegator(address);
                double totalEscrow = 0;
                if (!CollectionUtils.isEmpty(delegations)) {
                    for (Delegator d : delegations) {
                        Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityAddress(d.getValidator());
                        double shares = Double.parseDouble(Texts.formatDecimals(d.getShares(), Constants.DECIMALS, 9));
                        if (optional.isPresent()) {
                            ValidatorInfo validatorInfo = optional.get();
                            //tokens = shares * balance / total_shares
                            double totalShares = Double.parseDouble(Texts.formatDecimals(validatorInfo.getTotalShares(), Constants.DECIMALS, 9));
                            double escrow = Double.parseDouble(Texts.formatDecimals(validatorInfo.getEscrow(), Constants.DECIMALS, 9));
                            double amount = Numeric.divide(Numeric.multiply(shares, escrow), totalShares, 9);
                            totalEscrow = Numeric.add(amount, totalEscrow);
                        } else {
                            totalEscrow = Numeric.add(shares, totalEscrow);
                        }
                    }
                }
                String debonding = debondingRepository.sumDelegatorDebonding(address);
                if (Texts.isBlank(debonding)) {
                    debonding = "0";
                }
                account.setEscrow((long) (totalEscrow * Math.pow(10, 9)));
                account.setDebonding(Long.parseLong(debonding));
                account.setTotal(account.getAvailable() + account.getEscrow() + account.getDebonding());
                saveList.add(account);
            }
            accountRepository.saveAll(saveList);
            log.info("account sync done, size: {}", saveList.size());
        }
    }

    public long statsCount(List<String> tmAddressList, boolean proposal) {
        return statsCount(tmAddressList, proposal, null);
    }

    public long statsCount(List<String> tmAddressList, boolean proposal, Long from) {
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            if (proposal) {
                boolQueryBuilder.filter(QueryBuilders.termsQuery(BLOCK_PROPOSER_ADDRESS, tmAddressList));
            } else {
                boolQueryBuilder.filter(QueryBuilders.termsQuery(BLOCK_SIGN_ADDRESS, tmAddressList));
            }
            if (from != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery(BLOCK_HEIGHT)
                        .from(from, false)
                        .to(from + Constants.UPTIME_HEIGHT, true));
            }
            CountResponse countResponse = JestDao.count(elasticsearchClient, elasticsearchConfig.getBlockIndex(), boolQueryBuilder);
            return countResponse.getCount();
        } catch (IOException e) {
            log.error("error", e);
        }
        return 0;
    }

    public Long getHeightByTime(long timestamp) {
        Long height = null;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.rangeQuery(BLOCK_TIMESTAMP)
                .from(timestamp, true)
                .to(timestamp + 24 * 60 * 60, false));
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.fetchSource(BLOCK_HEIGHT, null);
        searchSourceBuilder.sort(BLOCK_HEIGHT, SortOrder.ASC);
        searchSourceBuilder.size(1);
        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    Block block = Mappers.parseJson(hit.getSourceAsString(), Block.class).orElse(null);
                    if (block != null) {
                        height = block.getHeight();
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }
        return height;
    }

    public String getEscrowByHeight(String address, Long height) {
        String ret = null;
        AccountInfo accountInfo = apiClient.accountInfo(address, height);
        if (accountInfo != null) {
            AccountInfo.Escrow escrow = accountInfo.getEscrow();
            if (escrow != null) {
                AccountInfo.Active active = escrow.getActive();
                if (active != null) {
                    ret = active.getBalance();
                }
            }
        }
        return ret;
    }

    public Long getDbHeight() {
        Long latestHeight = null;
        Optional<SystemProperty> optional = systemPropertyRepository.findByProperty(Constants.DB_HEIGHT_PROPERTY);
        if (optional.isPresent()) {
            SystemProperty property = optional.get();
            latestHeight = Long.parseLong(property.getValue());
        }
        return latestHeight;
    }
}
