package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"time"
)

var _ EscrowStatsModel = (*customEscrowStatsModel)(nil)

type (
	// EscrowStatsModel is an interface to be customized, add more methods here,
	// and implement the added methods in customEscrowStatsModel.
	EscrowStatsModel interface {
		escrowStatsModel
		withSession(session sqlx.Session) EscrowStatsModel
		FindLatestOne(ctx context.Context) (*EscrowStats, error)
		FindLatestRows(ctx context.Context, address string, limit int64) ([]*EscrowStats, error)
		TotalStats(ctx context.Context, day time.Time, limit int64) ([]*EscrowTotalStats, error)
		FindOneByEntityAddressDate(ctx context.Context, entityAddress string, day time.Time) (*EscrowStats, error)
	}

	customEscrowStatsModel struct {
		*defaultEscrowStatsModel
	}

	EscrowTotalStats struct {
		Day   time.Time `db:"day"`
		Value int64     `db:"value"`
	}
)

// NewEscrowStatsModel returns a model for the database table.
func NewEscrowStatsModel(conn sqlx.SqlConn) EscrowStatsModel {
	return &customEscrowStatsModel{
		defaultEscrowStatsModel: newEscrowStatsModel(conn),
	}
}

func (m *customEscrowStatsModel) withSession(session sqlx.Session) EscrowStatsModel {
	return NewEscrowStatsModel(sqlx.NewSqlConnFromSession(session))
}

func (m *customEscrowStatsModel) FindLatestOne(ctx context.Context) (*EscrowStats, error) {
	var resp EscrowStats
	query := fmt.Sprintf("select %s from %s order by date desc limit 1", escrowStatsRows, m.table)
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

func (m *customEscrowStatsModel) FindLatestRows(ctx context.Context, address string, limit int64) ([]*EscrowStats, error) {
	var resp []*EscrowStats
	query := fmt.Sprintf("select %s from %s where entity_address=$1 order by date desc limit %d", escrowStatsRows, m.table, limit)
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

func (m *customEscrowStatsModel) TotalStats(ctx context.Context, day time.Time, limit int64) ([]*EscrowTotalStats, error) {
	var resp []*EscrowTotalStats
	query := fmt.Sprintf("select sum(escrow) as value, date as day from %s where date <= $1 group by date order by date desc limit %d", m.table, limit)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, day)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customEscrowStatsModel) FindOneByEntityAddressDate(ctx context.Context, entityAddress string, day time.Time) (*EscrowStats, error) {
	var resp EscrowStats
	query := fmt.Sprintf("select %s from %s where entity_address = $1 and date = $2 limit 1", escrowStatsRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, entityAddress, day)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
