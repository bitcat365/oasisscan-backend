package logic

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func SignStatsCacheJob(ctx context.Context, svcCtx *svc.ServiceContext) {
	// Get all validators
	orderBy := "escrow"
	sortType := "desc"
	validatorList, err := svcCtx.ValidatorModel.FindAll(ctx, orderBy, sortType)
	if err != nil {
		logc.Errorf(ctx, "FindAll validators error: %v", err)
		return
	}

	// Refresh cache for each validator
	for _, validator := range validatorList {
		// Use validator address as cache key
		cacheKey := validator.EntityAddress

		stats := make([]*types.ValidatorSignStatsInfo, 0)
		resp := &types.ValidatorSignStatsResponse{
			Stats: stats,
		}

		// Get node information
		nodes, err := svcCtx.NodeModel.FindByEntityId(ctx, validator.EntityId)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "node FindByEntityId error: %v", err)
			continue
		}
		if nodes == nil {
			// Update empty result to cache
			svcCtx.LocalCache.ValidatorSignsStatsCache.Set(cacheKey, resp)
			continue
		}

		// Collect signature addresses
		signAddresses := make([]string, 0)
		for _, node := range nodes {
			signAddresses = append(signAddresses, node.ConsensusAddress)
		}

		// Get block count by days
		days, err := svcCtx.BlockSignatureModel.FindBlockCountDays(ctx)
		if err != nil {
			logc.Errorf(ctx, "FindBlockCountDays error: %v", err)
			continue
		}

		var timeResp []int64
		for i := len(days) - 1; i > 0; i-- {
			startDay := days[i].Day
			endDay := days[i].Day.AddDate(0, 0, 1)

			timeResp = append(timeResp, endDay.Unix())

			signs, err := svcCtx.BlockSignatureModel.CountSigns(ctx, signAddresses, 0, &startDay, &endDay)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "CountSigns error: %v", err)
				continue
			}

			stats = append(stats, &types.ValidatorSignStatsInfo{
				DateTime: uint64(endDay.Unix()),
				Expected: uint64(days[i].Count),
				Actual:   uint64(signs),
			})
		}

		resp.Stats = stats
		resp.Time = timeResp

		// Update cache
		svcCtx.LocalCache.ValidatorSignsStatsCache.Set(cacheKey, resp)
	}

	logc.Infof(ctx, "validator sign stats cache done.")
}
