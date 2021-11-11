package romever.scan.oasisscan.vo.response.runtime;

import lombok.Data;

@Data
public class RuntimeTransactionResponse {
    private String runtime_id;
    private String tx_hash;
    private long round;
    private boolean result;
    private String message;
    private long timestamp;
    private String type;

    private Consensus ctx;
    private Ethereum etx;

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
