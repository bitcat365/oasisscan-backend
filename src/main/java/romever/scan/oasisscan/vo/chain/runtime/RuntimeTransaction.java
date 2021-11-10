package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

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
        private long nonce;
        private AddressSpec address_spec;
    }

    @Data
    public static class AddressSpec {
        private Signature signature;
    }

    @Data
    public static class Signature {
        private String ed25519;
    }

    @Data
    public static class Fee {
        private long gas;
        private List<String> amount;
        private long consensus_messages;
    }

    @Data
    public static class Call {
        private Body body;
        private String method;
    }

    @Data
    public static class Body {
        private String to;
        private List<String> amount;
    }
}
