package romever.scan.oasisscan.vo.response;

import lombok.Data;

@Data
public class DashboardNetworkResponse {
    private long curHeight;
    private long curEpoch;
    private long totalTxs;
    private String totalEscrow;
    private long activeValidator;
    private long totalDelegate;
}
