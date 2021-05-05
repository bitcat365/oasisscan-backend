package romever.scan.oasisscan.vo.git;

import lombok.Data;

@Data
public class EntityRawInfo {
    private String untrusted_raw_value;
    private Signature signature;

    @Data
    public static class Signature {
        private String public_key;
        private String signature;
    }
}
