package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"time"
)

var _ BlockSignatureModel = (*customBlockSignatureModel)(nil)

type (
	// BlockSignatureModel is an interface to be customized, add more methods here,
	// and implement the added methods in customBlockSignatureModel.
	BlockSignatureModel interface {
		blockSignatureModel
		SessionInsert(ctx context.Context, session sqlx.Session, data *BlockSignature) (sql.Result, error)
		CountSigns(ctx context.Context, signAddresses []string, from int64, startTime int64, endTime int64) (int64, error)
		FindBlocks(ctx context.Context, pageable common.Pageable) ([]*BlockSignature, error)
		CountDistinctBlocksByTimestamp(ctx context.Context, start uint64, end uint64) ([]uint64, error)
		RefreshBlockCountDaysView(ctx context.Context) error
		FindBlockCountDays(ctx context.Context) ([]*BlockCountDay, error)
	}

	customBlockSignatureModel struct {
		*defaultBlockSignatureModel
	}

	BlockCountDay struct {
		Day   time.Time `db:"day"`
		Count int64     `db:"count"`
	}
)

// NewBlockSignatureModel returns a model for the database table.
func NewBlockSignatureModel(conn sqlx.SqlConn) BlockSignatureModel {
	return &customBlockSignatureModel{
		defaultBlockSignatureModel: newBlockSignatureModel(conn),
	}
}

func (m *customBlockSignatureModel) SessionInsert(ctx context.Context, session sqlx.Session, data *BlockSignature) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7)", m.table, blockSignatureRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.Height, data.BlockIdFlag, data.ValidatorAddress, data.Timestamp, data.Signature, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *customBlockSignatureModel) CountSigns(ctx context.Context, signAddresses []string, from int64, startTime int64, endTime int64) (int64, error) {
	query := fmt.Sprintf("select count(*) from %s where validator_address in (", m.table)
	vars := make([]interface{}, 0)
	for i, signAddress := range signAddresses {
		query += fmt.Sprintf("$%d,", i+1)
		vars = append(vars, signAddress)
	}
	query = query[:len(query)-1] + ")"

	paramIndex := len(signAddresses)
	if from > 0 {
		query += fmt.Sprintf(" and height > $%d and height <= $%d", paramIndex+1, paramIndex+2)
		vars = append(vars, from, from+common.UptimeHeight)
		paramIndex = paramIndex + 2
	}
	if startTime > 0 {
		query += fmt.Sprintf(" and timestamp > $%d", paramIndex+1)
		vars = append(vars, startTime)
		paramIndex = paramIndex + 1
	}
	if endTime > 0 {
		query += fmt.Sprintf(" and timestamp <= $%d", paramIndex+1)
		vars = append(vars, endTime)
		paramIndex = paramIndex + 1
	}
	var resp int64
	err := m.conn.QueryRowCtx(ctx, &resp, query, vars...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customBlockSignatureModel) FindBlocks(ctx context.Context, pageable common.Pageable) ([]*BlockSignature, error) {
	var resp []*BlockSignature
	query := fmt.Sprintf("select * from %s order by height desc limit %d offset %d ", m.table, pageable.Limit, pageable.Offset)
	err := m.conn.QueryRowsCtx(ctx, &resp, query)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customBlockSignatureModel) CountDistinctBlocksByTimestamp(ctx context.Context, start uint64, end uint64) ([]uint64, error) {
	var resp []uint64
	query := fmt.Sprintf("select count(distinct height) from %s where timestamp>=$1 and timestamp<=$2 order by height desc; ", m.table)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, start, end)
	switch err {
	case nil:
		return resp, nil
	default:
		return nil, err
	}
}

func (m *customBlockSignatureModel) RefreshBlockCountDaysView(ctx context.Context) error {
	query := fmt.Sprintf("REFRESH MATERIALIZED VIEW block_count_days")
	_, err := m.conn.ExecCtx(ctx, query)
	return err
}

func (m *customBlockSignatureModel) FindBlockCountDays(ctx context.Context) ([]*BlockCountDay, error) {
	var resp []*BlockCountDay
	query := fmt.Sprintf("select day,count from block_count_days order by day desc limit 11")
	err := m.conn.QueryRowsCtx(ctx, &resp, query)
	switch err {
	case nil:
		return resp, nil
	default:
		return nil, err
	}
}
