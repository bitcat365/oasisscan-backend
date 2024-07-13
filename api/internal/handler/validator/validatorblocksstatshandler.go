package validator

import (
	"net/http"

	"github.com/zeromicro/go-zero/rest/httpx"
	"oasisscan-backend/api/internal/logic/validator"
	"oasisscan-backend/api/internal/response"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
)

func ValidatorBlocksStatsHandler(svcCtx *svc.ServiceContext) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req types.ValidatorBlocksStatsRequest
		if err := httpx.Parse(r, &req); err != nil {
			httpx.Error(w, err)
			return
		}

		l := validator.NewValidatorBlocksStatsLogic(r.Context(), svcCtx)
		resp, err := l.ValidatorBlocksStats(&req)
		response.Response(w, resp, err)
	}
}
