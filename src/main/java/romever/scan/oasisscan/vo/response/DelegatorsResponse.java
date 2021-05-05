package romever.scan.oasisscan.vo.response;

import lombok.Data;

@Data
public class DelegatorsResponse {
    private String entityId;
    private String address;
    private String amount;
    private String shares;
    private double percent;
    private boolean self;
}
