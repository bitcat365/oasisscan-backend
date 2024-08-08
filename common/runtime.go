package common

import (
	"fmt"
	ethTypes "github.com/ethereum/go-ethereum/core/types"
	"github.com/oasisprotocol/oasis-core/go/common/crypto/signature"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	sdkClient "github.com/oasisprotocol/oasis-sdk/client-sdk/go/client"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/crypto/signature/secp256k1"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/modules/evm"
	sdkTypes "github.com/oasisprotocol/oasis-sdk/client-sdk/go/types"
)

type RuntimeTransactionBody struct {
	To     sdkTypes.Address   `cbor:"to,omitempty"`
	Amount sdkTypes.BaseUnits `cbor:"amount,omitempty"`
	From   sdkTypes.Address   `cbor:"from,omitempty"`
	Shares sdkTypes.Quantity  `cbor:"shares,omitempty"`
	Pk     interface{}        `cbor:"pk,omitempty"`
	Data   interface{}        `cbor:"data,omitempty"`
	Nonce  interface{}        `cbor:"nonce,omitempty"`
}

type RuntimeEvent struct {
	From             sdkTypes.Address         `json:"from,omitempty"`
	To               sdkTypes.Address         `json:"to,omitempty"`
	Amount           quantity.Quantity        `json:"amount,omitempty"`
	Owner            sdkTypes.Address         `json:"owner,omitempty"`
	Beneficiary      sdkTypes.Address         `json:"beneficiary,omitempty"`
	Allowance        quantity.Quantity        `json:"quantity,omitempty"`
	Negative         bool                     `json:"negative,omitempty"`
	AmountChange     quantity.Quantity        `json:"amount_change,omitempty"`
	TokenInformation TokenInformationResponse `json:"token_information,omitempty"`
}

type TokenInformationResponse struct {
	// Name is the name of the token.
	Name string `json:"name"`
	// Symbol is the token symbol.
	Symbol string `json:"symbol"`
	// Decimals is the number of token decimals.
	Decimals uint8 `json:"decimals"`
	// TotalSupply is the total supply of the token.
	TotalSupply quantity.Quantity `json:"total_supply"`
	// Minting is the information about minting in case the token supports minting.
	Minting *MintingInformation `json:"minting,omitempty"`
}

type MintingInformation struct {
	Minter sdkTypes.Address   `json:"minter"`
	Cap    *quantity.Quantity `json:"cap,omitempty"`
}

type RuntimeStats struct {
	// Rounds.
	Rounds uint64
	// Successful rounds.
	SuccessfulRounds uint64
	// Failed rounds.
	FailedRounds uint64
	// Rounds failed due to proposer timeouts.
	ProposerTimeoutedRounds uint64
	// Epoch transition rounds.
	EpochTransitionRounds uint64
	// Suspended rounds.
	SuspendedRounds uint64

	// Discrepancies.
	DiscrepancyDetected        uint64
	DiscrepancyDetectedTimeout uint64

	// Per-entity stats.
	Entities map[signature.PublicKey]*RuntimeEntityStats

	EntitiesOutput [][]string
	EntitiesHeader []string
}

type RuntimeEntityStats struct {
	// Rounds entity node was elected.
	RoundsElected uint64
	// Rounds entity node was elected as primary executor worker.
	RoundsPrimary uint64
	// Rounds entity node was elected as primary executor worker and workers were invoked.
	RoundsPrimaryRequired uint64
	// Rounds entity node was elected as a backup executor worker.
	RoundsBackup uint64
	// Rounds entity node was elected as a backup executor worker
	// and backup workers were invoked.
	RoundsBackupRequired uint64
	// Rounds entity node was a proposer.
	RoundsProposer uint64

	// How many good blocks committed while being primary worker.
	CommittedGoodBlocksPrimary uint64
	// How many bad blocs committed while being primary worker.
	CommittedBadBlocksPrimary uint64
	// How many good blocks committed while being backup worker.
	CommittedGoodBlocksBackup uint64
	// How many bad blocks committed while being backup worker.
	CommittedBadBlocksBackup uint64

	// How many rounds missed committing a block while being a primary worker.
	MissedPrimary uint64
	// How many rounds missed committing a block while being a backup worker (and discrepancy detection was invoked).
	MissedBackup uint64
}

func OpenUtxNoVerify(utx *sdkTypes.UnverifiedTransaction) (*sdkTypes.Transaction, error) {
	if len(utx.AuthProofs) == 1 && utx.AuthProofs[0].Module != "" {
		switch utx.AuthProofs[0].Module {
		case "evm.ethereum.v0":
			tx, err := decodeEthRawTx(utx.Body)
			if err != nil {
				return nil, err
			}
			return tx, nil
		default:
			return nil, fmt.Errorf("module-controlled decoding scheme %s not supported", utx.AuthProofs[0].Module)
		}
	} else {
		var tx sdkTypes.Transaction
		if err := CborUnmarshalOmit(utx.Body, &tx); err != nil {
			return nil, fmt.Errorf("tx cbor unmarshal: %w", err)
		}
		if err := tx.ValidateBasic(); err != nil {
			return nil, fmt.Errorf("tx validate basic: %w", err)
		}
		return &tx, nil
	}
}

func decodeEthRawTx(body []byte) (*sdkTypes.Transaction, error) {
	var ethTx ethTypes.Transaction
	if err := ethTx.UnmarshalBinary(body); err != nil {
		return nil, fmt.Errorf("rlp decode bytes: %w", err)
	}
	evmV1 := evm.NewV1(nil)
	var tb *sdkClient.TransactionBuilder
	if to := ethTx.To(); to != nil {
		tb = evmV1.Call(to.Bytes(), ethTx.Value().Bytes(), ethTx.Data())
	} else {
		tb = evmV1.Create(ethTx.Value().Bytes(), ethTx.Data())
	}
	chainIDBI := ethTx.ChainId()
	signer := ethTypes.LatestSignerForChainID(chainIDBI)
	pubUncompressed, err := CancunSenderPub(signer, &ethTx)
	if err != nil {
		return nil, fmt.Errorf("recover signer public key: %w", err)
	}
	var sender secp256k1.PublicKey
	if err = sender.UnmarshalBinary(pubUncompressed); err != nil {
		return nil, fmt.Errorf("sdk secp256k1 public key unmarshal binary: %w", err)
	}
	// Base fee is zero. Allocate only priority fee.
	gasPrice := ethTx.GasTipCap()
	if ethTx.GasFeeCapIntCmp(gasPrice) < 0 {
		gasPrice = ethTx.GasFeeCap()
	}
	var resolvedFeeAmount quantity.Quantity
	if err = resolvedFeeAmount.FromBigInt(gasPrice); err != nil {
		return nil, fmt.Errorf("converting gas price: %w", err)
	}
	if err = resolvedFeeAmount.Mul(quantity.NewFromUint64(ethTx.Gas())); err != nil {
		return nil, fmt.Errorf("computing total fee amount: %w", err)
	}
	tb.AppendAuthSignature(sdkTypes.SignatureAddressSpec{Secp256k1Eth: &sender}, ethTx.Nonce())
	tb.SetFeeAmount(sdkTypes.NewBaseUnits(resolvedFeeAmount, sdkTypes.NativeDenomination))
	tb.SetFeeGas(ethTx.Gas())
	return tb.GetTransaction(), nil
}
