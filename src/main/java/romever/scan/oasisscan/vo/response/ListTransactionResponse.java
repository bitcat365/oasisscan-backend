package romever.scan.oasisscan.vo.response;

import lombok.Data;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.MethodEnum;
import romever.scan.oasisscan.vo.chain.Transaction;
import romever.scan.oasisscan.vo.chain.TransactionResult;

@Data
public class ListTransactionResponse {
    private String txHash;
    private long height;
    private String method;
    private String fee = "0";
    private String amount;
    private String shares;
    private boolean add = true;
    private long timestamp;
    private long time;
    private boolean status = true;
    private String from;
    private String to;

    public static ListTransactionResponse of(Transaction transaction) {
        return of(transaction, 0, 0);
    }

    public static ListTransactionResponse of(Transaction transaction, double escrowBalance, double totalShares) {
        ListTransactionResponse response = new ListTransactionResponse();

        TransactionResult.Error error = transaction.getError();
        if (error != null) {
            if (Texts.isNotBlank(error.getMessage())) {
                response.setStatus(false);
            }
        }

        response.setTxHash(transaction.getTx_hash());
        response.setHeight(transaction.getHeight());
        response.setMethod(transaction.getMethod());
        response.setFrom(transaction.getSignature().getAddress());
        String feeAmount = transaction.getFee().getAmount();
        if (Texts.isNotBlank(feeAmount)) {
            response.setFee(Texts.formatDecimals(feeAmount, Constants.DECIMALS, 9));
        }

        MethodEnum methodEnum = MethodEnum.getEnumByName(transaction.getMethod());
        Transaction.Body body = transaction.getBody();
        if (methodEnum != null && body != null) {
            if (methodEnum.equals(MethodEnum.StakingTransfer)) {
                String a = body.getAmount();
                if (Texts.isBlank(a)) {
                    a = "0";
                }
                double amount = Double.parseDouble(Texts.toBigDecimal(a, Constants.DECIMALS));
                response.setAmount(Numeric.formatDouble(amount));
                response.setTo(body.getTo());
            } else if (methodEnum.equals(MethodEnum.StakingAllow)) {
                String a = body.getAmount_change();
                if (Texts.isBlank(a)) {
                    a = "0";
                }
                double amount = Double.parseDouble(Texts.toBigDecimal(a, Constants.DECIMALS));
                response.setAmount(Numeric.formatDouble(amount));
                response.setTo(body.getBeneficiary());
                if (body.getNegative() != null) {
                    response.setAdd(!body.getNegative());
                }
            }
        }

        if (methodEnum != null && body != null) {
            //shares = amount * total_shares / balance
            //tokens = shares * balance / total_shares
            if (methodEnum.equals(MethodEnum.StakingAddEscrow)) {
                String a = body.getAmount();
                if (Texts.isBlank(a)) {
                    a = "0";
                }
                double amount = Double.parseDouble(Texts.formatDecimals(a, Constants.DECIMALS, 2));
                double shares = 0;
                if (escrowBalance != 0) {
                    shares = Numeric.divide(Numeric.multiply(amount, totalShares), escrowBalance, 2);
                }
                response.setAmount(Numeric.formatDouble(amount));
                response.setShares(Numeric.formatDouble(shares));
                response.setTo(body.getAccount());
                response.setAdd(true);
            } else if (methodEnum.equals(MethodEnum.StakingReclaimEscrow)) {
                double shares = Double.parseDouble(Texts.formatDecimals(body.getShares(), Constants.DECIMALS, 2));
                double amount = 0;
                if (totalShares != 0) {
                    amount = Numeric.divide(Numeric.multiply(shares, escrowBalance), totalShares, 2);
                }
                response.setAmount(Numeric.formatDouble(amount));
                response.setShares(Numeric.formatDouble(shares));
                response.setTo(body.getAccount());
                response.setAdd(false);
            }
        }

        response.setTimestamp(transaction.getTimestamp());
        response.setTime(System.currentTimeMillis() / 1000 - transaction.getTimestamp());
        return response;
    }
}
