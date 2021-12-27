package romever.scan.oasisscan.common;

public interface Constants {
    int DECIMALS = 9;
    long RATE_DECIMALS = 100000;
    long UPTIME_HEIGHT = 1000;
    long ONE_DAY_HEIGHT = 14400;
    int ESCROW_STATS_LIMIT = 30;

    String SCAN_HEIGHT_PROPERTY = "scan_height";
    String DB_HEIGHT_PROPERTY = "db_height";

    int EMERALD_DECIMALS = 18;

    String RUNTIME_TX_DEPOSIT_HEX = "6163636f756e747300000001";
    String RUNTIME_TX_WITHDRAW_HEX = "6163636f756e747300000002";
    String RUNTIME_EVENT_EVM_HEX = "65766d00000001";
}
