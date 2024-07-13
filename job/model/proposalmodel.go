package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
)

var _ ProposalModel = (*customProposalModel)(nil)

type (
	// ProposalModel is an interface to be customized, add more methods here,
	// and implement the added methods in customProposalModel.
	ProposalModel interface {
		proposalModel
		withSession(session sqlx.Session) ProposalModel
		FindAllProposals(ctx context.Context) ([]*Proposal, error)
	}

	customProposalModel struct {
		*defaultProposalModel
	}
)

// NewProposalModel returns a model for the database table.
func NewProposalModel(conn sqlx.SqlConn) ProposalModel {
	return &customProposalModel{
		defaultProposalModel: newProposalModel(conn),
	}
}

func (m *customProposalModel) withSession(session sqlx.Session) ProposalModel {
	return NewProposalModel(sqlx.NewSqlConnFromSession(session))
}

func (m *customProposalModel) FindAllProposals(ctx context.Context) ([]*Proposal, error) {
	var resp []*Proposal
	query := fmt.Sprintf("select %s from %s order by proposal_id desc", proposalRows, m.table)
	err := m.conn.QueryRowsCtx(ctx, &resp, query)
	switch err {
	case nil:
		return resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
