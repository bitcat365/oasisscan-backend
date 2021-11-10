package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeState {
    private Runtime runtime;
    private RuntimeRound genesis_block;
    private RuntimeRound current_block;
    private Long current_block_height;
    private Long last_normal_round;
    private Long last_normal_height;
    private ExecutorPool executor_pool;


    @Data
    public static class ExecutorPool {
        private Runtime runtime;
        private Committee committee;
        private Long round;
        private boolean discrepancy;
        private Long next_timeout;
    }

    @Data
    public static class Committee {
        private String kind;
        private List<Member> members;
        private String runtime_id;
        private Long valid_for;
    }

    @Data
    public static class Member {
        private String role;
        private String public_key;
    }
}
