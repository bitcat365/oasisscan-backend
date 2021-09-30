package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class Runtime {
    private long v;
    private String id;
    private String entity_id;
    private Object genesis;
    private Long kind;
    private Long tee_hardware;
    private Object versions;
    private Object executor;
    private Object txn_scheduler;
    private Object storage;
    private Object admission_policy;
    private Object staking;
    private Object governance_model;
}
