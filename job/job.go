package main

import (
	"context"
	"flag"
	"github.com/robfig/cron/v3"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/logx"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/job/internal/config"
	"oasisscan-backend/job/internal/server"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/proto"
	"oasisscan-backend/job/task"

	"github.com/zeromicro/go-zero/core/conf"
	"github.com/zeromicro/go-zero/core/service"
	"github.com/zeromicro/go-zero/zrpc"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

var configFile = flag.String("f", "etc/job.yaml", "the config file")

func main() {
	flag.Parse()

	var c config.Config
	conf.MustLoad(*configFile, &c)
	svcCtx := svc.NewServiceContext(c)

	//close sql info log
	sqlx.DisableStmtLog()

	s := zrpc.MustNewServer(c.RpcServerConf, func(grpcServer *grpc.Server) {
		proto.RegisterJobServer(grpcServer, server.NewJobServer(svcCtx))

		if c.Mode == service.DevMode || c.Mode == service.TestMode {
			reflection.Register(grpcServer)
		}
	})
	defer s.Stop()

	var err error
	//cron job
	cr := cron.New(cron.WithChain(cron.DelayIfStillRunning(cron.DefaultLogger), cron.Recover(cron.DefaultLogger)))

	/** block **/
	_, err = cr.AddFunc("@every 5s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "BlockScanner start...")
		task.BlockScanner(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}
	_, err = cr.AddFunc("@every 5s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "StakingEventsScanner start...")
		task.StakingEventsScanner(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}
	_, err = cr.AddFunc("@daily", func() {
		ctx := context.Background()
		logc.Infof(ctx, "DailyJob start...")
		task.DailyJob(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	/** validator **/
	_, err = cr.AddFunc("@every 5s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "NodeScanner start...")
		task.NodeScanner(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 10m", func() {
		ctx := context.Background()
		logc.Infof(ctx, "ValidatorInfoSync start...")
		task.ValidatorInfoSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 60s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "ValidatorConsensusSync start...")
		task.ValidatorConsensusSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 60s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "DelegatorSync start...")
		task.DelegatorSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 1m", func() {
		ctx := context.Background()
		logc.Infof(ctx, "DelegatorRewardSync start...")
		task.DelegatorRewardSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 5m", func() {
		ctx := context.Background()
		logc.Infof(ctx, "EscrowStatsSync start...")
		task.EscrowStatsSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	/** runtime **/
	_, err = cr.AddFunc("@every 5s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "RuntimeRoundSync start...")
		task.RuntimeRoundSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 5s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "RuntimeTransactionSync start...")
		task.RuntimeTransactionSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	_, err = cr.AddFunc("@every 5s", func() {
		ctx := context.Background()
		logc.Infof(ctx, "RuntimeStats start...")
		task.RuntimeStats(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	/** proposal **/
	_, err = cr.AddFunc("@every 10m", func() {
		ctx := context.Background()
		logc.Infof(ctx, "ProposalSync start...")
		task.ProposalSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}
	_, err = cr.AddFunc("@every 10m", func() {
		ctx := context.Background()
		logc.Infof(ctx, "VoteSync start...")
		task.VoteSync(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	cr.Start()

	logx.Errorf("Starting rpc server at %s...\n", c.ListenOn)
	s.Start()
}
