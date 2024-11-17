package task

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	beacon "github.com/oasisprotocol/oasis-core/go/beacon/api"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	governance "github.com/oasisprotocol/oasis-core/go/governance/api"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/common"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/model"
	"strconv"
	"time"
)

func ProposalSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight
	currentEpoch := chainStatus.LatestEpoch
	proposals, err := svcCtx.Governance.Proposals(ctx, currentHeight)
	if err != nil {
		logc.Errorf(ctx, "Proposals error, %v", err)
		return
	}

	for _, proposal := range proposals {
		m, err := svcCtx.ProposalModel.FindOneByProposalId(ctx, int64(proposal.ID))
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(ctx, "FindOneByProposalId error, %v", err)
			return
		}
		var createdTime time.Time
		var closedTime time.Time
		createdHeight, err := svcCtx.Beacon.GetEpochBlock(ctx, proposal.CreatedAt)
		if err != nil {
			createdTime = common.GetEpochDurationTime(time.Now(), int64(proposal.CreatedAt)-int64(currentEpoch), true)
		} else {
			createdBlock, err := svcCtx.Consensus.GetBlock(ctx, createdHeight)
			if err != nil {
				logc.Errorf(ctx, "createdHeight getBlock error, %v", err)
				return
			}
			createdTime = createdBlock.Time.UTC()
		}

		epochDuration := int64(proposal.ClosesAt) - int64(proposal.CreatedAt)
		if m.ClosedEpoch <= int64(currentEpoch) {
			closedHeight, err := svcCtx.Beacon.GetEpochBlock(ctx, proposal.ClosesAt)
			if err != nil {
				closedTime = common.GetEpochDurationTime(time.Now(), int64(proposal.ClosesAt)-int64(currentEpoch), true)
			} else {
				closedBlock, err := svcCtx.Consensus.GetBlock(ctx, closedHeight)
				if err != nil {
					logc.Errorf(ctx, "closedBlock getBlock error, %v", err)
					return
				}
				closedTime = closedBlock.Time.UTC()
			}
		} else {
			closedTime = common.GetEpochDurationTime(createdTime, epochDuration, true)
		}

		if m != nil {
			//update state and closed time
			if proposal.State.String() != m.State {
				m.State = proposal.State.String()
			}
			m.CreatedTime = createdTime
			m.ClosedTime = closedTime
			err = svcCtx.ProposalModel.Update(ctx, m)
			if err != nil {
				logc.Errorf(ctx, "Proposal [%d] update error, %v", proposal.ID, err)
				return
			}
			continue
		}

		title := "Proposal-" + strconv.FormatUint(proposal.ID, 10)
		proposalType := ""
		content := proposal.Content
		if content.Upgrade != nil {
			title = string(content.Upgrade.Handler)
			proposalType = "upgrade"
		} else if content.CancelUpgrade != nil {
			title = "cancel-upgrade-" + strconv.FormatUint(content.CancelUpgrade.ProposalID, 10)
			proposalType = "cancel upgrade"
		} else if content.ChangeParameters != nil {
			var changesMap map[string]interface{}
			if err := cbor.Unmarshal(content.ChangeParameters.Changes, &changesMap); err != nil {
				logc.Errorf(ctx, "txData unmarshal error, %v", err)
				return
			}
			changeType := ""
			for k, v := range changesMap {
				if v != nil {
					changeType = k
				}
			}
			title = "change-parameter-" + content.ChangeParameters.Module + "-" + changeType
			proposalType = "change parameter"
		}
		rawJson, err := json.Marshal(proposal)
		if err != nil {
			logc.Errorf(ctx, "raw json error, %v", err)
		}
		m = &model.Proposal{
			ProposalId:   int64(proposal.ID),
			Title:        title,
			Type:         proposalType,
			Submitter:    proposal.Submitter.String(),
			State:        proposal.State.String(),
			CreatedEpoch: int64(proposal.CreatedAt),
			ClosedEpoch:  int64(proposal.ClosesAt),
			Raw:          string(rawJson),
			CreatedTime:  createdTime,
			ClosedTime:   closedTime,
		}
		_, err = svcCtx.ProposalModel.Insert(ctx, m)
		if err != nil {
			logc.Errorf(ctx, "proposal insert error, %v", err)
			return
		}
	}
	logc.Infof(ctx, "proposal sync done...")
}

func VoteSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight

	proposals, err := svcCtx.ProposalModel.FindAllProposals(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	for _, proposal := range proposals {
		proposalId := proposal.ProposalId
		votesQuery := governance.ProposalQuery{Height: currentHeight, ProposalID: uint64(proposalId)}
		votes, err := svcCtx.Governance.Votes(ctx, &votesQuery)
		if err != nil {
			logc.Errorf(ctx, "Votes error, %v", err)
			return
		}

		proposalHeight, err := svcCtx.Beacon.GetEpochBlock(ctx, beacon.EpochTime(proposal.ClosedEpoch))
		if err != nil {
			proposalHeight = chainStatus.GenesisHeight
		}
		totalVotes := quantity.NewQuantity()
		voteMap := make(map[staking.Address]*quantity.Quantity, 0)
		for _, vote := range votes {
			address := vote.Voter
			accountQuery := staking.OwnerQuery{Height: proposalHeight, Owner: address}
			accountInfo, err := svcCtx.Staking.Account(ctx, &accountQuery)
			if err != nil {
				logc.Errorf(ctx, "account info error, %v", err)
				return
			}
			if accountInfo != nil {
				escrow := accountInfo.Escrow.Active.Balance
				voteMap[address] = &escrow
				err = totalVotes.Add(&escrow)
				if err != nil {
					logc.Errorf(ctx, "totalVotes add error, %v", err)
					return
				}
			}
		}

		for _, vote := range votes {
			voteAddress := vote.Voter.String()
			m, err := svcCtx.VoteModel.FindOneByProposalIdVoteAddress(ctx, proposalId, voteAddress)
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(ctx, "FindOneByProposalIdVoteAddress error, %v", err)
				return
			}

			optionVoteFloat := new(big.Float).SetInt(voteMap[vote.Voter].ToBigInt())
			p, _ := new(big.Float).Quo(optionVoteFloat, new(big.Float).SetInt(totalVotes.ToBigInt())).Float64()
			percent, err := strconv.ParseFloat(fmt.Sprintf("%.4f", p), 64)
			if err != nil {
				logc.Errorf(ctx, "percent compute error, %v", err)
				return
			}

			if m == nil {
				m = &model.Vote{
					ProposalId:  proposalId,
					VoteAddress: voteAddress,
					Option:      vote.Vote.String(),
					Amount:      voteMap[vote.Voter].ToBigInt().Int64(),
					Percent:     percent,
				}
				_, err = svcCtx.VoteModel.Insert(ctx, m)
				if err != nil {
					logc.Errorf(ctx, "vote insert error, %v", err)
					return
				}
			} else {
				m.Amount = voteMap[vote.Voter].ToBigInt().Int64()
				m.Percent = percent
				err = svcCtx.VoteModel.Update(ctx, m)
				if err != nil {
					logc.Errorf(ctx, "vote update error, %v", err)
					return
				}
			}
		}
		logc.Infof(ctx, "proposal [%d] vote sync done...", proposal.Id)
	}
}
