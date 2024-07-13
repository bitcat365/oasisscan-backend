package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
)

var _ DelegatorModel = (*customDelegatorModel)(nil)

type (
	// DelegatorModel is an interface to be customized, add more methods here,
	// and implement the added methods in customDelegatorModel.
	DelegatorModel interface {
		delegatorModel
		SessionInsert(ctx context.Context, session sqlx.Session, data *Delegator) (sql.Result, error)
		SessionDeleteAllByValidator(ctx context.Context, session sqlx.Session, validatorAddress string) error
		CountByValidator(ctx context.Context, validatorAddress string) (int64, error)
		FindByValidator(ctx context.Context, validatorAddress string, pageable common.Pageable) ([]*Delegator, error)
	}

	customDelegatorModel struct {
		*defaultDelegatorModel
	}
)

// NewDelegatorModel returns a model for the database table.
func NewDelegatorModel(conn sqlx.SqlConn) DelegatorModel {
	return &customDelegatorModel{
		defaultDelegatorModel: newDelegatorModel(conn),
	}
}

func (m *customDelegatorModel) SessionInsert(ctx context.Context, session sqlx.Session, data *Delegator) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5)", m.table, delegatorRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.Validator, data.Delegator, data.Shares, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *customDelegatorModel) SessionDeleteAllByValidator(ctx context.Context, session sqlx.Session, validatorAddress string) error {
	query := fmt.Sprintf("delete from %s where validator = $1", m.table)
	_, err := session.ExecCtx(ctx, query, validatorAddress)
	return err
}

func (m *customDelegatorModel) CountByValidator(ctx context.Context, validatorAddress string) (int64, error) {
	query := fmt.Sprintf("select count(*) from %s where validator=$1", m.table)
	var resp int64
	err := m.conn.QueryRowCtx(ctx, &resp, query, validatorAddress)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customDelegatorModel) FindByValidator(ctx context.Context, validatorAddress string, pageable common.Pageable) ([]*Delegator, error) {
	query := fmt.Sprintf("select %s from %s where validator=$1 order by shares desc limit %d offset %d", delegatorRows, m.table, pageable.Limit, pageable.Offset)
	var resp []*Delegator
	err := m.conn.QueryRowsCtx(ctx, &resp, query, validatorAddress)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
