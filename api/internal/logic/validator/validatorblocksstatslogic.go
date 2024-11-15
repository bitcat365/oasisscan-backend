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

	latestSignBlock, err := l.svcCtx.BlockSignatureModel.FindLatestOne(l.ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "BlockSignatureModel FindLatestOne error, %v", err)
		return nil, errort.NewDefaultError()
	}
	latestHeight := latestSignBlock.Height

	signsBlocks, err := l.svcCtx.BlockSignatureModel.FindBlocksByHeight(l.ctx, consensusAddress, latestHeight-100)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "BlockSignatureModel FindBlocksByHeight error, %v", err)
		return nil, errort.NewDefaultError()
	}

	signSet := make(map[int64]bool, 0)
	for _, block := range signsBlocks {
		signSet[block.Height] = true
	}

	for i := latestHeight; i > latestHeight-100; i-- {
		sign := false
		if signSet[i] {
			sign = true
		}
		signs = append(signs, &types.ValidatorBlocksStatsInfo{
			Height: i,
			Block:  sign,
		})
	}

	resp = &types.ValidatorBlocksStatsResponse{
		Proposals: proposals,
		Signs:     signs,
	}
	return
}
