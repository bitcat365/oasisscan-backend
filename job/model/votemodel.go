package model

import "github.com/zeromicro/go-zero/core/stores/sqlx"

var _ VoteModel = (*customVoteModel)(nil)

type (
	// VoteModel is an interface to be customized, add more methods here,
	// and implement the added methods in customVoteModel.
	VoteModel interface {
		voteModel
		withSession(session sqlx.Session) VoteModel
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
