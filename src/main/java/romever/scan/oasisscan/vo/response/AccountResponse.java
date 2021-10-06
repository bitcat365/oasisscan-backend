package romever.scan.oasisscan.vo.response;

import com.google.common.collect.Lists;
import lombok.Data;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.entity.Account;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountResponse {
    private int rank;
    private String address;
    private String available = "0";
    private String escrow = "0";
    private String debonding = "0";
    private String total = "0";
    private long nonce;
    private List<Allowance> allowances = Lists.newArrayList();

    @Data
    public static class Allowance {
        private String address;
        private String amount;
    }

    public static AccountResponse of(Account account, int scale) {
        AccountResponse response = new AccountResponse();
        response.setAddress(account.getAddress());
        response.setAvailable(Texts.formatDecimals(String.valueOf(account.getAvailable()), Constants.DECIMALS, scale));
        response.setEscrow(Texts.formatDecimals(String.valueOf(account.getEscrow()), Constants.DECIMALS, scale));
        response.setDebonding(Texts.formatDecimals(String.valueOf(account.getDebonding()), Constants.DECIMALS, scale));
        double total = account.getAvailable() + account.getEscrow() + account.getDebonding();
        total = Numeric.add(total, Double.parseDouble(response.getDebonding()));
        response.setTotal(Texts.formatDecimals(String.valueOf(total), Constants.DECIMALS, scale));
        return response;
    }
}
