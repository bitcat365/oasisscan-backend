package governance

import (
	"net/http"

	"github.com/zeromicro/go-zero/rest/httpx"
	"oasisscan-backend/api/internal/logic/governance"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func GovernanceProposalWithVotesHandler(svcCtx *svc.ServiceContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req types.GovernanceProposalWithVotesRequest
		if err := httpx.Parse(r, &req); err != nil {
			httpx.Error(w, err)
			return
		}

		l := governance.NewGovernanceProposalWithVotesLogic(r.Context(), svcCtx)
		resp, err := l.GovernanceProposalWithVotes(&req)
		response.Response(w, resp, err)
	}
}
