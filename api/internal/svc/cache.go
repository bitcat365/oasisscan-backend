package svc

import (
	"github.com/zeromicro/go-zero/core/collection"
	"github.com/zeromicro/go-zero/core/logx"
	"time"
)

type LocalCache struct {
	MarketCache                *collection.Cache
	RuntimeStatsCache          *collection.Cache
	ProposalWithVotesCache     *collection.Cache
	AccountRewardFindDaysCache *collection.Cache
	ValidatorListCache         *collection.Cache
	ValidatorSignsStatsCache   *collection.Cache
}

func NewLocalCache() *LocalCache {
	marketCache, err := collection.NewCache(time.Minute*5, collection.WithName("market"))
	if err != nil {
		logx.Errorf("localCache error: %v\n", err)
	}
	runtimeStatsCache, err := collection.NewCache(time.Second*30, collection.WithName("runtime-stats"))
	if err != nil {
		logx.Errorf("localCache error: %v\n", err)
	}
	proposalWithVotesCache, err := collection.NewCache(time.Minute*1, collection.WithName("proposal-votes"))
	if err != nil {
		logx.Errorf("localCache error: %v\n", err)
	}
	accountRewardFindDaysCache, err := collection.NewCache(time.Minute*1, collection.WithName("account-reward-days"))
	if err != nil {
		logx.Errorf("localCache error: %v\n", err)
	}
	validatorListCache, err := collection.NewCache(time.Minute*1, collection.WithName("validator-list"))
	if err != nil {
		logx.Errorf("localCache error: %v\n", err)
	}
	validatorSignsStatsCache, err := collection.NewCache(time.Minute*10, collection.WithName("validator-signs"))
	if err != nil {
		logx.Errorf("localCache error: %v\n", err)
	}
	return &LocalCache{
		MarketCache:                marketCache,
		RuntimeStatsCache:          runtimeStatsCache,
		ProposalWithVotesCache:     proposalWithVotesCache,
		AccountRewardFindDaysCache: accountRewardFindDaysCache,
		ValidatorListCache:         validatorListCache,
		ValidatorSignsStatsCache:   validatorSignsStatsCache,
	}
}
