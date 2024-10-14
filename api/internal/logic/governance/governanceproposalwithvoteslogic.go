package governance

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	beacon "github.com/oasisprotocol/oasis-core/go/beacon/api"
	"github.com/oasisprotocol/oasis-core/go/common/quantity"
	governance "github.com/oasisprotocol/oasis-core/go/governance/api"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/logx"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"
	"sort"
	"strconv"
	"time"
)

type GovernanceProposalWithVotesLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
}

func NewGovernanceProposalWithVotesLogic(ctx context.Context, svcCtx *svc.ServiceContext) *GovernanceProposalWithVotesLogic {
	return &GovernanceProposalWithVotesLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
	}
}

func (l *GovernanceProposalWithVotesLogic) GovernanceProposalWithVotes(req *types.GovernanceProposalWithVotesRequest) (resp *types.GovernanceProposalWithVotesResponse, err error) {
	v, err := l.svcCtx.LocalCache.ProposalWithVotesCache.Take(strconv.FormatInt(req.Id, 10), func() (interface{}, error) {
		m, err := l.svcCtx.ProposalModel.FindOneByProposalId(l.ctx, req.Id)
		if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
			logc.Errorf(l.ctx, "FindOneByProposalId error, %v", err)
			return nil, errort.NewDefaultError()
		}

		var proposal governance.Proposal
		err = json.Unmarshal([]byte(m.Raw), &proposal)
		if err != nil {
			logc.Errorf(l.ctx, "json unmarshal error, %v", err)
			return nil, errort.NewDefaultError()
		}

		chainStatus, err := l.svcCtx.Consensus.GetStatus(l.ctx)
		if err != nil {
			logc.Errorf(l.ctx, "GetStatus error, %v", err)
			return nil, errort.NewDefaultError()
		}

		createdHeight, err := l.svcCtx.Beacon.GetEpochBlock(l.ctx, beacon.EpochTime(m.CreatedEpoch))
		if err != nil {
			logc.Errorf(l.ctx, "createHeight getEpochBlock error, %v", err)
			return nil, errort.NewDefaultError()
		}
		createdBlock, err := l.svcCtx.Consensus.GetBlock(l.ctx, createdHeight)
		if err != nil {
			logc.Errorf(l.ctx, "createdHeight getBlock error, %v", err)
			return nil, errort.NewDefaultError()
		}
		epochDuration := m.ClosedEpoch - m.CreatedEpoch
		var closedTime time.Time
		currentEpoch := chainStatus.LatestEpoch
		if m.ClosedEpoch <= int64(currentEpoch) {
			closedHeight, err := l.svcCtx.Beacon.GetEpochBlock(l.ctx, beacon.EpochTime(m.ClosedEpoch))
			if err != nil {
				logc.Errorf(l.ctx, "closedHeight getEpochBlock error, %v", err)
				return nil, errort.NewDefaultError()
			}
			closedBlock, err := l.svcCtx.Consensus.GetBlock(l.ctx, closedHeight)
			if err != nil {
				logc.Errorf(l.ctx, "closedBlock getBlock error, %v", err)
				return nil, errort.NewDefaultError()
			}
			closedTime = closedBlock.Time
		} else {
			closedTime = createdBlock.Time.Add(time.Duration(epochDuration) * time.Hour)
			closedTime = time.Date(closedTime.Year(), closedTime.Month(), closedTime.Day(), closedTime.Hour(), 0, 0, 0, createdBlock.Time.Location())
		}

		proposalResp := &types.GovernanceProposalInfo{
			Id:          m.ProposalId,
			Title:       m.Title,
			Type:        m.Type,
			Submitter:   m.Submitter,
			State:       m.State,
			Deposit:     fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(proposal.Deposit.ToBigInt(), common.Decimals)),
			CreatedAt:   m.CreatedEpoch,
			ClosedAt:    m.ClosedEpoch,
			CreatedTime: createdBlock.Time.Unix(),
			ClosedTime:  closedTime.Unix(),
		}

		currentHeight := chainStatus.LatestHeight
		votesQuery := governance.ProposalQuery{Height: currentHeight, ProposalID: uint64(req.Id)}
		votes, err := l.svcCtx.Governance.Votes(l.ctx, &votesQuery)
		if err != nil {
			logc.Errorf(l.ctx, "Votes error, %v", err)
			return nil, errort.NewDefaultError()
		}

		proposalHeight, err := l.svcCtx.Beacon.GetEpochBlock(l.ctx, beacon.EpochTime(m.ClosedEpoch))
		if err != nil {
			//logc.Errorf(l.ctx, "getEpochBlock error, %v", err)
			proposalHeight = chainStatus.GenesisHeight
		}

		totalVotes := quantity.NewQuantity()
		voteMap := make(map[staking.Address]*quantity.Quantity, 0)
		optionMap := make(map[string]*quantity.Quantity, 0)
		optionMap["yes"] = quantity.NewQuantity()
		optionMap["no"] = quantity.NewQuantity()
		optionMap["abstain"] = quantity.NewQuantity()
		for _, vote := range votes {
			address := vote.Voter
			accountQuery := staking.OwnerQuery{Height: proposalHeight, Owner: address}
			accountInfo, err := l.svcCtx.Staking.Account(l.ctx, &accountQuery)
			if err != nil {
				logc.Errorf(l.ctx, "account info error, %v", err)
				return nil, errort.NewDefaultError()
			}
			if accountInfo != nil {
				escrow := accountInfo.Escrow.Active.Balance
				voteMap[address] = &escrow
				err = totalVotes.Add(&escrow)
				if err != nil {
					logc.Errorf(l.ctx, "totalVotes add error, %v", err)
					return nil, errort.NewDefaultError()
				}

				optionVote := optionMap[vote.Vote.String()].Clone()
				err = optionVote.Add(&escrow)
				if err != nil {
					logc.Errorf(l.ctx, "optionVote add error, %v", err)
					return nil, errort.NewDefaultError()
				}
				optionMap[vote.Vote.String()] = optionVote
			}
		}

		optionList := make([]*types.ProposalOption, 0)
		for optionName, optionVote := range optionMap {
			optionVoteFloat := new(big.Float).SetInt(optionVote.ToBigInt())
			p, _ := new(big.Float).Quo(optionVoteFloat, new(big.Float).SetInt(totalVotes.ToBigInt())).Float64()
			percent, err := strconv.ParseFloat(fmt.Sprintf("%.4f", p), 64)
			if err != nil {
				logc.Errorf(l.ctx, "percent compute error, %v", err)
				return nil, errort.NewDefaultError()
			}
			optionList = append(optionList, &types.ProposalOption{
				Name:    optionName,
				Amount:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(optionVote.ToBigInt(), common.Decimals)),
				Percent: percent,
			})
		}
		sort.SliceStable(optionList, func(i, j int) bool {
			sharesI, _ := strconv.ParseFloat(optionList[i].Amount, 64)
			sharesJ, _ := strconv.ParseFloat(optionList[j].Amount, 64)
			return sharesI > sharesJ
		})

		voteList := make([]*types.ProposalVote, 0)
		//calculate every entity votes power
		for _, vote := range votes {
			validatorInfo, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, vote.Voter.String())
			if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
				logc.Errorf(l.ctx, "validator FindOneByEntityAddress error, %v", err)
				return nil, errort.NewDefaultError()
			}

			name, icon := "", ""
			if validatorInfo != nil {
				name = validatorInfo.Name
				icon = validatorInfo.Icon
			}

			optionVoteFloat := new(big.Float).SetInt(voteMap[vote.Voter].ToBigInt())
			p, _ := new(big.Float).Quo(optionVoteFloat, new(big.Float).SetInt(totalVotes.ToBigInt())).Float64()
			percent, err := strconv.ParseFloat(fmt.Sprintf("%.4f", p), 64)
			if err != nil {
				logc.Errorf(l.ctx, "percent compute error, %v", err)
				return nil, errort.NewDefaultError()
			}

			voteList = append(voteList, &types.ProposalVote{
				Name:    name,
				Icon:    icon,
				Address: vote.Voter.String(),
				Vote:    vote.Vote.String(),
				Amount:  fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(voteMap[vote.Voter].ToBigInt(), common.Decimals)),
				Percent: percent,
			})
		}
		sort.SliceStable(voteList, func(i, j int) bool {
			sharesI, _ := strconv.ParseFloat(voteList[i].Amount, 64)
			sharesJ, _ := strconv.ParseFloat(voteList[j].Amount, 64)
			return sharesI > sharesJ
		})
		resp = &types.GovernanceProposalWithVotesResponse{
			GovernanceProposalInfo: proposalResp,
			Options:                optionList,
			Votes:                  voteList,
		}
		return resp, nil
	})

	if err != nil {
		logc.Errorf(l.ctx, "cache error, %v", err)
		return nil, errort.NewDefaultError()
	}
	resp = v.(*types.GovernanceProposalWithVotesResponse)

	return
}
