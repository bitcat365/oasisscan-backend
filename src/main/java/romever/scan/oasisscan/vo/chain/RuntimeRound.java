package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeRound {

    private Header header;

    @Data
    public static class Header {
        private long version;
        private String namespace;
        private long round;
        private long timestamp;
        private long header_type;
        private String previous_hash;
        private String io_root;
        private String state_root;
        private String messages_hash;
        private List<StorageSignature> storage_signatures;
    }

    @Data
    public static class StorageSignature {
        private String public_key;
        private String signature;
    }
}
