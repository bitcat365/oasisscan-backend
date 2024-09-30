package chain

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ChainMethodsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewChainMethodsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ChainMethodsLogic {
	return &ChainMethodsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ChainMethodsLogic) ChainMethods(req *types.ChainMethodsRequest) (resp *types.ChainMethodsResponse, err error) {
	methods, err := l.svcCtx.TransactionModel.TransactionMethods(l.ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find block error, %v", err)
		return nil, errort.NewDefaultError()
	}
	resp = &types.ChainMethodsResponse{
		List: methods,
	}
	return
}
