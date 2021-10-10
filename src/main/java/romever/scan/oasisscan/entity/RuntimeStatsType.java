package romever.scan.oasisscan.entity;

public enum RuntimeStatsType {
    ELECTED,
    PRIMARY,
    BACKUP,
    PROPOSER,
    PRIMARY_INVOKED,
    PRIMARY_GOOD_COMMIT,
    PRIM_BAD_COMMMIT,
    BCKP_INVOKED,
    BCKP_GOOD_COMMIT,
    BCKP_BAD_COMMIT,
    PRIMARY_MISSED,
    BCKP_MISSED,
    PROPOSER_MISSED,
    PROPOSED_TIMEOUT,
    ;
}
