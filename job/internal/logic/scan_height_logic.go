package logic

import (
	"context"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/proto"

	"github.com/zeromicro/go-zero/core/logx"
)

type ScanHeightLogic struct {
	ctx    context.Context
	svcCtx *svc.ServiceContext
	logx.Logger
}

func NewScanHeightLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ScanHeightLogic {
	return &ScanHeightLogic{
		ctx:    ctx,
		svcCtx: svcCtx,
		Logger: logx.WithContext(ctx),
	}
}

func (l *ScanHeightLogic) ScanHeight(in *proto.ScanHeightRequest) (*proto.ScanHeightResponse, error) {
	consensusApi := l.svcCtx.Consensus
	status, err := consensusApi.GetStatus(l.ctx)
	if err != nil {
		return nil, err
	}
	return &proto.ScanHeightResponse{
		LatestHeight: status.LatestHeight,
	}, nil
}
