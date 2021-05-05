package romever.scan.oasisscan.vo.response;

import lombok.Data;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.AccountInfo;

@Data
public class AccountSimple {
    private String address;
    private String totalBalance;
    private String available;
    private String escrow;
    private String debonding;
    private String totalShares;
    private long nonce;

    public static AccountSimple of(AccountInfo accountInfo) {
        AccountSimple response = new AccountSimple();
        double total = 0;
        AccountInfo.General general = accountInfo.getGeneral();
        if (general != null) {
            response.setAvailable(Texts.formatDecimals(general.getBalance(), Constants.DECIMALS, Constants.DECIMALS));
            total = Numeric.add(total, Double.parseDouble(response.getAvailable()));
            response.setNonce(general.getNonce());
        }
        AccountInfo.Escrow escrow = accountInfo.getEscrow();
        if (escrow != null) {
            AccountInfo.Active active = escrow.getActive();
            if (active != null) {
                response.setEscrow(Texts.formatDecimals(active.getBalance(), Constants.DECIMALS, Constants.DECIMALS));
                response.setTotalShares(Texts.formatDecimals(active.getTotal_shares(), Constants.DECIMALS, Constants.DECIMALS));
                total = Numeric.add(total, Double.parseDouble(response.getEscrow()));
            }
            AccountInfo.Debonding debonding = escrow.getDebonding();
            if (debonding != null) {
                response.setDebonding(Texts.formatDecimals(debonding.getBalance(), Constants.DECIMALS, Constants.DECIMALS));
                total = Numeric.add(total, Double.parseDouble(response.getDebonding()));
            }
        }
        response.setTotalBalance(String.valueOf(total));
        return response;
    }
}
