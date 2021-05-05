package romever.scan.oasisscan.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ValidatorListResponse {
    private List<ValidatorResponse> list;
    private int active;
    private int inactive;
    private long delegators;
}
