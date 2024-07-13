package response

import (
	"fmt"
	"math/big"
	"net/http"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"
	"oasisscan-backend/job/model"
	"strconv"

	"github.com/zeromicro/go-zero/rest/httpx"
)

type Body struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

func Response(w http.ResponseWriter, resp interface{}, err error) {
	var body Body
	if err != nil {
		body.Code = -1
		body.Message = err.Error()
	} else {
		body.Message = "OK"
		body.Data = resp
	}
	httpx.OkJson(w, body)
}

func ValidatorResponseFormat(m *model.Validator) *types.ValidatorInfo {
	commission, _ := strconv.ParseFloat(fmt.Sprintf("%.4f", float64(m.Commission)/float64(100000)), 64)
	return &types.ValidatorInfo{
		EntityId:       m.EntityId,
		Rank:           int(m.Rank),
		Name:           m.Name,
		EntityAddress:  m.EntityAddress,
		NodeId:         m.NodeId,
		NodeAddress:    m.NodeAddress,
		Icon:           m.Icon,
		Website:        m.Website,
		Twitter:        m.Twitter,
		Keybase:        m.Keybase,
		Email:          m.Email,
		Description:    m.Description,
		Balance:        fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Balance), common.Decimals)),
		TotalShares:    fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.TotalShares), common.Decimals)),
		Escrow:         fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Escrow), common.Decimals)),
		EscrowChange24: fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Escrow24H), common.Decimals)),
		Score:          m.Score,
		Proposals:      m.Proposals,
		Signs:          m.Signs,
		Uptime:         fmt.Sprintf("%.0f", float64(m.SignsUptime)/float64(common.UptimeHeight)*100) + "%",
		Active:         m.Nodes == 1,
		Commission:     commission,
		Status:         m.Status,
		Delegators:     m.Delegators,
	}
}
