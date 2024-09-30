package validator

import (
	"context"
	"errors"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"strconv"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type DelegatorsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewDelegatorsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *DelegatorsLogic {
	return &DelegatorsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *DelegatorsLogic) Delegators(req *types.DelegatorsRequest) (resp *types.DelegatorsResponse, err error) {
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	validator, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, req.Address)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "FindOneByEntityAddress error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if validator == nil {
		return nil, nil
	}
	delegatorCount := validator.Delegators
	if delegatorCount == 0 {
		return
	}

	delegators, err := l.svcCtx.DelegatorModel.FindByValidator(l.ctx, req.Address, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "FindByValidator error, %v", err)
		return nil, errort.NewDefaultError()
	}
	list := make([]*types.DelegatorsInfo, 0)
	for _, delegator := range delegators {
		sharePool := staking.SharePool{
			Balance:     *quantity.NewFromUint64(uint64(validator.Escrow)),
			TotalShares: *quantity.NewFromUint64(uint64(validator.TotalShares)),
		}
		amount, err := sharePool.StakeForShares(quantity.NewFromUint64(uint64(delegator.Shares)))
		if err != nil {
			logc.Errorf(l.ctx, "delegator StakeForShares error, %v", err)
			return nil, errort.NewDefaultError()
		}
		p, _ := (new(big.Float).Quo(new(big.Float).SetInt64(delegator.Shares), new(big.Float).SetInt64(validator.TotalShares))).Float64()
		percent, err := strconv.ParseFloat(fmt.Sprintf("%.4f", p), 64)
		if err != nil {
			logc.Errorf(l.ctx, "percent compute error, %v", err)
			return nil, errort.NewDefaultError()
		}
		list = append(list, &types.DelegatorsInfo{
			Address: delegator.Delegator,
			Amount:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(amount.ToBigInt(), common.Decimals)),
			Shares:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(delegator.Shares), common.Decimals)),
			Percent: percent,
			Self:    delegator.Delegator == validator.EntityAddress,
		})
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, delegatorCount),
		TotalSize: delegatorCount,
	}
	resp = &types.DelegatorsResponse{
		List: list,
		Page: page,
	}
	return
}
