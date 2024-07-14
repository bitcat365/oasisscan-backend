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
	rewardFieldNames          = builder.RawFieldNames(&Reward{}, true)
	rewardRows                = strings.Join(rewardFieldNames, ",")
	rewardRowsExpectAutoSet   = strings.Join(stringx.Remove(rewardFieldNames, "id"), ",")
	rewardRowsWithPlaceHolder = builder.PostgreSqlJoin(stringx.Remove(rewardFieldNames, "id"))
)

type (
	rewardModel interface {
		Insert(ctx context.Context, data *Reward) (sql.Result, error)
		FindOne(ctx context.Context, id int64) (*Reward, error)
		FindOneByDelegatorValidatorEpoch(ctx context.Context, delegator string, validator string, epoch int64) (*Reward, error)
		Update(ctx context.Context, data *Reward) error
		Delete(ctx context.Context, id int64) error
	}

	defaultRewardModel struct {
		conn  sqlx.SqlConn
		table string
	}

	Reward struct {
		Id               int64     `db:"id"`
		Delegator        string    `db:"delegator"`
		Validator        string    `db:"validator"`
		Epoch            int64     `db:"epoch"`
		DelegationAmount int64     `db:"delegation_amount"`
		DelegationShares int64     `db:"delegation_shares"`
		Reward           int64     `db:"reward"`
		CreatedAt        time.Time `db:"created_at"`
		UpdatedAt        time.Time `db:"updated_at"`
	}
)

func newRewardModel(conn sqlx.SqlConn) *defaultRewardModel {
	return &defaultRewardModel{
		conn:  conn,
		table: `"public"."reward"`,
	}
}

func (m *defaultRewardModel) withSession(session sqlx.Session) *defaultRewardModel {
	return &defaultRewardModel{
		conn:  sqlx.NewSqlConnFromSession(session),
		table: `"public"."reward"`,
	}
}

func (m *defaultRewardModel) Delete(ctx context.Context, id int64) error {
	query := fmt.Sprintf("delete from %s where id = $1", m.table)
	_, err := m.conn.ExecCtx(ctx, query, id)
	return err
}

func (m *defaultRewardModel) FindOne(ctx context.Context, id int64) (*Reward, error) {
	query := fmt.Sprintf("select %s from %s where id = $1 limit 1", rewardRows, m.table)
	var resp Reward
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

func (m *defaultRewardModel) FindOneByDelegatorValidatorEpoch(ctx context.Context, delegator string, validator string, epoch int64) (*Reward, error) {
	var resp Reward
	query := fmt.Sprintf("select %s from %s where delegator = $1 and validator = $2 and epoch = $3 limit 1", rewardRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, delegator, validator, epoch)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *defaultRewardModel) Insert(ctx context.Context, data *Reward) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7, $8)", m.table, rewardRowsExpectAutoSet)
	ret, err := m.conn.ExecCtx(ctx, query, data.Delegator, data.Validator, data.Epoch, data.DelegationAmount, data.DelegationShares, data.Reward, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *defaultRewardModel) Update(ctx context.Context, newData *Reward) error {
	query := fmt.Sprintf("update %s set %s where id = $1", m.table, rewardRowsWithPlaceHolder)
	_, err := m.conn.ExecCtx(ctx, query, newData.Id, newData.Delegator, newData.Validator, newData.Epoch, newData.DelegationAmount, newData.DelegationShares, newData.Reward, newData.CreatedAt, newData.UpdatedAt)
	return err
}

func (m *defaultRewardModel) tableName() string {
	return m.table
}