// Code generated by goctl. DO NOT EDIT.
// Source: job.proto

package job

import (
	"context"

	"oasisscan-backend/job/proto"

	"github.com/zeromicro/go-zero/zrpc"
	"google.golang.org/grpc"
)

type (
	ScanHeightRequest  = proto.ScanHeightRequest
	ScanHeightResponse = proto.ScanHeightResponse

	Job interface {
		ScanHeight(ctx context.Context, in *ScanHeightRequest, opts ...grpc.CallOption) (*ScanHeightResponse, error)
	}

	defaultJob struct {
		cli zrpc.Client
	}
)

func NewJob(cli zrpc.Client) Job {
	return &defaultJob{
		cli: cli,
	}
}

func (m *defaultJob) ScanHeight(ctx context.Context, in *ScanHeightRequest, opts ...grpc.CallOption) (*ScanHeightResponse, error) {
	client := proto.NewJobClient(m.cli.Conn())
	return client.ScanHeight(ctx, in, opts...)
}
