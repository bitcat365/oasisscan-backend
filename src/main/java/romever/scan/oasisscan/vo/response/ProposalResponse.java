package romever.scan.oasisscan.vo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProposalResponse {
    private long id;
    private String handler;
    private String submitter;
    private String state;
    private String deposit;
    private long created_at;
    private long closes_at;
    private List<Option> options;
    private List<VoteResponse> votes;

    @Data
    public static class Option {
        private String name;
        private String amount;
        private double percent;
    }
}
