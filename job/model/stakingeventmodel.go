package model

import (
	"context"
	"fmt"
	"oasisscan-backend/common"

	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
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
	query := fmt.Sprintf(`
		WITH from_part AS (
			SELECT %s FROM %s WHERE event_from=$1 ORDER BY height DESC, position DESC LIMIT %d
		),
		to_part AS (
			SELECT %s FROM %s WHERE event_to=$1 ORDER BY height DESC, position DESC LIMIT %d
		),
		u AS (
			SELECT *, row_number() OVER (PARTITION BY id ORDER BY height DESC, position DESC) AS rn
			FROM (
				SELECT * FROM from_part
				UNION ALL
				SELECT * FROM to_part
			) x
		)
		SELECT %s FROM u WHERE rn = 1 ORDER BY height DESC, position DESC LIMIT %d OFFSET %d`,
		stakingEventRows, m.table, pageable.Limit,
		stakingEventRows, m.table, pageable.Limit,
		stakingEventRows, pageable.Limit, pageable.Offset)
	var resp []*StakingEvent
	err := m.conn.QueryRowsCtx(ctx, &resp, query, address)
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
