package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class StakingEvent {
    private Long height;
    private String tx_hash;
    private TransferEvent transfer;
    private BurnEvent burn;
    private EscrowEvent escrow;
    private AllowanceChangeEvent allowance_change;

    @Data
    public static class TransferEvent {
        private String from;
        private String to;
        private String amount;
    }

    @Data
    public static class BurnEvent {
        private String owner;
        private String amount;
    }

    @Data
    public static class EscrowEvent {
        private AddEscrowEvent add;
        private TakeEscrowEvent take;
        private DebondingStartEscrowEvent debonding_start;
        private ReclaimEscrowEvent reclaim;
    }

    @Data
    public static class AllowanceChangeEvent {
        private String owner;
        private String beneficiary;
        private String allowance;
        private boolean negative;
        private String amount_change;
    }

    @Data
    public static class AddEscrowEvent {
        private String owner;
        private String escrow;
        private String amount;
        private String new_shares;
    }

    @Data
    public static class TakeEscrowEvent {
        private String owner;
        private String amount;
    }

    @Data
    public static class DebondingStartEscrowEvent {
        private String owner;
        private String escrow;
        private String amount;
        private String active_shares;
        private String debonding_shares;
    }

    @Data
    public static class ReclaimEscrowEvent {
        private String owner;
        private String escrow;
        private String amount;
        private String shares;
    }
}
