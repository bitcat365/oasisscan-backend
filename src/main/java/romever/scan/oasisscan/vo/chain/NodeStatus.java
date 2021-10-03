package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class NodeStatus {
    private boolean expiration_processed;
    private Object freeze_end_time;
    private Long election_eligible_after;
}
