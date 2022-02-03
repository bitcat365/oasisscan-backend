package romever.scan.oasisscan.vo.response;

import lombok.Data;

@Data
public class ListStakingEventResponse {
    private String id;
    private Long height;
    private String tx_hash;
}
