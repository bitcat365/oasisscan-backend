package validator

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"

	"github.com/zeromicro/go-zero/core/logx"
)

type ValidatorBlocksStatsLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewValidatorBlocksStatsLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ValidatorBlocksStatsLogic {
	return &ValidatorBlocksStatsLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ValidatorBlocksStatsLogic) ValidatorBlocksStats(req *types.ValidatorBlocksStatsRequest) (resp *types.ValidatorBlocksStatsResponse, err error) {
	proposals := make([]*types.ValidatorBlocksStatsInfo, 0)
	signs := make([]*types.ValidatorBlocksStatsInfo, 0)

	var statLimit int64 = 100
	//proposed
	pageable := common.Pageable{
		Limit:  statLimit,
		Offset: 0,
	}
	blocks, err := l.svcCtx.BlockModel.FindBlocks(l.ctx, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "BlockModel FindBlocksByValidator error, %v", err)
		return nil, errort.NewDefaultError()
	}
	consensusAddress := ""
	for _, block := range blocks {
		consensusAddress = block.ProposerAddress
		b := false
		if block.EntityAddress == req.Address {
			b = true
		}
		proposals = append(proposals, &types.ValidatorBlocksStatsInfo{
			Height: block.Height,
			Block:  b,
		})
	}
	signsBlocks, err := l.svcCtx.BlockSignatureModel.FindBlocks(l.ctx, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "BlockSignatureModel FindBlocks error, %v", err)
		return nil, errort.NewDefaultError()
	}
	for _, singsBlock := range signsBlocks {
		b := false
		if singsBlock.ValidatorAddress == consensusAddress {
			b = true
		}
		signs = append(signs, &types.ValidatorBlocksStatsInfo{
			Height: singsBlock.Height,
			Block:  b,
		})
	}
	resp = &types.ValidatorBlocksStatsResponse{
		Proposals: proposals,
		Signs:     signs,
	}
	return
}
