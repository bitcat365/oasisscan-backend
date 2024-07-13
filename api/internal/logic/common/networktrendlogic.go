package common

import (
	"context"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"sort"
	"strconv"
	"time"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type NetworkTrendLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewNetworkTrendLogic(ctx context.Context, svcCtx *svc.ServiceContext) *NetworkTrendLogic {
	return &NetworkTrendLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *NetworkTrendLogic) NetworkTrend(req *types.NetworkTrendRequest) (resp *types.NetworkTrendResponse, err error) {
	now := time.Now().UTC()
	today := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, time.UTC)

	//transaction
	txList := make([]*types.Chart, 0)
	txStats, err := l.svcCtx.TransactionModel.TransactionCountStats(l.ctx, today, 30)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "TransactionModel FindTransactionCountStats error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for _, stat := range txStats {
		txList = append(txList, &types.Chart{
			Key:   strconv.FormatInt(stat.Day.Unix(), 10),
			Value: strconv.FormatInt(stat.Count, 10),
		})
	}
	//sort by day asc
	sort.SliceStable(txList, func(i, j int) bool {
		return txList[i].Key < txList[j].Key
	})

	//escrow
	escrowList := make([]*types.Chart, 0)
	escrowStats, err := l.svcCtx.EscrowStatsModel.TotalStats(l.ctx, today, 30)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "EscrowStatsModel TotalStats error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for _, stat := range escrowStats {
		escrowList = append(escrowList, &types.Chart{
			Key:   strconv.FormatInt(stat.Day.Unix(), 10),
			Value: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(stat.Value), common.Decimals)),
		})
	}
	//sort by day asc
	sort.SliceStable(escrowList, func(i, j int) bool {
		return escrowList[i].Key < escrowList[j].Key
	})

	resp = &types.NetworkTrendResponse{
		Tx:     txList,
		Escrow: escrowList,
	}
	return
}
