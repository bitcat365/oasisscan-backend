package romever.scan.oasisscan.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ValidatorResponse {
    private int rank;
    private String entityId;
    private String entityAddress;
    private String nodeId;
    private String nodeAddress;
    private String name;
    private String icon;
    private String website;
    private String twitter;
    private String keybase;
    private String email;
    private String description;
    private String escrow;
    private String escrowChange24;
    private double escrowPercent;
    private String balance;
    private String totalShares;
    private long signs;
    private long proposals;
    private long nonce;
    private long score;
    private long delegators;
    private List<String> nodes;
    private String uptime;
    private boolean active;
    private double commission;
    private Bound bound;
    private List<Rate> rates;
    private List<Bound> bounds;
    private EscrowStatus escrowSharesStatus;
    private EscrowStatus escrowAmountStatus;
    private boolean status;

    @Data
    public static class Bound {
        private long start;
        private double min;
        private double max;
    }

    @Data
    public static class Rate {
        private long start;
        private double rate;
    }

    @Data
    public static class EscrowStatus {
        private String self;
        private String other;
        private String total;
    }
}
