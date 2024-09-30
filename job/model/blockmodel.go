package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"time"
)

var _ BlockModel = (*customBlockModel)(nil)

type (
	// BlockModel is an interface to be customized, add more methods here,
	// and implement the added methods in customBlockModel.
	BlockModel interface {
		blockModel
		FindLatestBlock(ctx context.Context) (*Block, error)
		SessionInsert(ctx context.Context, session sqlx.Session, data *Block) (sql.Result, error)
		CountProposer(ctx context.Context, proposerAddresses []string) (int64, error)
		FindBlocks(ctx context.Context, pageable common.Pageable) ([]*BlockProposer, error)
		CountBlocks(ctx context.Context) (int64, error)
		FindBlockProposer(ctx context.Context, height int64) (*BlockProposer, error)
		FindBlocksByValidator(ctx context.Context, validator string, pageable common.Pageable) ([]*BlockProposer, error)
		CountBlocksByValidator(ctx context.Context, validator string) (int64, error)
		FindOneByHash(ctx context.Context, hash string) (*Block, error)
		FindOneByTime(ctx context.Context, day time.Time) (*Block, error)
	}

	customBlockModel struct {
		*defaultBlockModel
	}

	BlockProposer struct {
		Block
		EntityAddress string `db:"entity_address"`
		Name          string `db:"name"`
	}
)

// NewBlockModel returns a model for the database table.
func NewBlockModel(conn sqlx.SqlConn) BlockModel {
	return &customBlockModel{
		defaultBlockModel: newBlockModel(conn),
	}
}

func (m *customBlockModel) FindLatestBlock(ctx context.Context) (*Block, error) {
	query := fmt.Sprintf("select %s from %s order by height desc limit 1", blockRows, m.table)
	var resp Block
	err := m.conn.QueryRowCtx(ctx, &resp, query)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customBlockModel) CountProposer(ctx context.Context, proposerAddresses []string) (int64, error) {
	query := fmt.Sprintf("select count(*) from %s where proposer_address in (", m.table)
	vars := make([]interface{}, 0)
	for i, proposerAddress := range proposerAddresses {
		query += fmt.Sprintf("$%d,", i+1)
		vars = append(vars, proposerAddress)
	}
	query = query[:len(query)-1] + ")"
	var resp int64
	err := m.conn.QueryRowCtx(ctx, &resp, query, vars...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customBlockModel) SessionInsert(ctx context.Context, session sqlx.Session, data *Block) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7, $8, $9)", m.table, blockRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.Height, data.Epoch, data.Hash, data.Timestamp, data.Txs, data.ProposerAddress, data.Meta, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *customBlockModel) FindBlocks(ctx context.Context, pageable common.Pageable) ([]*BlockProposer, error) {
	var resp []*BlockProposer
	query := fmt.Sprintf("select b.*,v.entity_address,v.name from block b left join node n on b.proposer_address=n.consensus_address left join validator v on n.entity_id=v.entity_id order by b.height desc limit %d offset %d ", pageable.Limit, pageable.Offset)
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

func (m *customBlockModel) CountBlocks(ctx context.Context) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s", m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customBlockModel) FindBlockProposer(ctx context.Context, height int64) (*BlockProposer, error) {
	query := fmt.Sprintf("select b.*,v.entity_address,v.name from block b left join node n on b.proposer_address=n.consensus_address left join validator v on n.entity_id=v.entity_id where b.height=$1 limit 1")
	var resp BlockProposer
	err := m.conn.QueryRowCtx(ctx, &resp, query, height)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customBlockModel) FindBlocksByValidator(ctx context.Context, validator string, pageable common.Pageable) ([]*BlockProposer, error) {
	var resp []*BlockProposer
	query := fmt.Sprintf("select b.*,v.entity_address,v.name from block b left join node n on b.proposer_address=n.consensus_address left join validator v on n.entity_id=v.entity_id where v.entity_address=$1 order by b.height desc limit %d offset %d", pageable.Limit, pageable.Offset)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, validator)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customBlockModel) CountBlocksByValidator(ctx context.Context, validator string) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(b.id) from block b left join node n on b.proposer_address=n.consensus_address left join validator v on n.entity_id=v.entity_id where v.entity_address=$1")
	err := m.conn.QueryRowCtx(ctx, &resp, query, validator)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customBlockModel) FindOneByHash(ctx context.Context, hash string) (*Block, error) {
	var resp Block
	query := fmt.Sprintf("select %s from %s where hash = $1 limit 1", blockRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, hash)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customBlockModel) FindOneByTime(ctx context.Context, day time.Time) (*Block, error) {
	var resp Block
	query := fmt.Sprintf("select %s from %s where timestamp <= $1 order by timestamp desc limit 1", blockRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, day)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}
