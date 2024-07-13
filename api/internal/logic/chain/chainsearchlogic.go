package chain

import (
	"context"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"oasisscan-backend/common"
	"strconv"
	"strings"

	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"

	"github.com/zeromicro/go-zero/core/logx"
)

type ChainSearchLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewChainSearchLogic(ctx context.Context, svcCtx *svc.ServiceContext) *ChainSearchLogic {
	return &ChainSearchLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *ChainSearchLogic) ChainSearch(req *types.ChainSearchRequest) (resp *types.ChainSearchResponse, err error) {
	result := ""
	searchType := common.SearchNone
	resp = &types.ChainSearchResponse{
		Key:    req.Key,
		Result: result,
		Type:   searchType,
	}

	if strings.HasPrefix(req.Key, "oasis") {
		chainStatus, err := l.svcCtx.Consensus.GetStatus(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "GetStatus error, %v", err)
			return resp, nil
		}
		currentHeight := chainStatus.LatestHeight

		var accountAddress staking.Address
		err = accountAddress.UnmarshalText([]byte(req.Key))
		if err != nil {
			logc.Errorf(l.ctx, "account address unmarshal error, key:%s, %v", req.Key, err)
			return resp, nil
		}

		accountQuery := staking.OwnerQuery{Height: currentHeight, Owner: accountAddress}
		account, err := l.svcCtx.Staking.Account(l.ctx, &accountQuery)
		if err != nil {
			logc.Errorf(l.ctx, "staking account error, key:%s, %v", req.Key, err)
			return resp, nil
		}
		if account != nil {
			result = req.Key
			searchType = common.SearchAccount

			validatorInfo, _ := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, req.Key)
			if validatorInfo != nil {
				searchType = common.SearchValidator
			} else {
				validatorInfo, _ = l.svcCtx.ValidatorModel.FindOneByNodeAddress(l.ctx, req.Key)
				if validatorInfo != nil {
					result = validatorInfo.EntityAddress
					searchType = common.SearchValidator
				}
			}
		}
	} else if common.IsInteger(req.Key) {
		height, _ := strconv.Atoi(req.Key)
		block, _ := l.svcCtx.BlockModel.FindOneByHeight(l.ctx, int64(height))
		if block != nil {
			result = req.Key
			searchType = common.SearchBlock
		}
	} else {
		if common.IsHex(req.Key) {
			//transaction
			tx, _ := l.svcCtx.TransactionModel.FindOneByTxHash(l.ctx, req.Key)
			if tx != nil {
				result = req.Key
				searchType = common.SearchTransaction
			} else {
				//runtime transaction
				runtimeTx, _ := l.svcCtx.RuntimeTransactionModel.FindOneByTxHash(l.ctx, req.Key)
				if runtimeTx != nil {
					result = req.Key
					searchType = common.SearchRuntimeTransaction
				} else {
					//block
					block, _ := l.svcCtx.BlockModel.FindOneByHash(l.ctx, req.Key)
					if block != nil {
						result = strconv.FormatInt(block.Height, 10)
						searchType = common.SearchBlock
					}
				}
			}
		}
	}

	resp = &types.ChainSearchResponse{
		Key:    req.Key,
		Result: result,
		Type:   searchType,
	}
	return resp, nil
}
