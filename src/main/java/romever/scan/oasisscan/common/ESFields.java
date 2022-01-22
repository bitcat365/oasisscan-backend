package romever.scan.oasisscan.common;

public interface ESFields {
    //block
    String BLOCK_HEIGHT = "height";
    String BLOCK_PROPOSER_ADDRESS = "metadata.header.proposer_address";
    String BLOCK_SIGN_ADDRESS = "metadata.last_commit.signatures.validator_address";
    String BLOCK_TIMESTAMP = "metadata.header.time";
    String BLOCK_HASH = "hash";

    //transaction
    String TRANSACTION_TIMESTAMP = "timestamp";
    String TRANSACTION_TIME = "time";
    String TRANSACTION_HEIGHT = "height";
    String TRANSACTION_FROM = "signature.address";
    String TRANSACTION_BODY_SIG_PUBLIC_KEY = "body.signatures.public_key";
    String TRANSACTION_TRANSFER_TO = "body.to";
    String TRANSACTION_ESCROW_ACCOUNT = "body.account";
    String TRANSACTION_METHOD = "method";
    String TRANSACTION_RATE_EPOCH_START = "body.amendment.rates.start";
    String TRANSACTION_ERROR_MESSAGE = "error.message";
    String TRANSACTION_EVENT_STAKING_FROM = "events.staking.transfer.from";
    String TRANSACTION_EVENT_STAKING_TO = "events.staking.transfer.to";

    //runtime
    String RUNTIME_ROUND_NAMESPACE = "namespace";
    String RUNTIME_ROUND_ROUND = "round";

    //runtime transaction
    String RUNTIME_TRANSACTION_ID = "runtime_id";
    String RUNTIME_TRANSACTION_ROUND = "round";
    String RUNTIME_TRANSACTION_TX_HASH = "tx_hash";
    String RUNTIME_TRANSACTION_SIG_ADDRESS = "ai.si.address_spec.signature.address";
    String RUNTIME_TRANSACTION_TO = "call.body.to";
    String RUNTIME_TRANSACTION_EVENT_FROM = "events.logs.from";
    String RUNTIME_TRANSACTION_EVENT_TO = "events.logs.to";
    String RUNTIME_TRANSACTION_RESULT = "result";

    //runtime event
    String RUNTIME_EVENT_FROM = "from";
    String RUNTIME_EVENT_NONCE = "nonce";
}
