package runtime

import (
	"net/http"

	"github.com/zeromicro/go-zero/rest/httpx"
	"oasisscan-backend/api/internal/logic/runtime"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func RuntimeListHandler(svcCtx *svc.ServiceContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req types.RuntimeListRequest
		if err := httpx.Parse(r, &req); err != nil {
			httpx.Error(w, err)
			return
		}

		l := runtime.NewRuntimeListLogic(r.Context(), svcCtx)
		resp, err := l.RuntimeList(&req)
		response.Response(w, resp, err)
	}
}
