package task

import (
	"context"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	eth_types "github.com/ethereum/go-ethereum/core/types"
	common_namespace "github.com/oasisprotocol/oasis-core/go/common"
	"github.com/oasisprotocol/oasis-core/go/common/crypto/signature"
	"github.com/oasisprotocol/oasis-core/go/common/node"
	registry "github.com/oasisprotocol/oasis-core/go/registry/api"
	roothash "github.com/oasisprotocol/oasis-core/go/roothash/api"
	roothash_block "github.com/oasisprotocol/oasis-core/go/roothash/api/block"
	runtime "github.com/oasisprotocol/oasis-core/go/runtime/client/api"
	scheduler "github.com/oasisprotocol/oasis-core/go/scheduler/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/model"
	"strconv"
	"strings"
	"time"

	runtimeSdkClient "github.com/oasisprotocol/oasis-sdk/client-sdk/go/client"
)

func RuntimeInfoSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	query := registry.GetRuntimesQuery{Height: currentHeight, IncludeSuspended: false}
	runtimes, err := svcCtx.Registry.GetRuntimes(ctx, &query)
	if err != nil {
		logc.Errorf(ctx, "GetRuntimes error, %v", err)
		return
	}

	for _, r := range runtimes {
		if r.Kind != 1 {
			continue
		}

		runtimeId := r.ID.String()
		m, err := svcCtx.RuntimeModel.FindOneByRuntimeId(ctx, runtimeId)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "FindOneByRuntimeId error, %v", err)
			return
		}
		if m != nil {
			continue
		}
		m = &model.Runtime{
			RuntimeId: runtimeId,
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		}
		_, err = svcCtx.RuntimeModel.Insert(ctx, m)
		if err != nil {
			logc.Errorf(ctx, "runtime insert error, %v", err)
			return
		}
	}
	logc.Infof(ctx, "Runtime info sync done.")
}

func RuntimeRoundSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	runtimes, err := svcCtx.RuntimeModel.FindAll(ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "runtime FindAll error, %v", err)
		return
	}

	for _, r := range runtimes {
		runtimeId := r.RuntimeId

		var nameSpace common_namespace.Namespace
		err = nameSpace.UnmarshalText([]byte(runtimeId))
		if err != nil {
			logc.Errorf(ctx, "namespace unmarshal error, %v", err)
			return
		}

		blockRequest := roothash.RuntimeRequest{RuntimeID: nameSpace, Height: currentHeight}
		runtimeState, err := svcCtx.RootHash.GetRuntimeState(ctx, &blockRequest)
		if err != nil {
			logc.Errorf(ctx, "GetRuntimeState error, %v", err)
			return
		}
		currentRound := int64(runtimeState.LastNormalRound)

		roundSystemProperty := common.RuntimeRoundPrefix + runtimeId
		scanRound := int64(runtimeState.GenesisBlock.Header.Round)
		scanRoundProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, roundSystemProperty)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "FindByProperty error, %v", err)
			return
		}
		if scanRoundProperty != nil {
			scanRound, err = strconv.ParseInt(scanRoundProperty.Value, 10, 64)
			if err != nil {
				logc.Errorf(ctx, "ParseInt error, %v", err)
				return
			}
		} else {
			scanRoundProperty = &model.SystemProperty{
				Property:  roundSystemProperty,
				Value:     strconv.FormatInt(scanRound, 10),
				CreatedAt: time.Now(),
				UpdatedAt: time.Now(),
			}
			_, err := svcCtx.SystemPropertyModel.Insert(ctx, scanRoundProperty)
			if err != nil {
				logc.Errorf(ctx, "SystemPropertyModel insert error, %v", err)
				return
			}
		}

		for scanRound <= currentRound {
			blockRequest := runtime.GetBlockRequest{RuntimeID: nameSpace, Round: uint64(scanRound)}
			runtimeRound, err := svcCtx.Runtime.GetBlock(ctx, &blockRequest)
			if err != nil {
				if strings.Contains(err.Error(), "block not found") {
					return
				}
				logc.Errorf(ctx, "get runtime round error, %s, %d, %v", runtimeId, scanRound, err)
				return
			}

			header := runtimeRound.Header
			runtimeRoundModel, err := svcCtx.RuntimeRoundModel.FindOneByRuntimeIdRound(ctx, runtimeId, int64(header.Round))
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "FindOneByRuntimeIdRound error, %v", err)
				return
			}

			syncMode := ""
			if runtimeRoundModel == nil {
				runtimeRoundModel = &model.RuntimeRound{
					RuntimeId:    runtimeId,
					Round:        int64(header.Round),
					Version:      int64(header.Version),
					Timestamp:    time.Unix(int64(header.Timestamp), 0).UTC(),
					HeaderType:   int64(header.HeaderType),
					PreviousHash: header.PreviousHash.String(),
					IoRoot:       header.IORoot.String(),
					StateRoot:    header.StateRoot.String(),
					MessagesHash: header.MessagesHash.String(),
					InMsgsHash:   header.InMessagesHash.String(),
					CreatedAt:    time.Now(),
					UpdatedAt:    time.Now(),
				}
				_, err = svcCtx.RuntimeRoundModel.Insert(ctx, runtimeRoundModel)
				if err != nil {
					logc.Errorf(ctx, "runtime round insert error, %v", err)
					return
				}
				syncMode = "insert"
			} else {
				runtimeRoundModel = &model.RuntimeRound{
					RuntimeId:    runtimeId,
					Round:        int64(header.Round),
					Version:      int64(header.Version),
					Timestamp:    time.Unix(int64(header.Timestamp), 0).UTC(),
					HeaderType:   int64(header.HeaderType),
					PreviousHash: header.PreviousHash.String(),
					IoRoot:       header.IORoot.String(),
					StateRoot:    header.StateRoot.String(),
					MessagesHash: header.MessagesHash.String(),
					InMsgsHash:   header.InMessagesHash.String(),
					UpdatedAt:    time.Now(),
				}
				err = svcCtx.RuntimeRoundModel.Update(ctx, runtimeRoundModel)
				if err != nil {
					logc.Errorf(ctx, "runtime round update error, %v", err)
					return
				}
				syncMode = "update"
			}

			//update system property
			if scanRoundProperty != nil {
				scanRoundProperty.Value = strconv.FormatInt(scanRound, 10)
				scanRoundProperty.UpdatedAt = time.Now()
				err = svcCtx.SystemPropertyModel.UpdateByProperty(ctx, scanRoundProperty)
				if err != nil {
					logc.Errorf(ctx, "SystemPropertyModel update error, %v", err)
					return
				}
			}

			logc.Infof(ctx, "Runtime round sync done. mode:[%s] %d %d [%s]", syncMode, scanRound, currentRound, runtimeId)
			scanRound++
		}
	}
}

func RuntimeTransactionSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	runtimes, err := svcCtx.RuntimeModel.FindAllByStatus(ctx, 0)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "runtime FindAllByStatus error, %v", err)
		return
	}

	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	for _, r := range runtimes {
		runtimeId := r.RuntimeId

		var nameSpace common_namespace.Namespace
		err = nameSpace.UnmarshalText([]byte(runtimeId))
		if err != nil {
			logc.Errorf(ctx, "namespace unmarshal error, %v", err)
			return
		}

		blockRequest := roothash.RuntimeRequest{RuntimeID: nameSpace, Height: currentHeight}
		runtimeState, err := svcCtx.RootHash.GetRuntimeState(ctx, &blockRequest)
		if err != nil {
			logc.Errorf(ctx, "GetRuntimeState error, %v", err)
			return
		}
		currentRound := int64(runtimeState.LastNormalRound)

		roundSystemProperty := common.RuntimeTxRoundPrefix + runtimeId
		scanRound := int64(runtimeState.GenesisBlock.Header.Round)
		scanRoundProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, roundSystemProperty)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "FindByProperty error, %v", err)
			return
		}
		if scanRoundProperty != nil {
			scanRound, err = strconv.ParseInt(scanRoundProperty.Value, 10, 64)
			if err != nil {
				logc.Errorf(ctx, "ParseInt error, %v", err)
				return
			}
		} else {
			scanRoundProperty = &model.SystemProperty{
				Property:  roundSystemProperty,
				Value:     strconv.FormatInt(scanRound, 10),
				CreatedAt: time.Now(),
				UpdatedAt: time.Now(),
			}
			_, err := svcCtx.SystemPropertyModel.Insert(ctx, scanRoundProperty)
			if err != nil {
				logc.Errorf(ctx, "SystemPropertyModel insert error, %v", err)
				return
			}
		}

		runtimeClient := runtimeSdkClient.New(svcCtx.GrpcConn, nameSpace)

		for scanRound <= currentRound {
			scanBlockRequest := runtime.GetBlockRequest{RuntimeID: nameSpace, Round: uint64(scanRound)}
			scanRuntimeRound, err := svcCtx.Runtime.GetBlock(ctx, &scanBlockRequest)
			if err != nil {
				if strings.Contains(err.Error(), "block not found") {
					return
				}
				logc.Errorf(ctx, "get runtime round error, %s, %d, %v", runtimeId, scanRound, err)
				return
			}

			txList, err := runtimeClient.GetTransactionsWithResults(ctx, uint64(scanRound))
			if err != nil {
				logc.Errorf(ctx, "runtime GetTransactionsWithResults error, %s, %d, %v", runtimeId, scanRound, err)
				return
			}

			for i := 0; i < len(txList); i++ {
				txr := txList[i]

				runtimeTransactionModel, err := svcCtx.RuntimeTransactionModel.FindOneByRuntimeIdRoundTxHash(ctx, runtimeId, scanRound, txr.Tx.Hash().Hex())
				if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
					logc.Errorf(ctx, "FindOneByRuntimeIdRound error, %v", err)
					return
				}

				tx, err := common.OpenUtxNoVerify(&txr.Tx)
				if err != nil {
					logc.Errorf(ctx, "tx decode error, %v", err)
					return
				}

				consensusFrom, consensusTo := "", ""
				ethHash, evmFrom, evmTo := "", "", ""
				if len(txr.Tx.AuthProofs) == 1 && txr.Tx.AuthProofs[0].Module == "evm.ethereum.v0" {
					ethHash = hex.EncodeToString(common.Keccak256(txr.Tx.Body))

					txData := new(eth_types.Transaction)
					if err := txData.UnmarshalBinary(txr.Tx.Body); err != nil {
						logc.Errorf(ctx, "txData unmarshal error, %v", err)
						return
					}

					from, err := eth_types.Sender(eth_types.NewCancunSigner(txData.ChainId()), txData)
					if err != nil {
						logc.Errorf(ctx, "from address error, %s, %d, %v", runtimeId, scanRound, err)
						return
					}
					evmFrom = strings.ToLower(from.Hex())
					if txData.To() != nil {
						evmTo = strings.ToLower(txData.To().Hex())
					}
				} else {
					var body common.RuntimeTransactionBody
					err = common.CborUnmarshalOmit(tx.Call.Body, &body)
					if err != nil {
						logc.Errorf(ctx, "runtime id:%s ,current round:%d, consensus body unmarshal error, %v", runtimeId, scanRound, err)
						return
					}

					for _, si := range tx.AuthInfo.SignerInfo {
						address, err := si.AddressSpec.Address()
						if err != nil {
							logc.Errorf(ctx, "consensusFrom error, %v", err)
							return
						}
						consensusFrom = address.ConsensusAddress().String()
					}
					consensusTo = body.To.ConsensusAddress().String()
				}

				txrBodyJson, err := json.Marshal(txr)
				if err != nil {
					logc.Errorf(ctx, "txrBodyJson error, %v", err)
					return
				}

				eventsJson, err := json.Marshal(txr.Events)
				if err != nil {
					logc.Errorf(ctx, "eventsJson error, %v", err)
					return
				}

				message := ""
				if !txr.Result.IsSuccess() {
					message = txr.Result.Failed.Message
				}

				if runtimeTransactionModel == nil {
					runtimeTransactionModel = &model.RuntimeTransaction{
						RuntimeId:     runtimeId,
						Round:         scanRound,
						TxHash:        txr.Tx.Hash().Hex(),
						Position:      int64(i),
						EvmHash:       ethHash,
						ConsensusFrom: consensusFrom,
						ConsensusTo:   consensusTo,
						EvmFrom:       evmFrom,
						EvmTo:         evmTo,
						Method:        string(tx.Call.Method),
						Result:        txr.Result.IsSuccess(),
						Messages:      message,
						Timestamp:     time.Unix(int64(scanRuntimeRound.Header.Timestamp), 0).UTC(),
						Type:          txr.Tx.AuthProofs[0].Module,
						Raw:           string(txrBodyJson),
						Events:        string(eventsJson),
						CreatedAt:     time.Now(),
						UpdatedAt:     time.Now(),
					}
					_, err = svcCtx.RuntimeTransactionModel.Insert(ctx, runtimeTransactionModel)
					if err != nil {
						logc.Errorf(ctx, "runtime transaction insert error, %v", err)
						return
					}
				} else {
					runtimeTransactionModel = &model.RuntimeTransaction{
						RuntimeId:     runtimeId,
						Round:         scanRound,
						TxHash:        txr.Tx.Hash().Hex(),
						Position:      int64(i),
						EvmHash:       ethHash,
						ConsensusFrom: consensusFrom,
						ConsensusTo:   consensusTo,
						EvmFrom:       evmFrom,
						EvmTo:         evmTo,
						Method:        string(tx.Call.Method),
						Result:        txr.Result.IsSuccess(),
						Messages:      message,
						Timestamp:     time.Unix(int64(scanRuntimeRound.Header.Timestamp), 0).UTC(),
						Type:          txr.Tx.AuthProofs[0].Module,
						Raw:           string(txrBodyJson),
						Events:        string(eventsJson),
						UpdatedAt:     time.Now(),
					}
					err = svcCtx.RuntimeTransactionModel.Update(ctx, runtimeTransactionModel)
					if err != nil {
						logc.Errorf(ctx, "runtime transaction update error, %v", err)
						return
					}
				}
			}

			//update system property
			if scanRoundProperty != nil {
				scanRoundProperty.Value = strconv.FormatInt(scanRound, 10)
				scanRoundProperty.UpdatedAt = time.Now()
				err = svcCtx.SystemPropertyModel.UpdateByProperty(ctx, scanRoundProperty)
				if err != nil {
					logc.Errorf(ctx, "SystemPropertyModel update error, %v", err)
					return
				}
			}

			logc.Infof(ctx, "runtime transaction %s, round: %d, current round:%d, count: %d", runtimeId, scanRound, currentRound, len(txList))
			scanRound++
		}
	}
}

