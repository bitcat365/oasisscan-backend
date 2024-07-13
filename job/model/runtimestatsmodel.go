package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ RuntimeStatsModel = (*customRuntimeStatsModel)(nil)

type (
	// RuntimeStatsModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRuntimeStatsModel.
	RuntimeStatsModel interface {
		runtimeStatsModel
		withSession(session sqlx.Session) RuntimeStatsModel
		SessionUpdateStatsCount(ctx context.Context, session sqlx.Session, newData *RuntimeStats) error
		SessionInsert(ctx context.Context, session sqlx.Session, data *RuntimeStats) (sql.Result, error)
		SessionFindOneByRuntimeIdEntityId(ctx context.Context, session sqlx.Session, runtimeId string, entityId string) (*RuntimeStats, error)
	}

	customRuntimeStatsModel struct {
		*defaultRuntimeStatsModel
	}
)

// NewRuntimeStatsModel returns a model for the database table.
func NewRuntimeStatsModel(conn sqlx.SqlConn) RuntimeStatsModel {
	return &customRuntimeStatsModel{
		defaultRuntimeStatsModel: newRuntimeStatsModel(conn),
	}
}

func (m *customRuntimeStatsModel) withSession(session sqlx.Session) RuntimeStatsModel {
	return NewRuntimeStatsModel(sqlx.NewSqlConnFromSession(session))
}

func (m *customRuntimeStatsModel) SessionUpdateStatsCount(ctx context.Context, session sqlx.Session, newData *RuntimeStats) error {
	query := fmt.Sprintf("update %s set rounds_elected=rounds_elected+$1, rounds_primary=rounds_primary+$2, rounds_backup=rounds_backup+$3, rounds_proposer=rounds_proposer+$4 where runtime_id = $5 and entity_id = $6", m.table)
	_, err := session.ExecCtx(ctx, query, newData.RoundsElected, newData.RoundsPrimary, newData.RoundsBackup, newData.RoundsProposer, newData.RuntimeId, newData.EntityId)
	return err
}

func (m *customRuntimeStatsModel) SessionInsert(ctx context.Context, session sqlx.Session, data *RuntimeStats) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6)", m.table, runtimeStatsRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.RuntimeId, data.EntityId, data.RoundsElected, data.RoundsPrimary, data.RoundsBackup, data.RoundsProposer)
	return ret, err
}

func (m *customRuntimeStatsModel) SessionFindOneByRuntimeIdEntityId(ctx context.Context, session sqlx.Session, runtimeId string, entityId string) (*RuntimeStats, error) {
	var resp RuntimeStats
	query := fmt.Sprintf("select %s from %s where runtime_id = $1 and entity_id = $2 limit 1", runtimeStatsRows, m.table)
	err := session.QueryRowCtx(ctx, &resp, query, runtimeId, entityId)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
