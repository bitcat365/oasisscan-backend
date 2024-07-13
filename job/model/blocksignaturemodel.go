package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
)

var _ BlockSignatureModel = (*customBlockSignatureModel)(nil)

type (
	// BlockSignatureModel is an interface to be customized, add more methods here,
	// and implement the added methods in customBlockSignatureModel.
	BlockSignatureModel interface {
		blockSignatureModel
		SessionInsert(ctx context.Context, session sqlx.Session, data *BlockSignature) (sql.Result, error)
		CountSigns(ctx context.Context, signAddresses []string, from int64) (int64, error)
		FindBlocks(ctx context.Context, pageable common.Pageable) ([]*BlockSignature, error)
	}

	customBlockSignatureModel struct {
		*defaultBlockSignatureModel
	}
)

// NewBlockSignatureModel returns a model for the database table.
func NewBlockSignatureModel(conn sqlx.SqlConn) BlockSignatureModel {
	return &customBlockSignatureModel{
		defaultBlockSignatureModel: newBlockSignatureModel(conn),
	}
}

func (m *customBlockSignatureModel) SessionInsert(ctx context.Context, session sqlx.Session, data *BlockSignature) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7)", m.table, blockSignatureRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.Height, data.BlockIdFlag, data.ValidatorAddress, data.Timestamp, data.Signature, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *customBlockSignatureModel) CountSigns(ctx context.Context, signAddresses []string, from int64) (int64, error) {
	query := fmt.Sprintf("select count(*) from %s where validator_address in (", m.table)
	vars := make([]interface{}, 0)
	for i, signAddress := range signAddresses {
		query += fmt.Sprintf("$%d,", i+1)
		vars = append(vars, signAddress)
	}
	query = query[:len(query)-1] + ")"

	if from > 0 {
		query += fmt.Sprintf(" and height > $%d and height <= $%d", len(signAddresses)+1, len(signAddresses)+2)
		vars = append(vars, from, from+common.UptimeHeight)
	}
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

func (m *customBlockSignatureModel) FindBlocks(ctx context.Context, pageable common.Pageable) ([]*BlockSignature, error) {
	var resp []*BlockSignature
	query := fmt.Sprintf("select * from %s order by height desc limit %d offset %d ", m.table, pageable.Limit, pageable.Offset)
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
