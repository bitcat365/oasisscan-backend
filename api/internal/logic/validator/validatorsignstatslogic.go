package validator

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
	v, err := l.svcCtx.LocalCache.MarketCache.Take(req.Address, func() (interface{}, error) {
		stats := make([]*types.ValidatorSignStatsInfo, 0)
		resp = &types.ValidatorSignStatsResponse{
			Stats: stats,
		}

		validator, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, req.Address)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "validator FindOneByEntityAddress error, %v", err)
			return nil, errort.NewDefaultError()
		}

		if validator == nil {
			return resp, nil
		}
		nodes, err := l.svcCtx.NodeModel.FindByEntityId(l.ctx, validator.EntityId)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "node FindByEntityId error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if nodes == nil {
			return resp, nil
		}
		signAddresses := make([]string, 0)
		for _, node := range nodes {
			signAddresses = append(signAddresses, node.ConsensusAddress)
		}

		days, err := l.svcCtx.BlockSignatureModel.FindBlockCountDays(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "FindBlockCountDays error, %v", err)
			return nil, errort.NewDefaultError()
		}
		var timeResp []int64
		for i := len(days) - 1; i > 0; i-- {
			startDay := days[i].Day
			endDay := days[i].Day.AddDate(0, 0, 1)

			timeResp = append(timeResp, endDay.Unix())

			signs, err := l.svcCtx.BlockSignatureModel.CountSigns(l.ctx, signAddresses, 0, &startDay, &endDay)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(l.ctx, "CountSigns error, %v", err)
				return nil, errort.NewDefaultError()
			}
			stats = append(stats, &types.ValidatorSignStatsInfo{
				DateTime: uint64(endDay.Unix()),
				Expected: uint64(days[i].Count),
				Actual:   uint64(signs),
			})
		}
		resp.Stats = stats
		resp.Time = timeResp

		return resp, nil
	})
	if err != nil {
		logc.Errorf(l.ctx, "cache error, %v", err)
		return nil, errort.NewDefaultError()
	}
	resp = v.(*types.ValidatorSignStatsResponse)
	return
}
