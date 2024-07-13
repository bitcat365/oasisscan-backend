package account

import (
	"context"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"oasisscan-backend/job/model"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type AccountRewardStatsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountRewardStatsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountRewardStatsLogic {
	return &AccountRewardStatsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountRewardStatsLogic) AccountRewardStats(req *types.AccountRewardStatsRequest) (resp *types.AccountRewardStatsResponse, err error) {
	statsMap := make(map[string]*types.AccountRewardStatsInfo, 0)
	resp = &types.AccountRewardStatsResponse{
		Stats: statsMap,
	}
	accountAddress := req.Account
	rewardModels, err := l.svcCtx.RewardModel.FindByDelegatorGroupByDay(l.ctx, accountAddress)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find reward error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if len(rewardModels) == 0 {
		return
	}
	days, err := l.findDays()
	if err != nil {
		logc.Errorf(l.ctx, "%v", err)
		return nil, errort.NewDefaultError()
	}
	var timeResp []int64
	timeMap := make(map[int64]bool)
	for i := len(days) - 1; i > 0; i-- {
		timeResp = append(timeResp, days[i].Day.Unix())
		timeMap[days[i].Day.Unix()] = true
	}

	for _, m := range rewardModels {
		statsInfo := statsMap[m.ValidatorAddress]
		rewardItem := types.AccountRewardStatsItem{
			DateTime: uint64(m.Day.Unix()),
			Reward:   fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Reward), common.Decimals)),
		}
		if !timeMap[m.Day.Unix()] {
			continue
		}
		if statsInfo != nil {
			rewardList := statsInfo.RewardList
			rewardList = append(rewardList, &rewardItem)
			statsInfo.RewardList = rewardList
			total := new(big.Float)
			totalFloat, _ := total.SetString(statsInfo.Total)
			total = new(big.Float).Add(totalFloat, common.ValueToFloatByDecimals(big.NewInt(m.Reward), common.Decimals))
			statsInfo.Total = total.String()
		} else {
			rewardList := make([]*types.AccountRewardStatsItem, 0)
			rewardList = append(rewardList, &rewardItem)
			statsInfo = &types.AccountRewardStatsInfo{
				ValidatorName: m.ValidatorName,
				RewardList:    rewardList,
				Total:         fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Reward), common.Decimals)),
			}
		}
		statsMap[m.ValidatorAddress] = statsInfo
	}
	resp.Stats = statsMap
	resp.Time = timeResp

	return
}

func (l *AccountRewardStatsLogic) findDays() (days []*model.StatsDay, err error) {
	v, err := l.svcCtx.LocalCache.AccountRewardFindDaysCache.Take("days", func() (interface{}, error) {
		days, err = l.svcCtx.RewardModel.FindDays(l.ctx)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			return nil, fmt.Errorf("find days error, %v", err)
		}
		return days, nil
	})
	if err != nil {
		return nil, err
	}
	return v.([]*model.StatsDay), nil
}
