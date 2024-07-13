package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ SystemPropertyModel = (*customSystemPropertyModel)(nil)

type (
	// SystemPropertyModel is an interface to be customized, add more methods here,
	// and implement the added methods in customSystemPropertyModel.
	SystemPropertyModel interface {
		systemPropertyModel
		FindByProperty(ctx context.Context, property string) (*SystemProperty, error)
		UpdateByProperty(ctx context.Context, newData *SystemProperty) error
		SessionUpdateByProperty(ctx context.Context, session sqlx.Session, newData *SystemProperty) error
	}

	customSystemPropertyModel struct {
		*defaultSystemPropertyModel
	}
)

// NewSystemPropertyModel returns a model for the database table.
func NewSystemPropertyModel(conn sqlx.SqlConn) SystemPropertyModel {
	return &customSystemPropertyModel{
		defaultSystemPropertyModel: newSystemPropertyModel(conn),
	}
}

func (m *customSystemPropertyModel) FindByProperty(ctx context.Context, property string) (*SystemProperty, error) {
	query := fmt.Sprintf("select %s from %s where property=$1 limit 1", systemPropertyRows, m.table)
	var resp SystemProperty
	err := m.conn.QueryRowCtx(ctx, &resp, query, property)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customSystemPropertyModel) UpdateByProperty(ctx context.Context, newData *SystemProperty) error {
	query := fmt.Sprintf("update %s set value=$1,updated_at=$2 where property = $3", m.table)
	_, err := m.conn.ExecCtx(ctx, query, newData.Value, newData.UpdatedAt, newData.Property)
	return err
}

func (m *customSystemPropertyModel) SessionUpdateByProperty(ctx context.Context, session sqlx.Session, newData *SystemProperty) error {
	query := fmt.Sprintf("update %s set value=$1,updated_at=$2 where property = $3", m.table)
	_, err := session.ExecCtx(ctx, query, newData.Value, newData.UpdatedAt, newData.Property)
	return err
}
