package main

import (
	"context"
	"flag"
	"fmt"
	"github.com/robfig/cron/v3"
	"github.com/zeromicro/go-zero/core/conf"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/logx"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"github.com/zeromicro/go-zero/rest"
	"oasisscan-backend/api/internal/config"
	"oasisscan-backend/api/internal/handler"
	"oasisscan-backend/api/internal/logic"
	"oasisscan-backend/api/internal/svc"
)

var configFile = flag.String("f", "etc/oasisscan-api.yaml", "the config file")

func main() {
	flag.Parse()

	var c config.Config
	conf.MustLoad(*configFile, &c)

	//close sql info log
	sqlx.DisableStmtLog()

	server := rest.MustNewServer(c.RestConf)
	defer server.Stop()

	svcCtx := svc.NewServiceContext(c)
	handler.RegisterHandlers(server, svcCtx)

	var err error
	//cache cron job
	cr := cron.New(cron.WithChain(cron.DelayIfStillRunning(cron.DefaultLogger), cron.Recover(cron.DefaultLogger)))

	//init
	logic.SignStatsCacheJob(context.Background(), svcCtx)

	/** validator sign stats **/
	_, err = cr.AddFunc("@every 10m", func() {
		ctx := context.Background()
		logc.Infof(ctx, "signStatsCacheJob start...")
		logic.SignStatsCacheJob(ctx, svcCtx)
	})
	if err != nil {
		logx.Errorf("cron job add func error, %v\n", err)
	}

	cr.Start()

	fmt.Printf("Starting server at %s:%d...\n", c.Host, c.Port)
	server.Start()
}
