package romever.scan.oasisscan.vo.response;

import lombok.Data;
import org.springframework.beans.BeanUtils;
import romever.scan.oasisscan.vo.chain.RuntimeRound;

@Data
public class RuntimeRoundResponse {
    private long version;
    private String namespace;
    private long round;
    private long timestamp;
    private long header_type;
    private String previous_hash;
    private String io_root;
    private String state_root;
    private String messages_hash;
    private boolean next;

    public static RuntimeRoundResponse of(RuntimeRound.Header round) {
        RuntimeRoundResponse response = new RuntimeRoundResponse();
        BeanUtils.copyProperties(round, response);
        return response;
    }
}
