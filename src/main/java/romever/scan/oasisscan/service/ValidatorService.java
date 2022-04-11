package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Lists;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.ElasticsearchConfig;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.db.DataAccess;
import romever.scan.oasisscan.db.JestDao;
import romever.scan.oasisscan.entity.*;
import romever.scan.oasisscan.repository.DelegatorRepository;
import romever.scan.oasisscan.repository.EscrowStatsRepository;
import romever.scan.oasisscan.repository.ValidatorConsensusRepository;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.utils.Times;
import romever.scan.oasisscan.vo.SortEnum;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.Block;
import romever.scan.oasisscan.vo.chain.Delegations;
import romever.scan.oasisscan.vo.response.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static romever.scan.oasisscan.common.ESFields.*;

@Slf4j
@Service
public class ValidatorService {

    @Autowired
    private DataAccess dataAccess;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private ValidatorConsensusRepository validatorConsensusRepository;
    @Autowired
    private DelegatorRepository delegatorRepository;
    @Autowired
    private EscrowStatsRepository escrowStatsRepository;
    @Autowired
    private ValidatorService validatorService;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    private ScanChainService scanChainService;
    @Autowired
    private ElasticsearchConfig elasticsearchConfig;

    private static final int STAT_LIMIT = 100;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public NetworkInfo networkInfo() {
        NetworkInfo networkInfo = dataAccess.networkInfo();
        if (networkInfo != null) {
            Long height = null;
            try {
                height = apiClient.getCurHeight();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (height == null) {
                height = scanChainService.getEsHeight();
            }
            networkInfo.setHeight(height);
            if (networkInfo.getEscrow() != null) {
                networkInfo.setEscrow(Texts.formatDecimals(networkInfo.getEscrow(), Constants.DECIMALS, 2));
            }
        }
        return networkInfo;
    }

    @Cached(expire = 60 * 2, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    @CacheRefresh(refresh = 30, timeUnit = TimeUnit.SECONDS)
    public ValidatorResponse validatorInfo(String entityId, String address) {
        ValidatorResponse response = new ValidatorResponse();
        try {
            //        Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityId(entityId);
            ValidatorInfoRank info = dataAccess.getValidatorInfo(entityId, address);
            long currentEpoch = apiClient.epoch(null);
            if (info != null) {
                response = formatValidator(info);
            } else {
                return null;
            }
            if (Texts.isBlank(address)) {
                address = apiClient.pubkeyToBech32Address(entityId);
                if (Texts.isBlank(address)) {
                    throw new RuntimeException(String.format("address parse failed, %s", entityId));
                }
            }
            AccountInfo accountInfo = apiClient.accountInfo(address, null);
            if (accountInfo != null) {
                AccountInfo.General general = accountInfo.getGeneral();
                if (general != null) {
                    response.setNonce(general.getNonce());
                }
                AccountInfo.Escrow escrow = accountInfo.getEscrow();
                if (escrow != null) {
                    AccountInfo.Active active = escrow.getActive();
                    if (active != null) {
                        response.setEscrow(Texts.formatDecimals(active.getBalance(), Constants.DECIMALS, 2));
                        response.setTotalShares(Texts.formatDecimals(active.getTotal_shares(), Constants.DECIMALS, 2));
                    }

                    AccountInfo.CommissionSchedule commissionSchedule = escrow.getCommission_schedule();
                    if (commissionSchedule != null) {
                        //bounds
                        List<ValidatorResponse.Bound> boundsResponse = Lists.newArrayList();
                        List<AccountInfo.Bound> bounds = commissionSchedule.getBounds();
                        if (!CollectionUtils.isEmpty(bounds)) {
                            for (AccountInfo.Bound bound : bounds) {
                                ValidatorResponse.Bound boundResponse = new ValidatorResponse.Bound();
                                boundResponse.setStart(bound.getStart());
                                boundResponse.setMin(Numeric.divide(Double.parseDouble(bound.getRate_min()), Constants.RATE_DECIMALS, 4));
                                boundResponse.setMax(Numeric.divide(Double.parseDouble(bound.getRate_max()), Constants.RATE_DECIMALS, 4));
                                if (bound.getStart() <= currentEpoch) {
                                    if (boundsResponse.size() == 0) {
                                        boundsResponse.add(boundResponse);
                                    } else {
                                        boundsResponse.set(0, boundResponse);
                                    }
                                } else if (bound.getStart() > currentEpoch && boundsResponse.size() < 5) {
                                    boundsResponse.add(boundResponse);
                                } else {
                                    break;
                                }
                            }
                            response.setBound(boundsResponse.get(0));
                        }
                        response.setBounds(boundsResponse);

                        //rate
                        List<ValidatorResponse.Rate> ratesResponse = Lists.newArrayList();
                        List<AccountInfo.Rate> rates = commissionSchedule.getRates();
                        if (!CollectionUtils.isEmpty(rates)) {
                            for (AccountInfo.Rate rate : rates) {
                                ValidatorResponse.Rate rateResponse = new ValidatorResponse.Rate();
                                rateResponse.setStart(rate.getStart());
                                rateResponse.setRate(Numeric.divide(Double.parseDouble(rate.getRate()), Constants.RATE_DECIMALS, 4));
                                if (rate.getStart() <= currentEpoch) {
                                    if (ratesResponse.size() == 0) {
                                        ratesResponse.add(rateResponse);
                                    } else {
                                        ratesResponse.set(0, rateResponse);
                                    }
                                } else if (rate.getStart() > currentEpoch && ratesResponse.size() < 5) {
                                    ratesResponse.add(rateResponse);
                                } else {
                                    break;
                                }
                            }
                            response.setCommission(ratesResponse.get(0).getRate());
                        }
                        response.setRates(ratesResponse);
                    }
                }
            }

            double selfShares = 0;
            Map<String, Delegations> delegationsMap = apiClient.delegations(address, null);
            if (!CollectionUtils.isEmpty(delegationsMap)) {
                for (Map.Entry<String, Delegations> entry : delegationsMap.entrySet()) {
                    String entity = entry.getKey();
                    Delegations delegations = entry.getValue();
                    double shares = Double.parseDouble(Texts.formatDecimals(delegations.getShares(), Constants.DECIMALS, 2));
                    if (entity.equals(address)) {
                        selfShares = Numeric.add(selfShares, shares);
                        break;
                    }
                }
            }
            //tokens = shares * balance / total_shares
            double balance = Double.parseDouble(response.getEscrow());
            double totalShares = Double.parseDouble(response.getTotalShares());
            double otherShares = Numeric.subtract(totalShares, selfShares);
            double selfAmount = 0;
            double otherAmount = 0;
            if (totalShares > 0) {
                selfAmount = Numeric.divide(Numeric.multiply(selfShares, balance), totalShares, 2);
                otherAmount = Numeric.divide(Numeric.multiply(otherShares, balance), totalShares, 2);
            }

            ValidatorResponse.EscrowStatus escrowSharesStatus = new ValidatorResponse.EscrowStatus();
            escrowSharesStatus.setSelf(Numeric.formatDouble(selfShares));
            escrowSharesStatus.setOther(Numeric.formatDouble(otherShares));
            escrowSharesStatus.setTotal(Numeric.formatDouble(Numeric.add(selfShares, otherShares)));
            ValidatorResponse.EscrowStatus escrowAmountStatus = new ValidatorResponse.EscrowStatus();
            escrowAmountStatus.setSelf(Numeric.formatDouble(selfAmount));
            escrowAmountStatus.setOther(Numeric.formatDouble(otherAmount));
            escrowAmountStatus.setTotal(Numeric.formatDouble(Numeric.add(selfAmount, otherAmount)));
            response.setEscrowSharesStatus(escrowSharesStatus);
            response.setEscrowAmountStatus(escrowAmountStatus);
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
        return response;
    }

    @Cached(expire = 60 * 2, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    @CacheRefresh(refresh = 30, timeUnit = TimeUnit.SECONDS)
    public ValidatorListResponse validators(SortEnum orderBy, String sort, int page, int pageSize) {
        ValidatorListResponse validatorListResponse = new ValidatorListResponse();
        List<ValidatorResponse> list = Lists.newArrayList();
//        List<ValidatorInfo> validatorInfoList = validatorInfoRepository.findByOrderByEscrowDesc();
        List<ValidatorInfoRank> validatorInfoList = dataAccess.getValidatorList();

        int active = 0;
        if (!CollectionUtils.isEmpty(validatorInfoList)) {
            NetworkInfo networkInfo = validatorService.networkInfo();
            double totalEscrow = 0;
            if (networkInfo != null) {
                totalEscrow = Double.parseDouble(networkInfo.getEscrow());
            }

            for (ValidatorInfoRank info : validatorInfoList) {
                ValidatorResponse response = formatValidator(info);
                if (response.isActive()) {
                    active++;
                }
                if (totalEscrow != 0) {
                    response.setEscrowPercent(Numeric.divide(Double.parseDouble(response.getEscrow()), totalEscrow, 4));
                }
                list.add(response);
            }

            Comparator<ValidatorResponse> comparator = null;
            switch (orderBy) {
                case SCORE:
                    comparator = (c1, c2) -> Long.compare(c2.getScore(), c1.getScore());
                    break;
                case SIGNS:
                    comparator = (c1, c2) -> Long.compare(c2.getSigns(), c1.getSigns());
                    break;
                case PROPOSALS:
                    comparator = (c1, c2) -> Long.compare(c2.getProposals(), c1.getProposals());
                    break;
                case ESCROW:
                    comparator = (c1, c2) -> Double.compare(Double.parseDouble(c2.getEscrow()), Double.parseDouble(c1.getEscrow()));
                    break;
                case UPTIME:
                    comparator = (c1, c2) -> Integer.compare(Integer.parseInt(c2.getUptime().replace("%", "")), Integer.parseInt(c1.getUptime().replace("%", "")));
                    break;
                case CHANGE:
                    comparator = (c1, c2) -> Double.compare(Double.parseDouble(c2.getEscrowChange24()), Double.parseDouble(c1.getEscrowChange24()));
                    break;
                case DELEGATOR:
                    comparator = (c1, c2) -> Long.compare(c2.getDelegators(), c1.getDelegators());
                    break;
                case COMMISSION:
                    comparator = (c1, c2) -> Double.compare(c2.getCommission(), c1.getCommission());
                    break;
                default:
                    comparator = Comparator.comparingInt(ValidatorResponse::getRank);
                    break;
            }
            if ("asc".equalsIgnoreCase(sort)) {
                list.sort(comparator.reversed());
            } else {
                list.sort(comparator);
            }
            if (pageSize > list.size()) {
                pageSize = list.size();
            }
            list = list.subList(0, pageSize);
        }
        validatorListResponse.setList(list);
        validatorListResponse.setActive(active);
        validatorListResponse.setInactive(list.size() - active);
        validatorListResponse.setDelegators(delegatorRepository.countDistinctDelegator());
        return validatorListResponse;
    }

    private ValidatorResponse formatValidator(ValidatorInfoRank info) {
        ValidatorResponse response = new ValidatorResponse();
        response.setEntityId(info.getEntityId());
        response.setRank(info.getRank());
        response.setName(info.getName());
        response.setEntityAddress(info.getEntityAddress());
        response.setNodeId(info.getNodeId());
        response.setNodeAddress(info.getNodeAddress());
        response.setIcon(info.getIcon());
        response.setWebsite(info.getWebsite());
        response.setTwitter(info.getTwitter());
        response.setKeybase(info.getKeybase());
        response.setEmail(info.getEmail());
        response.setDescription(info.getDescription());
        response.setBalance(Texts.formatDecimals(info.getBalance(), Constants.DECIMALS, 2));
        response.setTotalShares(Texts.formatDecimals(info.getTotalShares(), Constants.DECIMALS, 2));
        response.setEscrow(Texts.formatDecimals(info.getEscrow(), Constants.DECIMALS, 2));
        response.setEscrowChange24(Texts.formatDecimals(info.getEscrow_24h(), Constants.DECIMALS, 2));
        response.setScore(info.getScore());
        response.setProposals(info.getProposals());
        response.setSigns(info.getSigns());
        response.setUptime(String.format("%.0f", (double) info.getSignsUptime() / (double) Constants.UPTIME_HEIGHT * 100) + "%");
        response.setActive(info.getNodes() == 1);
        response.setCommission(Numeric.divide(info.getCommission(), 100000, 4));
        response.setStatus(info.isStatus());

        response.setDelegators(delegatorRepository.countByValidator(info.getEntityAddress()));
        return response;
    }

    @Cached(expire = 60 * 2, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    @CacheRefresh(refresh = 30, timeUnit = TimeUnit.SECONDS)
    public ValidatorStatsResponse validatorStats(String entityId, String address) {
        ValidatorStatsResponse response = new ValidatorStatsResponse();
        try {
            if (Texts.isBlank(address)) {
                address = apiClient.pubkeyToBech32Address(entityId);
                if (Texts.isBlank(address)) {
                    throw new RuntimeException(String.format("address parse failed, %s", entityId));
                }
            }

            List<ValidatorStatsResponse.Stats> proposals = Lists.newArrayList();
            List<ValidatorStatsResponse.Stats> signs = Lists.newArrayList();

            long height = scanChainService.getEsHeight();
            if (height > 0) {
                Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityAddress(address);
                if (optional.isPresent()) {
                    List<ValidatorConsensus> consensusList = validatorConsensusRepository.findByEntityId(optional.get().getEntityId());
                    if (!CollectionUtils.isEmpty(consensusList)) {
                        List<String> tmAddressList = Lists.newArrayList();
                        for (ValidatorConsensus consensus : consensusList) {
                            String tmAddress = Texts.hexToBase64(apiClient.pubkeyToTendermintAddress(consensus.getConsensusId()));
                            tmAddressList.add(tmAddress);
                        }

                        List<Long> proposalsList = getStats(tmAddressList, STAT_LIMIT, true);
                        List<Long> signsList = getStats(tmAddressList, STAT_LIMIT, false);

                        for (long i = height; i > height - STAT_LIMIT; i--) {
                            ValidatorStatsResponse.Stats stats = new ValidatorStatsResponse.Stats();
                            stats.setHeight(i);
                            stats.setBlock(false);
                            if (!CollectionUtils.isEmpty(proposalsList)) {
                                if (proposalsList.contains(i)) {
                                    stats.setBlock(true);
                                }
                            }
                            proposals.add(stats);
                        }
                        height -= 1;
                        for (long i = height; i > height - STAT_LIMIT; i--) {
                            ValidatorStatsResponse.Stats stats = new ValidatorStatsResponse.Stats();
                            stats.setHeight(i);
                            stats.setBlock(false);
                            if (!CollectionUtils.isEmpty(signsList)) {
                                if (signsList.contains(i)) {
                                    stats.setBlock(true);
                                }
                            }
                            signs.add(stats);
                        }
                    }
                }
            }
            response.setProposals(proposals);
            response.setSigns(signs);
        } catch (Exception e) {
            log.error("", e);
            return null;
        }

        return response;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult delegators(String entityId, String validatorAddress, int page, int size) {
        List<DelegatorsResponse> responses = Lists.newArrayList();
        long total = 0;
        try {
            if (Texts.isBlank(validatorAddress)) {
                validatorAddress = apiClient.pubkeyToBech32Address(entityId);
                if (Texts.isBlank(validatorAddress)) {
                    throw new RuntimeException(String.format("address parse failed, %s", entityId));
                }
            }
            total = delegatorRepository.countByValidator(validatorAddress);
            if (total > 0) {
                ValidatorResponse validatorResponse = validatorService.validatorInfo(entityId, validatorAddress);
                PageRequest pageRequest = PageRequest.of(page - 1, size);
                Page<Delegator> delegators = delegatorRepository.findByValidator(validatorAddress, pageRequest);
                for (Delegator delegator : delegators) {
                    DelegatorsResponse response = new DelegatorsResponse();
                    response.setEntityId(delegator.getDelegator());
                    response.setAddress(delegator.getDelegator());

                    double totalShares = Double.parseDouble(validatorResponse.getTotalShares());
                    double escrow = Double.parseDouble(validatorResponse.getEscrow());
                    double shares = Double.parseDouble(Texts.formatDecimals(delegator.getShares(), Constants.DECIMALS, 2));
                    //tokens = shares * balance / total_shares
                    double amount = Numeric.divide(Numeric.multiply(shares, escrow), totalShares, 2);
                    double percent = Numeric.divide(shares, totalShares, 4);

                    response.setAmount(Numeric.formatDouble(amount));
                    response.setShares(Numeric.formatDouble(shares));
                    response.setPercent(percent);
                    response.setSelf(delegator.getDelegator().equals(validatorAddress));

                    responses.add(response);
                }

                Comparator<DelegatorsResponse> comparator = (c1, c2) -> Double.compare(Double.parseDouble(c2.getShares()), Double.parseDouble(c1.getShares()));
                responses.sort(comparator);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return ApiResult.page(responses, page, size, total);
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<EscrowStatsResponse> escrowStats(String address) {
        List<EscrowStatsResponse> responses = Lists.newArrayList();
        PageRequest pageRequest = PageRequest.of(0, Constants.ESCROW_STATS_LIMIT);
        List<EscrowStats> list = escrowStatsRepository.findByEntityAddressOrderByHeightDesc(address, pageRequest);
        if (!CollectionUtils.isEmpty(list)) {
            for (EscrowStats escrowStats : list) {
                EscrowStatsResponse response = new EscrowStatsResponse();
                response.setTimestamp(Times.toEpochMilli(Times.parseDay(escrowStats.getDate())));
                response.setEscrow(Texts.formatDecimals(escrowStats.getEscrow(), Constants.DECIMALS, 2));
                responses.add(response);
            }
        }
        return responses;
    }

    public List<Long> getStats(List<String> tmAddressList, int limit, boolean proposal) {
        List<Long> stats = Lists.newArrayList();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (proposal) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery(BLOCK_PROPOSER_ADDRESS, tmAddressList));
        } else {
            boolQueryBuilder.filter(QueryBuilders.termsQuery(BLOCK_SIGN_ADDRESS, tmAddressList));
        }

        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.fetchSource(BLOCK_HEIGHT, null);
        searchSourceBuilder.sort(BLOCK_HEIGHT, SortOrder.DESC);

        try {
            SearchResponse searchResponse = JestDao.search(elasticsearchClient, elasticsearchConfig.getBlockIndex(), searchSourceBuilder);
            if (searchResponse.getTotalShards() == searchResponse.getSuccessfulShards()) {
                SearchHits hits = searchResponse.getHits();
                SearchHit[] searchHits = hits.getHits();
                for (SearchHit hit : searchHits) {
                    Block block = Mappers.parseJson(hit.getSourceAsString(), Block.class).orElse(null);
                    if (block != null) {
                        long blockHeight = block.getHeight();
                        if (proposal) {
                            stats.add(block.getHeight());
                        } else {
                            if (blockHeight > 0) {
                                stats.add(block.getHeight() - 1);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("error", e);
        }

        return stats;
    }
}
