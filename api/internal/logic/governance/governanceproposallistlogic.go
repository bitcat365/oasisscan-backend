package governance

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	governance "github.com/oasisprotocol/oasis-core/go/governance/api"

	"github.com/zeromicro/go-zero/core/logx"
)

type GovernanceProposalListLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewGovernanceProposalListLogic(ctx context.Context, svcCtx *svc.ServiceContext) *GovernanceProposalListLogic {
	return &GovernanceProposalListLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *GovernanceProposalListLogic) GovernanceProposalList(req *types.GovernanceProposalListRequest) (resp *types.GovernanceProposalListResponse, err error) {
	proposals, err := l.svcCtx.ProposalModel.FindAllProposals(l.ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "FindAllProposals error, %v", err)
		return nil, errort.NewDefaultError()
	}
	list := make([]*types.GovernanceProposalInfo, 0)
	for _, m := range proposals {
		var proposal governance.Proposal
		err = json.Unmarshal([]byte(m.Raw), &proposal)
		if err != nil {
			logc.Errorf(l.ctx, "json unmarshal error, %v", err)
			return nil, errort.NewDefaultError()
		}
		list = append(list, &types.GovernanceProposalInfo{
			Id:        m.ProposalId,
			Title:     m.Title,
			Type:      m.Type,
			Submitter: m.Submitter,
			State:     m.State,
			Deposit:   fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(proposal.Deposit.ToBigInt(), common.Decimals)),
			CreatedAt: m.CreatedEpoch,
			ClosedAt:  m.ClosedEpoch,
		})
	}
	resp = &types.GovernanceProposalListResponse{
		List: list,
	}
	return
}
