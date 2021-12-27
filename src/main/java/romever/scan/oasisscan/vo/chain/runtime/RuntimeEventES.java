package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeEventES {
    private String type;
    private String tx_hash;

    private List<EventLog> logs;
}
