package model

import (
	"context"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"time"
)

var _ RewardModel = (*customRewardModel)(nil)

type (
	// RewardModel is an interface to be customized, add more methods here,
	// and implement the added methods in customRewardModel.
	RewardModel interface {
		rewardModel
		FindByDelegator(ctx context.Context, delegator string, pageable common.Pageable) ([]*Reward, error)
		CountByDelegator(ctx context.Context, delegator string) (int64, error)
		FindByDelegatorGroupByDay(ctx context.Context, delegator string) ([]*RewardDay, error)
		FindDays(ctx context.Context) ([]*StatsDay, error)
		DeleteBeforeEpoch(ctx context.Context, epoch int64) error
		RefreshRewardDaysView(ctx context.Context) error
	}

	customRewardModel struct {
		*defaultRewardModel
	}

	RewardDay struct {
		Delegator        string    `db:"delegator"`
		ValidatorAddress string    `db:"validator"`
		ValidatorName    string    `db:"name"`
		Day              time.Time `db:"day"`
		Reward           int64     `db:"reward"`
	}

	StatsDay struct {
		Day time.Time `db:"day"`
	}
)

// NewRewardModel returns a model for the database table.
func NewRewardModel(conn sqlx.SqlConn) RewardModel {
	return &customRewardModel{
		defaultRewardModel: newRewardModel(conn),
	}
}

func (m *customRewardModel) FindByDelegator(ctx context.Context, delegator string, pageable common.Pageable) ([]*Reward, error) {
	var resp []*Reward
	query := fmt.Sprintf("select %s from %s where delegator = $1 and reward>0 order by epoch desc,reward desc limit %d offset %d", rewardRows, m.table, pageable.Limit, pageable.Offset)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, delegator)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customRewardModel) CountByDelegator(ctx context.Context, delegator string) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s where delegator = $1 and reward>0", m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, delegator)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customRewardModel) FindByDelegatorGroupByDay(ctx context.Context, delegator string) ([]*RewardDay, error) {
	var resp []*RewardDay
	query := fmt.Sprintf("select r.delegator,r.validator,v.name,DATE_TRUNC('day', r.created_at) AS day, sum(r.reward) as reward from reward r left join validator v on r.validator=v.entity_address where r.delegator=$1 group by r.delegator,r.validator,v.name,day order by day asc,reward desc")
	err := m.conn.QueryRowsCtx(ctx, &resp, query, delegator)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customRewardModel) FindDays(ctx context.Context) ([]*StatsDay, error) {
	var resp []*StatsDay
	query := fmt.Sprintf("select day from reward_days order by day desc limit 10")
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

func (m *customRewardModel) DeleteBeforeEpoch(ctx context.Context, epoch int64) error {
	query := fmt.Sprintf("delete from %s where epoch < $1", m.table)
	_, err := m.conn.ExecCtx(ctx, query, epoch)
	return err
}

func (m *customRewardModel) RefreshRewardDaysView(ctx context.Context) error {
	query := fmt.Sprintf("REFRESH MATERIALIZED VIEW reward_days")
	_, err := m.conn.ExecCtx(ctx, query)
	return err
}
