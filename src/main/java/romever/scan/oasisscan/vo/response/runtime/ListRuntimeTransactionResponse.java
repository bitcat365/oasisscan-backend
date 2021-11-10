package romever.scan.oasisscan.vo.response.runtime;

import lombok.Data;

@Data
public class ListRuntimeTransactionResponse {
    private String runtime_id;
    private String tx_hash;
    private long round;
    private boolean result;
    private long timestamp;
    private String type;
}
