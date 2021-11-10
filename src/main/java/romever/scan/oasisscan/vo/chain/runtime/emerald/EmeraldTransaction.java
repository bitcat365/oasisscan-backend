package romever.scan.oasisscan.vo.chain.runtime.emerald;

import lombok.Data;
import romever.scan.oasisscan.vo.chain.runtime.AbstractRuntimeTransaction;

/**
 * evm transaction
 */
@Data
public class EmeraldTransaction extends AbstractRuntimeTransaction {
    private String from;
    private String to;
    private long nonce;
    private long gasPrice;
    private long gasLimit;
    private String data;
    private String value;
}
