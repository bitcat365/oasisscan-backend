package task

import (
	"context"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	consensus "github.com/oasisprotocol/oasis-core/go/consensus/api"
	"github.com/oasisprotocol/oasis-core/go/consensus/api/transaction"
	"github.com/oasisprotocol/oasis-core/go/consensus/api/transaction/results"
	cometbft "github.com/oasisprotocol/oasis-core/go/consensus/cometbft/api"
	keymanager "github.com/oasisprotocol/oasis-core/go/keymanager/api"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/model"
	"strconv"
	"time"
)

func BlockScanner(ctx context.Context, svcCtx *svc.ServiceContext) {
	consensusApi := svcCtx.Consensus
	chainStatus, err := consensusApi.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	scanHeight := chainStatus.GenesisHeight
	scanBlock, err := svcCtx.BlockModel.FindLatestBlock(ctx)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(ctx, "FindLatestBlock error, %v", err)
		return
	}
	if scanBlock != nil {
		scanHeight = scanBlock.Height
		scanHeight++
	}

	logc.Infof(ctx, "block scan start: scanHeight:[%d], currentHeight:[%d]", scanHeight, currentHeight)

	for scanHeight <= currentHeight {
		//block
		block, err := consensusApi.GetBlock(ctx, scanHeight)
		if err != nil {
			logc.Errorf(ctx, "GetBlock error, %v", err)
			return
		}
		txsWithRes, err := consensusApi.GetTransactionsWithResults(ctx, scanHeight)
		if err != nil {
			logc.Errorf(ctx, "GetTransactionsWithResults error, %v", err)
			return
		}

		//transaction
		txs := txsWithRes.Transactions
		txResults := txsWithRes.Results
		var txsModels = make([]*model.Transaction, 0)
		for i := 0; i < len(txs); i++ {
			tx := txs[i]
			res := txResults[i]

			var signedTx transaction.SignedTransaction
			err = cbor.Unmarshal(tx, &signedTx)
			if err != nil {
				logc.Errorf(ctx, "tx cbor unmarshal error, %v", err)
				return
			}

			var raw transaction.Transaction
			err = cbor.Unmarshal(signedTx.Signed.Blob, &raw)
			if err != nil {
				logc.Errorf(ctx, "tr cbor unmarshal error, %v", err)
				return
			}

			m, err := parseMethod(block, &signedTx, &raw, res)
			if err != nil {
				logc.Errorf(ctx, "parse method error, %v", err)
				return
			}
			txsModels = append(txsModels, m)
		}

		epoch, err := svcCtx.Beacon.GetEpoch(ctx, scanHeight)
		if err != nil {
			logc.Errorf(ctx, "GetEpoch error, %v", err)
			return
		}

		startTime := time.Now()
		//sql transact
		err = svcCtx.PostgreDB.TransactCtx(ctx, func(ctx context.Context, session sqlx.Session) error {
			var meta cometbft.BlockMeta
			err := cbor.Unmarshal(block.Meta, &meta)
			if err != nil {
				return fmt.Errorf("block meta cbor unmarshal error, %v", err)
			}

			//block signature
			signatures := make([]*model.BlockSignature, 0, len(meta.LastCommit.Signatures))
			for _, s := range meta.LastCommit.Signatures {
				if s.ValidatorAddress.String() == "" {
					continue
				}
				signatures = append(signatures, &model.BlockSignature{
					Height:           block.Height,
					BlockIdFlag:      int64(s.BlockIDFlag),
					ValidatorAddress: s.ValidatorAddress.String(),
					Timestamp:        s.Timestamp,
					Signature:        hex.EncodeToString(s.Signature),
					CreatedAt:        time.Now(),
					UpdatedAt:        time.Now(),
				})
			}
			_, err = svcCtx.BlockSignatureModel.BatchSessionInsert(ctx, session, signatures)
			if err != nil {
				return fmt.Errorf("signature insert error, %v", err)
			}

			//block
			meta.LastCommit.Signatures = nil
			metaJson, err := json.Marshal(meta)
			if err != nil {
				return fmt.Errorf("block meta json unmarshal error, %v", err)
			}
			blockModel := model.Block{
				Height:          block.Height,
				Epoch:           int64(epoch),
				Hash:            block.Hash.Hex(),
				Timestamp:       block.Time,
				Meta:            string(metaJson),
				Txs:             int64(len(txs)),
				ProposerAddress: meta.Header.ProposerAddress.String(),
				CreatedAt:       time.Now(),
				UpdatedAt:       time.Now(),
			}
			_, err = svcCtx.BlockModel.SessionInsert(ctx, session, &blockModel)
			if err != nil {
				return fmt.Errorf("block insert error, %v", err)
			}

			//transaction
			_, err = svcCtx.TransactionModel.BatchSessionInsert(ctx, session, txsModels)
			if err != nil {
				return fmt.Errorf("transaction insert error, %v", err)
			}

			return nil
		})
		if err != nil {
			logc.Errorf(ctx, "block save db error, %v", err)
			return
		}
		logc.Infof(ctx, "transact duration [%v]", time.Since(startTime))

		logc.Infof(ctx, "block scan height: %d, current height: %d, size:[%d]", scanHeight, currentHeight, len(txsModels))
		scanHeight++
	}
}

