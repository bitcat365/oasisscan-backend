package runtime

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/ethereum/go-ethereum/common/hexutil"
	eth_types "github.com/ethereum/go-ethereum/core/types"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/client"
	sdkClient "github.com/oasisprotocol/oasis-sdk/client-sdk/go/client"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/modules/accounts"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/modules/consensusaccounts"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/modules/contracts"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/modules/core"
	"github.com/oasisprotocol/oasis-sdk/client-sdk/go/modules/evm"
	sdktypes "github.com/oasisprotocol/oasis-sdk/client-sdk/go/types"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/logx"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"
	"strings"
)

type RuntimeTransactionInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewRuntimeTransactionInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *RuntimeTransactionInfoLogic {
	return &RuntimeTransactionInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *RuntimeTransactionInfoLogic) RuntimeTransactionInfo(req *types.RuntimeTransactionInfoRequest) (resp *types.RuntimeTransactionInfoResponse, err error) {
	runtimeId := req.Id
	round := req.Round
	txHash := req.Hash
	txModel, err := l.svcCtx.RuntimeTransactionModel.FindOneByRuntimeIdRoundTxHash(l.ctx, runtimeId, round, txHash)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime transaction info error, %v", err)
		return nil, errort.NewDefaultError()
	}

	if txModel == nil {
		return nil, nil
	}

	runtimeName := ""
	runtimeModel, err := l.svcCtx.RuntimeModel.FindOneByRuntimeId(l.ctx, runtimeId)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime info error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if runtimeModel != nil {
		runtimeName = runtimeModel.Name
	}

	var consensusTx *types.RuntimeTransactionConsensusTx = nil
	var evmTx *types.RuntimeTransactionEvmTx = nil
	txr := new(client.TransactionWithResults)
	if err := json.Unmarshal([]byte(txModel.Raw), txr); err != nil {
		logc.Errorf(l.ctx, "transaction json unmarshal error, %v", err)
		return nil, errort.NewDefaultError()
	}
	t := "consensus"
	if txModel.Type == "evm.ethereum.v0" {
		t = "evm"
		txData := new(eth_types.Transaction)
		if err := txData.UnmarshalBinary(txr.Tx.Body); err != nil {
			logc.Errorf(l.ctx, "txData unmarshal error, %v", err)
			return nil, errort.NewDefaultError()
		}
		evmTx = &types.RuntimeTransactionEvmTx{
			Hash:     txModel.EvmHash,
			From:     txModel.EvmFrom,
			To:       txModel.EvmTo,
			Nonce:    int64(txData.Nonce()),
			GasPrice: txData.GasPrice().Int64(),
			GasLimit: int64(txData.Gas()),
			Data:     hexutil.Encode(txData.Data()),
			Value:    txData.Value().String(),
		}
	} else {
		sdkTx, err := common.OpenUtxNoVerify(&txr.Tx)
		if err != nil {
			logc.Errorf(l.ctx, "tx decode error, %v", err)
			return nil, errort.NewDefaultError()
		}
		txData := new(common.RuntimeTransactionBody)
		if err := cbor.Unmarshal(sdkTx.Call.Body, txData); err != nil {
			logc.Errorf(l.ctx, "txData unmarshal error, %v", err)
			return nil, errort.NewDefaultError()
		}

		amountFloat := common.ValueToFloatByDecimals(txData.Amount.Amount.ToBigInt(), common.RuntimeDecimals)
		consensusTx = &types.RuntimeTransactionConsensusTx{
			Method: txModel.Method,
			From:   txModel.ConsensusFrom,
			To:     txModel.ConsensusTo,
			Amount: amountFloat.String(),
			Nonce:  int64(sdkTx.AuthInfo.SignerInfo[0].Nonce),
		}
	}

	//events
	var events []sdktypes.Event
	err = json.Unmarshal([]byte(txModel.Events), &events)
	if err != nil {
		logc.Errorf(l.ctx, "events json unmarshal error, %v", err)
		return nil, errort.NewDefaultError()
	}
	var decodeEvents []sdkClient.DecodedEvent
	for _, e := range events {
		if e.Module == accounts.ModuleName {
			de, err := accounts.DecodeEvent(&e)
			if err != nil {
				logc.Errorf(l.ctx, "decode event error, %v", err)
				return nil, errort.NewDefaultError()
			}
			decodeEvents = append(decodeEvents, de)
		} else if e.Module == consensusaccounts.ModuleName {
			de, err := consensusaccounts.DecodeEvent(&e)
			if err != nil {
				logc.Errorf(l.ctx, "decode event error, %v", err)
				return nil, errort.NewDefaultError()
			}
			decodeEvents = append(decodeEvents, de)
		} else if e.Module == contracts.ModuleName || strings.HasPrefix(e.Module, contracts.ModuleName+".") {
			de, err := contracts.DecodeEvent(&e)
			if err != nil {
				logc.Errorf(l.ctx, "decode event error, %v", err)
				return nil, errort.NewDefaultError()
			}
			decodeEvents = append(decodeEvents, de)
		} else if e.Module == core.ModuleName {
			de, err := core.DecodeEvent(&e)
			if err != nil {
				logc.Errorf(l.ctx, "decode event error, %v", err)
				return nil, errort.NewDefaultError()
			}
			decodeEvents = append(decodeEvents, de)
		} else if e.Module == evm.ModuleName {
			de, err := evm.DecodeEvent(&e)
			if err != nil {
				logc.Errorf(l.ctx, "decode event error, %v", err)
				return nil, errort.NewDefaultError()
			}
			decodeEvents = append(decodeEvents, de)
		}
	}

	resp = &types.RuntimeTransactionInfoResponse{
		RuntimeId:   runtimeId,
		RuntimeName: runtimeName,
		TxHash:      txModel.TxHash,
		Round:       txModel.Round,
		Result:      txModel.Result,
		Message:     txModel.Messages,
		Timestamp:   uint64(txModel.Timestamp.Unix()),
		Type:        t,
		Ctx:         consensusTx,
		Etx:         evmTx,
		Events:      decodeEvents,
	}
	return
}
