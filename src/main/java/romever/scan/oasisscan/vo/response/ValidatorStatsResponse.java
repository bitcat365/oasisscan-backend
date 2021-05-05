package romever.scan.oasisscan.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ValidatorStatsResponse {

    private List<Stats> signs;
    private List<Stats> proposals;

    @Data
    public static class Stats {
//        private String blocks;
//        private int count;
//        private long score;
//        private String percent;

        private long height;
        private boolean block;
    }
}
