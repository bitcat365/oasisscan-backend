package governance

import (
	"context"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type GovernanceVotesLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewGovernanceVotesLogic(ctx context.Context, svcCtx *svc.ServiceContext) *GovernanceVotesLogic {
	return &GovernanceVotesLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *GovernanceVotesLogic) GovernanceVotes(req *types.GovernanceVotesRequest) (resp *types.GovernanceVotesResponse, err error) {
	// todo: add your logic here and delete this line
	return
}
