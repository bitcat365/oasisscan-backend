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

    @Data
    public static class Event {
        private String type;
        private List<EventLog> logs;
    }

    @Data
    public static class EventLog {
        private String from;
        private String to;
        private List<String> amount;
    }
}
