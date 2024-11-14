package task

import (
	"context"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/cometbft/cometbft/crypto"
	beacon "github.com/oasisprotocol/oasis-core/go/beacon/api"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	"github.com/oasisprotocol/oasis-core/go/common/crypto/signature"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/common"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/model"
	"strconv"
	"strings"
	"time"
)

func NodeScanner(ctx context.Context, svcCtx *svc.ServiceContext) {
	consensusApi := svcCtx.Consensus
	chainStatus, err := consensusApi.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	nodeHeight := chainStatus.GenesisHeight
	nodeHeightProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, common.NodeHeightProperty)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "FindByProperty error, %v", err)
		return
	}
	if nodeHeightProperty != nil {
		nodeHeight, err = strconv.ParseInt(nodeHeightProperty.Value, 10, 64)
		if err != nil {
			logc.Errorf(ctx, "ParseInt error, %v", err)
			return
		}
		nodeHeight++
	} else {
		nodeHeightProperty = &model.SystemProperty{
			Property:  common.NodeHeightProperty,
			Value:     strconv.FormatInt(nodeHeight, 10),
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		}
		_, err := svcCtx.SystemPropertyModel.Insert(ctx, nodeHeightProperty)
		if err != nil {
			logc.Errorf(ctx, "SystemPropertyModel insert error, %v", err)
			return
		}
	}

	registryApi := svcCtx.Registry

	for nodeHeight <= currentHeight {
		nodes, err := registryApi.GetNodes(ctx, nodeHeight)
		if err != nil {
			logc.Errorf(ctx, "GetNodes error, %v", err)
			return
		}
		for _, node := range nodes {
			entityId := node.EntityID
			nodeId := node.ID
			consensusIDBytes, err := node.Consensus.ID.MarshalBinary()
			if err != nil {
				logc.Errorf(ctx, "Consensus ID error, %v", err)
				return
			}
			consensusAddress := crypto.AddressHash(consensusIDBytes)

			//save node
			mNode, err := svcCtx.NodeModel.FindOneByEntityIdNodeIdConsensusAddress(ctx, entityId.String(), nodeId.String(), consensusAddress.String())
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "Node FindOneByEntityIdNodeIdConsensusAddress error, %v", err)
				return
			}

			if mNode == nil {
				mNode = &model.Node{
					EntityId:         entityId.String(),
					NodeId:           nodeId.String(),
					ConsensusAddress: consensusAddress.String(),
					Height:           nodeHeight,
					CreatedAt:        time.Now(),
					UpdatedAt:        time.Now(),
				}
				_, err = svcCtx.NodeModel.Insert(ctx, mNode)
				if err != nil {
					logc.Errorf(ctx, "node insert error, %v", err)
					return
				}
			}

			//save validator
			entityAddress, err := common.PubKeyToBech32Address(ctx, entityId.String())
			if err != nil {
				logc.Errorf(ctx, "entityAddress error, %v", err)
				return
			}
			nodeAddress, err := common.PubKeyToBech32Address(ctx, nodeId.String())
			if err != nil {
				logc.Errorf(ctx, "nodeAddress error, %v", err)
				return
			}

			validator, err := svcCtx.ValidatorModel.FindOneByEntityId(ctx, entityId.String())
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "Validator FindOneByEntityId error, %v", err)
				return
			}
			if validator == nil {
				validator = &model.Validator{
					EntityId:         entityId.String(),
					EntityAddress:    entityAddress.String(),
					NodeId:           nodeId.String(),
					NodeAddress:      nodeAddress.String(),
					ConsensusAddress: consensusAddress.String(),
					CreatedAt:        time.Now(),
					UpdatedAt:        time.Now(),
				}
				_, err = svcCtx.ValidatorModel.Insert(ctx, validator)
				if err != nil {
					logc.Errorf(ctx, "validator insert error, %v", err)
					return
				}
			} else {
				if strings.Contains(node.Roles.String(), "validator") {
					validator.NodeId = nodeId.String()
				}
				validator.NodeAddress = nodeAddress.String()
				validator.ConsensusAddress = consensusAddress.String()
				validator.UpdatedAt = time.Now()
				err = svcCtx.ValidatorModel.Update(ctx, validator)
				if err != nil {
					logc.Errorf(ctx, "validator update error, %v", err)
					return
				}
			}
		}

		//update system property
		if nodeHeightProperty != nil {
			nodeHeightProperty.Value = strconv.FormatInt(nodeHeight, 10)
			nodeHeightProperty.UpdatedAt = time.Now()
			err = svcCtx.SystemPropertyModel.UpdateByProperty(ctx, nodeHeightProperty)
			if err != nil {
				logc.Errorf(ctx, "SystemPropertyModel update error, %v", err)
				return
			}
		}

		logc.Infof(ctx, "node scan height: %d, current height: %d", nodeHeight, currentHeight)
		nodeHeight++
	}
}

func ValidatorInfoSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	//metadata from github
	gitUrl := svcCtx.Config.Validator.GitInfo
	gitContent, err := common.HttpRequest(gitUrl, "GET", nil)
	if err != nil {
		logc.Errorf(ctx, "validator git info request error, %v", err)
		return
	}
	var infos []*common.ValidatorGitInfo
	err = json.Unmarshal(gitContent, &infos)
	if err != nil {
		logc.Errorf(ctx, "validator git info json error: %s, %v", string(gitContent), err)
		return
	}

	for _, info := range infos {
		if info.DownloadUrl == "" {
			continue
		}

		validatorHex := strings.ReplaceAll(info.Name, ".json", "")
		hexBytes, err := hex.DecodeString(validatorHex)
		if err != nil {
			continue
		}
		entityId := base64.StdEncoding.EncodeToString(hexBytes)

		validator, err := svcCtx.ValidatorModel.FindOneByEntityId(ctx, entityId)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "Validator FindOneByEntityId error, %v", err)
			return
		} else if errors.Is(err, sqlx.ErrNotFound) {
			continue
		}

		entityRawInfoBytes, err := common.HttpRequest(info.DownloadUrl, "GET", nil)
		if err != nil {
			logc.Errorf(ctx, "entity info download request error, %v", err)
			return
		}
		if len(entityRawInfoBytes) == 0 {
			continue
		}

		var entityRawInfo signature.Signed
		err = json.Unmarshal(entityRawInfoBytes, &entityRawInfo)
		if err != nil {
			logc.Errorf(ctx, "entity info json Unmarshal error: %s, %v", entityRawInfo, err)
			return
		}

		var entityInfo common.EntityInfo
		if err := cbor.Unmarshal(entityRawInfo.Blob, &entityInfo); err != nil {
			logc.Errorf(ctx, "entity info cbor Unmarshal error: %s, %v", entityRawInfo, err)
			return
		}

		//icon from keybase
		icon := ""
		if entityInfo.Keybase != "" {
			param := make(map[string]interface{})
			param["usernames"] = entityInfo.Keybase
			keybaseResultBytes, err := common.HttpRequest(svcCtx.Config.Validator.KeybaseJson, "GET", param)
			if err != nil {
				logc.Errorf(ctx, "keybase icon request error: %s,%s, %v", entityId, entityInfo.Keybase, err)
				return
			}
			if len(keybaseResultBytes) > 0 {
				var keybaseResultData interface{}
				if err := json.Unmarshal(keybaseResultBytes, &keybaseResultData); err != nil {
					logc.Errorf(ctx, "JSON unmarshal error: %s, %s, %s, %v", entityId, entityInfo.Keybase, err)
				} else {
					icon, err = common.GetNestedStringValue(keybaseResultData, "them", "0", "pictures", "primary", "url")
					if err != nil {
						logc.Infof(ctx, "icon json error: %s,%s,", entityId, entityInfo.Keybase, err)
					}
				}
			}
		}

		validator.Name = entityInfo.Name
		validator.Icon = icon
		validator.Website = entityInfo.Url
		validator.Twitter = entityInfo.Twitter
		validator.Keybase = entityInfo.Keybase
		validator.Email = entityInfo.Email

		err = svcCtx.ValidatorModel.Update(ctx, validator)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "Validator Update error, %v", err)
			return
		}
	}

	logc.Infof(ctx, "validator info sync done...")
}

func ValidatorConsensusSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	var nodeHeight int64 = 0
	nodeHeightProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, common.NodeHeightProperty)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "FindByProperty error, %v", err)
		return
	}
	if nodeHeightProperty != nil {
		nodeHeight, err = strconv.ParseInt(nodeHeightProperty.Value, 10, 64)
		if err != nil {
			logc.Errorf(ctx, "ParseInt error, %v", err)
			return
		}
		nodeHeight++
	}
	if nodeHeight == 0 {
		return
	}

	validators, err := svcCtx.ValidatorModel.FindAll(ctx, "escrow", "desc")
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "validator FindAll error, %v", err)
		return
	}

	stakingApi := svcCtx.Staking
	consensusApi := svcCtx.Consensus
	chainStatus, err := consensusApi.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight
	currentEpoch := chainStatus.LatestEpoch

	registryApi := svcCtx.Registry
	registryGenesis, err := registryApi.StateToGenesis(ctx, currentHeight)
	if err != nil {
		logc.Errorf(ctx, "StateToGenesis error, %v", err)
		return
	}

	for i, validator := range validators {
		entityId := validator.EntityId

		//init
		validator.Nodes = 0
		validator.Signs = 0
		validator.Proposals = 0
		validator.SignsUptime = 0
		validator.Escrow24H = 0

		//rank by escrow
		validator.Rank = int64(i + 1)

		//consensus info
		nodeStatusMap := registryGenesis.NodeStatuses
		var pubKey signature.PublicKey
		err = pubKey.UnmarshalText([]byte(validator.NodeId))
		if err != nil {
			logc.Errorf(ctx, "pubKey unmarshal error: %s", entityId, err)
			return
		}
		nodeStatus := nodeStatusMap[pubKey]
		if nodeStatus != nil {
			validator.Status = !nodeStatus.ExpirationProcessed
		}
		//escrow info
		entityAddress, err := common.PubKeyToBech32Address(ctx, entityId)
		if err != nil {
			logc.Errorf(ctx, "entityAddress error, %v", err)
			return
		}
		accountQuery := staking.OwnerQuery{Height: currentHeight, Owner: *entityAddress}
		account, err := stakingApi.Account(ctx, &accountQuery)
		if err != nil {
			logc.Errorf(ctx, "stakingApi account error: %s, %v", entityId, err)
			return
		}
		validator.Escrow = account.Escrow.Active.Balance.ToBigInt().Int64()
		validator.TotalShares = account.Escrow.Active.TotalShares.ToBigInt().Int64()
		rates := account.Escrow.CommissionSchedule.Rates
		for _, rate := range rates {
			if currentEpoch >= rate.Start {
				validator.Commission = rate.Rate.ToBigInt().Int64()
			}
		}
		validator.Balance = account.General.Balance.ToBigInt().Int64()

		consensusList, err := svcCtx.NodeModel.FindByEntityId(ctx, validator.EntityId)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "validator FindListByConsensusAddress error, %v", err)
			return
		}
		consAddresses := make([]string, 0)
		for _, cons := range consensusList {
			consAddresses = append(consAddresses, cons.ConsensusAddress)
		}
		//proposals
		if len(consAddresses) > 0 {
			proposalsCount, err := svcCtx.BlockModel.CountProposer(ctx, consAddresses)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "CountProposer error, %v", err)
				return
			}
			validator.Proposals = proposalsCount
			//signs
			signsCount, err := svcCtx.BlockSignatureModel.CountSigns(ctx, consAddresses, 0, nil, nil)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "CountSigns error, %v", err)
				return
			}
			validator.Signs = signsCount
			validator.Score = signsCount*common.SignScore + proposalsCount*common.ProposalScore

			statsCount, err := svcCtx.BlockSignatureModel.CountSigns(ctx, consAddresses, nodeHeight-common.UptimeHeight, nil, nil)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "Validator CountSigns error, %v", err)
				return
			}
			validator.SignsUptime = statsCount
			if statsCount > 0 {
				validator.Nodes = 1
			}
		}

		accountLastDayQuery := staking.OwnerQuery{Height: currentHeight - common.OneDayHeight, Owner: *entityAddress}
		accountLastDay, err := stakingApi.Account(ctx, &accountLastDayQuery)
		if err != nil {
			logc.Errorf(ctx, "stakingApi account error: %s", entityId, err)
			return
		}
		lastEscrow := accountLastDay.Escrow.Active.Balance.ToBigInt().Int64()
		if lastEscrow != 0 {
			validator.Escrow24H = validator.Escrow - lastEscrow
		}

		//delegator count
		if validator.EntityAddress != "" {
			delegatorCount, err := svcCtx.DelegatorModel.CountByValidator(ctx, validator.EntityAddress)
			if err != nil {
				logc.Errorf(ctx, "delegator CountByValidator error: %s", entityId, err)
				return
			}
			validator.Delegators = delegatorCount
		}

		err = svcCtx.ValidatorModel.Update(ctx, validator)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "Validator Update error, %v", err)
			return
		}
	}

	logc.Infof(ctx, "validator consensus info sync done...")
}

func DelegatorSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	validators, err := svcCtx.ValidatorModel.FindAll(ctx, "escrow", "desc")
	if err != nil {
		logc.Errorf(ctx, "Validator FindAll error, %v", err)
		return
	}
	err = svcCtx.PostgreDB.TransactCtx(ctx, func(ctx context.Context, session sqlx.Session) error {
		err = svcCtx.DelegatorModel.SessionDeleteAll(ctx, session)
		if err != nil {
			return fmt.Errorf("delegator SessionDeleteAll error, %v", err)
		}
		err = svcCtx.DelegatorModel.SessionResetSequence(ctx, session)
		if err != nil {
			return fmt.Errorf("delegator table reset sequence error, %v", err)
		}
		for _, validator := range validators {
			var validatorAddress staking.Address
			err := validatorAddress.UnmarshalText([]byte(validator.EntityAddress))
			if err != nil {
				return fmt.Errorf("validator entityAddress error, %v", err)
			}
			query := staking.OwnerQuery{Height: currentHeight, Owner: validatorAddress}
			delegationsTo, err := svcCtx.Staking.DelegationsTo(ctx, &query)
			if err != nil {
				return fmt.Errorf("staking api DelegationsTo error, %v", err)
			}

			//err = svcCtx.DelegatorModel.SessionDeleteAllByValidator(ctx, session, validatorAddress.String())
			//if err != nil {
			//	return fmt.Errorf("delegator SessionDeleteAllByValidator error, %v", err)
			//}
			for delegatorAddress, shares := range delegationsTo {
				delegatorModel := &model.Delegator{
					Validator: validatorAddress.String(),
					Delegator: delegatorAddress.String(),
					Shares:    shares.Shares.ToBigInt().Int64(),
					CreatedAt: time.Now(),
					UpdatedAt: time.Now(),
				}
				_, err = svcCtx.DelegatorModel.SessionInsert(ctx, session, delegatorModel)
				if err != nil {
					return fmt.Errorf("delegator SessionInsert error, %v", err)
				}
			}
			logc.Infof(ctx, "delegators sync done, %s, size: %d", validatorAddress.String(), len(delegationsTo))
		}
		logc.Infof(ctx, "delegators sync done....................")
		return nil
	})
	if err != nil {
		logc.Errorf(ctx, "delegators save db error, %v", err)
		return
	}
}

func DelegatorRewardSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	consensusApi := svcCtx.Consensus
	chainStatus, err := consensusApi.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentEpoch := chainStatus.LatestEpoch
	rewardEpoch := int64(currentEpoch - common.RewardStatsDays*24)

	//remove data before the stats days
	err = svcCtx.RewardModel.DeleteBeforeEpoch(ctx, rewardEpoch)
	if err != nil {
		logc.Errorf(ctx, "DeleteBeforeEpoch error, %v", err)
		return
	}

	rewardEpochProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, common.RewardEpochProperty)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "FindByProperty error, %v", err)
		return
	}
	if rewardEpochProperty != nil {
		rewardEpochDB, err := strconv.ParseInt(rewardEpochProperty.Value, 10, 64)
		if err != nil {
			logc.Errorf(ctx, "ParseInt error, %v", err)
			return
		}
		if rewardEpoch < rewardEpochDB {
			rewardEpoch = rewardEpochDB
		}
	} else {
		rewardEpochProperty = &model.SystemProperty{
			Property:  common.RewardEpochProperty,
			Value:     strconv.FormatInt(rewardEpoch, 10),
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		}
		_, err := svcCtx.SystemPropertyModel.Insert(ctx, rewardEpochProperty)
		if err != nil {
			logc.Errorf(ctx, "SystemPropertyModel insert error, %v", err)
			return
		}
	}

	validators, err := svcCtx.ValidatorModel.FindAll(ctx, "escrow", "desc")
	if err != nil {
		logc.Errorf(ctx, "Validator FindAll error, %v", err)
		return
	}

	for rewardEpoch < int64(currentEpoch) {
		rewardHeight, err := svcCtx.Beacon.GetEpochBlock(ctx, beacon.EpochTime(rewardEpoch))
		if err != nil {
			logc.Errorf(ctx, "getEpochBlock error, %v", err)
			return
		}
		block, err := svcCtx.Consensus.GetBlock(ctx, rewardHeight)
		if err != nil {
			logc.Errorf(ctx, "getBlock error, %v", err)
			return
		}

		for _, validator := range validators {
			var validatorAddress staking.Address
			if err = validatorAddress.UnmarshalText([]byte(validator.EntityAddress)); err != nil {
				logc.Errorf(ctx, "validator entityAddress error, %v", err)
				return
			}
			query := staking.OwnerQuery{Height: rewardHeight, Owner: validatorAddress}
			delegationsTo, err := svcCtx.Staking.DelegationsTo(ctx, &query)
			if err != nil {
				logc.Errorf(ctx, "staking api DelegationsTo error, %v", err)
				return
			}
			for delegatorAddress, delegation := range delegationsTo {
				logc.Infof(ctx, "delegation reward [%s][%s] start.", validatorAddress.String(), delegatorAddress.String())
				rewardModel, err := svcCtx.RewardModel.FindOneByDelegatorValidatorEpoch(ctx, delegatorAddress.String(), validatorAddress.String(), rewardEpoch)
				if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
					logc.Errorf(ctx, "FindOneByDelegatorValidatorEpoch error, %v", err)
					return
				}
				if rewardModel != nil {
					continue
				}

				validatorAccountQuery := staking.OwnerQuery{Height: rewardHeight, Owner: validatorAddress}
				validatorAccount, err := svcCtx.Staking.Account(ctx, &validatorAccountQuery)
				if err != nil {
					logc.Errorf(ctx, "stakingApi account error: %s, %v", delegatorAddress.String(), err)
					return
				}
				delegationAmount, err := validatorAccount.Escrow.Active.StakeForShares(&delegation.Shares)
				if err != nil {
					logc.Errorf(ctx, "computes the delegation amount error: %s, %v", delegatorAddress.String(), err)
					return
				}

				//computes the reward
				reward := big.NewInt(0)
				lastEpochRewardModel, err := svcCtx.RewardModel.FindOneByDelegatorValidatorEpoch(ctx, delegatorAddress.String(), validatorAddress.String(), rewardEpoch-1)
				if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
					logc.Errorf(ctx, "FindOneByDelegatorValidatorEpoch error, %v", err)
					return
				}
				if lastEpochRewardModel != nil {
					reward = new(big.Int).Sub(delegationAmount.ToBigInt(), big.NewInt(lastEpochRewardModel.DelegationAmount))
					if err != nil {
						logc.Errorf(ctx, "computes the reward error, %v", err)
						return
					}

					//remove reward caused by add or reclaim shares
					changeShares := new(big.Int).Sub(delegation.Shares.ToBigInt(), big.NewInt(lastEpochRewardModel.DelegationShares))
					if changeShares.CmpAbs(big.NewInt(0)) > 0 {
						errorReward, err := validatorAccount.Escrow.Active.StakeForShares(quantity.NewFromUint64(changeShares.Uint64()))
						if err != nil {
							logc.Errorf(ctx, "computes the error reward error: %s, %v", delegatorAddress.String(), err)
							return
						}
						reward = new(big.Int).Sub(reward, new(big.Int).Abs(errorReward.ToBigInt()))
					}

					if reward.Cmp(big.NewInt(0)) < 0 {
						reward = big.NewInt(0)
					}
				}

				rewardModel = &model.Reward{
					Delegator:        delegatorAddress.String(),
					Validator:        validatorAddress.String(),
					Epoch:            rewardEpoch,
					DelegationAmount: delegationAmount.ToBigInt().Int64(),
					DelegationShares: delegation.Shares.ToBigInt().Int64(),
					Reward:           reward.Int64(),
					CreatedAt:        block.Time,
					UpdatedAt:        time.Now(),
				}
				_, err = svcCtx.RewardModel.Insert(ctx, rewardModel)
				if err != nil {
					logc.Errorf(ctx, "RewardModel insert error, %v", err)
					return
				}
			}
			logc.Infof(ctx, "delegation reward validator [%s] done.", validatorAddress.String())
		}

		//update system property
		if rewardEpochProperty != nil {
			rewardEpochProperty.Value = strconv.FormatInt(rewardEpoch, 10)
			rewardEpochProperty.UpdatedAt = time.Now()
			err = svcCtx.SystemPropertyModel.UpdateByProperty(ctx, rewardEpochProperty)
			if err != nil {
				logc.Errorf(ctx, "SystemPropertyModel update error, %v", err)
				return
			}
		}

		logc.Infof(ctx, "delegation reward sync done, epoch:%d, current epoch:%d", rewardEpoch, currentEpoch)
		rewardEpoch++
	}
}

func EscrowStatsSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	now := time.Now().UTC()
	today := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, time.UTC)

	latestEscrow, err := svcCtx.EscrowStatsModel.FindLatestOne(ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "escrow stats FindLatestOne error, %v", err)
		return
	}
	scanDay := today.AddDate(0, 0, -35)
	if latestEscrow != nil {
		scanDay = latestEscrow.Date
	}

	validators, err := svcCtx.ValidatorModel.FindAll(ctx, "escrow", "desc")
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "validator FindAll error, %v", err)
		return
	}

	for scanDay.Compare(today) <= 0 {
		block, err := svcCtx.BlockModel.FindOneByTime(ctx, scanDay)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "block FindOneByTime error, %v", err)
			return
		}
		height := block.Height

		for _, validator := range validators {
			m, err := svcCtx.EscrowStatsModel.FindOneByEntityAddressHeight(ctx, validator.EntityAddress, height)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "EscrowStatsModel FindOneByEntityAddressHeight error, %v", err)
				return
			}
			if m != nil {
				continue
			}

			m, err = svcCtx.EscrowStatsModel.FindOneByEntityAddressDate(ctx, validator.EntityAddress, scanDay)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "EscrowStatsModel FindOneByEntityAddressDate error, %v", err)
				return
			}
			if m != nil {
				continue
			}

			var validatorAddress staking.Address
			err = validatorAddress.UnmarshalText([]byte(validator.EntityAddress))
			if err != nil {
				logc.Errorf(ctx, "validator entityAddress error, %v", err)
				return
			}
			accountQuery := staking.OwnerQuery{Height: height, Owner: validatorAddress}
			account, err := svcCtx.Staking.Account(ctx, &accountQuery)
			if err != nil {
				logc.Errorf(ctx, "staking Account error, %v", err)
				return
			}
			escrow := account.Escrow.Active.Balance
			m = &model.EscrowStats{
				Height:        height,
				EntityAddress: validator.EntityAddress,
				Escrow:        escrow.ToBigInt().Int64(),
				Date:          scanDay,
			}
			_, err = svcCtx.EscrowStatsModel.Insert(ctx, m)
			if err != nil {
				logc.Errorf(ctx, "EscrowStatsModel Insert error, %v", err)
				return
			}
		}
		logc.Infof(ctx, "escrow stats save done. %s", scanDay.Format("2006-01-02 15:04:05"))
		scanDay = scanDay.AddDate(0, 0, 1)
	}
}
