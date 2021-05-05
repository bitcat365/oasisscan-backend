package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RegistryGenesis {
    private Object params;
    private List<Object> entities;
    private List<Object> runtimes;
    private List<Object> suspended_runtimes;
    private List<Object> nodes;
    private Map<String, NodeStatus> node_statuses;
}
