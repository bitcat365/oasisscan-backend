package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

@Data
public abstract class AbstractRuntimeTransaction {
    private String runtime_id;
    private String tx_hash;
    private long round;
    private boolean result;
    private String message;
    private long timestamp;
    private String type;
}
