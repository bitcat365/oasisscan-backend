package romever.scan.oasisscan.vo;

public enum CommitteeRoleEnum {
    WORKER("worker"),
    BACKUPWORKER("backup-worker"),
    INVALID("invalid");

    private final String name;

    CommitteeRoleEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
