package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.util.List;

@Data
public class AccountInfo {
    private General general;
    private Escrow escrow;

    @Data
    public static class General {
        private String balance;
        private long nonce;
    }

    @Data
    public static class Escrow {
        private Active active;
        private Debonding debonding;
        private CommissionSchedule commission_schedule;
        private StakeAccumulator stake_accumulator;
    }

    @Data
    public static class Active {
        private String balance;
        private String total_shares;
    }

    @Data
    public static class Debonding {
        private String balance;
        private String total_shares;
    }

    @Data
    public static class StakeAccumulator {
        private Object claims;
    }

    @Data
    public static class CommissionSchedule {
        private List<Rate> rates;
        private List<Bound> bounds;
    }

    @Data
    public static class Rate {
        private long start;
        private String rate;
    }

    @Data
    public static class Bound {
        private long start;
        private String rate_min;
        private String rate_max;
    }
}
