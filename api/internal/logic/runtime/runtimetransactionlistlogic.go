package runtime

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type RuntimeTransactionListLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewRuntimeTransactionListLogic(ctx context.Context, svcCtx *svc.ServiceContext) *RuntimeTransactionListLogic {
	return &RuntimeTransactionListLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *RuntimeTransactionListLogic) RuntimeTransactionList(req *types.RuntimeTransactionListRequest) (resp *types.RuntimeTransactionListResponse, err error) {
	runtimeId := req.Id
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	txModels, err := l.svcCtx.RuntimeTransactionModel.FindAll(l.ctx, runtimeId, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime transactions error, %v", err)
		return nil, errort.NewDefaultError()
	}
	txList := make([]types.RuntimeTransactionListInfo, 0)
	for _, m := range txModels {
		t := "consensus"
		if m.Type == "evm.ethereum.v0" {
			t = "evm"
		}
		info := types.RuntimeTransactionListInfo{
			RuntimeId: runtimeId,
			TxHash:    m.TxHash,
			Round:     m.Round,
			Result:    m.Result,
			Timestamp: uint64(m.Timestamp.Unix()),
			Type:      t,
		}
		txList = append(txList, info)
	}

	totalSize, err := l.svcCtx.RuntimeTransactionModel.CountAll(l.ctx, runtimeId)
	if err != nil {
		logc.Errorf(l.ctx, "runtime transaction CountAll error: %v", err)
		return nil, errort.NewDefaultError()
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}

	resp = &types.RuntimeTransactionListResponse{
		List: txList,
		Page: page,
	}
	return
}
