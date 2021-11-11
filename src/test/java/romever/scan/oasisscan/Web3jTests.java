package romever.scan.oasisscan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeTransaction;

import java.io.IOException;
import java.security.SignatureException;

public class Web3jTests {

    @Test
    public void test1() {
        System.out.println(Texts.numberFromBase64("Fx7SLFPNAAA="));
    }

    public static void main(String[] args) throws IOException, SignatureException {
        String code = "gljRo2F2AWJhaaJic2mBomVub25jZQJsYWRkcmVzc19zcGVjoWlzaWduYXR1cmWhZ2VkMjU1MTlYIAuZHaGU6xLSfSjndixDLwObOl1sD7lWDyexjDViLxYwY2ZlZaNjZ2FzGcNQZmFtb3VudIJAQHJjb25zZW5zdXNfbWVzc2FnZXMBZGNhbGyiZGJvZHmiYnRvVQCK12JFZMoKOr7GZu028AbnUqW9CmZhbW91bnSCSBEgDHZE1QAAQGZtZXRob2RxY29uc2Vuc3VzLkRlcG9zaXSBoWlzaWduYXR1cmVYQICGDTt+di3LFMJXLrt3iXeAcyJp4jjIm+u9YwraoB3oECP+OL70gFRnHDzdyKVxV7SrRpsU1FOp8G5FyeJvVwg=";
        JsonNode rawJson = Mappers.parseCborFromBase64(code, new TypeReference<JsonNode>() {
        });

        System.out.println(rawJson);
        String raw = "";
        String type = "";
        if (rawJson.isArray()) {
            raw = rawJson.get(0).asText();
            type = rawJson.get(1).toString();
            System.out.println(raw);
        }

        if (type.contains("evm")) {
            String hex = Texts.base64ToHex(raw);
            RawTransaction rawTransaction = TransactionDecoder.decode(hex);
            System.out.println(Hash.sha3(hex));
            System.out.println(rawTransaction.getNonce());
            System.out.println(rawTransaction.getGasLimit());
            System.out.println(rawTransaction.getTo());
            System.out.println(rawTransaction.getData());
            System.out.println(rawTransaction.getType());
            System.out.println(rawTransaction.getValue());
            System.out.println(rawTransaction.getGasPrice());

            if (rawTransaction instanceof SignedRawTransaction) {
                SignedRawTransaction signedResult = (SignedRawTransaction) rawTransaction;
                System.out.println(signedResult.getChainId());
                System.out.println(signedResult.getFrom());
            }
        } else {
            RuntimeTransaction runtimeTransaction = Mappers.parseCborFromBase64(raw, new TypeReference<RuntimeTransaction>() {
            });
            System.out.println(Mappers.json(runtimeTransaction));
        }

        String test = "oWJva1ggAAAAAAAAAAAAAAAApYTOzdAj85OqqBJ3srgcqL45UZc=";
        JsonNode j1 = Mappers.parseCborFromBase64(test, new TypeReference<JsonNode>() {
        });
        System.out.println(j1);
        String result = j1.fieldNames().next();
        System.out.println(Texts.base64ToHex(j1.path(result).asText()));
    }

}
