package model

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/zeromicro/go-zero/core/stores/sqlc"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"oasisscan-backend/common"
	"strings"
	"time"
)

var _ TransactionModel = (*customTransactionModel)(nil)

type (
	// TransactionModel is an interface to be customized, add more methods here,
	// and implement the added methods in customTransactionModel.
	TransactionModel interface {
		transactionModel
		SessionInsert(ctx context.Context, session sqlx.Session, data *Transaction) (sql.Result, error)
		FindOneByTxHash(ctx context.Context, txHash string) (*Transaction, error)
		FindTxs(ctx context.Context, height int64, address string, method string, pageable common.Pageable) ([]*Transaction, error)
		CountTxs(ctx context.Context, height int64, address string, method string) (int64, error)
		TransactionCountStats(ctx context.Context, day time.Time, limit int64) ([]*TransactionCountStats, error)
		RefreshDailyCountsStatsView(ctx context.Context) error
	}

	customTransactionModel struct {
		*defaultTransactionModel
	}

	TransactionCountStats struct {
		Day   time.Time `db:"day"`
		Count int64     `db:"count"`
	}
)

// NewTransactionModel returns a model for the database table.
func NewTransactionModel(conn sqlx.SqlConn) TransactionModel {
	return &customTransactionModel{
		defaultTransactionModel: newTransactionModel(conn),
	}
}

func (m *customTransactionModel) SessionInsert(ctx context.Context, session sqlx.Session, data *Transaction) (sql.Result, error) {
	query := fmt.Sprintf("insert into %s (%s) values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)", m.table, transactionRowsExpectAutoSet)
	ret, err := session.ExecCtx(ctx, query, data.TxHash, data.Method, data.Status, data.Nonce, data.Height, data.Timestamp, data.SignAddr, data.ToAddr, data.Fee, data.Amount, data.Shares, data.Error, data.Events, data.Raw, data.CreatedAt, data.UpdatedAt)
	return ret, err
}

func (m *customTransactionModel) FindOneByTxHash(ctx context.Context, txHash string) (*Transaction, error) {
	var resp Transaction
	query := fmt.Sprintf("select %s from %s where tx_hash = $1 order by status desc limit 1", transactionRows, m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, txHash)
	switch err {
	case nil:
		return &resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customTransactionModel) FindTxs(ctx context.Context, height int64, address string, method string, pageable common.Pageable) ([]*Transaction, error) {
	var resp []*Transaction
	query := fmt.Sprintf("select %s from %s where ", transactionRows, m.table)
	var conditions []string
	conditions = append(conditions, "1=1")
	var args []interface{}
	paramIndex := 1
	if height > 0 {
		conditions = append(conditions, fmt.Sprintf("height = $%d", paramIndex))
		args = append(args, height)
		paramIndex++
	}
	if address != "" {
		conditions = append(conditions, fmt.Sprintf("(sign_addr = $%d or to_addr=$%d)", paramIndex, paramIndex+1))
		args = append(args, address, address)
		paramIndex += 2
	}
	if method != "" {
		conditions = append(conditions, fmt.Sprintf("method = $%d", paramIndex))
		args = append(args, method)
		paramIndex++
	}

	if len(conditions) > 0 {
		query += strings.Join(conditions, " AND ")
	}
	query += fmt.Sprintf(" order by timestamp desc limit %d offset %d", pageable.Limit, pageable.Offset)

	err := m.conn.QueryRowsCtx(ctx, &resp, query, args...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customTransactionModel) CountTxs(ctx context.Context, height int64, address string, method string) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s where ", m.table)
	var conditions []string
	conditions = append(conditions, "1=1")
	var args []interface{}
	paramIndex := 1
	if height > 0 {
		conditions = append(conditions, fmt.Sprintf("height = $%d", paramIndex))
		args = append(args, height)
		paramIndex++
	}
	if address != "" {
		conditions = append(conditions, fmt.Sprintf("(sign_addr = $%d or to_addr=$%d)", paramIndex, paramIndex+1))
		args = append(args, address, address)
		paramIndex += 2
	}
	if method != "" {
		conditions = append(conditions, fmt.Sprintf("method = $%d", paramIndex))
		args = append(args, method)
		paramIndex++
	}

	if len(conditions) > 0 {
		query += strings.Join(conditions, " AND ")
	}

	err := m.conn.QueryRowCtx(ctx, &resp, query, args...)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}

func (m *customTransactionModel) TransactionCountStats(ctx context.Context, day time.Time, limit int64) ([]*TransactionCountStats, error) {
	var resp []*TransactionCountStats
	query := fmt.Sprintf("select day,count from daily_transaction_counts where day < $1 order by day desc limit %d", limit)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, day)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customTransactionModel) RefreshDailyCountsStatsView(ctx context.Context) error {
	query := fmt.Sprintf("REFRESH MATERIALIZED VIEW daily_transaction_counts")
	_, err := m.conn.ExecCtx(ctx, query)
	return err
}
