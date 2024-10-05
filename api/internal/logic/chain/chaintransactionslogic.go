package chain

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	"github.com/oasisprotocol/oasis-core/go/consensus/api/transaction"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"oasisscan-backend/job/model"
	"time"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ChainTransactionsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewChainTransactionsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ChainTransactionsLogic {
	return &ChainTransactionsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ChainTransactionsLogic) ChainTransactions(req *types.ChainTransactionsRequest) (resp *types.ChainTransactionsResponse, err error) {
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	txs, err := l.svcCtx.TransactionModel.FindTxs(l.ctx, req.Height, req.Address, req.Method, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find txs error, %v", err)
		return nil, errort.NewDefaultError()
	}
	list := make([]*types.ChainTransactionListInfo, 0)
	for _, tx := range txs {
		txResponse, err := FormatTx(tx, l.ctx, l.svcCtx)
		if err != nil {
			logc.Errorf(l.ctx, "FormatTx error, %v", err)
			return nil, errort.NewDefaultError()
		}
		list = append(list, txResponse)
	}

	totalSize, err := l.svcCtx.TransactionModel.CountTxs(l.ctx, req.Height, req.Address, req.Method)
	if err != nil {
		logc.Errorf(l.ctx, "transaction CountTxs error: %v", err)
		return nil, errort.NewDefaultError()
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}
	resp = &types.ChainTransactionsResponse{
		List: list,
		Page: page,
	}
	return
}

func FormatTx(tx *model.Transaction, ctx context.Context, svcCtx *svc.ServiceContext) (*types.ChainTransactionListInfo, error) {
	var raw transaction.Transaction
	err := json.Unmarshal([]byte(tx.Raw), &raw)
	if err != nil {
		logc.Errorf(ctx, "raw json error, %v", err)
		return nil, errort.NewDefaultError()
	}

	amount := fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(tx.Amount), common.Decimals))
	shares := fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(tx.Shares), common.Decimals))
	add := true
	if tx.Method == "staking.ReclaimEscrow" {
		validatorAccount, err := accountInfo(ctx, svcCtx, tx)
		if err != nil {
			logc.Errorf(ctx, "account info error, %v", err)
			return nil, errort.NewDefaultError()
		}
		sharePool := staking.SharePool{
			Balance:     validatorAccount.Escrow.Active.Balance,
			TotalShares: validatorAccount.Escrow.Active.TotalShares,
		}
		amountQuantity, err := sharePool.StakeForShares(quantity.NewFromUint64(uint64(tx.Shares)))
		if err != nil {
			logc.Errorf(ctx, "compute StakeForShares error, %v", err)
			return nil, errort.NewDefaultError()
		}
		amount = fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(amountQuantity.ToBigInt(), common.Decimals))
		add = false
	} else if tx.Method == "staking.AddEscrow" {
		validatorAccount, err := accountInfo(ctx, svcCtx, tx)
		if err != nil {
			logc.Errorf(ctx, "account info error, %v", err)
			return nil, errort.NewDefaultError()
		}
		//shares = amount * total_shares / balance
		a := quantity.NewFromUint64(uint64(tx.Amount))
		t := validatorAccount.Escrow.Active.TotalShares
		b := validatorAccount.Escrow.Active.Balance
		if !b.IsZero() {
			q := a.Clone()
			if err := q.Mul(&t); err != nil {
				logc.Errorf(ctx, "shares compute error, %v", err)
				return nil, errort.NewDefaultError()
			}
			if err := q.Quo(&b); err != nil {
				logc.Errorf(ctx, "shares compute error, %v", err)
				return nil, errort.NewDefaultError()
			}
			shares = fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(q.ToBigInt(), common.Decimals))
		}
		add = true
	} else if tx.Method == "staking.Allow" {
		var t staking.Allow
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			logc.Errorf(ctx, "cbor allow error, %v", err)
			return nil, errort.NewDefaultError()
		}
		add = !t.Negative
	}
	txResponse := &types.ChainTransactionListInfo{
		TxHash:    tx.TxHash,
		Height:    tx.Height,
		Method:    tx.Method,
		Fee:       fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(tx.Fee), common.Decimals)),
		Amount:    amount,
		Shares:    shares,
		Add:       add,
		Timestamp: uint64(tx.Timestamp.Unix()),
		Time:      uint64(time.Now().Unix() - tx.Timestamp.Unix()),
		Status:    tx.Status,
		From:      tx.SignAddr,
		To:        tx.ToAddr,
	}
	return txResponse, errort.NewDefaultError()
}

func accountInfo(ctx context.Context, svcCtx *svc.ServiceContext, tx *model.Transaction) (*staking.Account, error) {
	var validatorAddress staking.Address
	err := validatorAddress.UnmarshalText([]byte(tx.ToAddr))
	if err != nil {
		return nil, fmt.Errorf("address error, %v", err)
	}
	accountQuery := staking.OwnerQuery{Height: tx.Height - 1, Owner: validatorAddress}
	validatorAccount, err := svcCtx.Staking.Account(ctx, &accountQuery)
	if err != nil {
		return nil, fmt.Errorf("staking account error, %v", err)
	}
	return validatorAccount, nil
}
