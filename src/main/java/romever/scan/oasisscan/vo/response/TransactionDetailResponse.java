package romever.scan.oasisscan.vo.response;

import lombok.Data;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.MethodEnum;
import romever.scan.oasisscan.vo.chain.Transaction;
import romever.scan.oasisscan.vo.chain.TransactionResult;

@Data
public class TransactionDetailResponse {
    private String txHash;
    private long timestamp;
    private long time;
    private long height;
    private String fee;
    private long nonce;
    private String method;
    private String from;
    private String to;
    private String amount;

    private String raw;
    private boolean status = true;
    private String errorMessage;

    public static TransactionDetailResponse of(Transaction transaction,  double escrowBalance, double totalShares) {
        TransactionDetailResponse response = new TransactionDetailResponse();

        TransactionResult.Error error = transaction.getError();
        if (error != null) {
            if (Texts.isNotBlank(error.getMessage())) {
                response.setStatus(false);
                response.setErrorMessage(error.getMessage());
            }
        }

        response.setTxHash(transaction.getTx_hash());
        response.setHeight(transaction.getHeight());
        String method = transaction.getMethod();
        response.setMethod(method);
        String feeAmount = transaction.getFee().getAmount();
        if (Texts.isNotBlank(feeAmount)) {
            response.setFee(Texts.formatDecimals(feeAmount, Constants.DECIMALS, 9));
        }
        response.setTimestamp(transaction.getTimestamp());
        response.setTime(System.currentTimeMillis() / 1000 - transaction.getTimestamp());
        response.setRaw(Mappers.json(transaction));

        response.setFrom(transaction.getSignature().getAddress());
        response.setNonce(transaction.getNonce());
        Transaction.Body body = transaction.getBody();
        if (body != null) {
            MethodEnum methodEnum = MethodEnum.getEnumByName(method);
            if (methodEnum != null) {
                switch (methodEnum) {
                    case StakingTransfer:
                        response.setTo(body.getTo());
                        response.setAmount(Texts.formatDecimals(body.getAmount(), Constants.DECIMALS, 9));
                        break;
                    case StakingAddEscrow:
                        response.setTo(body.getAccount());
                        response.setAmount(Texts.formatDecimals(body.getAmount(), Constants.DECIMALS, 9));
                        break;
                    case StakingReclaimEscrow:
                        //tokens = shares * balance / total_shares
                        double shares = Double.parseDouble(Texts.formatDecimals(body.getShares(), Constants.DECIMALS, 2));
                        double amount = 0;
                        if (totalShares != 0) {
                            amount = Numeric.divide(Numeric.multiply(shares, escrowBalance), totalShares, 2);
                        }

                        response.setTo(body.getAccount());
                        response.setAmount(String.valueOf(amount));
                        break;
                    default:
                        break;
                }
            }
        }


        return response;
    }

    public static void main(String[] args) {
        System.out.println(Texts.toHex(Texts.base64Decode("/udtNFLj5k5VPzHxg0tDEalfkOmkUSURpyAiUaVGcjI=")));

        System.out.println(Texts.toHex(Texts.base64Decode("ND60v9LrslMFI46zbMsUxyeabzg=")));

        System.out.println(Texts.base64Encode(Texts.hexStringToByteArray("36e73d90616c19e8bb637739a7dce45d361283c3e8ea6ba5191c61885ff29d8d")));
        System.out.println(Texts.base64Encode(Texts.hexStringToByteArray("2A3FEBF3A17DECA5DE155D06D671C0FB22103AE6")));

    }
}
