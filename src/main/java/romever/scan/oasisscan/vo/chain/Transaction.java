package romever.scan.oasisscan.vo.chain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import romever.scan.oasisscan.utils.Texts;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {

    private String tx_hash;
    private String method;
    private Fee fee;
    private Body body;
    private Long nonce;
    private Node node;
    private Signature signature;
    private Long height;
    private Long timestamp;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime time;
    private TransactionResult.Error error;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Fee {
        private Long gas;
        private String amount;

        public String getAmount() {
            return Texts.numberFromBase64(amount);
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Body {
        private String id;
        private Long round;
        private Long epoch;
        private Object commits;
        private Object commit;
        private Object reveal;
        /**
         * staking
         */
        private String amount;
        //transfer
        private String to;
        //allowance
        private String beneficiary;
        private String amount_change;
        private boolean negative;

        //Escrow
        private String account;
        //ReclaimEscrow
        private String shares;
        //AmendCommissionSchedule
        private CommissionSchedule amendment;

        //governance
        private Long vote;

        /**
         * registry
         */
        //RegisterEntity
        private List<Signature> signatures;
        private String untrusted_raw_value;

        private Signature signature;

        public String getAmount() {
            return Texts.numberFromBase64(amount);
        }

        public String getAmount_change() {
            return Texts.numberFromBase64(amount_change);
        }

        public String getShares() {
            return Texts.numberFromBase64(shares);
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommissionSchedule {
        List<CommissionRateStep> rates;
        List<CommissionRateBoundStep> bounds;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommissionRateStep {
        private String start;
        private String rate;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommissionRateBoundStep {
        private String start;
        private String rate_min;
        private String rate_max;
    }
}

