package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"strings"
	"time"
)

var _ BlockSignatureModel = (*customBlockSignatureModel)(nil)

type (
	// BlockSignatureModel is an interface to be customized, add more methods here,
	// and implement the added methods in customBlockSignatureModel.
	BlockSignatureModel interface {
		blockSignatureModel
		SessionInsert(ctx context.Context, session sqlx.Session, data *BlockSignature) (sql.Result, error)
		BatchSessionInsert(ctx context.Context, session sqlx.Session, data []*BlockSignature) (sql.Result, error)
		CountSigns(ctx context.Context, signAddresses []string, from int64, startTime *time.Time, endTime *time.Time) (int64, error)
		ValidatorSignStats(ctx context.Context, signAddresses []string, days int64) ([]*BlockCountDay, error)
		FindBlocksByHeight(ctx context.Context, signAddresses []string, startHeight int64) ([]*BlockSignature, error)
		RefreshBlockCountDaysView(ctx context.Context) error
		FindBlockCountDays(ctx context.Context) ([]*BlockCountDay, error)
		FindLatestOne(ctx context.Context) (*BlockSignature, error)
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

func (m *customBlockSignatureModel) BatchSessionInsert(ctx context.Context, session sqlx.Session, data []*BlockSignature) (sql.Result, error) {
	if len(data) == 0 {
		return nil, nil
	}

	valueStrings := make([]string, 0, len(data))
	valueArgs := make([]interface{}, 0, len(data)*7)

	for i := 0; i < len(data); i++ {
		n := i * 7
		valueStrings = append(valueStrings, fmt.Sprintf("($%d, $%d, $%d, $%d, $%d, $%d, $%d)",
			n+1, n+2, n+3, n+4, n+5, n+6, n+7))

		record := data[i]
		valueArgs = append(valueArgs,
			record.Height,
			record.BlockIdFlag,
			record.ValidatorAddress,
			record.Timestamp,
			record.Signature,
			record.CreatedAt,
			record.UpdatedAt)
	}

	query := fmt.Sprintf("insert into %s (%s) values %s",
		m.table,
		blockSignatureRowsExpectAutoSet,
		strings.Join(valueStrings, ","))

	ret, err := session.ExecCtx(ctx, query, valueArgs...)
	return ret, err
}

func (m *customBlockSignatureModel) CountSigns(ctx context.Context, signAddresses []string, from int64, startTime *time.Time, endTime *time.Time) (int64, error) {
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
	if startTime != nil {
		query += fmt.Sprintf(" and timestamp > $%d", paramIndex+1)
		vars = append(vars, startTime)
		paramIndex = paramIndex + 1
	}
	if endTime != nil {
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

func (m *customBlockSignatureModel) ValidatorSignStats(ctx context.Context, signAddresses []string, days int64) ([]*BlockCountDay, error) {
	query := fmt.Sprintf("select DATE_TRUNC('day', timestamp) AS day, count(distinct height) from %s where validator_address in (", m.table)
	vars := make([]interface{}, 0)
	for i, signAddress := range signAddresses {
		query += fmt.Sprintf("$%d,", i+1)
		vars = append(vars, signAddress)
	}
	query = query[:len(query)-1] + ")"

	paramIndex := len(signAddresses)
	if days > 0 {
		query += fmt.Sprintf(" and timestamp >= now() - interval '$%d days'", paramIndex+1)
		vars = append(vars, days)
		paramIndex = paramIndex + 1
	}
	var resp []*BlockCountDay
	err := m.conn.QueryRowCtx(ctx, &resp, query, vars...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customBlockSignatureModel) FindBlocksByHeight(ctx context.Context, signAddresses []string, startHeight int64) ([]*BlockSignature, error) {
	var resp []*BlockSignature
	query := fmt.Sprintf("select %s from %s where validator_address in (", blockSignatureRows, m.table)
	vars := make([]interface{}, 0)
	for i, signAddress := range signAddresses {
		query += fmt.Sprintf("$%d,", i+1)
		vars = append(vars, signAddress)
	}
	query = query[:len(query)-1] + ")"
	paramIndex := len(signAddresses)
	query += fmt.Sprintf(" and height>$%d order by height desc", paramIndex+1)
	vars = append(vars, startHeight)

	err := m.conn.QueryRowsCtx(ctx, &resp, query, vars...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
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

func (m *customBlockSignatureModel) FindLatestOne(ctx context.Context) (*BlockSignature, error) {
	var resp BlockSignature
	query := fmt.Sprintf("select %s from %s order by id desc limit 1", blockSignatureRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
