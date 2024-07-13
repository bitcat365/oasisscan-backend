package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
)

var _ StakingEventModel = (*customStakingEventModel)(nil)

type (
	// StakingEventModel is an interface to be customized, add more methods here,
	// and implement the added methods in customStakingEventModel.
	StakingEventModel interface {
		stakingEventModel
		withSession(session sqlx.Session) StakingEventModel
		FindByAddress(ctx context.Context, address string, pageable common.Pageable) ([]*StakingEvent, error)
		CountByAddress(ctx context.Context, address string) (int64, error)
	}

	customStakingEventModel struct {
		*defaultStakingEventModel
	}
)

// NewStakingEventModel returns a model for the database table.
func NewStakingEventModel(conn sqlx.SqlConn) StakingEventModel {
	return &customStakingEventModel{
		defaultStakingEventModel: newStakingEventModel(conn),
	}
}

func (m *customStakingEventModel) withSession(session sqlx.Session) StakingEventModel {
	return NewStakingEventModel(sqlx.NewSqlConnFromSession(session))
}

func (m *customStakingEventModel) FindByAddress(ctx context.Context, address string, pageable common.Pageable) ([]*StakingEvent, error) {
	query := fmt.Sprintf("select %s from %s where event_from=$1 or event_to=$2 order by height desc,position desc limit %d offset %d", stakingEventRows, m.table, pageable.Limit, pageable.Offset)
	var resp []*StakingEvent
	err := m.conn.QueryRowsCtx(ctx, &resp, query, address, address)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customStakingEventModel) CountByAddress(ctx context.Context, address string) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s where event_from=$1 or event_to=$2", m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, address, address)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}
