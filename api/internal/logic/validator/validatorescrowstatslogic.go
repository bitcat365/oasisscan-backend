package validator

import (
	"context"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ValidatorEscrowStatsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewValidatorEscrowStatsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ValidatorEscrowStatsLogic {
	return &ValidatorEscrowStatsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ValidatorEscrowStatsLogic) ValidatorEscrowStats(req *types.ValidatorEscrowStatsRequest) (resp *types.ValidatorEscrowStatsResponse, err error) {
	list := make([]*types.ValidatorEscrowStatsInfo, 0)
	rows, err := l.svcCtx.EscrowStatsModel.FindLatestRows(l.ctx, req.Address, 30)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "EscrowStatsModel FindLatestRows error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for _, row := range rows {
		list = append(list, &types.ValidatorEscrowStatsInfo{
			Timestamp: row.Date.Unix(),
			Escrow:    fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(row.Escrow), common.Decimals)),
		})
	}

	resp = &types.ValidatorEscrowStatsResponse{
		List: list,
	}

	return
}
