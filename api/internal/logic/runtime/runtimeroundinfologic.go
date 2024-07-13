package runtime

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type RuntimeRoundInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewRuntimeRoundInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *RuntimeRoundInfoLogic {
	return &RuntimeRoundInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *RuntimeRoundInfoLogic) RuntimeRoundInfo(req *types.RuntimeRoundInfoRequest) (resp *types.RuntimeRoundInfoResponse, err error) {
	runtimeId := req.Id

	runtime, err := l.svcCtx.RuntimeModel.FindOneByRuntimeId(l.ctx, runtimeId)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if runtime == nil {
		return nil, errort.NewCodeError(errort.RuntimeNotFoundCode, errort.RuntimeNotFoundMsg)
	}

	round, err := l.svcCtx.RuntimeRoundModel.FindOneByRuntimeIdRound(l.ctx, runtimeId, req.Round)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime round error, %v", err)
		return nil, errort.NewDefaultError()
	}

	latestRound, _ := l.svcCtx.RuntimeRoundModel.FindLatestOne(l.ctx, runtimeId)
	next := false
	if latestRound != nil {
		next = latestRound.Round > round.Round
	}
	info := types.RuntimeRoundInfo{
		Version:      round.Version,
		RuntimeId:    round.RuntimeId,
		RuntimeName:  runtime.Name,
		Round:        round.Round,
		Timestamp:    round.Timestamp.Unix(),
		HeaderType:   round.HeaderType,
		PreviousHash: round.PreviousHash,
		IoRoot:       round.IoRoot,
		StateRoot:    round.StateRoot,
		MessagesHash: round.MessagesHash,
		Next:         next,
	}
	resp = &types.RuntimeRoundInfoResponse{
		RuntimeRoundInfo: info,
	}
	return
}
