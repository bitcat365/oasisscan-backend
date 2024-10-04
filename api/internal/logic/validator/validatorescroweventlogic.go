package validator

import (
	"context"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
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
	for _, tx := range txs {
		fmt.Println(tx.TxHash)
	}
	return
}
