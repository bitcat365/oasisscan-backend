package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

@Data
public class RuntimeEvent {
    private String key;
    private String value;
    private String tx_hash;
}
