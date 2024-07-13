package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ RuntimeNodeModel = (*customRuntimeNodeModel)(nil)

type (
	// RuntimeNodeModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRuntimeNodeModel.
	RuntimeNodeModel interface {
		runtimeNodeModel
		withSession(session sqlx.Session) RuntimeNodeModel
		FindOneByRuntimeIdNodeId(ctx context.Context, runtimeId string, nodeId string) (*RuntimeNode, error)
		SessionFindOneByRuntimeIdNodeId(ctx context.Context, session sqlx.Session, runtimeId string, nodeId string) (*RuntimeNode, error)
		SessionInsert(ctx context.Context, session sqlx.Session, data *RuntimeNode) (sql.Result, error)
		FindEntities(ctx context.Context, runtimeId string) ([]string, error)
	}

	customRuntimeNodeModel struct {
		*defaultRuntimeNodeModel
	}
)

// NewRuntimeNodeModel returns a model for the database table.
func NewRuntimeNodeModel(conn sqlx.SqlConn) RuntimeNodeModel {
	return &customRuntimeNodeModel{
		defaultRuntimeNodeModel: newRuntimeNodeModel(conn),
	}
}

func (m *customRuntimeNodeModel) withSession(session sqlx.Session) RuntimeNodeModel {
	return NewRuntimeNodeModel(sqlx.NewSqlConnFromSession(session))
}

func (m *customRuntimeNodeModel) FindOneByRuntimeIdNodeId(ctx context.Context, runtimeId string, nodeId string) (*RuntimeNode, error) {
	var resp RuntimeNode
	query := fmt.Sprintf("select %s from %s where runtime_id = $1 and node_id = $2 limit 1", runtimeNodeRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, runtimeId, nodeId)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customRuntimeNodeModel) SessionFindOneByRuntimeIdNodeId(ctx context.Context, session sqlx.Session, runtimeId string, nodeId string) (*RuntimeNode, error) {
	var resp RuntimeNode
	query := fmt.Sprintf("select %s from %s where runtime_id = $1 and node_id = $2 limit 1", runtimeNodeRows, m.table)
	err := session.QueryRowCtx(ctx, &resp, query, runtimeId, nodeId)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customRuntimeNodeModel) SessionInsert(ctx context.Context, session sqlx.Session, data *RuntimeNode) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3)", m.table, runtimeNodeRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.RuntimeId, data.NodeId, data.EntityId)
	return ret, err
}

func (m *customRuntimeNodeModel) FindEntities(ctx context.Context, runtimeId string) ([]string, error) {
	var resp []string
	query := fmt.Sprintf("select distinct entity_id from %s where runtime_id = $1", m.table)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, runtimeId)
	switch err {
	case nil:
		return resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
