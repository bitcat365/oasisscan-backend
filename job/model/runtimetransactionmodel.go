package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"strings"
)

var _ RuntimeTransactionModel = (*customRuntimeTransactionModel)(nil)

type (
	// RuntimeTransactionModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRuntimeTransactionModel.
	RuntimeTransactionModel interface {
		runtimeTransactionModel
		FindAll(ctx context.Context, runtimeId string, round int64, pageable common.Pageable) ([]*RuntimeTransaction, error)
		CountAll(ctx context.Context, runtimeId string, round int64) (int64, error)
		FindOneByTxHash(ctx context.Context, runtimeId string) (*RuntimeTransaction, error)
	}

	customRuntimeTransactionModel struct {
		*defaultRuntimeTransactionModel
	}
)

// NewRuntimeTransactionModel returns a model for the database table.
func NewRuntimeTransactionModel(conn sqlx.SqlConn) RuntimeTransactionModel {
	return &customRuntimeTransactionModel{
		defaultRuntimeTransactionModel: newRuntimeTransactionModel(conn),
	}
}

func (m *customRuntimeTransactionModel) FindAll(ctx context.Context, runtimeId string, round int64, pageable common.Pageable) ([]*RuntimeTransaction, error) {
	var resp []*RuntimeTransaction
	query := fmt.Sprintf("select %s from %s where ", runtimeTransactionRows, m.table)
	var conditions []string
	conditions = append(conditions, "1=1")
	var args []interface{}
	paramIndex := 1

	if runtimeId != "" {
		conditions = append(conditions, fmt.Sprintf("runtime_id = $%d", paramIndex))
		args = append(args, runtimeId)
		paramIndex++
	}
	if round > 0 {
		conditions = append(conditions, fmt.Sprintf("round = $%d", paramIndex))
		args = append(args, round)
		paramIndex++
	}

	if len(conditions) > 0 {
		query += strings.Join(conditions, " AND ")
	}
	query += fmt.Sprintf(" order by id desc limit %d offset %d", pageable.Limit, pageable.Offset)

	err := m.conn.QueryRowsCtx(ctx, &resp, query, args...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customRuntimeTransactionModel) CountAll(ctx context.Context, runtimeId string, round int64) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s where ", m.table)
	var conditions []string
	conditions = append(conditions, "1=1")
	var args []interface{}
	paramIndex := 1

	if runtimeId != "" {
		conditions = append(conditions, fmt.Sprintf("runtime_id = $%d", paramIndex))
		args = append(args, runtimeId)
		paramIndex++
	}
	if round > 0 {
		conditions = append(conditions, fmt.Sprintf("round = $%d", paramIndex))
		args = append(args, round)
		paramIndex++
	}

	if len(conditions) > 0 {
		query += strings.Join(conditions, " AND ")
	}

	err := m.conn.QueryRowCtx(ctx, &resp, query, args...)
	switch err {
	case nil:
		return resp, nil
	default:
		return 0, err
	}
}

func (m *customRuntimeTransactionModel) FindOneByTxHash(ctx context.Context, hash string) (*RuntimeTransaction, error) {
	query := fmt.Sprintf("select %s from %s where tx_hash=$1 order by result desc limit 1", runtimeTransactionRows, m.table)
	var resp RuntimeTransaction
	err := m.conn.QueryRowCtx(ctx, &resp, query, hash)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
