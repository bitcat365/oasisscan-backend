// Code generated by goctl. DO NOT EDIT.

package model

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	"github.com/zeromicro/go-zero/core/stores/builder"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"github.com/zeromicro/go-zero/core/stringx"
)

var (
	proposalFieldNames          = builder.RawFieldNames(&Proposal{}, true)
	proposalRows                = strings.Join(proposalFieldNames, ",")
	proposalRowsExpectAutoSet   = strings.Join(stringx.Remove(proposalFieldNames, "id", "create_at", "create_time", "created_at", "update_at", "update_time", "updated_at"), ",")
	proposalRowsWithPlaceHolder = builder.PostgreSqlJoin(stringx.Remove(proposalFieldNames, "id", "create_at", "create_time", "created_at", "update_at", "update_time", "updated_at"))
)

type (
	proposalModel interface {
		Insert(ctx context.Context, data *Proposal) (sql.Result, error)
		FindOne(ctx context.Context, id int64) (*Proposal, error)
		FindOneByProposalId(ctx context.Context, proposalId int64) (*Proposal, error)
		Update(ctx context.Context, data *Proposal) error
		Delete(ctx context.Context, id int64) error
	}

	defaultProposalModel struct {
		conn  sqlx.SqlConn
		table string
	}

	Proposal struct {
		Id           int64     `db:"id"`
		ProposalId   int64     `db:"proposal_id"`
		Title        string    `db:"title"`
		Type         string    `db:"type"`
		Submitter    string    `db:"submitter"`
		State        string    `db:"state"`
		CreatedEpoch int64     `db:"created_epoch"`
		ClosedEpoch  int64     `db:"closed_epoch"`
		Raw          string    `db:"raw"`
		CreatedAt    time.Time `db:"created_at"`
		UpdatedAt    time.Time `db:"updated_at"`
	}
)

func newProposalModel(conn sqlx.SqlConn) *defaultProposalModel {
	return &defaultProposalModel{
		conn:  conn,
		table: `"public"."proposal"`,
	}
}

func (m *defaultProposalModel) Delete(ctx context.Context, id int64) error {
	query := fmt.Sprintf("delete from %s where id = $1", m.table)
	_, err := m.conn.ExecCtx(ctx, query, id)
	return err
}

func (m *defaultProposalModel) FindOne(ctx context.Context, id int64) (*Proposal, error) {
	query := fmt.Sprintf("select %s from %s where id = $1 limit 1", proposalRows, m.table)
	var resp Proposal
	err := m.conn.QueryRowCtx(ctx, &resp, query, id)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *defaultProposalModel) FindOneByProposalId(ctx context.Context, proposalId int64) (*Proposal, error) {
	var resp Proposal
	query := fmt.Sprintf("select %s from %s where proposal_id = $1 limit 1", proposalRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, proposalId)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *defaultProposalModel) Insert(ctx context.Context, data *Proposal) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7, $8)", m.table, proposalRowsExpectAutoSet)
	ret, err := m.conn.ExecCtx(ctx, query, data.ProposalId, data.Title, data.Type, data.Submitter, data.State, data.CreatedEpoch, data.ClosedEpoch, data.Raw)
	return ret, err
}

func (m *defaultProposalModel) Update(ctx context.Context, newData *Proposal) error {
	query := fmt.Sprintf("update %s set %s where id = $1", m.table, proposalRowsWithPlaceHolder)
	_, err := m.conn.ExecCtx(ctx, query, newData.Id, newData.ProposalId, newData.Title, newData.Type, newData.Submitter, newData.State, newData.CreatedEpoch, newData.ClosedEpoch, newData.Raw)
	return err
}

func (m *defaultProposalModel) tableName() string {
	return m.table
}