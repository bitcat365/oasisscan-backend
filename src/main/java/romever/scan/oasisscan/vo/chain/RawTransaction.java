package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class RawTransaction {
    private String untrusted_raw_value;
    private Signature signature;
}
