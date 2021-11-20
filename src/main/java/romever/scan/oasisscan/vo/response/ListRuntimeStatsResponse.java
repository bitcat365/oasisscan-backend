package romever.scan.oasisscan.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ListRuntimeStatsResponse {
    private int online;
    private int offline;
    private List<RuntimeStatsResponse> list;
}
