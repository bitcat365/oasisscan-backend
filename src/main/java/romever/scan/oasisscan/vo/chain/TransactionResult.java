package romever.scan.oasisscan.vo.chain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class TransactionResult {

    private Error error;
    private Object events;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private String module;
        private int code;
        private String message;
    }
}
