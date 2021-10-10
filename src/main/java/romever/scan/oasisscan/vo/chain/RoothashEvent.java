package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.util.List;

@Data
public class RoothashEvent {
    private Long height;
    private String tx_hash;
    private String runtime_id;
    private ExecutorCommitted executor_committed;
    private ExecutionDiscrepancy execution_discrepancy;
    private Finalized finalized;
    private Message message;

    @Data
    public static class ExecutorCommitted {
        private Commit commit;
    }

    @Data
    public static class ExecutionDiscrepancy {
        private Boolean timeout;
    }

    @Data
    public static class Finalized {
        private Long round;
        private List<String> good_compute_nodes;
        private List<String> bad_compute_nodes;
    }

    @Data
    public static class Message {
        private String module;
        private Long code;
        private Long index;
    }

    @Data
    public static class Commit {
        private String untrusted_raw_value;
        private Signature signature;
    }

    @Data
    public static class Signature {
        private String public_key;
        private String signature;
    }
}
