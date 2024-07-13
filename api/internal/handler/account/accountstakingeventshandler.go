package account

import (
	"net/http"

	"github.com/zeromicro/go-zero/rest/httpx"
	"oasisscan-backend/api/internal/logic/account"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func AccountStakingEventsHandler(svcCtx *svc.ServiceContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req types.AccountStakingEventsRequest
		if err := httpx.Parse(r, &req); err != nil {
			httpx.Error(w, err)
			return
		}

		l := account.NewAccountStakingEventsLogic(r.Context(), svcCtx)
		resp, err := l.AccountStakingEvents(&req)
		response.Response(w, resp, err)
	}
}
