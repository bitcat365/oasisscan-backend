package romever.scan.oasisscan.vo.response.runtime;

import lombok.Data;

@Data
public class ListRuntimeTransactionResponse {
    private String runtimeId;
    private String txHash;
    private long round;
    private boolean result;
    private long timestamp;
    private String type;
}
