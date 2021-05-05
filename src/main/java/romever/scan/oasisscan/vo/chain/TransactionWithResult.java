package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.util.List;

@Data
public class TransactionWithResult {
    private List<String> transactions;
    private List<TransactionResult> results;
}
