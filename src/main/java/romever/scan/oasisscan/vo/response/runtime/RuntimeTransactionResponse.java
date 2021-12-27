package romever.scan.oasisscan.vo.response.runtime;

import lombok.Data;
import romever.scan.oasisscan.vo.chain.runtime.AbstractRuntimeTransaction;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeEventES;

import java.util.List;

@Data
public class RuntimeTransactionResponse {
    private String runtimeId;
    private String runtimeName;
    private String txHash;
    private long round;
    private boolean result;
    private String message;
    private long timestamp;
    private String type;

    private Consensus ctx;
    private Ethereum etx;

    private List<RuntimeEventES> events;

    @Data
    public static class Consensus {
        private String method;
        private String from;
        private String to;
        private String amount;
        private long nonce;
    }

    @Data
    public static class Ethereum {
        private String from;
        private String to;
        private long nonce;
        private long gasPrice;
        private long gasLimit;
        private String data;
        private String value;
    }
}
