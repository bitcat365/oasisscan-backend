package romever.scan.oasisscan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.web3j.crypto.*;
import romever.scan.oasisscan.common.Constants;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.common.exception.AddressFormatException;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.crypto.Bech32;
import romever.scan.oasisscan.utils.crypto.ConvertBits;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.runtime.AbstractRuntimeTransaction;
import romever.scan.oasisscan.vo.chain.runtime.EventLog;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeTransaction;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.List;

import static org.bitcoinj.core.ECKey.CURVE;

public class Web3jTests {

    @Test
    public void test1() throws IOException {
//        System.out.println(Texts.numberFromBase64("Fx7SLFPNAAA="));
        String s = "Y29uc2Vuc3VzX2FjY291bnRzAAAAAg==";
        System.out.println(Texts.base64ToHex(s));
        System.out.println(Texts.formatDecimals(String.valueOf(Texts.numberFromBase64("FNESDXsWAAA=")), Constants.EMERALD_DECIMALS, Constants.EMERALD_DECIMALS));
//        System.out.println(Mappers.parseCborFromBase64("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE", new TypeReference<JsonNode>() {
//        }));

        String hexCompressed = Texts.base64ToHex("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE");
        byte[] c = Texts.hexStringToByteArray(hexCompressed);
        byte[] uc = Texts.compressedToUncompressed(c);
        String address = Keys.toChecksumAddress(Keys.getAddress(new BigInteger(Texts.toHex(uc), 16))).toLowerCase();
        System.out.println(address);
    }

    @Test
    public void test2() throws IOException {
        ApiClient apiClient = new ApiClient("http://135.181.112.38:9180", "oasisscan_testnet");
        String address = apiClient.base64ToBech32Address("ANJ3lLMjzUOc99mE5+BiZPLvCMYs");
        System.out.println(address);
    }

    @Test
    public void bech32() throws IOException {
        String hexCompressed = Texts.base64ToHex("AhMWP64MyA5vN7TUeGXNuPTSepI5C8L7s2S3F6bfrk/e");
        System.out.println(hexCompressed);
        byte[] c = Texts.hexStringToByteArray(hexCompressed);
        Bech32.Bech32Data data = Bech32.decode("oasis1qpl202ahu2r7fk76vzwsn644dv25n84c8yxttegj");
        System.out.println(Texts.toHex(data.getData()));

        //oasis1qpl202ahu2r7fk76vzwsn644dv25n84c8yxttegj
        String a = "93f198121A048f8f917bCEf9ee492d1aBbA0D020";

        String V0_SECP256K1ETH_CONTEXT_IDENTIFIER = "oasis-runtime-sdk/address: secp256k1eth";

        byte[] aa = Texts.concat(V0_SECP256K1ETH_CONTEXT_IDENTIFIER.getBytes(StandardCharsets.UTF_8), new byte[]{0});
        byte[] aaa = Texts.concat(aa, a.getBytes(StandardCharsets.UTF_8));

        byte[] r = Texts.slice(Texts.hexStringToByteArray(Texts.sha512_256(aaa)), 0, 20);
        byte[] address = Texts.concat(new byte[]{0}, r);

        String bech32Address = Bech32.encode("oasis", encode(address));
        System.out.println(bech32Address);

    }

    public static String createNewAddressSecp256k1(String mainPrefix, byte[] publickKey) {
        // convert 33 bytes public key to 65 bytes public key

        String addressResult = null;
        try {
            byte[] uncompressedPubKey = CURVE.getCurve().decodePoint(publickKey).getEncoded(false);
            byte[] pub = new byte[64];
            // truncate last 64 bytes to generate address
            System.arraycopy(uncompressedPubKey, 1, pub, 0, 64);

            //get address
            byte[] address = Keys.getAddress(pub);
            byte[] bytes = encode(address);
            addressResult = Bech32.encode(mainPrefix, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return addressResult;
    }

    public static String convertAddressFromHexToBech32(String hexAddress) {
        byte[] address = Texts.hexStringToByteArray(hexAddress);

        String bech32Address = null;
        try {
            byte[] bytes = encode(address);
            bech32Address = Bech32.encode("oasis", bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bech32Address;
    }

    public static byte[] encode(byte[] witnessProgram) throws AddressFormatException {
        byte[] convertedProgram = ConvertBits.convertBits(witnessProgram, 0, witnessProgram.length, 8, 5, true);
        return convertedProgram;
    }


    public static void main(String[] args) throws IOException, SignatureException {
        String code = "gaRidG9VAMvEOZyMfMLfDuYKdWMBu15CkA2pZGZyb21VAO1D91JQJv1Tegv1JEiLfFSfA5glZW5vbmNlF2ZhbW91bnSCSBTREg17FgAAQA==";
        JsonNode rawJson = Mappers.parseCborFromBase64(code, new TypeReference<JsonNode>() {
        });
        System.out.println(rawJson);
        List<EventLog> eventLogs = Mappers.parseCborFromBase64(code, new TypeReference<List<EventLog>>() {
        });
        System.out.println(eventLogs);

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
