package romever.scan.oasisscan.vo.response;

import lombok.Data;

@Data
public class AccountDebondingResponse {
    private String validatorAddress;
    private String validatorName;
    private String icon;
    private String shares;
    private long debondEnd;
    private long epochLeft;
}
