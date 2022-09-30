package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class RuntimeTransaction extends AbstractRuntimeTransaction {
    private long v;
    private Ai ai;
    private Call call;

    @Data
    public static class Ai {
        private List<Si> si;
        private Fee fee;
    }

    @Data
    public static class Si {
        private Long nonce;
        private AddressSpec address_spec;
    }

    @Data
    public static class AddressSpec {
        private Signature signature;
    }

    @Data
    public static class Signature {
        private String ed25519;
        private String address;
        private String secp256k1eth;
    }

    @Data
    public static class Fee {
        private Long gas;
        private List<String> amount;
        private Long consensus_messages;
    }

    @Data
    public static class Call {
        private Body body;
        private String method;
        private Long format;
    }

    @Data
    public static class Body {
        private String to;
        private List<String> amount;

        private String pk;
        private String data;
        private String nonce;
    }
}
