package validator

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/logic/chain"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ValidatorEscrowEventLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewValidatorEscrowEventLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ValidatorEscrowEventLogic {
	return &ValidatorEscrowEventLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ValidatorEscrowEventLogic) ValidatorEscrowEvent(req *types.ValidatorEscrowEventRequest) (resp *types.ValidatorEscrowEventResponse, err error) {
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	txs, err := l.svcCtx.TransactionModel.FindEscrowEvents(l.ctx, req.Address, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "FindEscrowEvents error, %v", err)
		return nil, errort.NewDefaultError()
	}
	list := make([]*types.ChainTransactionListInfo, 0)
	for _, tx := range txs {
		txResponse, err := chain.FormatTx(tx, l.ctx, l.svcCtx)
		if err != nil {
			logc.Errorf(l.ctx, "FormatTx error, %v", err)
			return nil, errort.NewDefaultError()
		}
		list = append(list, txResponse)
	}
	totalSize, err := l.svcCtx.TransactionModel.CountEscrowEvents(l.ctx, req.Address)
	if err != nil {
		logc.Errorf(l.ctx, "transaction CountEscrowEvents error: %v", err)
		return nil, errort.NewDefaultError()
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}
	resp = &types.ValidatorEscrowEventResponse{
		List: list,
		Page: page,
	}
	return
}
