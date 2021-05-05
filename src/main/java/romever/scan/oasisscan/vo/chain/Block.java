package romever.scan.oasisscan.vo.chain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Block {
    private long height;
    private String hash;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime time;
    private String meta;
    private MetaData metadata;
    private int txs;

    @Data
    public static class MetaData {
        private LastCommit last_commit;
        private BlockHeader header;

        @Data
        public static class LastCommit {
            private int round;
            private int height;
            private BlockId block_id;
            private List<Signature> signatures;
        }

        @Data
        public static class Signature {
            private String signature;
            private long timestamp;
            private int block_id_flag;
            private String validator_address;
        }

        @Data
        public static class BlockId {
            private String hash;
            private Parts parts;
        }

        @Data
        public static class Parts {
            private String hash;
            private int total;
        }

        @Data
        public static class BlockHeader {
            private Version version;
            private String chain_id;
            private long height;
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            private long time;
            private BlockId last_block_id;
            private String last_commit_hash;
            private String data_hash;
            private String validators_hash;
            private String next_validators_hash;
            private String consensus_hash;
            private String app_hash;
            private String last_results_hash;
            private String evidence_hash;
            private String proposer_address;

            @Data
            public static class Version {
                private int block;
                private long app;
            }

            @Data
            public static class Parts {
                private int total;
                private String hash;
            }
        }
    }
}