// RuntimeStats See https://github.com/oasisprotocol/oasis-core/blob/master/go/oasis-node/cmd/control/runtime_stats.go
func RuntimeStats(ctx context.Context, svcCtx *svc.ServiceContext) {
	runtimes, err := svcCtx.RuntimeModel.FindAll(ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "runtime FindAllByStatus error, %v", err)
		return
	}

	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	for _, r := range runtimes {
		runtimeStatsProperty := common.RuntimeStatsHeightPrefix + r.RuntimeId
		scanHeight := chainStatus.GenesisHeight
		scanHeightProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, runtimeStatsProperty)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "FindByProperty error, %v", err)
			return
		}
		if scanHeightProperty != nil {
			scanHeight, err = strconv.ParseInt(scanHeightProperty.Value, 10, 64)
			if err != nil {
				logc.Errorf(ctx, "ParseInt error, %v", err)
				return
			}
			scanHeight++
		} else {
			scanHeightProperty = &model.SystemProperty{
				Property:  runtimeStatsProperty,
				Value:     strconv.FormatInt(scanHeight, 10),
				CreatedAt: time.Now(),
				UpdatedAt: time.Now(),
			}
			_, err := svcCtx.SystemPropertyModel.Insert(ctx, scanHeightProperty)
			if err != nil {
				logc.Errorf(ctx, "SystemPropertyModel insert error, %v", err)
				return
			}
		}

		var runtimeID common_namespace.Namespace
		err = runtimeID.UnmarshalText([]byte(r.RuntimeId))
		if err != nil {
			logc.Errorf(ctx, "namespace unmarshal error, %v", err)
			return
		}
		//logc.Info(ctx, "gathering statistics",
		//	"runtime_id", runtimeID,
		//	"start_height", scanHeight,
		//	"end_height", currentHeight,
		//)

		// Do the actual work

		var (
			currentRound     uint64
			currentCommittee *scheduler.Committee
			currentScheduler *scheduler.CommitteeNode
			roundDiscrepancy bool
		)

		nodeToEntity := make(map[signature.PublicKey]signature.PublicKey)

		stats := &common.RuntimeStats{
			Entities: make(map[signature.PublicKey]*common.RuntimeEntityStats),
		}
		for ; scanHeight <= currentHeight; scanHeight++ {
			err = svcCtx.PostgreDB.TransactCtx(ctx, func(ctx context.Context, session sqlx.Session) error {
				if scanHeight%1000 == 0 {
					logc.Debug(ctx, "progressed",
						"height", scanHeight,
					)
				}
				// Update node to entity map.
				var nodes []*node.Node
				if nodes, err = svcCtx.Registry.GetNodes(ctx, scanHeight); err != nil {
					return fmt.Errorf("failed to get nodes, %v, height: %d", err, scanHeight)
				}
				for _, n := range nodes {
					nodeToEntity[n.ID] = n.EntityID

					//save node entity
					nodeRuntimes := n.Runtimes
					for _, nr := range nodeRuntimes {
						if nr.ID.Equal(&runtimeID) {
							rnm, err := svcCtx.RuntimeNodeModel.SessionFindOneByRuntimeIdNodeId(ctx, session, runtimeID.String(), n.ID.String())
							if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
								return fmt.Errorf("FindOneByRuntimeIdNodeId error, %v", err)
							}
							if rnm == nil {
								rnm = &model.RuntimeNode{
									RuntimeId: runtimeID.String(),
									NodeId:    n.ID.String(),
									EntityId:  n.EntityID.String(),
								}
								_, err := svcCtx.RuntimeNodeModel.SessionInsert(ctx, session, rnm)
								if err != nil {
									return fmt.Errorf("runtime node insert error, %v", err)
								}
							}
							break
						}
					}
				}

				// Query latest roothash block and events.
				var blk *roothash_block.Block
				blk, err = svcCtx.RootHash.GetLatestBlock(ctx, &roothash.RuntimeRequest{RuntimeID: runtimeID, Height: scanHeight})
				switch err {
				case nil:
				case roothash.ErrInvalidRuntime:
					return nil
				default:
					return fmt.Errorf("failed to get roothash block, %v, height: %d", err, scanHeight)
				}
				var evs []*roothash.Event
				if evs, err = svcCtx.RootHash.GetEvents(ctx, scanHeight); err != nil {
					return fmt.Errorf("failed to get roothash events, %v, height: %d", err, scanHeight)
				}

				// Go over events before updating potential new round committee info.
				// Even if round transition happened at this height, all events emitted
				// at this height belong to the previous round.
				for _, ev := range evs {
					// Skip events for initial height where we don't have round info yet.
					if scanHeight == chainStatus.GenesisHeight {
						break
					}
					// Skip events for other runtimes.
					if ev.RuntimeID != runtimeID {
						continue
					}

					switch {
					case ev.ExecutorCommitted != nil:
						// Nothing to do here. We use Finalized event Good/Bad Compute node
						// fields to process commitments.
					case ev.ExecutionDiscrepancyDetected != nil:
						if ev.ExecutionDiscrepancyDetected.Timeout {
							stats.DiscrepancyDetectedTimeout++
						} else {
							stats.DiscrepancyDetected++
						}
						roundDiscrepancy = true
					case ev.Finalized != nil:
						var rtResults *roothash.RoundResults
						if rtResults, err = svcCtx.RootHash.GetLastRoundResults(ctx, &roothash.RuntimeRequest{RuntimeID: runtimeID, Height: scanHeight}); err != nil {
							return fmt.Errorf("failed to get last round results, %v, height: %d", err, scanHeight)
						}

						// Skip the empty finalized event that is triggered on initial round.
						if len(rtResults.GoodComputeEntities) == 0 && len(rtResults.BadComputeEntities) == 0 && currentCommittee == nil {
							continue
						}
						// Skip if epoch transition or suspended blocks.
						if blk.Header.HeaderType == roothash_block.EpochTransition || blk.Header.HeaderType == roothash_block.Suspended {
							continue
						}

						// Update stats.
						if currentCommittee != nil {
						OUTER:
							for _, member := range currentCommittee.Members {
								entity := nodeToEntity[member.PublicKey]
								// Primary workers are always required.
								if member.Role == scheduler.RoleWorker {
									stats.Entities[entity].RoundsPrimaryRequired++
								}
								// In case of discrepancies backup workers were invoked as well.
								if roundDiscrepancy && member.Role == scheduler.RoleBackupWorker {
									stats.Entities[entity].RoundsBackupRequired++
								}

								// Go over good commitments.
								for _, v := range rtResults.GoodComputeEntities {
									if entity != v {
										continue
									}
									switch member.Role {
									case scheduler.RoleWorker:
										stats.Entities[entity].CommittedGoodBlocksPrimary++
										continue OUTER
									case scheduler.RoleBackupWorker:
										if roundDiscrepancy {
											stats.Entities[entity].CommittedGoodBlocksBackup++
											continue OUTER
										}
									}
								}

								// Go over bad commitments.
								for _, v := range rtResults.BadComputeEntities {
									if entity != v {
										continue
									}
									switch member.Role {
									case scheduler.RoleWorker:
										stats.Entities[entity].CommittedBadBlocksPrimary++
										continue OUTER
									case scheduler.RoleBackupWorker:
										if roundDiscrepancy {
											stats.Entities[entity].CommittedBadBlocksBackup++
											continue OUTER
										}

									}
								}

								// Neither good nor bad - missed commitment.
								if member.Role == scheduler.RoleWorker {
									stats.Entities[entity].MissedPrimary++
								}
								if roundDiscrepancy && member.Role == scheduler.RoleBackupWorker {
									stats.Entities[entity].MissedBackup++
								}
							}
						}
					}
				}

				// New round.
				if currentRound != blk.Header.Round {
					currentRound = blk.Header.Round
					stats.Rounds++

					switch blk.Header.HeaderType {
					case roothash_block.Normal:
						stats.SuccessfulRounds++
					case roothash_block.EpochTransition:
						stats.EpochTransitionRounds++
					case roothash_block.RoundFailed:
						stats.FailedRounds++
					case roothash_block.Suspended:
						stats.SuspendedRounds++
						currentCommittee = nil
						currentScheduler = nil
						return nil
					default:
						return fmt.Errorf("unexpected block header type, %v, height: %d", blk.Header.HeaderType, scanHeight)
					}

					// Query runtime state and setup committee info for the round.
					var state *roothash.RuntimeState
					if state, err = svcCtx.RootHash.GetRuntimeState(ctx, &roothash.RuntimeRequest{RuntimeID: runtimeID, Height: scanHeight}); err != nil {
						return fmt.Errorf("failed to query runtime state, %v, height: %d", err, scanHeight)
					}
					if state.Committee == nil || state.CommitmentPool == nil {
						// No committee - election failed(?)
						logc.Debug(ctx, "unexpected missing committee for runtime",
							"height", scanHeight,
						)
						currentCommittee = nil
						currentScheduler = nil
						return nil
					}
					// Set committee info.
					var ok bool
					currentCommittee = state.Committee
					currentScheduler, ok = currentCommittee.Scheduler(currentRound, 0)
					if !ok {
						return fmt.Errorf("failed to query primary scheduler, no workers in committee, %v, height: %d", err, scanHeight)
					}
					roundDiscrepancy = false

					// Update election stats.
					seen := make(map[signature.PublicKey]bool)
					for _, member := range currentCommittee.Members {
						entity := nodeToEntity[member.PublicKey]
						if _, ok := stats.Entities[entity]; !ok {
							stats.Entities[entity] = &common.RuntimeEntityStats{}
						}

						//update stats
						rs, err := svcCtx.RuntimeStatsModel.SessionFindOneByRuntimeIdEntityId(ctx, session, runtimeID.String(), entity.String())
						if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
							return fmt.Errorf("FindOneByRuntimeIdEntityId error, %v", err)
						}
						rs = &model.RuntimeStats{
							RuntimeId:      runtimeID.String(),
							EntityId:       entity.String(),
							RoundsElected:  0,
							RoundsPrimary:  0,
							RoundsBackup:   0,
							RoundsProposer: 0,
						}

						// Multiple records for same node in case the node has
						// multiple roles. Only count it as elected once.
						if !seen[member.PublicKey] {
							stats.Entities[entity].RoundsElected++
							rs.RoundsElected = 1
						}
						seen[member.PublicKey] = true

						if member.Role == scheduler.RoleWorker {
							stats.Entities[entity].RoundsPrimary++
							rs.RoundsPrimary = 1
						}
						if member.Role == scheduler.RoleBackupWorker {
							stats.Entities[entity].RoundsBackup++
							rs.RoundsBackup = 1
						}
						if member.PublicKey == currentScheduler.PublicKey {
							stats.Entities[entity].RoundsProposer++
							rs.RoundsProposer = 1
						}

						if errors.Is(err, sqlx.ErrNotFound) {
							_, err := svcCtx.RuntimeStatsModel.SessionInsert(ctx, session, rs)
							if err != nil {
								return fmt.Errorf("runtime stats insert error, %v", err)
							}
						} else {
							err := svcCtx.RuntimeStatsModel.SessionUpdateStatsCount(ctx, session, rs)
							if err != nil {
								return fmt.Errorf("runtime stats update error, %v", err)
							}
						}
					}
				}

				//update system property
				if scanHeightProperty != nil {
					scanHeightProperty.Value = strconv.FormatInt(scanHeight, 10)
					scanHeightProperty.UpdatedAt = time.Now()
					err = svcCtx.SystemPropertyModel.SessionUpdateByProperty(ctx, session, scanHeightProperty)
					if err != nil {
						return fmt.Errorf("SystemPropertyModel update error, %v", err)
					}
				}

				logc.Infof(ctx, "runtime stats: %s, %d", r.RuntimeId, scanHeight)
				return nil
			})
			if err != nil {
				logc.Errorf(ctx, "stats error: %v", err)
				return
			}
		}
	}
}
