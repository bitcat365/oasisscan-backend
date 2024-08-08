package validator

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"sort"
	"strconv"

	"github.com/zeromicro/go-zero/core/logx"
)

type ValidatorListLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewValidatorListLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ValidatorListLogic {
	return &ValidatorListLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ValidatorListLogic) ValidatorList(req *types.ValidatorListRequest) (resp *types.ValidatorListResponse, err error) {
	cacheKey := fmt.Sprintf("%s_%s", req.OrderBy, req.Sort)
	v, err := l.svcCtx.LocalCache.MarketCache.Take(cacheKey, func() (interface{}, error) {
		orderBy, sortType := "escrow", "desc"
		switch req.OrderBy {
		case "escrowChange24":
			orderBy = "escrow_24h"
		case "commission":
			orderBy = "commission"
		}
		if req.Sort == "asc" {
			sortType = "asc"
		}

		totalEscrow, err := l.svcCtx.ValidatorModel.SumEscrow(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "validator SumEscrow error, %v", err)
			return nil, errort.NewDefaultError()
		}
		totalEscrowFloat := new(big.Float).SetInt64(totalEscrow)

		var active, inactive int64 = 0, 0
		validators, err := l.svcCtx.ValidatorModel.FindAll(l.ctx, orderBy, sortType)
		if err != nil {
			logc.Errorf(l.ctx, "validator findAll error, %v", err)
			return nil, errort.NewDefaultError()
		}
		validatorList := make([]types.ValidatorInfo, 0)
		for _, validator := range validators {
			r := response.ValidatorResponseFormat(validator)
			if err != nil {
				logc.Errorf(l.ctx, "validator response format error, %v", err)
				return nil, errort.NewDefaultError()
			}

			p, _ := new(big.Float).Quo(new(big.Float).SetInt64(validator.Escrow), totalEscrowFloat).Float64()
			escrowPercent, err := strconv.ParseFloat(fmt.Sprintf("%.4f", p), 64)
			if err != nil {
				logc.Errorf(l.ctx, "percent compute error, %v", err)
				return nil, errort.NewDefaultError()
			}
			r.EscrowPercent = escrowPercent

			validatorList = append(validatorList, *r)

			if r.Active {
				active++
			} else {
				inactive++
			}
		}

		if req.OrderBy == "delegators" {
			if sortType == "asc" {
				sort.SliceStable(validatorList, func(i, j int) bool {
					return validatorList[i].Delegators < validatorList[j].Delegators
				})
			} else {
				sort.SliceStable(validatorList, func(i, j int) bool {
					return validatorList[i].Delegators > validatorList[j].Delegators
				})
			}
		}

		delegatorCount, err := l.svcCtx.DelegatorModel.CountDistinctDelegator(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "delegator CountDistinctDelegator error, %v", err)
			return nil, errort.NewDefaultError()
		}

		resp = &types.ValidatorListResponse{
			List:       validatorList,
			Active:     active,
			Inactive:   inactive,
			Delegators: delegatorCount,
		}
		return resp, nil
	})
	resp = v.(*types.ValidatorListResponse)
	return
}
