package romever.scan.oasisscan.vo.chain;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StakingGenesis {
    private Object params;
    private String token_symbol;
    private long token_value_exponent;
    private String total_supply;
    private String common_pool;
    private String last_block_fees;
    private Map<String, AccountInfo> ledger;
    private Map<String, Map<String, Delegations>> delegations;
    private Map<String, Map<String, List<Debonding>>> debonding_delegations;
}
