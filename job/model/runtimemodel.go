package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ RuntimeModel = (*customRuntimeModel)(nil)

type (
	// RuntimeModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRuntimeModel.
	RuntimeModel interface {
		runtimeModel
		FindAll(ctx context.Context) ([]*Runtime, error)
		FindAllByStatus(ctx context.Context, status int64) ([]*Runtime, error)
	}

	customRuntimeModel struct {
		*defaultRuntimeModel
	}
)

// NewRuntimeModel returns a model for the database table.
func NewRuntimeModel(conn sqlx.SqlConn) RuntimeModel {
	return &customRuntimeModel{
		defaultRuntimeModel: newRuntimeModel(conn),
	}
}

func (m *customRuntimeModel) FindAll(ctx context.Context) ([]*Runtime, error) {
	query := fmt.Sprintf("select %s from %s", runtimeRows, m.table)
	var resp []*Runtime
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

func (m *customRuntimeModel) FindAllByStatus(ctx context.Context, status int64) ([]*Runtime, error) {
	query := fmt.Sprintf("select %s from %s where status=$1 order by id", runtimeRows, m.table)
	var resp []*Runtime
	err := m.conn.QueryRowsCtx(ctx, &resp, query, status)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
