package chain

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/common"
	"time"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ChainBlocksLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewChainBlocksLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ChainBlocksLogic {
	return &ChainBlocksLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ChainBlocksLogic) ChainBlocks(req *types.ChainBlocksRequest) (resp *types.ChainBlocksResponse, err error) {
	totalSize, err := l.svcCtx.BlockModel.CountBlocks(l.ctx)
	if err != nil {
		logc.Errorf(l.ctx, "runtime transaction CountAll error: %v", err)
		return nil, errort.NewDefaultError()
	}
	list := make([]*types.ChainBlockInfo, 0)
	if totalSize != 0 {
		pageable := common.Pageable{
			Limit:  req.Size,
			Offset: (req.Page - 1) * req.Size,
		}
		blocks, err := l.svcCtx.BlockModel.FindBlocks(l.ctx, pageable)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "find blocks error, %v", err)
			return nil, errort.NewDefaultError()
		}
		for _, block := range blocks {
			list = append(list, &types.ChainBlockInfo{
				Height:        block.Height,
				Epoch:         block.Epoch,
				Timestamp:     uint64(block.Timestamp.Unix()),
				Time:          uint64(time.Now().Unix() - block.Timestamp.Unix()),
				Hash:          block.Hash,
				Txs:           block.Txs,
				EntityAddress: block.EntityAddress,
				Name:          block.Name,
			})
		}
	}
	page := types.Page{
		Page:      req.Page,
		Size:      req.Size,
		MaxPage:   common.MaxPage(req.Size, totalSize),
		TotalSize: totalSize,
	}
	resp = &types.ChainBlocksResponse{
		List: list,
		Page: page,
	}
	return
}
