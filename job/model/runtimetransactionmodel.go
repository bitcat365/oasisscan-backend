package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
)

var _ RuntimeTransactionModel = (*customRuntimeTransactionModel)(nil)

type (
	// RuntimeTransactionModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRuntimeTransactionModel.
	RuntimeTransactionModel interface {
		runtimeTransactionModel
		FindAll(ctx context.Context, runtimeId string, pageable common.Pageable) ([]*RuntimeTransaction, error)
		CountAll(ctx context.Context, runtimeId string) (int64, error)
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

func (m *customRuntimeTransactionModel) FindAll(ctx context.Context, runtimeId string, pageable common.Pageable) ([]*RuntimeTransaction, error) {
	query := fmt.Sprintf("select %s from %s where runtime_id=$1 order by id desc limit %d offset %d", runtimeTransactionRows, m.table, pageable.Limit, pageable.Offset)
	var resp []*RuntimeTransaction
	err := m.conn.QueryRowsCtx(ctx, &resp, query, runtimeId)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customRuntimeTransactionModel) CountAll(ctx context.Context, runtimeId string) (int64, error) {
	query := fmt.Sprintf("select count(*) from %s where runtime_id=$1", m.table)
	var resp int64
	err := m.conn.QueryRowCtx(ctx, &resp, query, runtimeId)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
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
