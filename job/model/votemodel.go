package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"strings"
)

var _ VoteModel = (*customVoteModel)(nil)

type (
	// VoteModel is an interface to be customized, add more methods here,
	// and implement the added methods in customVoteModel.
	VoteModel interface {
		voteModel
		withSession(session sqlx.Session) VoteModel
		FindByValidator(ctx context.Context, validator string, proposalId int64, pageable common.Pageable) ([]*Vote, error)
		CountByValidator(ctx context.Context, validator string, proposalId int64) (int64, error)
	}

	customVoteModel struct {
		*defaultVoteModel
	}
)

// NewVoteModel returns a model for the database table.
func NewVoteModel(conn sqlx.SqlConn) VoteModel {
	return &customVoteModel{
		defaultVoteModel: newVoteModel(conn),
	}
}

func (m *customVoteModel) withSession(session sqlx.Session) VoteModel {
	return NewVoteModel(sqlx.NewSqlConnFromSession(session))
}

func (m *customVoteModel) FindByValidator(ctx context.Context, validator string, proposalId int64, pageable common.Pageable) ([]*Vote, error) {
	var resp []*Vote
	query := fmt.Sprintf("select %s from %s where ", voteRows, m.table)
	var conditions []string
	conditions = append(conditions, "1=1")
	var args []interface{}
	paramIndex := 1
	if validator != "" {
		conditions = append(conditions, fmt.Sprintf("vote_address = $%d", paramIndex))
		args = append(args, validator)
		paramIndex++
	}
	if proposalId != 0 {
		conditions = append(conditions, fmt.Sprintf("proposal_id = $%d", paramIndex))
		args = append(args, proposalId)
		paramIndex++
	}

	if len(conditions) > 0 {
		query += strings.Join(conditions, " AND ")
	}
	query += fmt.Sprintf(" order by proposal_id desc,percent desc limit %d offset %d", pageable.Limit, pageable.Offset)

	err := m.conn.QueryRowsCtx(ctx, &resp, query, args...)
	switch err {
	case nil:
		return resp, nil
	case sqlx.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customVoteModel) CountByValidator(ctx context.Context, validator string, proposalId int64) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s where ", m.table)
	var conditions []string
	conditions = append(conditions, "1=1")
	var args []interface{}
	paramIndex := 1
	if validator != "" {
		conditions = append(conditions, fmt.Sprintf("vote_address = $%d", paramIndex))
		args = append(args, validator)
		paramIndex++
	}
	if proposalId != 0 {
		conditions = append(conditions, fmt.Sprintf("proposal_id = $%d", paramIndex))
		args = append(args, proposalId)
		paramIndex++
	}

	if len(conditions) > 0 {
		query += strings.Join(conditions, " AND ")
	}

	err := m.conn.QueryRowCtx(ctx, &resp, query, args...)
	switch err {
	case nil:
		return resp, nil
	default:
		return 0, err
	}
}
