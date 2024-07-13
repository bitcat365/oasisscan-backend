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

type RuntimeListLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewRuntimeListLogic(ctx context.Context, svcCtx *svc.ServiceContext) *RuntimeListLogic {
	return &RuntimeListLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *RuntimeListLogic) RuntimeList(req *types.RuntimeListRequest) (resp *types.RuntimeListResponse, err error) {
	runtimes, err := l.svcCtx.RuntimeModel.FindAll(l.ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "runtime FindAll error, %v", err)
		return nil, errort.NewDefaultError()
	}
	runtimeList := make([]*types.RuntimeListInfo, 0)
	for _, runtime := range runtimes {
		runtimeList = append(runtimeList, &types.RuntimeListInfo{
			RuntimeId: runtime.RuntimeId,
			Name:      runtime.Name,
		})
	}
	resp = &types.RuntimeListResponse{
		List: runtimeList,
	}
	return
}
