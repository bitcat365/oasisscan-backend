package romever.scan.oasisscan;

import com.alibaba.fastjson.JSONObject;
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
import romever.scan.oasisscan.vo.chain.Node;
import romever.scan.oasisscan.vo.chain.Transaction;
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
        System.out.println(Texts.numberFromBase64("Ah4Z4Mm6skAAAA=="));
        System.out.println(Texts.base64Encode(new BigInteger("10000000000000000000000").toByteArray()));

        String value = "gaFmYW1vdW50GgABFWw=";
        JsonNode eventJson = Mappers.parseCborFromBase64(value, new TypeReference<JsonNode>() {
        });
        System.out.println(eventJson);

        List<EventLog> eventLogs = Mappers.parseCborFromBase64(value, new TypeReference<List<EventLog>>() {
        });
        System.out.println(eventLogs);
//        String s = "Y29uc2Vuc3VzX2FjY291bnRzAAAAAg==";
//        System.out.println(Texts.base64ToHex(s));
//        System.out.println(Texts.formatDecimals(String.valueOf(Texts.numberFromBase64("FNESDXsWAAA=")), Constants.EMERALD_DECIMALS, Constants.EMERALD_DECIMALS));
////        System.out.println(Mappers.parseCborFromBase64("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE", new TypeReference<JsonNode>() {
////        }));
//
//        String hexCompressed = Texts.base64ToHex("AwF6GNjbybMzhi3XRj5R1oTiMMkO1nAwB7NZAlH1X4BE");
//        byte[] c = Texts.hexStringToByteArray(hexCompressed);
//        byte[] uc = Texts.compressedToUncompressed(c);
//        String address = Keys.toChecksumAddress(Keys.getAddress(new BigInteger(Texts.toHex(uc), 16))).toLowerCase();
//        System.out.println(address);
    }

    @Test
    public void test2() throws Exception {
        ApiClient apiClient = new ApiClient("http://135.181.112.38:9180", "oasisscan_testnet");
        String address = apiClient.base64ToBech32Address("ANJ3lLMjzUOc99mE5+BiZPLvCMYs");
        System.out.println(address);
    }

    @Test
    public void test3() throws Exception {
        ApiClient apiClient = new ApiClient("http://135.181.112.38:9180", "oasisscan_testnet");
        String address = apiClient.pubkeyToBech32Address("vlG7mUtP7s2PsnARfyrI3mW/q4pcqRi3SHk2GxmQ2NM=");
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
        String code = "gljRo2F2AWJhaaJic2mBomVub25jZQxsYWRkcmVzc19zcGVjoWlzaWduYXR1cmWhZ2VkMjU1MTlYIC7G6fj+D5dhB3zzQW4nf/o4GB29dpGOdv438sXfh3dUY2ZlZaNjZ2FzGTqYZmFtb3VudIJAQHJjb25zZW5zdXNfbWVzc2FnZXMBZGNhbGyiZGJvZHmiYnRvVQB5MdPDzVdOa2wLZxc33J1JZFsAK2ZhbW91bnSCSIrHIwSJ6AAAQGZtZXRob2RxY29uc2Vuc3VzLkRlcG9zaXSBoWlzaWduYXR1cmVYQMP1stHIA1rJLtobnV8q9KuWmzeinOvgY6rK1Xa1OpcM+gnVqBFXNsYzPQH8l4CgbtCtjemBaVQDW1ioEUDhjwA=";
        JsonNode rawJson = Mappers.parseCborFromBase64(code, new TypeReference<JsonNode>() {
        });
        System.out.println(rawJson);
//        List<EventLog> eventLogs = Mappers.parseCborFromBase64(code, new TypeReference<List<EventLog>>() {
//        });
//        System.out.println(eventLogs);

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

//        String test = "oWRmYWlso2Rjb2RlAmZtb2R1bGVjZXZtZ21lc3NhZ2V4HGV4ZWN1dGlvbiBmYWlsZWQ6IG91dCBvZiBnYXM=";
//        JsonNode j1 = Mappers.parseCborFromBase64(test, new TypeReference<JsonNode>() {
//        });
//        System.out.println(j1);
//        String result = j1.fieldNames().next();
//        System.out.println(result);
////        System.out.println(Texts.base64ToHex(j1.path(result).asText()));
//
//        System.out.println(j1.path(result).toString());
    }

    @Test
    public void test0() throws IOException {
        System.out.println(Texts.base64ToHex("AAAAAAAAAAAAAAAAAAAAAAAAcGFyY2Vsc3RhZwAAAAI="));
        System.out.println(Texts.hexToBase64("00000000000000000000000000000000000000000000000072c8215e60d5bca7"));
//        CBORFactory f = new CBORFactory();
//        ObjectMapper mapper = new ObjectMapper(f);
//        byte[] decodedBytes = Base64.getDecoder().decode("ok03EoPvJkXdt+qlD0RG5Qxk8GQ=");
//        System.out.println(Texts.toHex(decodedBytes));
//
//        System.out.println(Texts.urlEncode("GHy8F8+OZ4QT59caArS7oK3QVH4teRdst39DfmFizgo="));
//
//        byte[] b = Texts.hexStringToByteArray("A2153069BC2FA61EAF7831DA38361565AEB21B69");
//        System.out.println(Texts.base64Encode(b));
//        byte[] c = Arrays.copyOf(b, 32);
//        System.out.println(Texts.toHex(c));

        String raw = "omlzaWduYXR1cmWiaXNpZ25hdHVyZVhAwdkugEJ2c06+LERy2DmTvGhYw2hMEM9f7nfOYL2/VbfOsbqQFXS09fLR/QfNeaaGh+/66lQRbjcveqZuW6xwCWpwdWJsaWNfa2V5WCDh4zZJ2Oe2QYYfQOqGWgbwcmw81KgR5gcUAK45X72pt3N1bnRydXN0ZWRfcmF3X3ZhbHVlWQLtpGNmZWWiY2dhcxkTiGZhbW91bnRAZGJvZHmiZnBvbGljeaNiaWRYIEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEoaU9/yrkgtZnNlcmlhbAFoZW5jbGF2ZXOhomltcl9zaWduZXJYIEAl2rfr2h++zE42N2BuAhIU0PQcbQQi/TeLKouIgYRZam1yX2VuY2xhdmVYINsvBAjpTlwEK9Jy/7DTrrns/syHPmxRbKpJcpiWap9WomltYXlfcXVlcnmhWCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIGiaW1yX3NpZ25lclggQCXat+vaH77MTjY3YG4CEhTQ9BxtBCL9N4sqi4iBhFlqbXJfZW5jbGF2ZVggc0SidcKhBx3iuonmtXURnFB+qIVkg+nAiaAozAh16lttbWF5X3JlcGxpY2F0ZfZqc2lnbmF0dXJlc4OiaXNpZ25hdHVyZVhA+a4FweC6QMJIDcOTHpt3G3osAh/rpuBXG8NQYyLd2q+drzO7LoxV5Bt/J/LciyouodE5iRaiTYkZikIO3sHqAmpwdWJsaWNfa2V5WCDfjKn8eM4sAfghfoznqlguiVJUX0EkJv4H1CyhGeEhZqJpc2lnbmF0dXJlWEBu1I+1grX6LNAB02t0O1shBoXgXRyWIprMajmcGQjUKo19debG77JFjb7x682J5RMVzWbm/a84WSXhIaVvYx8LanB1YmxpY19rZXlYILJ7PQJF1MvXi+jgTkc/NqvuNQ/PvEOAADE9sbsGEXpDomlzaWduYXR1cmVYQOCFl90xZTFPtPVPFJdbatDty2UFCGaKSsWWNZ5UV2YGJtpJslGLOOZFQsoLkLFtnGMS7boAPfkPW0z9Fhi3NgNqcHVibGljX2tleVggw3y9A0WWX9qE+6o3KgH8hAt7Zu6/62bf3TW7PoAfLPNlbm9uY2UYP2ZtZXRob2R3a2V5bWFuYWdlci5VcGRhdGVQb2xpY3k=";
//        System.out.println(new String(bb));
        System.out.println(Mappers.parseCborFromBase64(raw, new TypeReference<JsonNode>() {
        }));

//        String raw = "omlzaWduYXR1cmWiaXNpZ25hdHVyZVhADvUz1NpOMlEVaN/TLbcXEJTaKIqUqR9E1TU/Nic2iP9wYiTfdNGOYOWb43NwtTqLU/mxOOk025UPCNy94J8RDGpwdWJsaWNfa2V5WCAJXOoUgAMPYR3SSEYE1/hG+Hu9I22svVQtORZhCzMOl3N1bnRydXN0ZWRfcmF3X3ZhbHVlWHSkY2ZlZaJjZ2FzGQPoZmFtb3VudEIH0GRib2R5omd4ZmVyX3RvWCBJ5YwRqKlGH+0gicWzsnkr6DIIsO5o15obKVLUnXvPGmt4ZmVyX3Rva2Vuc0FkZW5vbmNlD2ZtZXRob2Rwc3Rha2luZy5UcmFuc2Zlcg==";
//        System.out.println(getSHA512_256(Texts.base64Decode(raw)));
//
//        JSONObject json1 = cborToJson(raw);
//        System.out.println(json1);
//        byte[] bytes = json1.getBytes("untrusted_raw_value");
//
//        JSONObject json2 = mapper.readValue(bytes, new TypeReference<JSONObject>() {
//        });
//        System.out.println(json2);
//        byte[] bytes2 = json2.getBytes("untrusted_raw_value");
//        JSONObject json3 = mapper.readValue(bytes2, new TypeReference<JSONObject>() {
//        });
//        System.out.println(json3);


        long t1 = System.currentTimeMillis();
        String txHash = Texts.sha512_256(Texts.base64Decode(raw));
        long t2 = System.currentTimeMillis();
        System.out.println(txHash + "   " + (t2 - t1));
        JSONObject txJson = Mappers.parseCborFromBase64(raw, new TypeReference<JSONObject>() {
        });
        System.out.println("aaa  " + txJson);
//        JSONObject txJson2 = Mappers.parseCborFromBase64(txJson.getString("untrusted_raw_value"), new TypeReference<JSONObject>() {
//        });
//        JSONObject txJson2 = Mappers.parseCbor(txJson.getBytes("untrusted_raw_value"), new TypeReference<JSONObject>() {
//        });
//        System.out.println("bbb  " + txJson2);
        Transaction tx = Mappers.parseCborFromBase64(txJson.getString("untrusted_raw_value"), new TypeReference<Transaction>() {
        });
        System.out.println(Mappers.json(tx));
        if (tx != null) {
            Transaction.Body body = tx.getBody();
            if (body != null) {
                String bodyRawData = tx.getBody().getUntrusted_raw_value();
                body.setUntrusted_raw_value(null);
                JSONObject nodeJson = Mappers.parseCborFromBase64(bodyRawData, new TypeReference<JSONObject>() {
                });
                System.out.println("ccc  " + nodeJson);
                Node node = Mappers.parseCborFromBase64(bodyRawData, new TypeReference<Node>() {
                });
                System.out.println("ddd  " + Mappers.json(node));
            }
        }
//
//
//        String cbor = "ZA==";
//        byte[] decodedBytes = Base64.getDecoder().decode(cbor);
//        String hex = Texts.toHex(decodedBytes);
//        System.out.println(new String(decodedBytes, "UTF-8"));
//        System.out.println(hex);
//        System.out.println(Long.parseLong(hex, 16));
//
//        System.out.println(Texts.toHex(Base64.getDecoder().decode("L6D3C0jckcSiUDLxgFYNPJezWfnwT9eQ1zvvC4P6/HA=")));

//        String res = Mappers.parseCbor(decodedBytes, new TypeReference<String>() {
//        });
////
//        System.out.println(res);
//        run();
    }

}
