package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ ValidatorModel = (*customValidatorModel)(nil)

type (
	// ValidatorModel is an interface to be customized, add more methods here,
	// and implement the added methods in customValidatorModel.
	ValidatorModel interface {
		validatorModel
		FindListByConsensusAddress(ctx context.Context, consensusAddress string) ([]*Validator, error)
		FindAll(ctx context.Context, orderBy, sort string) ([]*Validator, error)
		FindOneByEntityAddress(ctx context.Context, address string) (*Validator, error)
		FindOneByNodeAddress(ctx context.Context, nodeAddress string) (*Validator, error)
		SumEscrow(ctx context.Context) (int64, error)
	}

	customValidatorModel struct {
		*defaultValidatorModel
	}
)

// NewValidatorModel returns a model for the database table.
func NewValidatorModel(conn sqlx.SqlConn) ValidatorModel {
	return &customValidatorModel{
		defaultValidatorModel: newValidatorModel(conn),
	}
}

func (m *customValidatorModel) FindListByConsensusAddress(ctx context.Context, consensusAddress string) ([]*Validator, error) {
	query := fmt.Sprintf("select %s from %s where consensus_address=$1", validatorRows, m.table)
	var resp []*Validator
	err := m.conn.QueryRowsCtx(ctx, &resp, query, consensusAddress)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customValidatorModel) FindAll(ctx context.Context, orderBy, sort string) ([]*Validator, error) {
	query := fmt.Sprintf("select %s from %s order by %s %s", validatorRows, m.table, orderBy, sort)
	var resp []*Validator
	err := m.conn.QueryRowsCtx(ctx, &resp, query)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customValidatorModel) FindOneByEntityAddress(ctx context.Context, address string) (*Validator, error) {
	var resp Validator
	query := fmt.Sprintf("select %s from %s where entity_address = $1 limit 1", validatorRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, address)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customValidatorModel) FindOneByNodeAddress(ctx context.Context, nodeAddress string) (*Validator, error) {
	var resp Validator
	query := fmt.Sprintf("select %s from %s where node_address = $1 limit 1", validatorRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, nodeAddress)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customValidatorModel) SumEscrow(ctx context.Context) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select sum(escrow) from %s", m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query)
	switch err {
	case nil:
		return resp, nil
	default:
		return 0, err
	}
}
