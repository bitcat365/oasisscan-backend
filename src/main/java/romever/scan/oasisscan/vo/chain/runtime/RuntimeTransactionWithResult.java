package romever.scan.oasisscan.vo.chain.runtime;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeTransactionWithResult {
    private String tx;
    private String result;
    private List<RuntimeEvent> events;
}
