package validator

import (
	"context"
	"github.com/zeromicro/go-zero/core/logc"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/response"
	"sort"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

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
	var orderBy, sortType string
	switch req.OrderBy {
	case "escrowChange24":
		orderBy = "escrow_24h"
	case "commission":
		orderBy = "commission"
	default:
		orderBy = "escrow"
		sortType = "desc"
	}
	if sortType == "asc" {
		sortType = "asc"
	}

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
}
