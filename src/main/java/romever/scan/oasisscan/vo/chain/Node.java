package romever.scan.oasisscan.vo.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Node {
    private ConsensusInfo consensus;
    private Object roles;
    private long expiration;
    private String id;
    private String entity_id;
    private P2P p2p;
    private List<Runtime> runtimes;
    private long v;
    private Tls tls;
    private List<String> nodes;
    private Object beacon;
    private Vrf vrf;
    private String software_version;
    //    private boolean allow_entity_signed_nodes;
    //    private CommitteeInfo committee;

    @Data
    public static class Vrf {
        private String id;
    }

    @Data
    public static class Tls {
        private String pub_key;
        private String next_pub_key;
        private List<Object> addresses;
    }

    @Data
    public static class P2P {
        private String id;
        private List<Object> addresses;
    }

//    @Data
//    public static class CommitteeInfo {
//        private String certificate;
//        private String next_certificate;
//        private List<CommitteeAddress> addresses;
//
//        @Data
//        public static class CommitteeAddress {
//            private String certificate;
//            private String address;
//        }
//    }

    @Data
    public static class ConsensusInfo {
        private String id;
        private List<Object> addresses;
    }

    @Data
    public static class ConsensusAddress {
        private String id;
        private Address address;
    }

    @Data
    public static class Address {
        @JsonProperty("IP")
        private String ip;
        @JsonProperty("Port")
        private long port;
        @JsonProperty("Zone")
        private String zone;
    }


    @Data
    public static class Runtime {
        private String id;
        private Object version;
        private Capabilities capabilities;
        private String extra_info;
    }

    @Data
    public static class Capabilities {
        private CapabilityTEE tee;
    }

    @Data
    public static class CapabilityTEE {
        private int hardware;
        private String rak;
        private String attestation;
    }

    @Data
    public static class TlsAddress {
        private String pub_key;
        private Address address;
    }
}
