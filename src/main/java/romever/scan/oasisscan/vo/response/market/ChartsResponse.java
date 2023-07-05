package romever.scan.oasisscan.vo.response.market;

import com.google.common.collect.Lists;
import lombok.Data;
import romever.scan.oasisscan.vo.response.ChartResponse;

import java.util.List;

@Data
public class ChartsResponse {
    private List<ChartResponse> price = Lists.newArrayList();
    private List<ChartResponse> marketCap = Lists.newArrayList();
    private List<ChartResponse> volume = Lists.newArrayList();
}
