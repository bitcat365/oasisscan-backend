package runtime

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"sort"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type RuntimeStatsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewRuntimeStatsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *RuntimeStatsLogic {
	return &RuntimeStatsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *RuntimeStatsLogic) RuntimeStats(req *types.RuntimeStatsRequest) (resp *types.RuntimeStatsResponse, err error) {
	v, err := l.svcCtx.LocalCache.RuntimeStatsCache.Take(req.Id, func() (interface{}, error) {
		list := make([]*types.RuntimeStatsInfo, 0)
		var online, offline int64 = 0, 0
		resp = &types.RuntimeStatsResponse{
			Online:  online,
			Offline: offline,
			List:    list,
		}
		entities, err := l.svcCtx.RuntimeNodeModel.FindEntities(l.ctx, req.Id)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "find runtime entities error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if entities == nil || len(entities) == 0 {
			return resp, nil
		}

		chainStatus, err := l.svcCtx.Consensus.GetStatus(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "GetStatus error, %v", err)
			return nil, errort.NewDefaultError()
		}
		currentHeight := chainStatus.LatestHeight

		nodes, err := l.svcCtx.Registry.GetNodes(l.ctx, currentHeight)
		if err != nil {
			logc.Errorf(l.ctx, "GetNodes error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if nodes == nil || len(nodes) == 0 {
			return resp, nil
		}

		onlineNodeSet := make(map[string]bool)
		for _, node := range nodes {
			runtimeList := node.Runtimes
			if len(runtimeList) == 0 {
				continue
			}
			for _, r := range runtimeList {
				if r.ID.String() == req.Id {
					onlineNodeSet[node.EntityID.String()] = true
					break
				}
			}
		}

		for _, entity := range entities {
			validatorInfo, err := l.svcCtx.ValidatorModel.FindOneByEntityId(l.ctx, entity)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(l.ctx, "find validator error, %v", err)
				return nil, errort.NewDefaultError()
			}

			isValidator, validatorName, validatorIcon, address, status := false, "", "", "", false
			if validatorInfo != nil {
				isValidator = true
				validatorName = validatorInfo.Name
				validatorIcon = validatorInfo.Icon
				address = validatorInfo.EntityAddress
			}
			if address == "" {
				stakingAddress, err := common.PubKeyToBech32Address(l.ctx, entity)
				if err != nil {
					logc.Errorf(l.ctx, "entityAddress error, %v", err)
					return nil, errort.NewDefaultError()
				}
				address = stakingAddress.String()
			}
			if onlineNodeSet[entity] {
				online++
				status = true
			}

			stats := &types.RuntimeStatsItem{
				Elected:  0,
				Primary:  0,
				Backup:   0,
				Proposer: 0,
			}
			statsModel, err := l.svcCtx.RuntimeStatsModel.FindOneByRuntimeIdEntityId(l.ctx, req.Id, entity)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(l.ctx, "find stats info error, %v", err)
				return nil, errort.NewDefaultError()
			}
			if statsModel != nil {
				stats.Elected = statsModel.RoundsElected
				stats.Primary = statsModel.RoundsPrimary
				stats.Backup = statsModel.RoundsBackup
				stats.Proposer = statsModel.RoundsProposer
			}

			statsInfo := &types.RuntimeStatsInfo{
				EntityId:  entity,
				Name:      validatorName,
				Address:   address,
				Validator: isValidator,
				Icon:      validatorIcon,
				Status:    status,
				Stats:     stats,
			}
			list = append(list, statsInfo)
		}
		sort.Slice(list, func(i, j int) bool {
			return list[i].Stats.Elected > list[j].Stats.Elected
		})
		offline = int64(len(list)) - online
		resp.Online = online
		resp.Offline = offline
		resp.List = list

		return resp, nil
	})
	if err != nil {
		logc.Errorf(l.ctx, "cache error, %v", err)
		return nil, errort.NewDefaultError()
	}
	resp = v.(*types.RuntimeStatsResponse)
	return
}
