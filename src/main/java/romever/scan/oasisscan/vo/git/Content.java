package romever.scan.oasisscan.vo.git;

import lombok.Data;

@Data
public class Content {
    private String name;
    private String path;
    private String sha;
    private int size;
    private String url;
    private String html_url;
    private String git_url;
    private String download_url;
    private String type;
    private Links _links;

    public static class Links {
        private String self;
        private String git;
        private String html;
    }
}
