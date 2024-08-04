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
	voteFieldNames          = builder.RawFieldNames(&Vote{}, true)
	voteRows                = strings.Join(voteFieldNames, ",")
	voteRowsExpectAutoSet   = strings.Join(stringx.Remove(voteFieldNames, "id", "create_at", "create_time", "created_at", "update_at", "update_time", "updated_at"), ",")
	voteRowsWithPlaceHolder = builder.PostgreSqlJoin(stringx.Remove(voteFieldNames, "id", "create_at", "create_time", "created_at", "update_at", "update_time", "updated_at"))
)

type (
	voteModel interface {
		Insert(ctx context.Context, data *Vote) (sql.Result, error)
		FindOne(ctx context.Context, id int64) (*Vote, error)
		FindOneByProposalIdVoteAddress(ctx context.Context, proposalId int64, voteAddress string) (*Vote, error)
		Update(ctx context.Context, data *Vote) error
		Delete(ctx context.Context, id int64) error
	}

	defaultVoteModel struct {
		conn  sqlx.SqlConn
		table string
	}

	Vote struct {
		Id          int64     `db:"id"`
		ProposalId  int64     `db:"proposal_id"`
		VoteAddress string    `db:"vote_address"`
		Option      string    `db:"option"`
		Amount      int64     `db:"amount"`
		Percent     float64   `db:"percent"`
		CreatedAt   time.Time `db:"created_at"`
		UpdatedAt   time.Time `db:"updated_at"`
	}
)

func newVoteModel(conn sqlx.SqlConn) *defaultVoteModel {
	return &defaultVoteModel{
		conn:  conn,
		table: `"public"."vote"`,
	}
}

func (m *defaultVoteModel) Delete(ctx context.Context, id int64) error {
	query := fmt.Sprintf("delete from %s where id = $1", m.table)
	_, err := m.conn.ExecCtx(ctx, query, id)
	return err
}

func (m *defaultVoteModel) FindOne(ctx context.Context, id int64) (*Vote, error) {
	query := fmt.Sprintf("select %s from %s where id = $1 limit 1", voteRows, m.table)
	var resp Vote
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

func (m *defaultVoteModel) FindOneByProposalIdVoteAddress(ctx context.Context, proposalId int64, voteAddress string) (*Vote, error) {
	var resp Vote
	query := fmt.Sprintf("select %s from %s where proposal_id = $1 and vote_address = $2 limit 1", voteRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, proposalId, voteAddress)
	switch err {
	case nil:
		return &resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *defaultVoteModel) Insert(ctx context.Context, data *Vote) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5)", m.table, voteRowsExpectAutoSet)
	ret, err := m.conn.ExecCtx(ctx, query, data.ProposalId, data.VoteAddress, data.Option, data.Amount, data.Percent)
	return ret, err
}

func (m *defaultVoteModel) Update(ctx context.Context, newData *Vote) error {
	query := fmt.Sprintf("update %s set %s where id = $1", m.table, voteRowsWithPlaceHolder)
	_, err := m.conn.ExecCtx(ctx, query, newData.Id, newData.ProposalId, newData.VoteAddress, newData.Option, newData.Amount, newData.Percent)
	return err
}

func (m *defaultVoteModel) tableName() string {
	return m.table
}