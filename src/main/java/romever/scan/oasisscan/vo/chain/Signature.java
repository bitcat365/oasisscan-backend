package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class Signature {
    private String signature;
    private String public_key;
    private String address;
}
