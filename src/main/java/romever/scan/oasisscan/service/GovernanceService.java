package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.chain.Proposal;
import romever.scan.oasisscan.vo.chain.Vote;
import romever.scan.oasisscan.vo.response.VoteResponse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GovernanceService {

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private ValidatorService validatorService;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<Proposal> proposalList() {
        List<Proposal> list = Lists.newArrayList();
        try {
            list = apiClient.proposalList();
            Comparator<Proposal> comparator = (c1, c2) -> Long.compare(c2.getId(), c1.getId());
            list.sort(comparator);
        } catch (Exception e) {
            log.error("", e);
        }
        return list;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public Proposal proposal(long id) {
        try {
            return apiClient.proposal(id);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<VoteResponse> votes(long id) {
        List<VoteResponse> list = Lists.newArrayList();
        try {
            Map<String, BigDecimal> voteMap = Maps.newHashMap();
//            Map<String, BigDecimal> optionTotalMap = Maps.newHashMap();
            BigDecimal totalVotes = BigDecimal.ZERO;

            List<Vote> votes = apiClient.votes(id);
            long currentEpoch = apiClient.epoch(null);
            Proposal proposal = apiClient.proposal(id);

            long closeEpoch = proposal.getCloses_at();
            Long proposalHeight = null;
            if (currentEpoch > closeEpoch) {
                proposalHeight = apiClient.epochBlock(closeEpoch);
            }

            //total votes power
            for (Vote vote : votes) {
                String address = vote.getVoter();
                String option = vote.getVote();

                AccountInfo accountInfo = apiClient.accountInfo(address, proposalHeight);
                if (accountInfo != null) {
                    String escrow = accountInfo.getEscrow().getActive().getBalance();
                    BigDecimal votePower = new BigDecimal(escrow);
                    voteMap.put(address, votePower);

                    totalVotes = totalVotes.add(votePower);

//                    if (optionTotalMap.containsKey(option)) {
//                        optionTotalMap.put(option, optionTotalMap.get(option).add(votePower));
//                    }
                }
            }

            //calculate every entity votes power
            for (Vote vote : votes) {
                VoteResponse response = new VoteResponse();
                response.setAddress(vote.getVoter());
                response.setVote(vote.getVote());

                //info
                Optional<ValidatorInfo> optionalValidatorInfo = validatorInfoRepository.findByEntityAddress(vote.getVoter());
                if (optionalValidatorInfo.isPresent()) {
                    ValidatorInfo info = optionalValidatorInfo.get();
                    response.setName(info.getName());
                    response.setIcon(info.getIcon());
                }

                //calculate
                BigDecimal amount = voteMap.get(vote.getVoter());
                response.setAmount(amount.toString());
                response.setPercent(amount.divide(totalVotes, 4, RoundingMode.HALF_UP).doubleValue());

                list.add(response);
            }

            Comparator<VoteResponse> comparator = (c1, c2) -> new BigDecimal(c2.getVote()).compareTo(new BigDecimal(c1.getVote()));
            list.sort(comparator);
        } catch (Exception e) {
            log.error("", e);
        }
        return list;
    }

}
