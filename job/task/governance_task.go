package task

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/oasisprotocol/oasis-core/go/common/cbor"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/job/internal/svc"
	"oasisscan-backend/job/model"
	"strconv"
)

func ProposalSync(ctx context.Context, svcCtx *svc.ServiceContext) {
	chainStatus, err := svcCtx.Consensus.GetStatus(ctx)
	if err != nil {
		logc.Errorf(ctx, "GetStatus error, %v", err)
		return
	}
	currentHeight := chainStatus.LatestHeight
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
		if m != nil {
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
		}
		_, err = svcCtx.ProposalModel.Insert(ctx, m)
		if err != nil {
			logc.Errorf(ctx, "proposal insert error, %v", err)
			return
		}
	}
	logc.Infof(ctx, "proposal sync done...")
}
