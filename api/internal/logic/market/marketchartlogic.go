package market

import (
	"context"
	"github.com/zeromicro/go-zero/core/logc"
	"oasisscan-backend/api/internal/errort"
	"strconv"
	"time"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type MarketChartLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewMarketChartLogic(ctx context.Context, svcCtx *svc.ServiceContext) *MarketChartLogic {
	return &MarketChartLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *MarketChartLogic) MarketChart(req *types.MarketChartRequest) (resp *types.MarketChartResponse, err error) {
	v, err := l.svcCtx.LocalCache.MarketCache.Take("chart", func() (interface{}, error) {
		marketChart, err := l.svcCtx.CoingeckoClient.MarketChart(l.ctx, 10)
		if err != nil {
			logc.Errorf(l.ctx, "market chart request error, %v", err)
			return nil, errort.NewDefaultError()
		}
		prices := make([]*types.Chart, 0)
		for _, price := range marketChart.Prices {
			prices = append(prices, &types.Chart{
				Key:   time.UnixMilli(int64(price[0])).Format("2006-01-02 15:04:05"),
				Value: strconv.FormatFloat(price[1], 'f', 8, 64),
			})
		}
		marketCaps := make([]*types.Chart, 0)
		for _, marketCap := range marketChart.MarketCaps {
			marketCaps = append(marketCaps, &types.Chart{
				Key:   time.UnixMilli(int64(marketCap[0])).Format("2006-01-02 15:04:05"),
				Value: strconv.FormatFloat(marketCap[1], 'f', 8, 64),
			})
		}
		volumes := make([]*types.Chart, 0)
		for _, volume := range marketChart.TotalVolumes {
			volumes = append(volumes, &types.Chart{
				Key:   time.UnixMilli(int64(volume[0])).Format("2006-01-02 15:04:05"),
				Value: strconv.FormatFloat(volume[1], 'f', 8, 64),
			})
		}
		resp = &types.MarketChartResponse{
			Price:     prices,
			MarketCap: marketCaps,
			Volume:    volumes,
		}
		return resp, nil
	})
	if err != nil {
		logc.Errorf(l.ctx, "cache error, %v", err)
		return nil, errort.NewDefaultError()
	}
	resp = v.(*types.MarketChartResponse)
	return
}
