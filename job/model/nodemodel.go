package model

import (
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ NodeModel = (*customNodeModel)(nil)

type (
	// NodeModel is an interface to be customized, add more methods here,
	// and implement the added methods in customNodeModel.
	NodeModel interface {
		nodeModel
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
