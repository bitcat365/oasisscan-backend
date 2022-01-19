package romever.scan.oasisscan.vo.chain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class TransactionResult {

    private Error error;
    private List<Event> events;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private String module;
        private int code;
        private String message;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Event {
        private Staking staking;
        private RoothashEvent roothash;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Staking {
        private Long height;
        private String tx_hash;
        private Transfer transfer;
        private AllowanceChange allowance_change;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Transfer {
        private String from;
        private String to;
        private String amount;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AllowanceChange {
        private String owner;
        private String beneficiary;
        private String allowance;
        private boolean negative;
        private String amount_change;
    }
}
