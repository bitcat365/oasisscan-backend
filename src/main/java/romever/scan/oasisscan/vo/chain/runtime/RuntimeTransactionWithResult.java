package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeTransactionWithResult {
    private String tx;
    private String result;
    private List<PlainEvent> events;

    @Data
    public static class PlainEvent {
        private String key;
        private String value;
    }
}
