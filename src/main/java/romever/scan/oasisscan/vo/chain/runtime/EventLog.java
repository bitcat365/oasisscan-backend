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
}
