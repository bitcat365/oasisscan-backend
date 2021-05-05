package romever.scan.oasisscan.vo;

public enum SortEnum {
    SIGNS("signs"), PROPOSALS("proposals"), ESCROW("escrow"), UPTIME("uptime"), SCORE("score"),
    CHANGE("escrowChange24"), DELEGATOR("delegators"), COMMISSION("commission");

    private String code;

    SortEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SortEnum getEnumByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (SortEnum one : SortEnum.values()) {
            if (one.getCode().equalsIgnoreCase(code)) {
                return one;
            }
        }
        return null;
    }
}
