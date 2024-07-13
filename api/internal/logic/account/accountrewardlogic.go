package account

import (
	"context"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"

	"github.com/zeromicro/go-zero/core/logx"
)

type AccountRewardLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountRewardLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountRewardLogic {
	return &AccountRewardLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountRewardLogic) AccountReward(req *types.AccountRewardRequest) (resp *types.AccountRewardResponse, err error) {
	accountAddress := req.Account
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	rewardModels, err := l.svcCtx.RewardModel.FindByDelegator(l.ctx, accountAddress, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find reward error, %v", err)
		return nil, errort.NewDefaultError()
	}
	rewardList := make([]types.AccountRewardInfo, 0)
	for _, m := range rewardModels {
		v, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, m.Validator)
		if err != nil {
			logc.Errorf(l.ctx, "find validator info error, %v", err)
			return nil, errort.NewDefaultError()
		}
		reward := types.AccountRewardInfo{
			ValidatorAddress: v.EntityAddress,
			ValidatorName:    v.Name,
			ValidatorIcon:    v.Icon,
			Epoch:            m.Epoch,
			Timestamp:        m.CreatedAt.Unix(),
			Reward:           fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Reward), common.Decimals)),
		}
		rewardList = append(rewardList, reward)
	}

	totalSize, err := l.svcCtx.RewardModel.CountByDelegator(l.ctx, accountAddress)
	if err != nil {
		logc.Errorf(l.ctx, "reward CountByDelegator error: %v", err)
		return nil, errort.NewDefaultError()
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}

	resp = &types.AccountRewardResponse{
		List: rewardList,
		Page: page,
	}
	return
}
