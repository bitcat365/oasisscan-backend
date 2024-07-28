package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ NodeModel = (*customNodeModel)(nil)

type (
	// NodeModel is an interface to be customized, add more methods here,
	// and implement the added methods in customNodeModel.
	NodeModel interface {
		nodeModel
		FindByEntityId(ctx context.Context, entityId string) ([]*Node, error)
	}

	customNodeModel struct {
		*defaultNodeModel
	}
)

// NewNodeModel returns a model for the database table.
func NewNodeModel(conn sqlx.SqlConn) NodeModel {
	return &customNodeModel{
		defaultNodeModel: newNodeModel(conn),
	}
}

func (m *customNodeModel) FindByEntityId(ctx context.Context, entityId string) ([]*Node, error) {
	var resp []*Node
	query := fmt.Sprintf("select %s from %s where entity_id = $1", nodeRows, m.table)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, entityId)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
