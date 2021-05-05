package romever.scan.oasisscan.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Map;

@Data
public class Validator {
    private General general;
    private Escrow escrow;

    @Data
    public static class General {
        private String balance;
        private int nonce;
    }

    @Data
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class Escrow {
        private Active active;
        private Debonding debonding;
        private CommissionSchedule commissionSchedule;
        private StakeAccumulator stakeAccumulator;
    }

    @Data
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class Active {
        private String balance;
        private String totalShares;
    }

    @Data
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class Debonding {
        private String balance;
        private String totalShares;
    }

    @Data
    public static class StakeAccumulator {
        private Map<String, Object> claims;
    }

    @Data
    public static class CommissionSchedule {
        private String rates;
        private String bounds;
    }
}
