package account

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

type AccountStakingEventsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountStakingEventsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountStakingEventsLogic {
	return &AccountStakingEventsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountStakingEventsLogic) AccountStakingEvents(req *types.AccountStakingEventsRequest) (resp *types.AccountStakingEventsResponse, err error) {
	list := make([]*types.AccountStakingEventsInfo, 0)
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	events, err := l.svcCtx.StakingEventModel.FindByAddress(l.ctx, req.Address, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "staking event FindByAddress error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for _, event := range events {
		list = append(list, &types.AccountStakingEventsInfo{
			Id:     fmt.Sprintf("%d_%d", event.Height, event.Position),
			Height: event.Height,
			TxHash: event.TxHash,
			Kind:   event.Kind,
		})
	}

	totalSize, err := l.svcCtx.StakingEventModel.CountByAddress(l.ctx, req.Address)
	if err != nil {
		logc.Errorf(l.ctx, "staking event CountByDelegator error: %v", err)
		return nil, errort.NewDefaultError()
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}
	resp = &types.AccountStakingEventsResponse{
		List: list,
		Page: page,
	}
	return
}
