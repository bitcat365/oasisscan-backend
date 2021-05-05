package romever.scan.oasisscan.vo;

import lombok.Data;

import java.util.List;

@Data
public class Entity {
    private String id;
    private List<String> nodes;
    private boolean allowEntitySignedNodes;
}
