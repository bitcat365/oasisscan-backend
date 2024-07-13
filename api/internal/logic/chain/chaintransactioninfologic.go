package chain

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
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
	if tx.Method == "staking.ReclaimEscrow" {
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
		Raw:          tx.Raw,
		Status:       tx.Status,
		ErrorMessage: txError.Message,
	}

	return
}
