package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
)

var _ RuntimeRoundModel = (*customRuntimeRoundModel)(nil)

type (
	// RuntimeRoundModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRuntimeRoundModel.
	RuntimeRoundModel interface {
		runtimeRoundModel
		FindByRuntimeId(ctx context.Context, runtimeId string, pageable common.Pageable) ([]*RuntimeRound, error)
		CountByRuntimeId(ctx context.Context, runtimeId string) (int64, error)
		FindLatestOne(ctx context.Context, runtimeId string) (*RuntimeRound, error)
	}

	customRuntimeRoundModel struct {
		*defaultRuntimeRoundModel
	}
)

// NewRuntimeRoundModel returns a model for the database table.
func NewRuntimeRoundModel(conn sqlx.SqlConn) RuntimeRoundModel {
	return &customRuntimeRoundModel{
		defaultRuntimeRoundModel: newRuntimeRoundModel(conn),
	}
}

func (m *customRuntimeRoundModel) FindByRuntimeId(ctx context.Context, runtimeId string, pageable common.Pageable) ([]*RuntimeRound, error) {
	query := fmt.Sprintf("select %s from %s where runtime_id=$1 order by round desc limit %d offset %d", runtimeRoundRows, m.table, pageable.Limit, pageable.Offset)
	var resp []*RuntimeRound
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

func (m *customRuntimeRoundModel) CountByRuntimeId(ctx context.Context, runtimeId string) (int64, error) {
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

func (m *customRuntimeRoundModel) FindLatestOne(ctx context.Context, runtimeId string) (*RuntimeRound, error) {
	var resp RuntimeRound
	query := fmt.Sprintf("select %s from %s where runtime_id = $1 order by round desc limit 1", runtimeRoundRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, runtimeId)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
