package chain

import (
	"net/http"

	"github.com/zeromicro/go-zero/rest/httpx"
	"oasisscan-backend/api/internal/logic/chain"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func ChainBlockInfoHandler(svcCtx *svc.ServiceContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req types.ChainBlockInfoRequest
		if err := httpx.Parse(r, &req); err != nil {
			httpx.Error(w, err)
			return
		}

		l := chain.NewChainBlockInfoLogic(r.Context(), svcCtx)
		resp, err := l.ChainBlockInfo(&req)
		response.Response(w, resp, err)
	}
}
