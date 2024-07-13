package coingecko

import (
	"context"
	"encoding/json"
	"fmt"
	"oasisscan-backend/common"
	"strconv"
)

const (
	baseUrl     = "https://api.coingecko.com"
	coingeckoId = "oasis-network"
)

type (
	CoingeckoClient interface {
		CoinInfo(ctx context.Context) (*CoinInfo, error)
		MarketChart(ctx context.Context, days int) (*MarketChart, error)
	}

	defaultCoingeckoClient struct {
		baseUrl string
	}

	CoinInfo struct {
		MarketCapRank int64       `json:"market_cap_rank"`
		MarketData    *MarketData `json:"market_data"`
	}
	MarketData struct {
		CurrentPrice                 map[string]float64 `json:"current_price"`
		TotalVolume                  map[string]float64 `json:"total_volume"`
		MarketCap                    map[string]float64 `json:"market_cap"`
		PriceChangePercentage24h     float64            `json:"price_change_percentage_24h"`
		MarketCapChangePercentage24h float64            `json:"market_cap_change_percentage_24h"`
	}

	MarketChart struct {
		Prices       [][]float64 `json:"prices"`
		MarketCaps   [][]float64 `json:"market_caps"`
		TotalVolumes [][]float64 `json:"total_volumes"`
	}
)

func NewCoingeckoClient() CoingeckoClient {
	return &defaultCoingeckoClient{
		baseUrl: baseUrl,
	}
}

func (c *defaultCoingeckoClient) CoinInfo(ctx context.Context) (*CoinInfo, error) {
	var info CoinInfo
	url := fmt.Sprintf("%s/api/v3/coins/%s", baseUrl, coingeckoId)
	param := make(map[string]interface{})
	param["localization"] = "false"
	param["tickers"] = "false"
	param["market_data"] = "true"
	param["community_data"] = "false"
	param["developer_data"] = "false"
	param["sparkline"] = "false"
	responseContent, err := common.HttpRequest(url, "GET", param)
	if err != nil {
		return nil, fmt.Errorf("coingecko coin info api request error, %v", err)
	}
	if len(responseContent) > 0 {
		err = json.Unmarshal(responseContent, &info)
		if err != nil {
			return nil, fmt.Errorf("coingecko coin info json error: %s, %v", string(responseContent), err)
		}
	}
	return &info, nil
}

func (c *defaultCoingeckoClient) MarketChart(ctx context.Context, days int) (*MarketChart, error) {
	var info MarketChart
	url := fmt.Sprintf("%s/api/v3/coins/%s/market_chart", baseUrl, coingeckoId)
	param := make(map[string]interface{})
	param["vs_currency"] = "usd"
	param["days"] = strconv.Itoa(days)
	param["interval"] = "daily"
	param["precision"] = "8"
	responseContent, err := common.HttpRequest(url, "GET", param)
	if err != nil {
		return nil, fmt.Errorf("coingecko market chart api request error, %v", err)
	}
	if len(responseContent) > 0 {
		err = json.Unmarshal(responseContent, &info)
		if err != nil {
			return nil, fmt.Errorf("coingecko coin info json error: %s, %v", string(responseContent), err)
		}
	}
	return &info, nil
}
