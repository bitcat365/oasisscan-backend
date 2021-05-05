package romever.scan.oasisscan.vo.response;

import lombok.Data;

@Data
public class AccountDebondingResponse {
    private String validatorAddress;
    private String validatorName;
    private String shares;
    private long debondEnd;
}
