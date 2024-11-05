package validator

import (
	"context"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ValidatorSignStatsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewValidatorSignStatsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ValidatorSignStatsLogic {
	return &ValidatorSignStatsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ValidatorSignStatsLogic) ValidatorSignStats(req *types.ValidatorSignStatsRequest) (resp *types.ValidatorSignStatsResponse, err error) {
	// Get data directly from cache
	v, _ := l.svcCtx.LocalCache.ValidatorSignsStatsCache.Get(req.Address)
	if v == nil {
		// Return empty result if no data in cache
		return &types.ValidatorSignStatsResponse{
			Stats: make([]*types.ValidatorSignStatsInfo, 0),
			Time:  make([]int64, 0),
		}, nil
	}

	resp = v.(*types.ValidatorSignStatsResponse)
	return resp, nil
}
