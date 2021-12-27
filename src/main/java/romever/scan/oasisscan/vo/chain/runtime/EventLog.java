package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public class EventLog {
    private String from;
    private String to;
    private String owner;
    private Long nonce;
    private List<String> amount;
    private Error error;

    @Data
    public static class Error {
        private Long code;
        private String module;
    }
}
