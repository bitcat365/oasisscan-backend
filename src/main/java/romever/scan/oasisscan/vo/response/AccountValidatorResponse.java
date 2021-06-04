package romever.scan.oasisscan.vo.response;

import lombok.Data;

@Data
public class AccountValidatorResponse {
    private String validatorAddress;
    private String validatorName;
    private String icon;
    private String entityAddress;
    private String shares;
    private String amount;
    private boolean active;
}
