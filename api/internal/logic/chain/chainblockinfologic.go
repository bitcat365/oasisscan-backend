package chain

import (
	"context"
	"errors"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/api/internal/errort"
	"time"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ChainBlockInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewChainBlockInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ChainBlockInfoLogic {
	return &ChainBlockInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ChainBlockInfoLogic) ChainBlockInfo(req *types.ChainBlockInfoRequest) (resp *types.ChainBlockInfoResponse, err error) {
	block, err := l.svcCtx.BlockModel.FindBlockProposer(l.ctx, req.Height)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find block error, %v", err)
		return nil, errort.NewDefaultError()
	}
	if block == nil {
		return nil, nil
	}
	info := types.ChainBlockInfo{
		Height:        block.Height,
		Epoch:         block.Epoch,
		Timestamp:     uint64(block.Timestamp.Unix()),
		Time:          uint64(time.Now().Unix() - block.Timestamp.Unix()),
		Hash:          block.Hash,
		Txs:           block.Txs,
		EntityAddress: block.EntityAddress,
		Name:          block.Name,
	}
	resp = &types.ChainBlockInfoResponse{
		ChainBlockInfo: info,
	}
	return
}
