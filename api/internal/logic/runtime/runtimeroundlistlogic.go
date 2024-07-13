package runtime

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type RuntimeRoundListLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewRuntimeRoundListLogic(ctx context.Context, svcCtx *svc.ServiceContext) *RuntimeRoundListLogic {
	return &RuntimeRoundListLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *RuntimeRoundListLogic) RuntimeRoundList(req *types.RuntimeRoundListRequest) (resp *types.RuntimeRoundListResponse, err error) {
	runtimeId := req.Id
	list := make([]*types.RuntimeRoundInfo, 0)

	runtime, err := l.svcCtx.RuntimeModel.FindOneByRuntimeId(l.ctx, runtimeId)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if runtime == nil {
		return nil, errort.NewCodeError(errort.RuntimeNotFoundCode, errort.RuntimeNotFoundMsg)
	}

	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	rounds, err := l.svcCtx.RuntimeRoundModel.FindByRuntimeId(l.ctx, runtimeId, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find runtime rounds error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for _, round := range rounds {
		list = append(list, &types.RuntimeRoundInfo{
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
		})
	}

	totalSize, err := l.svcCtx.RuntimeRoundModel.CountByRuntimeId(l.ctx, runtimeId)
	if err != nil {
		logc.Errorf(l.ctx, "runtime round CountByRuntimeId error: %v", err)
		return nil, errort.NewDefaultError()
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}

	resp = &types.RuntimeRoundListResponse{
		List: list,
		Page: page,
	}
	return
}