func parseMethod(block *consensus.Block, signedTx *transaction.SignedTransaction, raw *transaction.Transaction, result *results.Result) (*model.Transaction, error) {
	errorJson, err := json.Marshal(result.Error)
	if err != nil {
		return nil, fmt.Errorf("errorJson error, %v", err)
	}
	eventsJson, err := json.Marshal(result.Events)
	if err != nil {
		return nil, fmt.Errorf("eventsJson error, %v", err)
	}
	rawJson, err := json.Marshal(raw)
	if err != nil {
		return nil, fmt.Errorf("rawJson error, %v", err)
	}
	m := model.Transaction{
		TxHash:    signedTx.Hash().String(),
		Method:    string(raw.Method),
		Status:    result.IsSuccess(),
		Nonce:     int64(raw.Nonce),
		Height:    block.Height,
		Timestamp: block.Time,
		SignAddr:  staking.NewAddress(signedTx.Signature.PublicKey).String(),
		Error:     string(errorJson),
		Events:    string(eventsJson),
		Raw:       string(rawJson),
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}
	if raw.Fee != nil {
		m.Fee = raw.Fee.Amount.ToBigInt().Int64()
	}

	switch raw.Method {
	case "staking.Transfer":
		var t staking.Transfer
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
		m.ToAddr = t.To.String()
		m.Amount = t.Amount.ToBigInt().Int64()
	case "staking.AddEscrow":
		var t staking.Escrow
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
		m.ToAddr = t.Account.String()
		m.Amount = t.Amount.ToBigInt().Int64()
	case "staking.ReclaimEscrow":
		var t staking.ReclaimEscrow
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
		m.ToAddr = t.Account.String()
		m.Shares = t.Shares.ToBigInt().Int64()
	case "staking.Burn":
		var t staking.Burn
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
		m.Amount = t.Amount.ToBigInt().Int64()
	case "staking.Allow":
		var t staking.Allow
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
		m.ToAddr = t.Beneficiary.String()
		m.Amount = t.AmountChange.ToBigInt().Int64()
	case "staking.Withdraw":
		var t staking.Withdraw
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
		m.ToAddr = t.From.String()
		m.Amount = t.Amount.ToBigInt().Int64()
	case "keymanager.UpdatePolicy":
		var t keymanager.SignedPolicySGX
		if err := cbor.Unmarshal(raw.Body, &t); err != nil {
			return nil, err
		}
	}

	return &m, nil
}

func StakingEventsScanner(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight
	scanHeight := chainStatus.GenesisHeight

	eventsSystemProperty := common.StakingEventsProperty
	scanHeightProperty, err := svcCtx.SystemPropertyModel.FindByProperty(ctx, eventsSystemProperty)
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
	} else {
		scanHeightProperty = &model.SystemProperty{
			Property:  eventsSystemProperty,
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

	for scanHeight <= currentHeight {
		events, err := svcCtx.Staking.GetEvents(ctx, scanHeight)
		if err != nil {
			logc.Errorf(ctx, "staking api GetEvents error, %v", err)
			return
		}
		for i, event := range events {
			m, err := svcCtx.StakingEventModel.FindOneByHeightPosition(ctx, scanHeight, int64(i))
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "FindOneByHeightPosition error, %v", err)
				return
			}
			if m == nil {
				j, err := json.Marshal(event)
				if err != nil {
					logc.Errorf(ctx, "event json marshal error, %v", err)
					return
				}
				m = &model.StakingEvent{
					Height:   scanHeight,
					Position: int64(i),
					TxHash:   event.TxHash.Hex(),
					Raw:      string(j),
				}
				kind, from, to := "", "", ""
				var amount int64 = 0
				if event.Transfer != nil {
					kind = event.Transfer.EventKind()
					from = event.Transfer.From.String()
					to = event.Transfer.To.String()
					amount = event.Transfer.Amount.ToBigInt().Int64()
				}
				if event.Burn != nil {
					kind = event.Burn.EventKind()
					from = event.Burn.Owner.String()
					amount = event.Burn.Amount.ToBigInt().Int64()
				}
				if event.Escrow != nil {
					escrow := event.Escrow
					if escrow.Add != nil {
						kind = escrow.Add.EventKind()
						from = escrow.Add.Owner.String()
						to = escrow.Add.Escrow.String()
						amount = escrow.Add.Amount.ToBigInt().Int64()
					}
					if escrow.Take != nil {
						kind = escrow.Take.EventKind()
						from = escrow.Take.Owner.String()
						amount = escrow.Take.Amount.ToBigInt().Int64()
					}
					if escrow.Reclaim != nil {
						kind = escrow.Reclaim.EventKind()
						from = escrow.Reclaim.Owner.String()
						to = escrow.Reclaim.Escrow.String()
						amount = escrow.Reclaim.Amount.ToBigInt().Int64()
					}
					if escrow.DebondingStart != nil {
						kind = escrow.DebondingStart.EventKind()
						from = escrow.DebondingStart.Owner.String()
						to = escrow.DebondingStart.Escrow.String()
						amount = escrow.DebondingStart.Amount.ToBigInt().Int64()
					}
				}
				if event.AllowanceChange != nil {
					kind = event.AllowanceChange.EventKind()
				}
				m.Kind = kind
				m.EventFrom = from
				m.EventTo = to
				m.Amount = amount

				_, err = svcCtx.StakingEventModel.Insert(ctx, m)
				if err != nil {
					logc.Errorf(ctx, "StakingEventModel insert error, %v", err)
					return
				}
			}
		}
		//update system property
		if scanHeightProperty != nil {
			scanHeightProperty.Value = strconv.FormatInt(scanHeight, 10)
			scanHeightProperty.UpdatedAt = time.Now()
			err = svcCtx.SystemPropertyModel.UpdateByProperty(ctx, scanHeightProperty)
			if err != nil {
				logc.Errorf(ctx, "SystemPropertyModel update error, %v", err)
				return
			}
		}

		logc.Infof(ctx, "staking events: height: %d, current height: %d, count: [%d]", scanHeight, currentHeight, len(events))
		scanHeight++
	}
}
