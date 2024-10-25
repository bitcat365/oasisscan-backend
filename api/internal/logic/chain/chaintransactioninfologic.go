package chain

import (
	"context"
	"encoding/json"
	"fmt"
	beacon "github.com/oasisprotocol/oasis-core/go/beacon/api"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	"github.com/oasisprotocol/oasis-core/go/common/node"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	consensus "github.com/oasisprotocol/oasis-core/go/consensus/api"
	"github.com/oasisprotocol/oasis-core/go/consensus/api/transaction"
	keymanager "github.com/oasisprotocol/oasis-core/go/keymanager/api"
	roothash "github.com/oasisprotocol/oasis-core/go/roothash/api"
	"github.com/zeromicro/go-zero/core/logc"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"time"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/oasisprotocol/oasis-core/go/consensus/api/transaction/results"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logx"
)

type ChainTransactionInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewChainTransactionInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ChainTransactionInfoLogic {
	return &ChainTransactionInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ChainTransactionInfoLogic) ChainTransactionInfo(req *types.ChainTransactionInfoRequest) (resp *types.ChainTransactionInfoResponse, err error) {
	hash := req.Hash

	tx, err := l.svcCtx.TransactionModel.FindOneByTxHash(l.ctx, hash)
	if err != nil {
		logc.Errorf(l.ctx, "transaction FindOneByTxHash error, %v", err)
		return nil, errort.NewDefaultError()
	}

	var txError results.Error
	if tx.Error != "" {
		err = json.Unmarshal([]byte(tx.Error), &txError)
		if err != nil {
			logc.Errorf(l.ctx, "json unmarshal error, %v", err)
			return nil, errort.NewDefaultError()
		}
	}

	amount := fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(tx.Amount), common.Decimals))
	if tx.Method == "staking.ReclaimEscrow" {
		var validatorAddress staking.Address
		err = validatorAddress.UnmarshalText([]byte(tx.ToAddr))
		if err != nil {
			logc.Errorf(l.ctx, "address error, %v", err)
			return nil, errort.NewDefaultError()
		}
		accountQuery := staking.OwnerQuery{Height: tx.Height - 1, Owner: validatorAddress}
		validatorAccount, err := l.svcCtx.Staking.Account(l.ctx, &accountQuery)
		if err != nil {
			logc.Errorf(l.ctx, "staking account error, %v", err)
			return nil, errort.NewDefaultError()
		}
		sharePool := staking.SharePool{
			Balance:     validatorAccount.Escrow.Active.Balance,
			TotalShares: validatorAccount.Escrow.Active.TotalShares,
		}
		amountQuantity, err := sharePool.StakeForShares(quantity.NewFromUint64(uint64(tx.Shares)))
		if err != nil {
			logc.Errorf(l.ctx, "compute StakeForShares error, %v", err)
			return nil, errort.NewDefaultError()
		}
		amount = fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(amountQuantity.ToBigInt(), common.Decimals))
	}

	var rawTx transaction.Transaction
	err = json.Unmarshal([]byte(tx.Raw), &rawTx)
	if err != nil {
		logc.Errorf(l.ctx, "rawTx json Unmarshal error, %v", err)
		return nil, errort.NewDefaultError()
	}
	bodyStr, err := parseMethod(&rawTx)
	if err != nil {
		logc.Errorf(l.ctx, "parseMethod error, %v", err)
		return nil, errort.NewDefaultError()
	}

	resp = &types.ChainTransactionInfoResponse{
		TxHash:       tx.TxHash,
		Timestamp:    uint64(tx.Timestamp.Unix()),
		Time:         uint64(time.Now().Unix() - tx.Timestamp.Unix()),
		Height:       tx.Height,
		Fee:          fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(tx.Fee), common.Decimals)),
		Nonce:        tx.Nonce,
		Method:       tx.Method,
		From:         tx.SignAddr,
		To:           tx.ToAddr,
		Amount:       amount,
		Raw:          bodyStr,
		Status:       tx.Status,
		ErrorMessage: txError.Message,
	}

	return
}

func parseMethod(raw *transaction.Transaction) (string, error) {
	bodyJson, err := json.Marshal(raw.Body)
	if err != nil {
		return "", err
	}
	switch raw.Method {
	case "staking.Transfer":
		var t staking.Transfer
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "staking.AddEscrow":
		var t staking.Escrow
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "staking.ReclaimEscrow":
		var t staking.ReclaimEscrow
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "staking.Burn":
		var t staking.Burn
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "staking.Allow":
		var t staking.Allow
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "staking.Withdraw":
		var t staking.Withdraw
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "keymanager.UpdatePolicy":
		var t keymanager.SignedPolicySGX
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "roothash.ExecutorCommit":
		var t roothash.ExecutorCommit
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "registry.RegisterNode":
		var t node.Node
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "consensus.Meta":
		var t consensus.BlockMetadata
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	case "beacon.VRFProve":
		var t beacon.VRFProve
		_ = cbor.Unmarshal(raw.Body, &t)
		bodyJson, _ := json.Marshal(t)
		return string(bodyJson), nil
	}
	return string(bodyJson), nil
}
