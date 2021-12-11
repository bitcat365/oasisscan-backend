package romever.scan.oasisscan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.web3j.crypto.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeTransaction;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;

public class Web3jTests {

    @Test
    public void test1() throws IOException {
//        System.out.println(Texts.numberFromBase64("Fx7SLFPNAAA="));
        System.out.println(Texts.base64ToHex("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE"));
        System.out.println(Mappers.parseCborFromBase64("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE", new TypeReference<JsonNode>() {
        }));

        String hexCompressed = Texts.base64ToHex("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE");
        byte[] c = Texts.hexStringToByteArray(hexCompressed);
        byte[] uc = Texts.compressedToUncompressed(c);
        String address = Keys.toChecksumAddress(Keys.getAddress(new BigInteger(Texts.toHex(uc), 16))).toLowerCase();
        System.out.println(address);
    }

    public static void main(String[] args) throws IOException, SignatureException {
        String code = "gljYo2F2AWJhaaJic2mBomVub25jZRdsYWRkcmVzc19zcGVjoWlzaWduYXR1cmWhbHNlY3AyNTZrMWV0aFghAwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BEY2ZlZaNjZ2FzGcNQZmFtb3VudIJAQHJjb25zZW5zdXNfbWVzc2FnZXMBZGNhbGyiZGJvZHmiYnRvVQDLxDmcjHzC3w7mCnVjAbteQpANqWZhbW91bnSCSBTREg17FgAAQGZtZXRob2RyY29uc2Vuc3VzLldpdGhkcmF3gaFpc2lnbmF0dXJlWEcwRQIhAKGkMfZEcdVDVfJ2sFSrEr6wWNkJFnOmWxEe49Za2fdpAiAPtGjXQvP8nLzEhRkfXDoRXHSRBCLzjmi1FNak8GExTw==";
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

        if (type.contains("evm.ethereum")) {
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
            System.out.println(Mappers.json(Mappers.parseCborFromBase64(raw, new TypeReference<JsonNode>() {
            })));
            RuntimeTransaction runtimeTransaction = Mappers.parseCborFromBase64(raw, new TypeReference<RuntimeTransaction>() {
            });
            System.out.println(Mappers.json(runtimeTransaction));
        }

        String test = "oWRmYWlso2Rjb2RlAmZtb2R1bGVjZXZtZ21lc3NhZ2V4HGV4ZWN1dGlvbiBmYWlsZWQ6IG91dCBvZiBnYXM=";
        JsonNode j1 = Mappers.parseCborFromBase64(test, new TypeReference<JsonNode>() {
        });
        System.out.println(j1);
        String result = j1.fieldNames().next();
        System.out.println(result);
//        System.out.println(Texts.base64ToHex(j1.path(result).asText()));

        System.out.println(j1.path(result).toString());
    }

}
