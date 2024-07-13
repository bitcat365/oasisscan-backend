package account

import (
	"context"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type AccountInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountInfoLogic {
	return &AccountInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountInfoLogic) AccountInfo(req *types.AccountInfoRequest) (resp *types.AccountInfoResponse, err error) {
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
	account, err := l.svcCtx.Staking.Account(l.ctx, &accountQuery)
	if err != nil {
		logc.Errorf(l.ctx, "staking Account error, %v", err)
		return nil, errort.NewDefaultError()
	}

	delegations, err := l.svcCtx.Staking.DelegationsFor(l.ctx, &accountQuery)
	if err != nil {
		logc.Errorf(l.ctx, "staking DelegationsFor error, %v", err)
		return nil, errort.NewDefaultError()
	}

	totalEscrow := quantity.NewQuantity()
	for validator, delegation := range delegations {
		validatorAccountQuery := staking.OwnerQuery{Height: currentHeight, Owner: validator}
		validatorAccount, err := l.svcCtx.Staking.Account(l.ctx, &validatorAccountQuery)
		if err != nil {
			logc.Errorf(l.ctx, "staking Account error, %v", err)
			return nil, errort.NewDefaultError()
		}

		amount, err := validatorAccount.Escrow.Active.StakeForShares(&delegation.Shares)
		if err != nil {
			logc.Errorf(l.ctx, "computes the self amount error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if err = totalEscrow.Add(amount); err != nil {
			logc.Errorf(l.ctx, "totalEscrow add error, %v", err)
			return nil, errort.NewDefaultError()
		}
	}

	debondingMap, err := l.svcCtx.Staking.DebondingDelegationsFor(l.ctx, &accountQuery)
	if err != nil {
		logc.Errorf(l.ctx, "staking DebondingDelegationsFor error, %v", err)
		return nil, errort.NewDefaultError()
	}
	totalDebonding := quantity.NewQuantity()
	for _, debondings := range debondingMap {
		for _, debonding := range debondings {
			if err = totalDebonding.Add(&debonding.Shares); err != nil {
				logc.Errorf(l.ctx, "totalDebonding add error, %v", err)
				return nil, errort.NewDefaultError()
			}
		}
	}

	totalAmount := quantity.NewQuantity()
	if err = totalAmount.Add(&account.General.Balance); err != nil {
		logc.Errorf(l.ctx, "totalAmount add balance error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if err = totalAmount.Add(totalEscrow); err != nil {
		logc.Errorf(l.ctx, "totalAmount add totalEscrow error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if err = totalAmount.Add(totalDebonding); err != nil {
		logc.Errorf(l.ctx, "totalAmount add totalDebonding error, %v", err)
		return nil, errort.NewDefaultError()
	}

	allowances := make([]*types.AccountAllowance, 0)
	allowanceMap := account.General.Allowances
	if allowanceMap != nil {
		for address, q := range allowanceMap {
			allowances = append(allowances, &types.AccountAllowance{
				Address: address.String(),
				Amount:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(q.ToBigInt(), common.Decimals)),
			})
		}
	}

	resp = &types.AccountInfoResponse{
		Address:    req.Address,
		Available:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(account.General.Balance.ToBigInt(), common.Decimals)),
		Escrow:     fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(totalEscrow.ToBigInt(), common.Decimals)),
		Debonding:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(totalDebonding.ToBigInt(), common.Decimals)),
		Total:      fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(totalAmount.ToBigInt(), common.Decimals)),
		Nonce:      account.General.Nonce,
		Allowances: allowances,
	}
	return
}
