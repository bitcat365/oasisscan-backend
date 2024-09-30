package common

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type NetworkStatusLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewNetworkStatusLogic(ctx context.Context, svcCtx *svc.ServiceContext) *NetworkStatusLogic {
	return &NetworkStatusLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *NetworkStatusLogic) NetworkStatus(req *types.NetworkStatusRequest) (resp *types.NetworkStatusResponse, err error) {
	v, err := l.svcCtx.LocalCache.NetworkStatusCache.Take("status", func() (interface{}, error) {
		chainStatus, err := l.svcCtx.Consensus.GetStatus(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "GetStatus error, %v", err)
			return nil, errort.NewDefaultError()
		}
		latestTx, err := l.svcCtx.TransactionModel.LatestTx(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "LatestTx error, %v", err)
			return nil, errort.NewDefaultError()
		}

		totalEscrow, err := l.svcCtx.ValidatorModel.SumEscrow(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "validator SumEscrow error, %v", err)
			return nil, errort.NewDefaultError()
		}

		activeValidator, err := l.svcCtx.ValidatorModel.CountActive(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "validator CountActive error, %v", err)
			return nil, errort.NewDefaultError()
		}

		delegatorCount, err := l.svcCtx.DelegatorModel.CountDistinctDelegator(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "delegator CountDistinctDelegator error, %v", err)
			return nil, errort.NewDefaultError()
		}

		resp = &types.NetworkStatusResponse{
			CurHeight:       chainStatus.LatestHeight,
			CurEpoch:        int64(chainStatus.LatestEpoch),
			TotalTxs:        latestTx.Id,
			TotalEscrow:     fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(totalEscrow), common.Decimals)),
			ActiveValidator: activeValidator,
			TotalDelegator:  delegatorCount,
		}
		return resp, nil
	})
	if err != nil {
		logc.Errorf(l.ctx, "cache error, %v", err)
		return nil, errort.NewDefaultError()
	}
	resp = v.(*types.NetworkStatusResponse)
	return
}
