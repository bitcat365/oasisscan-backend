package task

import (
	"context"
	"github.com/zeromicro/go-zero/core/logc"
	"oasisscan-backend/job/internal/svc"
)

func DailyJob(ctx context.Context, svcCtx *svc.ServiceContext) {
	err := svcCtx.TransactionModel.RefreshDailyCountsStatsView(ctx)
	if err != nil {
		logc.Errorf(ctx, "RefreshDailyCountsStatsView error, %v", err)
	}
	logc.Infof(ctx, "refresh DailyCountsStatsView done.")

	err = svcCtx.RewardModel.RefreshRewardDaysView(ctx)
	if err != nil {
		logc.Errorf(ctx, "RefreshRewardDaysView error, %v", err)
	}
	logc.Infof(ctx, "refresh RewardDaysView done.")

	err = svcCtx.BlockSignatureModel.RefreshBlockCountDaysView(ctx)
	if err != nil {
		logc.Errorf(ctx, "RefreshBlockCountDaysView error, %v", err)
	}
	logc.Infof(ctx, "refresh BlockCountDaysView done.")

	err = svcCtx.TransactionModel.RefreshTransactionMethodView(ctx)
	if err != nil {
		logc.Errorf(ctx, "RefreshTransactionMethodView error, %v", err)
	}
	logc.Infof(ctx, "refresh TransactionMethodView done.")
}
