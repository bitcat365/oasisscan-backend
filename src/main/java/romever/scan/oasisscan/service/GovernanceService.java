package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.vo.chain.Proposal;
import romever.scan.oasisscan.vo.chain.Vote;
import romever.scan.oasisscan.vo.response.VoteResponse;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GovernanceService {

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<Proposal> proposalList() {
        List<Proposal> list = Lists.newArrayList();
        try {
            list = apiClient.proposalList();
            Comparator<Proposal> comparator = (c1, c2) -> Long.compare(c2.getId(), c1.getId());
            list.sort(comparator);
        } catch (IOException e) {
            log.error("", e);
        }
        return list;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public Proposal proposal(long id) {
        try {
            return apiClient.proposal(id);
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public List<VoteResponse> votes(long id) {
        List<VoteResponse> list = Lists.newArrayList();
        try {
            List<Vote> votes = apiClient.votes(id);
            for (Vote vote : votes) {
                VoteResponse response = new VoteResponse();
                response.setAddress(vote.getVoter());
                response.setVote(vote.getVote());
                //info
                Optional<ValidatorInfo> optionalValidatorInfo = validatorInfoRepository.findByEntityId(vote.getVote());
                if (optionalValidatorInfo.isPresent()) {
                    ValidatorInfo info = optionalValidatorInfo.get();
                    response.setName(info.getName());
                    response.setIcon(info.getIcon());
                }
                list.add(response);
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return list;
    }

}
