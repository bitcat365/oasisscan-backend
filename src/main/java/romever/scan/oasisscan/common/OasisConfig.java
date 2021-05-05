package romever.scan.oasisscan.common;

import lombok.Data;

@Data
public class OasisConfig {
    private String url;
    private String node;
    private String stats;
    private String internal;
    private Api api;

    @Data
    public static class Api {
        private String url;
        private String name;
    }
}
