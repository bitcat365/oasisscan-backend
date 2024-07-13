package account

import (
	"context"
	"errors"
	"fmt"
	"github.com/zeromicro/go-zero/core/logc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"io"
	"math/big"
	"oasisscan-backend/api/internal/errort"
	"oasisscan-backend/api/internal/svc"
	"oasisscan-backend/api/internal/types"
	"oasisscan-backend/common"
	"strconv"

	"github.com/zeromicro/go-zero/core/logx"
)

type AccountRewardExportLogic struct {
	logx.Logger
	ctx    context.Context
	svcCtx *svc.ServiceContext
	writer io.Writer
}

func NewAccountRewardExportLogic(ctx context.Context, svcCtx *svc.ServiceContext, writer io.Writer) *AccountRewardExportLogic {
	return &AccountRewardExportLogic{
		Logger: logx.WithContext(ctx),
		ctx:    ctx,
		svcCtx: svcCtx,
		writer: writer,
	}
}

func (l *AccountRewardExportLogic) AccountRewardExport(req *types.AccountRewardExportRequest) (resp *types.AccountRewardExportResponse, err error) {
	accountAddress := req.Account
	pageable := common.Pageable{
		Limit:  1000,
		Offset: 0,
	}
	rewardModels, err := l.svcCtx.RewardModel.FindByDelegator(l.ctx, accountAddress, pageable)
	if err != nil && !errors.Is(err, sqlx.ErrNotFound) {
		logc.Errorf(l.ctx, "find reward error, %v", err)
		return nil, errort.NewDefaultError()
	}
	excelTitle := []string{"time", "validator address", "validator name", "epoch", "reward"}
	excelData := make([][]string, 0)
	for _, m := range rewardModels {
		v, err := l.svcCtx.ValidatorModel.FindOneByEntityAddress(l.ctx, m.Validator)
		if err != nil {
			logc.Errorf(l.ctx, "find validator info error, %v", err)
			return nil, errort.NewDefaultError()
		}
		data := []string{
			m.CreatedAt.Format("2006-01-02 15:04:05"),
			v.EntityAddress,
			v.Name,
			strconv.FormatInt(m.Epoch, 10),
			fmt.Sprintf("%.9f", common.ValueToFloatByDecimals(big.NewInt(m.Reward), common.Decimals)),
		}
		excelData = append(excelData, data)
	}
	excelBytes, err := common.ToExcel(excelTitle, excelData)
	if err != nil {
		logc.Errorf(l.ctx, "to excel error, %v", err)
		return nil, errort.NewDefaultError()
	}
	_, err = l.writer.Write(excelBytes)
	if err != nil {
		logc.Errorf(l.ctx, "excel write error, %v", err)
		return nil, errort.NewDefaultError()
	}

	return
}
