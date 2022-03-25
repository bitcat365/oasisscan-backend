package romever.scan.oasisscan.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.vo.chain.Proposal;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class GovernanceService {

    @Autowired
    private ApiClient apiClient;

    public List<Proposal> proposalList() {
        List<Proposal> list = Lists.newArrayList();
        try {
            list = apiClient.proposalList();
            Comparator<Proposal> comparator = (c1, c2) -> Long.compare(c2.getId(), c1.getId());
            list.sort(comparator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Proposal proposal(long id) {
        try {
            return apiClient.proposal(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
