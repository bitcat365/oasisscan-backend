package romever.scan.oasisscan.vo;

public enum RuntimeTransactionType {

    CONSENSUS("consensus", "regular"),
    EVM("evm", "evm");

    private final String type;
    private final String displayName;

    RuntimeTransactionType(String type, String displayName) {
        this.type = type;
        this.displayName = displayName;
    }

    public String getType() {
        return this.type;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static String getDisplayNameByType(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        for (RuntimeTransactionType one : RuntimeTransactionType.values()) {
            if (one.getType().equalsIgnoreCase(type)) {
                return one.getDisplayName();
            }
        }
        return null;
    }
}
