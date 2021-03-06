package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public abstract class AbstractRuntimeTransaction {
    private String runtime_id;
    private String tx_hash;
    private long round;
    private boolean result;
    private String message;
    private long timestamp;
    private String type;
    private List<Event> events;
    private Long position;

    @Data
    public static class Event {
        private String type;
        private List<EventLog> logs;
    }
}
