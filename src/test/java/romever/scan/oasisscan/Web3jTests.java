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
    public void test2() throws IOException {
        ApiClient apiClient = new ApiClient("http://135.181.112.38:9180", "oasisscan_testnet");
        String address = apiClient.base64ToBech32Address("ANJ3lLMjzUOc99mE5+BiZPLvCMYs");
        System.out.println(address);
    }

    @Test
    public void test3() throws IOException {
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
        String code = "glkBS/kBSICAg6A3oJTqP1RA2GzN78+kgvXeuT0Nzv/7doC45HcDjQ0AAAAAAAAAAAAAAAA4MQsNt+BLV5HS3I30BPg4OJYEcwAAAAAAAAAAAAAAAFGwrFP8kw+cm3j83XLCtcaVQwS0AAAAAAAAAAAAAAAACj7eKcTH6JKsPgoCTHGOclo33DkAAAAAAAAAAAAAAABceKZa1tDsZhh4i26OIR8xcpERygAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACdY4MzKaBUiRAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA//////////4MBSk+gPmOFZmIBPh7MenEhQpuF0edDvLCkAhAl0pvdN4MftcagMzbM5pQaCNHZAUpODO6WNL2ydRG6mRmzD9Sb02mxh+6BoWZtb2R1bGVvZXZtLmV0aGVyZXVtLnYw";
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

        String raw = "pmZoZWFkZXKlZXJvdW5kGgACNO5naW9fcm9vdFggdBXDQ9xCjz4D+8xc1uelowG4vD+pbl40eVMlLn3MysBqc3RhdGVfcm9vdFggCHAD3+bVoxCPl19FbvTviefvgbxyUm5MtOvU+O8uwWxtbWVzc2FnZXNfaGFzaFggCh8JJks9I467NMq9H+U+fxU7/2JAzeA5ETpTyxebSM5tcHJldmlvdXNfaGFzaFggK6I+LmbqnrshN7DI3gr0xALkwv5TpoEFOHNwdQFd6atncmFrX3NpZ1hAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGppbnB1dF9yb290WCCTVVN1PJpdbiZDhtOTJH9pe/o4/Zu8pq58Q2v/p1q4+210eG5fc2NoZWRfc2lnomlzaWduYXR1cmVYQC23RBJROx9brKG11wQqiz1NMId7RJ1XTQEIMiTYfwe73GMR8vXMeCUjf9qPCautHLa2wvmfD67xYpdyKSLK3AJqcHVibGljX2tleVggDRpdX8GoGUrGCX1tLNQFI/KfKd8+8L8mwtAOc0yGXX5yaW5wdXRfc3RvcmFnZV9zaWdzg6Jpc2lnbmF0dXJlWEBRAhqK7d0Yu6rzs1hzOAjiLGvzZrSMx9dQtMORSJYsXS7VOLKtX5dB3hXlamAC9Bs55wPR+fgklh4BwTog90gDanB1YmxpY19rZXlYICE68BHpHWTal6xM78Nxk+kNwLRw3edX9fZw9p1VP39HomlzaWduYXR1cmVYQF3p+4U4h/WtgURDk5B4sAarQfQd+Tdfx6J8pa/YTieJnQxIjBLMgKF7qNN4NvwHUnJGonWEVq/VQyId1xkxnAhqcHVibGljX2tleVggUkkubHbsH4ptdieTlL2qbki94Ff6xp5RNz08hIGx4oyiaXNpZ25hdHVyZVhAp97cF1xAuAPMFA6zXtJI+e20sjUdxDCtQ8BQEhWkStF9EB/ekYiee1Bpo1kb0IdSV+0MquUQKFGXRFHZzJIyD2pwdWJsaWNfa2V5WCD4k46no4ybOV2b0PLsKduX8UTrps2RTRMmq45idAi/u3JzdG9yYWdlX3NpZ25hdHVyZXODomlzaWduYXR1cmVYQM7DZQ1T5zDJd82WxsN/4SKffY/DGPUTeeRDSObMNHcmhtUMOXkNYcD2OYVtmwCJXs9AL/luwMcnke5MWiAu7AdqcHVibGljX2tleVggUkkubHbsH4ptdieTlL2qbki94Ff6xp5RNz08hIGx4oyiaXNpZ25hdHVyZVhAy6AAyNiyGrVC0G2/14kzUpnDM7ML4ANw2aYgfaaH6xK26OCDAxcdHC2oijfXI4vi/pvDzbzGdoc7F81NKWsQAGpwdWJsaWNfa2V5WCAhOvAR6R1k2pesTO/DcZPpDcC0cN3nV/X2cPadVT9/R6Jpc2lnbmF0dXJlWECx6wzL4XYzEpoE03gXqZcwmUcLqSFeaZO1iUe5/kcjnmOHA1H717iSoEFrw60ey9Jr9/3j7QFWixOGQubwXfoHanB1YmxpY19rZXlYIMFMBYTUWqrSMbfq4pU7IIGJuE4cJPHupcQFJlrWAMr2";
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
        System.out.println(txJson);
        JSONObject txJson2 = Mappers.parseCbor(txJson.getBytes("untrusted_raw_value"), new TypeReference<JSONObject>() {
        });
        System.out.println(txJson2);
        Transaction tx = Mappers.parseCbor(txJson.getBytes("untrusted_raw_value"), new TypeReference<Transaction>() {
        });
        System.out.println(Mappers.json(tx));
        if (tx != null) {
            Transaction.Body body = tx.getBody();
            if (body != null) {
                String bodyRawData = tx.getBody().getUntrusted_raw_value();
                body.setUntrusted_raw_value(null);
                JSONObject nodeJson = Mappers.parseCborFromBase64(bodyRawData, new TypeReference<JSONObject>() {
                });
                System.out.println(nodeJson);
                Node node = Mappers.parseCborFromBase64(bodyRawData, new TypeReference<Node>() {
                });
                System.out.println(Mappers.json(node));
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
