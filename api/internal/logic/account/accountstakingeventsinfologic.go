package account

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"strconv"
	"strings"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logx"
)

type AccountStakingEventsInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewAccountStakingEventsInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *AccountStakingEventsInfoLogic {
	return &AccountStakingEventsInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *AccountStakingEventsInfoLogic) AccountStakingEventsInfo(req *types.AccountStakingEventsInfoRequest) (resp *types.AccountStakingEventsInfoResponse, err error) {
	id := strings.Split(req.Id, "_")
	height, err := strconv.ParseInt(id[0], 10, 64)
	if err != nil {
		logc.Errorf(l.ctx, "ParseInt error, %v", err)
		return nil, errort.NewCodeError(errort.RequestParameterErrCode, errort.RequestParameterErrMsg)
	}
	position, err := strconv.ParseInt(id[1], 10, 64)
	if err != nil {
		logc.Errorf(l.ctx, "ParseInt error, %v", err)
		return nil, errort.NewCodeError(errort.RequestParameterErrCode, errort.RequestParameterErrMsg)
	}
	event, err := l.svcCtx.StakingEventModel.FindOneByHeightPosition(l.ctx, height, position)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "staking event FindOneByHeightPosition error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if event == nil {
		return nil, errort.NewCodeError(errort.NotFoundErrCode, errort.NotFoundErrMsg)
	}
	block, err := l.svcCtx.BlockModel.FindOneByHeight(l.ctx, event.Height)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "block FindOneByHeight error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if block == nil {
		logc.Errorf(l.ctx, "block not found error")
		return nil, errort.NewCodeError(errort.NotFoundErrCode, errort.NotFoundErrMsg)
	}
	resp = &types.AccountStakingEventsInfoResponse{
		Height:    event.Height,
		TxHash:    event.TxHash,
		Kind:      event.Kind,
		Timestamp: block.Timestamp.Unix(),
	}
	var rawEvent staking.Event
	err = json.Unmarshal([]byte(event.Raw), &rawEvent)
	if err != nil {
		logc.Errorf(l.ctx, "event json Unmarshal error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if rawEvent.Transfer != nil {
		resp.Transafer = rawEvent.Transfer
	}
	if rawEvent.Burn != nil {
		resp.Burn = rawEvent.Burn
	}
	if rawEvent.Escrow != nil {
		resp.Escrow = rawEvent.Escrow
	}
	if rawEvent.AllowanceChange != nil {
		resp.AllowanceChange = rawEvent.AllowanceChange
	}
	return resp, nil
}
