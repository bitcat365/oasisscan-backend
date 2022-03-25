package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class Proposal {
    private long id;
    private String submitter;
    private String state;
    private String deposit;
    private Content content;
    private int created_at;
    private int closes_at;

    @Data
    public static class Content {
        private Upgrade upgrade;
    }

    @Data
    public static class Upgrade {
        private long v;
        private String handler;
        private Target target;
        private int epoch;
    }

    @Data
    public static class Target {
        private Version consensus_protocol;
        private Version runtime_host_protocol;
        private Version runtime_committee_protocol;
    }


}
