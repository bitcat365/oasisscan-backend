package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class TransferEvent {
    private String tx_hash;
    private Transfer transfer;

    @Data
    public static class Transfer {
        private String from;
        private String to;
        private String tokens;
    }
}
