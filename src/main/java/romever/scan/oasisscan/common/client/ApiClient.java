package romever.scan.oasisscan.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.utils.okhttp.OkHttp;
import romever.scan.oasisscan.vo.chain.*;
import romever.scan.oasisscan.vo.chain.runtime.Runtime;
import romever.scan.oasisscan.vo.chain.runtime.*;

import java.io.IOException;
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

    private <T> void checkError(Result<T> result) throws Exception {
        if (Texts.isNotBlank(result.getError())) {
            throw new Exception(result.getError());
        }
    }

    public Block block(Long height) throws Exception {
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
            checkError(result);
            block = result.getResult();
        }
        return block;
    }

    public Long getCurHeight() throws Exception {
        Long curHeight = null;
        Block block = block(null);
        if (block != null) {
            curHeight = block.getHeight();
        }
        return curHeight;
    }

    public List<String> transactions(long height) throws Exception {
        List<String> transactions = null;
        String url = String.format("%s/api/consensus/transactions/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("height", height);
        Result<List<String>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<String>>>() {
        });
        if (result != null) {
            checkError(result);
            transactions = result.getResult();
        }
        return transactions;
    }

    public TransactionWithResult transactionswithresults(long height) throws Exception {
        TransactionWithResult transactionWithResult = null;
        String url = String.format("%s/api/consensus/transactionswithresults/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("height", height);
        Result<TransactionWithResult> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<TransactionWithResult>>() {
        });
        if (result != null) {
            checkError(result);
            transactionWithResult = result.getResult();
        }
        return transactionWithResult;
    }

    public long epoch(Long height) throws IOException {
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

    public long epochBlock(Long epoch) throws IOException {
        long height = 0;
        String url = String.format("%s/api/consensus/epochblock/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (epoch != null) {
            params.put("epoch", epoch);
        }
        Result<Long> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Long>>() {
        });
        if (result != null && result.getResult() != null) {
            height = result.getResult();
        }
        return height;
    }

    public AccountInfo accountInfo(String address, Long height) throws Exception {
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
            checkError(result);
            accountInfo = result.getResult();
        }
        return accountInfo;
    }

    public String pubkeyToBech32Address(String pubKey) throws Exception {
        String address = null;
        String url = String.format("%s/api/consensus/pubkeybech32address/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("consensus_public_key", Texts.urlEncode(pubKey));
        Result<String> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<String>>() {
        });
        if (result != null) {
            checkError(result);
            address = result.getResult();
        }
        return address;
    }

    public String base64ToBech32Address(String base64) throws Exception {
        String address = null;
        String url = String.format("%s/api/consensus/base64bech32address/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("address", Texts.urlEncode(base64));
        Result<String> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<String>>() {
        });
        if (result != null) {
            checkError(result);
            address = result.getResult();
        }
        return address;
    }

    public String pubkeyToTendermintAddress(String consensusId) throws Exception {
        String address = null;
        String url = String.format("%s/api/consensus/pubkeyaddress/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("consensus_public_key", Texts.urlEncode(consensusId));
        Result<String> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<String>>() {
        });
        if (result != null) {
            checkError(result);
            address = result.getResult();
        }
        return address;
    }

    public Map<String, Delegations> delegations(String address, Long height) throws Exception {
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
            checkError(result);
            delegations = result.getResult();
        }
        return delegations;
    }

    public Map<String, Delegations> delegationsTo(String address, Long height) throws Exception {
        Map<String, Delegations> delegations = null;
        String url = String.format("%s/api/staking/delegationsto/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("ownerKey", Texts.urlEncode(address));
        if (height != null) {
            params.put("height", height);
        }
        Result<Map<String, Delegations>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Map<String, Delegations>>>() {
        });
        if (result != null) {
            checkError(result);
            delegations = result.getResult();
        }
        return delegations;
    }

    public Map<String, List<Debonding>> debondingdelegations(String address, Long height) throws Exception {
        Map<String, List<Debonding>> debondingMap = null;
        String url = String.format("%s/api/staking/debondingdelegations/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("ownerKey", Texts.urlEncode(address));
        if (height != null) {
            params.put("height", height);
        }
        Result<Map<String, List<Debonding>>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Map<String, List<Debonding>>>>() {
        });
        if (result != null) {
            checkError(result);
            debondingMap = result.getResult();
        }
        return debondingMap;
    }

    public StakingGenesis stakingGenesis(Long height) throws Exception {
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
            checkError(result);
            stakingGenesis = result.getResult();
        }
        return stakingGenesis;
    }

    public RegistryGenesis registryGenesis(Long height) throws Exception {
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
            checkError(result);
            registryGenesis = result.getResult();
        }
        return registryGenesis;
    }

    public List<Node> registryNodes(Long height) throws Exception {
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
            checkError(result);
            nodes = result.getResult();
        }
        return nodes;
    }

    public List<String> accounts(Long height) throws Exception {
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
            checkError(result);
            accounts = result.getResult();
        }
        return accounts;
    }

    public List<StakingEvent> stakingEvents(Long height) throws Exception {
        List<StakingEvent> events = null;
        String url = String.format("%s/api/staking/events/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<StakingEvent>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<StakingEvent>>>() {
        });
        if (result != null) {
            checkError(result);
            events = result.getResult();
        }
        return events;
    }

    public List<SchedulerValidator> schedulerValidators(Long height) throws Exception {
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
            checkError(result);
            validators = result.getResult();
        }
        return validators;
    }

    public List<Runtime> runtimes(Long height) throws Exception {
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
            checkError(result);
            runtimes = result.getResult();
        }
        return runtimes;
    }

    public RuntimeRound roothashLatestblock(String namespace, Long height) throws Exception {
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
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public RuntimeState roothashRuntimeState(String namespace, Long height) throws Exception {
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
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public List<RoothashEvent> roothashEvents(Long height) throws Exception {
        String url = String.format("%s/api/roothash/events/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        if (height != null) {
            params.put("height", height);
        }
        Result<List<RoothashEvent>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<RoothashEvent>>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public List<RuntimeTransactionWithResult> runtimeTransactionsWithResults(String runtimeId, long round) throws Exception {
        String url = String.format("%s/api/runtime/transactionswithresults/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", runtimeId);
        params.put("round", round);
        Result<List<RuntimeTransactionWithResult>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<RuntimeTransactionWithResult>>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public RuntimeRound runtimeRound(String runtimeId, long round) throws Exception {
        String url = String.format("%s/api/runtime/block/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", runtimeId);
        params.put("round", round);
        Result<RuntimeRound> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<RuntimeRound>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public List<RuntimeEvent> runtimeEvent(String runtimeId, long round) throws Exception {
        String url = String.format("%s/api/runtime/events/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", runtimeId);
        params.put("round", round);
        Result<List<RuntimeEvent>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<RuntimeEvent>>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }


    /*============================= governance api =============================*/

    public List<Proposal> proposalList() throws Exception {
        String url = String.format("%s/api/governance/proposals/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        Result<List<Proposal>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<Proposal>>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public Proposal proposal(long id) throws Exception {
        String url = String.format("%s/api/governance/proposal/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", id);
        Result<Proposal> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<Proposal>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }

    public List<Vote> votes(long id) throws Exception {
        String url = String.format("%s/api/governance/votes/", api);
        Map<String, Object> params = Maps.newHashMap();
        params.put("name", this.name);
        params.put("id", id);
        Result<List<Vote>> result = OkHttp.of(url).queries(params).exec(new TypeReference<Result<List<Vote>>>() {
        });
        if (result != null) {
            checkError(result);
            return result.getResult();
        }
        return null;
    }
}
