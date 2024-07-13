package account

import (
	"context"
	"errors"
	"fmt"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"
	"sort"
	"strconv"

	"github.com/zeromicro/go-zero/core/logx"
)

type AccountDebondingLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountDebondingLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountDebondingLogic {
	return &AccountDebondingLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountDebondingLogic) AccountDebonding(req *types.AccountDebondingRequest) (resp *types.AccountDebondingResponse, err error) {
	var accountAddress staking.Address
	err = accountAddress.UnmarshalText([]byte(req.Address))
	if err != nil {
		logc.Errorf(l.ctx, "account address unmarshal error, %v", err)
		return nil, errort.NewDefaultError()
	}
	chainStatus, err := l.svcCtx.Consensus.GetStatus(l.ctx)
	if err != nil {
		logc.Errorf(l.ctx, "GetStatus error, %v", err)
		return nil, errort.NewDefaultError()
	}
	currentHeight := chainStatus.LatestHeight
	currentEpoch := chainStatus.LatestEpoch
	accountQuery := staking.OwnerQuery{Height: currentHeight, Owner: accountAddress}
	debondingMap, err := l.svcCtx.Staking.DebondingDelegationsFor(l.ctx, &accountQuery)
	if err != nil {
		logc.Errorf(l.ctx, "DebondingDelegationsFor error, %v", err)
		return nil, errort.NewDefaultError()
	}
	all := make([]*types.AccountDebondingInfo, 0)
	for validator, delegations := range debondingMap {
		validatorInfo, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, validator.String())
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "FindOneByEntityAddress error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if validatorInfo == nil {
			continue
		}
		for _, delegation := range delegations {
			all = append(all, &types.AccountDebondingInfo{
				ValidatorAddress: validator.String(),
				ValidatorName:    validatorInfo.Name,
				Icon:             validatorInfo.Icon,
				Shares:           fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(delegation.Shares.ToBigInt(), common.Decimals)),
				DebondEnd:        int64(delegation.DebondEndTime),
				EpochLeft:        int64(delegation.DebondEndTime - currentEpoch),
			})
		}
	}
	//sort by shares
	sort.SliceStable(all, func(i, j int) bool {
		sharesI, _ := strconv.ParseFloat(all[i].Shares, 64)
		sharesJ, _ := strconv.ParseFloat(all[j].Shares, 64)
		return sharesI > sharesJ
	})

	list := common.PageLimit(all, req.Page, req.Size)
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, int64(len(all))),
		TotalSize: int64(len(all)),
	}
	resp = &types.AccountDebondingResponse{
		List: list,
		Page: page,
	}

	return
}
