package validator

import (
	"context"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ValidatorInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewValidatorInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ValidatorInfoLogic {
	return &ValidatorInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ValidatorInfoLogic) ValidatorInfo(req *types.ValidatorInfoRequest) (resp *types.ValidatorInfoResponse, err error) {
	m, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, req.Address)
	if err != nil {
		logc.Errorf(l.ctx, "validator FindOneByEntityAddress error, %v", err)
		return nil, errort.NewDefaultError()
	}
	validatorInfo := response.ValidatorResponseFormat(m)

	chainStatus, err := l.svcCtx.Consensus.GetStatus(l.ctx)
	if err != nil {
		logc.Errorf(l.ctx, "GetStatus error, %v", err)
		return nil, errort.NewDefaultError()
	}
	currentHeight := chainStatus.LatestHeight
	currentEpoch := chainStatus.LatestEpoch

	var validatorAddress staking.Address
	err = validatorAddress.UnmarshalText([]byte(m.EntityAddress))
	if err != nil {
		logc.Errorf(l.ctx, "validator entityAddress error, %v", err)
		return nil, errort.NewDefaultError()
	}
	accountQuery := staking.OwnerQuery{Height: currentHeight, Owner: validatorAddress}
	account, err := l.svcCtx.Staking.Account(l.ctx, &accountQuery)
	if err != nil {
		logc.Errorf(l.ctx, "account api error, %v", err)
		return nil, errort.NewDefaultError()
	}
	validatorInfo.Nonce = int64(account.General.Nonce)
	escrowFloat := common.ValueToFloatByDecimals(account.Escrow.Active.Balance.ToBigInt(), common.Decimals)
	totalSharesFloat := common.ValueToFloatByDecimals(account.Escrow.Active.TotalShares.ToBigInt(), common.Decimals)
	validatorInfo.Escrow = escrowFloat.String()
	validatorInfo.TotalShares = totalSharesFloat.String()

	//bounds
	bounds := account.Escrow.CommissionSchedule.Bounds
	boundsResponse := make([]types.Bound, 0)
	if len(bounds) > 0 {
		for _, b := range bounds {
			rateMin, _ := new(big.Float).Quo(new(big.Float).SetInt(b.RateMin.ToBigInt()), big.NewFloat(common.RateDecimals)).Float64()
			rateMax, _ := new(big.Float).Quo(new(big.Float).SetInt(b.RateMax.ToBigInt()), big.NewFloat(common.RateDecimals)).Float64()
			boundResponse := types.Bound{
				Start: int64(b.Start),
				Min:   rateMin,
				Max:   rateMax,
			}
			if b.Start <= currentEpoch {
				if len(boundsResponse) == 0 {
					boundsResponse = append(boundsResponse, boundResponse)
				} else {
					boundsResponse[0] = boundResponse
				}
			} else if len(boundsResponse) < 5 {
				boundsResponse = append(boundsResponse, boundResponse)
			} else {
				break
			}
		}
	}
	validatorInfo.Bounds = boundsResponse

	//rate
	rates := account.Escrow.CommissionSchedule.Rates
	ratesResponse := make([]types.Rate, 0)
	if len(rates) > 0 {
		for _, r := range rates {
			rate, _ := new(big.Float).Quo(new(big.Float).SetInt(r.Rate.ToBigInt()), big.NewFloat(common.RateDecimals)).Float64()
			rateResponse := types.Rate{
				Start: int64(r.Start),
				Rate:  rate,
			}
			if r.Start <= currentEpoch {
				if len(ratesResponse) == 0 {
					ratesResponse = append(ratesResponse, rateResponse)
				} else {
					ratesResponse[0] = rateResponse
				}
			} else if len(ratesResponse) < 5 {
				ratesResponse = append(ratesResponse, rateResponse)
			} else {
				break
			}
		}
		validatorInfo.Commission = ratesResponse[0].Rate
	}
	validatorInfo.Rates = ratesResponse

	//delegations
	selfShares := quantity.NewQuantity()
	selfAmount := quantity.NewQuantity()
	otherAmount := quantity.NewQuantity()
	delegationQuery := staking.OwnerQuery{Height: currentHeight, Owner: validatorAddress}
	delegationsMap, err := l.svcCtx.Staking.DelegationsFor(l.ctx, &delegationQuery)
	if err != nil {
		logc.Errorf(l.ctx, "delegationsFor api error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for entityAddress, delegations := range delegationsMap {
		amount, err := account.Escrow.Active.StakeForShares(&delegations.Shares)
		if err != nil {
			logc.Errorf(l.ctx, "computes the self amount error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if err = selfAmount.Add(amount); err != nil {
			logc.Errorf(l.ctx, "amount add error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if entityAddress.String() == req.Address {
			if err = selfShares.Add(&delegations.Shares); err != nil {
				logc.Errorf(l.ctx, "shares add error, %v", err)
				return nil, errort.NewDefaultError()
			}
		}
	}
	otherShares := account.Escrow.Active.TotalShares.Clone()
	if err = otherShares.Sub(selfShares); err != nil {
		logc.Errorf(l.ctx, "otherShares sub error, %v", err)
		return nil, errort.NewDefaultError()
	}
	escrowSharesStatus := &types.EscrowStatus{
		Self:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(selfShares.ToBigInt(), common.Decimals)),
		Other: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(otherShares.ToBigInt(), common.Decimals)),
		Total: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(account.Escrow.Active.TotalShares.ToBigInt(), common.Decimals)),
	}
	otherAmount, err = account.Escrow.Active.StakeForShares(otherShares)
	if err != nil {
		logc.Errorf(l.ctx, "computes the other amount error, %v", err)
		return nil, errort.NewDefaultError()
	}
	escrowAmountStatus := &types.EscrowStatus{
		Self:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(selfAmount.ToBigInt(), common.Decimals)),
		Other: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(otherAmount.ToBigInt(), common.Decimals)),
		Total: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(new(big.Int).Add(selfAmount.ToBigInt(), otherAmount.ToBigInt()), common.Decimals)),
	}
	validatorInfo.EscrowSharesStatus = escrowSharesStatus
	validatorInfo.EscrowAmountStatus = escrowAmountStatus

	resp = &types.ValidatorInfoResponse{
		ValidatorInfo: *validatorInfo,
	}
	return resp, nil
}
