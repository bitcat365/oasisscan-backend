package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.math.BigInteger;

@Data
public class NodeStatus {
    private boolean expiration_processed;
    private Object freeze_end_time;
    private BigInteger election_eligible_after;
}
