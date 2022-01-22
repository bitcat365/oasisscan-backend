package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeEventES {
    private String type;
    private String tx_hash;
    private Long round;
    private Long position;
    private Long i;

    private String from;
    private String to;
    private String owner;
    private Long nonce;
    private List<String> amount;
    private EventLog.Error error;
}
