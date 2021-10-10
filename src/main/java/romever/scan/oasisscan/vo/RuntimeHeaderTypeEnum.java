package romever.scan.oasisscan.vo;

public enum RuntimeHeaderTypeEnum {
    Invalid(0),
    Normal(1),
    RoundFailed(2),
    EpochTransition(3),
    Suspended(4);

    private final int code;

    RuntimeHeaderTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
