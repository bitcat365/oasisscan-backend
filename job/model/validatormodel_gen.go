// Code generated by goctl. DO NOT EDIT.

package model

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	"github.com/zeromicro/go-zero/core/stores/builder"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"github.com/zeromicro/go-zero/core/stringx"
)

var (
	validatorFieldNames          = builder.RawFieldNames(&Validator{}, true)
	validatorRows                = strings.Join(validatorFieldNames, ",")
	validatorRowsExpectAutoSet   = strings.Join(stringx.Remove(validatorFieldNames, "id"), ",")
	validatorRowsWithPlaceHolder = builder.PostgreSqlJoin(stringx.Remove(validatorFieldNames, "id"))
)

type (
	validatorModel interface {
		Insert(ctx context.Context, data *Validator) (sql.Result, error)
		FindOne(ctx context.Context, id int64) (*Validator, error)
		FindOneByEntityId(ctx context.Context, entityId string) (*Validator, error)
		Update(ctx context.Context, data *Validator) error
		Delete(ctx context.Context, id int64) error
	}

	defaultValidatorModel struct {
		conn  sqlx.SqlConn
		table string
	}

	Validator struct {
		Id               int64     `db:"id"`
		EntityId         string    `db:"entity_id"`
		EntityAddress    string    `db:"entity_address"`
		NodeId           string    `db:"node_id"`
		NodeAddress      string    `db:"node_address"`
		ConsensusAddress string    `db:"consensus_address"`
		Name             string    `db:"name"`
		Icon             string    `db:"icon"`
		Website          string    `db:"website"`
		Twitter          string    `db:"twitter"`
		Keybase          string    `db:"keybase"`
		Email            string    `db:"email"`
		Description      string    `db:"description"`
		Escrow           int64     `db:"escrow"`
		Escrow24H        int64     `db:"escrow_24h"`
		Balance          int64     `db:"balance"`
		TotalShares      int64     `db:"total_shares"`
		Signs            int64     `db:"signs"`
		Proposals        int64     `db:"proposals"`
		Score            int64     `db:"score"`
		SignsUptime      int64     `db:"signs_uptime"`
		Nodes            int64     `db:"nodes"`
		Status           bool      `db:"status"`
		Commission       int64     `db:"commission"`
		Delegators       int64     `db:"delegators"`
		Rank             int64     `db:"rank"`
		CreatedAt        time.Time `db:"created_at"`
		UpdatedAt        time.Time `db:"updated_at"`
	}
)

func newValidatorModel(conn sqlx.SqlConn) *defaultValidatorModel {
	return &defaultValidatorModel{
		conn:  conn,
		table: `"public"."validator"`,
	}
}

func (m *defaultValidatorModel) withSession(session sqlx.Session) *defaultValidatorModel {
	return &defaultValidatorModel{
		conn:  sqlx.NewSqlConnFromSession(session),
		table: `"public"."validator"`,
	}
}

func (m *defaultValidatorModel) Delete(ctx context.Context, id int64) error {
	query := fmt.Sprintf("delete from %s where id = $1", m.table)
	_, err := m.conn.ExecCtx(ctx, query, id)
	return err
}

func (m *defaultValidatorModel) FindOne(ctx context.Context, id int64) (*Validator, error) {
	query := fmt.Sprintf("select %s from %s where id = $1 limit 1", validatorRows, m.table)
	var resp Validator
	err := m.conn.QueryRowCtx(ctx, &resp, query, id)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *defaultValidatorModel) FindOneByEntityId(ctx context.Context, entityId string) (*Validator, error) {
	var resp Validator
	query := fmt.Sprintf("select %s from %s where entity_id = $1 limit 1", validatorRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, entityId)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *defaultValidatorModel) Insert(ctx context.Context, data *Validator) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23, $24, $25, $26, $27)", m.table, validatorRowsExpectAutoSet)
	ret, err := m.conn.ExecCtx(ctx, query, data.EntityId, data.EntityAddress, data.NodeId, data.NodeAddress, data.ConsensusAddress, data.Name, data.Icon, data.Website, data.Twitter, data.Keybase, data.Email, data.Description, data.Escrow, data.Escrow24H, data.Balance, data.TotalShares, data.Signs, data.Proposals, data.Score, data.SignsUptime, data.Nodes, data.Status, data.Commission, data.Delegators, data.Rank, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *defaultValidatorModel) Update(ctx context.Context, newData *Validator) error {
	query := fmt.Sprintf("update %s set %s where id = $1", m.table, validatorRowsWithPlaceHolder)
	_, err := m.conn.ExecCtx(ctx, query, newData.Id, newData.EntityId, newData.EntityAddress, newData.NodeId, newData.NodeAddress, newData.ConsensusAddress, newData.Name, newData.Icon, newData.Website, newData.Twitter, newData.Keybase, newData.Email, newData.Description, newData.Escrow, newData.Escrow24H, newData.Balance, newData.TotalShares, newData.Signs, newData.Proposals, newData.Score, newData.SignsUptime, newData.Nodes, newData.Status, newData.Commission, newData.Delegators, newData.Rank, newData.CreatedAt, newData.UpdatedAt)
	return err
}

func (m *defaultValidatorModel) tableName() string {
	return m.table
}
