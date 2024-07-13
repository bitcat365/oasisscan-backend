package market

import (
	"context"
	"github.com/zeromicro/go-zero/core/logc"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type MarketInfoLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewMarketInfoLogic(ctx context.Context, svcCtx *svc.ServiceContext) *MarketInfoLogic {
	return &MarketInfoLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *MarketInfoLogic) MarketInfo(req *types.MarketInfoRequest) (resp *types.MarketInfoResponse, err error) {
	v, err := l.svcCtx.LocalCache.MarketCache.Take("info", func() (interface{}, error) {
		coinInfo, err := l.svcCtx.CoingeckoClient.CoinInfo(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "coin info request error, %v", err)
			return nil, errort.NewDefaultError()
		}
		//volume change
		marketChartLogic := NewMarketChartLogic(l.ctx, l.svcCtx)
		var req types.MarketChartRequest
		chart, err := marketChartLogic.MarketChart(&req)
		if err != nil {
			logc.Errorf(l.ctx, "chart logic error, %v", err)
			return nil, errort.NewDefaultError()
		}
		volumes := chart.Volume
		var volumePct float64 = 0
		if len(volumes) > 0 {
			todayVolume := volumes[len(volumes)-1].Value
			tv := new(big.Float)
			_, _, err = tv.Parse(todayVolume, 10)
			if err != nil {
				logc.Errorf(l.ctx, "Error parsing float, %v", err)
				return nil, errort.NewDefaultError()
			}
			yesterdayVolume := volumes[len(volumes)-2].Value
			yv := new(big.Float)
			_, _, err = yv.Parse(yesterdayVolume, 10)
			if err != nil {
				logc.Errorf(l.ctx, "Error parsing float, %v", err)
				return nil, errort.NewDefaultError()
			}
			result := new(big.Float).Mul(new(big.Float).Quo(new(big.Float).Sub(tv, yv), yv), big.NewFloat(100))
			volumePct, _ = result.Float64()
		}

		resp = &types.MarketInfoResponse{
			Price:                 coinInfo.MarketData.CurrentPrice["usd"],
			PriceChangePct24h:     coinInfo.MarketData.PriceChangePercentage24h,
			MarketCap:             int64(coinInfo.MarketData.MarketCap["usd"]),
			MarketCapChangePct24h: coinInfo.MarketData.MarketCapChangePercentage24h,
			Rank:                  coinInfo.MarketCapRank,
			Volume:                int64(coinInfo.MarketData.TotalVolume["usd"]),
			VolumeChangePct24h:    volumePct,
		}
		return resp, nil
	})
	resp = v.(*types.MarketInfoResponse)
	return
}
