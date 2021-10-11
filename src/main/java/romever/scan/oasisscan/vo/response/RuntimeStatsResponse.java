package romever.scan.oasisscan.vo.response;

import lombok.Data;

import java.util.Map;

@Data
public class RuntimeStatsResponse {
    private String entityId;
    private Map<String, Long> stats;

    @Data
    public static class Stats {
        private long elected;
        private long primary;
        private long backup;
        private long proposer;
        private long primaryInvoked;
        private long primaryGoodCommit;
        private long primBadCommmit;
        private long bckpInvoked;
        private long bckpGoodCommit;
        private long bckpBadCommit;
        private long primaryMissed;
        private long bckpMissed;
        private long proposerMissed;
        private long proposedTimeout;
    }
}
