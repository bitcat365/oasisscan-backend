package governance

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
	pageable := common.Pageable{
		Limit:  req.Size,
		Offset: (req.Page - 1) * req.Size,
	}
	votes, err := l.svcCtx.VoteModel.FindByValidator(l.ctx, req.ValidatorAddress, req.ProposalId, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "FindAllByValidator error, %v", err)
		return nil, errort.NewDefaultError()
	}
	list := make([]*types.ProposalVote, 0)
	totalSize, err := l.svcCtx.VoteModel.CountByValidator(l.ctx, req.ValidatorAddress, req.ProposalId)
	if err != nil {
		logc.Errorf(l.ctx, "CountByValidator error, %v", err)
		return nil, errort.NewDefaultError()
	}
	validatorName, validatorIcon := "", ""
	if req.ValidatorAddress != "" {
		validatorInfo, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, req.ValidatorAddress)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "validator FindOneByEntityAddress error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if validatorInfo != nil {
			validatorName = validatorInfo.Name
			validatorIcon = validatorInfo.Icon
		}
	}

	for _, vote := range votes {
		title := ""
		proposal, err := l.svcCtx.ProposalModel.FindOneByProposalId(l.ctx, vote.ProposalId)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "proposal FindOneByProposalId error, %v", err)
			return nil, errort.NewDefaultError()
		}
		if proposal != nil {
			title = proposal.Title
		}
		if req.ValidatorAddress == "" {
			validatorInfo, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, vote.VoteAddress)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(l.ctx, "validator FindOneByEntityAddress error, %v", err)
				return nil, errort.NewDefaultError()
			}
			if validatorInfo != nil {
				validatorName = validatorInfo.Name
				validatorIcon = validatorInfo.Icon
			}
		}
		list = append(list, &types.ProposalVote{
			ProposalId: vote.ProposalId,
			Title:      title,
			Name:       validatorName,
			Icon:       validatorIcon,
			Address:    vote.VoteAddress,
			Vote:       vote.Option,
			Amount:     fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(vote.Amount), common.Decimals)),
			Percent:    vote.Percent,
		})
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}
	resp = &types.GovernanceVotesResponse{
		List: list,
		Page: page,
	}
	return
}
