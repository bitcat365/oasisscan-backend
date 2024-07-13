package account

import (
	"context"
	"errors"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
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

type AccountDelegationsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountDelegationsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountDelegationsLogic {
	return &AccountDelegationsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountDelegationsLogic) AccountDelegations(req *types.AccountDelegationsRequest) (resp *types.AccountDelegationsResponse, err error) {
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
	accountQuery := staking.OwnerQuery{Height: currentHeight, Owner: accountAddress}
	delegationsMap, err := l.svcCtx.Staking.DelegationsFor(l.ctx, &accountQuery)
	if err != nil {
		logc.Errorf(l.ctx, "DelegationsFor error, %v", err)
		return nil, errort.NewDefaultError()
	}
	all := make([]*types.AccountDelegationsInfo, 0)
	for validator, delegation := range delegationsMap {
		validatorInfo, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, validator.String())
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "FindOneByEntityAddress error, %v", err)
			return nil, errort.NewDefaultError()
		}
		info := &types.AccountDelegationsInfo{
			Shares: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(delegation.Shares.ToBigInt(), common.Decimals)),
		}
		if validatorInfo != nil {
			sharePool := staking.SharePool{
				Balance:     *quantity.NewFromUint64(uint64(validatorInfo.Escrow)),
				TotalShares: *quantity.NewFromUint64(uint64(validatorInfo.TotalShares)),
			}
			amount, err := sharePool.StakeForShares(&delegation.Shares)
			if err != nil {
				logc.Errorf(l.ctx, "delegator StakeForShares error, %v", err)
				return nil, errort.NewDefaultError()
			}
			info.ValidatorAddress = validator.String()
			info.ValidatorName = validatorInfo.Name
			info.Icon = validatorInfo.Icon
			info.Active = validatorInfo.Nodes == 1
			info.Amount = fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(amount.ToBigInt(), common.Decimals))
		} else {
			info.EntityAddress = validator.String()
			info.Amount = fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(delegation.Shares.ToBigInt(), common.Decimals))
		}
		all = append(all, info)
	}
	//sort by amount
	sort.SliceStable(all, func(i, j int) bool {
		amountI, _ := strconv.ParseFloat(all[i].Amount, 64)
		amountJ, _ := strconv.ParseFloat(all[j].Amount, 64)
		return amountI > amountJ
	})

	list := common.PageLimit(all, req.Page, req.Size)
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, int64(len(all))),
		TotalSize: int64(len(all)),
	}
	resp = &types.AccountDelegationsResponse{
		List: list,
		Page: page,
	}
	return
}
