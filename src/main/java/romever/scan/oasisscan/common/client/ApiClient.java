package romever.scan.oasisscan.common.client;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.utils.okhttp.OkHttp;
import romever.scan.oasisscan.vo.chain.*;
import romever.scan.oasisscan.vo.chain.runtime.Runtime;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeRound;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeState;
import romever.scan.oasisscan.vo.chain.runtime.RuntimeTransactionWithResult;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author li
 */
@Slf4j
@AllArgsConstructor
public class ApiClient {

    private String api;
    private String name;

    public Block block(Long height) {
        Block block = null;
        String url = String.format("%s/api/consensus/block/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<Block> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Block>>() {
        });
        if (result != null) {
            block = result.getResult();
        }
        return block;
    }

    public Long getCurHeight() {
        Long curHeight = null;
        Block block = block(null);
        if (block != null) {
            curHeight = block.getHeight();
        }
        return curHeight;
    }

    public List<String> transactions(long height) {
        List<String> transactions = null;
        String url = String.format("%s/api/consensus/transactions/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("height", height);
        Result<List<String>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<String>>>() {
        });
        if (result != null) {
            transactions = result.getResult();
        }
        return transactions;
    }

    public TransactionWithResult transactionswithresults(long height) {
        TransactionWithResult transactionWithResult = null;
        String url = String.format("%s/api/consensus/transactionswithresults/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("height", height);
        Result<TransactionWithResult> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<TransactionWithResult>>() {
        });
        if (result != null) {
            transactionWithResult = result.getResult();
        }
        return transactionWithResult;
    }

    public long epoch(Long height) {
        long epoch = 0;
        String url = String.format("%s/api/consensus/epoch/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<Long> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Long>>() {
        });
        if (result != null && result.getResult() != null) {
            epoch = result.getResult();
        }
        return epoch;
    }

    public AccountInfo accountInfo(String address, Long height) {
        AccountInfo accountInfo = null;
        String url = String.format("%s/api/staking/accountinfo/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("ownerKey", Texts.urlEncode(address));
        if (height != null) {
            params.put("height", height);
        }
        Result<AccountInfo> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<AccountInfo>>() {
        });
        if (result != null) {
            accountInfo = result.getResult();
        }
        return accountInfo;
    }

    public String pubkeyToBech32Address(String pubKey) {
        String address = null;
        String url = String.format("%s/api/consensus/pubkeybech32address/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("consensus_public_key", Texts.urlEncode(pubKey));
        Result<String> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<String>>() {
        });
        if (result != null) {
            address = result.getResult();
        }
        return address;
    }

    public String base64ToBech32Address(String base64) {
        String address = null;
        String url = String.format("%s/api/consensus/base64bech32address/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("address", Texts.urlEncode(base64));
        Result<String> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<String>>() {
        });
        if (result != null) {
            address = result.getResult();
        }
        return address;
    }

    public String pubkeyToTendermintAddress(String consensusId) {
        String address = null;
        String url = String.format("%s/api/consensus/pubkeyaddress/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("consensus_public_key", Texts.urlEncode(consensusId));
        Result<String> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<String>>() {
        });
        if (result != null) {
            address = result.getResult();
        }
        return address;
    }

    public Map<String, Delegations> delegations(String address, Long height) {
        Map<String, Delegations> delegations = null;
        String url = String.format("%s/api/staking/delegations/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("ownerKey", Texts.urlEncode(address));
        if (height != null) {
            params.put("height", height);
        }
        Result<Map<String, Delegations>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Map<String, Delegations>>>() {
        });
        if (result != null) {
            delegations = result.getResult();
        }
        return delegations;
    }

    public StakingGenesis stakingGenesis(Long height) {
        StakingGenesis stakingGenesis = null;
        String url = String.format("%s/api/staking/genesis/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<StakingGenesis> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<StakingGenesis>>() {
        });
        if (result != null) {
            stakingGenesis = result.getResult();
        }
        return stakingGenesis;
    }

    public RegistryGenesis registryGenesis(Long height) {
        RegistryGenesis registryGenesis = null;
        String url = String.format("%s/api/registry/genesis/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<RegistryGenesis> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<RegistryGenesis>>() {
        });
        if (result != null) {
            registryGenesis = result.getResult();
        }
        return registryGenesis;
    }

    public List<Node> registryNodes(Long height) {
        List<Node> nodes = null;
        String url = String.format("%s/api/registry/nodes/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<Node>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<Node>>>() {
        });
        if (result != null) {
            nodes = result.getResult();
        }
        return nodes;
    }

    public List<String> accounts(Long height) {
        List<String> accounts = null;
        String url = String.format("%s/api/staking/accounts/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<String>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<String>>>() {
        });
        if (result != null) {
            accounts = result.getResult();
        }
        return accounts;
    }

    public List<SchedulerValidator> schedulerValidators(Long height) {
        List<SchedulerValidator> validators = Lists.newArrayList();
        String url = String.format("%s/api/scheduler/validators/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<SchedulerValidator>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<SchedulerValidator>>>() {
        });
        if (result != null) {
            validators = result.getResult();
        }
        return validators;
    }

    public List<Runtime> runtimes(Long height) {
        List<Runtime> runtimes = Lists.newArrayList();
        String url = String.format("%s/api/registry/runtimes/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<Runtime>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<Runtime>>>() {
        });
        if (result != null) {
            runtimes = result.getResult();
        }
        return runtimes;
    }

    public RuntimeRound roothashLatestblock(String namespace, Long height) {
        String url = String.format("%s/api/roothash/latestblock/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("namespace", namespace);
        if (height != null) {
            params.put("height", height);
        }
        Result<RuntimeRound> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<RuntimeRound>>() {
        });
        if (result != null) {
            return result.getResult();
        }
        return null;
    }

    public RuntimeState roothashRuntimeState(String namespace, Long height) {
        String url = String.format("%s/api/roothash/runtimestate/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("namespace", namespace);
        if (height != null) {
            params.put("height", height);
        }
        Result<RuntimeState> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<RuntimeState>>() {
        });
        if (result != null) {
            return result.getResult();
        }
        return null;
    }

    public List<RoothashEvent> roothashEvents(Long height) {
        String url = String.format("%s/api/roothash/events/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<RoothashEvent>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<RoothashEvent>>>() {
        });
        if (result != null) {
            return result.getResult();
        }
        return null;
    }

    public List<RuntimeTransactionWithResult> runtimeTransactionsWithResults(String runtimeId, long round) {
        String url = String.format("%s/api/runtime/transactionswithresults/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", runtimeId);
        params.put("round", round);
        Result<List<RuntimeTransactionWithResult>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<RuntimeTransactionWithResult>>>() {
        });
        if (result != null) {
            return result.getResult();
        }
        return null;
    }

    public RuntimeRound runtimeRound(String runtimeId, long round) {
        String url = String.format("%s/api/runtime/block/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", runtimeId);
        params.put("round", round);
        Result<RuntimeRound> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<RuntimeRound>>() {
        });
        if (result != null) {
            return result.getResult();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
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

        String raw = "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";
        byte[] bb = Texts.base64Decode(raw);
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

    public static String getSHA512_256(byte[] bytes) {
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest messageDigest;
        String encodestr = "";
        messageDigest = DigestUtils.getSha512_256Digest();
        messageDigest.update(bytes);
        encodestr = byte2Hex(messageDigest.digest());
        return encodestr;
    }

    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                //1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }


    private static JSONObject cborToJson(String s) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(s);
        CBORFactory f = new CBORFactory();
        ObjectMapper mapper = new ObjectMapper(f);
        JSONObject json = mapper.readValue(decodedBytes, new TypeReference<JSONObject>() {
        });
        return json;
    }
}
