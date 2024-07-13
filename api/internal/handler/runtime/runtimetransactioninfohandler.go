package runtime

import (
	"net/http"

	"github.com/zeromicro/go-zero/rest/httpx"
	"oasisscan-backend/api/internal/logic/runtime"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func RuntimeTransactionInfoHandler(svcCtx *svc.ServiceContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req types.RuntimeTransactionInfoRequest
		if err := httpx.Parse(r, &req); err != nil {
			httpx.Error(w, err)
			return
		}

		l := runtime.NewRuntimeTransactionInfoLogic(r.Context(), svcCtx)
		resp, err := l.RuntimeTransactionInfo(&req)
		response.Response(w, resp, err)
	}
}
