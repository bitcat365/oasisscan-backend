package romever.scan.oasisscan.vo;

public enum MethodEnum {
    StakingTransfer("staking.Transfer"),
    StakingAllow("staking.Allow"),
    StakingAddEscrow("staking.AddEscrow"),
    StakingReclaimEscrow("staking.ReclaimEscrow"),
    StakingAmendCommissionSchedule("staking.AmendCommissionSchedule"),
    RegistryRegisterNode("registry.RegisterNode");

    private final String name;

    MethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static MethodEnum getEnumByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (MethodEnum one : MethodEnum.values()) {
            if (one.getName().equalsIgnoreCase(name)) {
                return one;
            }
        }
        return null;
    }
}
