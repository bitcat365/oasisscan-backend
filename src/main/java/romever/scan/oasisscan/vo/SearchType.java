package romever.scan.oasisscan.vo;

public enum SearchType {
    Transaction("transaction"),
    Block("block"),
    Validator("validator"),
    Account("account"),
    None("none"),
    RuntimeTransaction("runtime-transaction"),;

    private final String name;

    SearchType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SearchType getEnumByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (SearchType one : SearchType.values()) {
            if (one.getName().equalsIgnoreCase(name)) {
                return one;
            }
        }
        return null;
    }
}
