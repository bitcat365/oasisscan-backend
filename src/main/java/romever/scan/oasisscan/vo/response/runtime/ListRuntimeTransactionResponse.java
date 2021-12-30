package romever.scan.oasisscan.vo.response.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListRuntimeTransactionResponse {
    private String runtimeId;
    private String runtimeName;
    private String txHash;
    private long round;
    private boolean result;
    private long timestamp;
    private String type;
}
