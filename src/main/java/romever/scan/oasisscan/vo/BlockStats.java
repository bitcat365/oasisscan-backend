package romever.scan.oasisscan.vo;

import lombok.Data;

@Data
public class BlockStats {
    private String entityId;
    private int rank;
    private int nodes;
    private int signatures;
    private int proposals;
    private long availabilityScore;
}
