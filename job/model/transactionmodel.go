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
		FindTxs(ctx context.Context, height int64, address string, method string, runtime bool, pageable common.Pageable) ([]*TransactionWithType, error)
		CountTxs(ctx context.Context, height int64, address string, method string, runtime bool) (int64, error)
		TransactionCountStats(ctx context.Context, day time.Time, limit int64) ([]*TransactionCountStats, error)
		RefreshDailyCountsStatsView(ctx context.Context) error
		LatestTx(ctx context.Context) (*Transaction, error)
		RefreshTransactionMethodView(ctx context.Context) error
		TransactionMethods(ctx context.Context) ([]string, error)
		FindEscrowEvents(ctx context.Context, address string, pageable common.Pageable) ([]*Transaction, error)
		CountEscrowEvents(ctx context.Context, address string) (int64, error)
	}

	customTransactionModel struct {
		*defaultTransactionModel
	}

	TransactionCountStats struct {
		Day   time.Time `db:"day"`
		Count int64     `db:"count"`
	}

	TransactionWithType struct {
		TxType string `db:"tx_type"`
		Transaction
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

func (m *customTransactionModel) FindTxs(ctx context.Context, height int64, address string, method string, runtime bool, pageable common.Pageable) ([]*TransactionWithType, error) {
	var resp []*TransactionWithType
	query := fmt.Sprintf(`
        SELECT * FROM (
            SELECT 
                'consensus' as tx_type,
                %s
            FROM %s 
            WHERE `, transactionRows, m.table)
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
		conditions = append(conditions, fmt.Sprintf("(sign_addr = $%d or (method='staking.Transfer' and to_addr=$%d))", paramIndex, paramIndex+1))
		args = append(args, address, address)
		paramIndex += 2
	}
	if method != "" {
		conditions = append(conditions, fmt.Sprintf("method = $%d", paramIndex))
		args = append(args, method)
		paramIndex++
	}

	//latest 10000 blocks tx
	if len(conditions) == 1 {
		conditions = append(conditions, fmt.Sprintf("height>((select max(height) from %s)-10000)", m.table))
	}

	query += strings.Join(conditions, " AND ")
	if runtime {
		runtimeQuery := fmt.Sprintf(`
            UNION ALL
            SELECT 
                'runtime' as tx_type,
				id,
                tx_hash,
                method,
                result as status,
                0 as nonce,
                round as height,
                timestamp,
                consensus_from as sign_addr,
                consensus_to as to_addr,
                0 as fee,
                0 as amount,
                0 as shares,
                '{}' as error,
                events,
                raw,
                timestamp as created_at,
                timestamp as updated_at
            FROM runtime_transaction
            WHERE `)

		runtimeConditions := []string{"1=1"}
		if address != "" {
			runtimeConditions = append(runtimeConditions, fmt.Sprintf("(consensus_from = $%d or consensus_to = $%d)", paramIndex, paramIndex+1))
			args = append(args, address, address)
			paramIndex += 2
		}

		runtimeQuery += strings.Join(runtimeConditions, " AND ")
		query = query + " " + runtimeQuery
	}
	query += fmt.Sprintf(`) AS combined_txs ORDER BY height DESC limit %d offset %d`, pageable.Limit, pageable.Offset)

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

func (m *customTransactionModel) CountTxs(ctx context.Context, height int64, address string, method string, runtime bool) (int64, error) {
	var resp int64
	query := fmt.Sprintf(`
        SELECT COUNT(*) FROM (
            SELECT tx_hash FROM %s WHERE `, m.table)
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
		conditions = append(conditions, fmt.Sprintf("(sign_addr = $%d or (method='staking.Transfer' and to_addr=$%d))", paramIndex, paramIndex+1))
		args = append(args, address, address)
		paramIndex += 2
	}
	if method != "" {
		conditions = append(conditions, fmt.Sprintf("method = $%d", paramIndex))
		args = append(args, method)
		paramIndex++
	}

	//latest 10000 blocks tx
	if len(conditions) == 1 {
		conditions = append(conditions, fmt.Sprintf("height>((select max(height) from %s)-10000)", m.table))
	}

	query += strings.Join(conditions, " AND ")

	if runtime {
		//runtime query
		query += ` UNION ALL 
        SELECT tx_hash FROM runtime_transaction WHERE `

		runtimeConditions := []string{"1=1"}
		if address != "" {
			runtimeConditions = append(runtimeConditions, fmt.Sprintf("(consensus_from = $%d or consensus_to = $%d)", paramIndex-2, paramIndex-1))
		}
		if method != "" {
			runtimeConditions = append(runtimeConditions, fmt.Sprintf("method = $%d", paramIndex-1))
		}

		query += strings.Join(runtimeConditions, " AND ")
	}

	query += ") t"

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

func (m *customTransactionModel) LatestTx(ctx context.Context) (*Transaction, error) {
	var resp Transaction
	query := fmt.Sprintf("select %s from %s order by id desc limit 1", transactionRows, m.table)
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

func (m *customTransactionModel) RefreshTransactionMethodView(ctx context.Context) error {
	query := fmt.Sprintf("REFRESH MATERIALIZED VIEW transaction_method")
	_, err := m.conn.ExecCtx(ctx, query)
	return err
}

func (m *customTransactionModel) TransactionMethods(ctx context.Context) ([]string, error) {
	var resp []string
	query := fmt.Sprintf("select method from transaction_method order by method")
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

func (m *customTransactionModel) FindEscrowEvents(ctx context.Context, address string, pageable common.Pageable) ([]*Transaction, error) {
	var resp []*Transaction
	query := fmt.Sprintf("select %s from %s where to_addr = $1 and method in('staking.AddEscrow','staking.ReclaimEscrow') and status=true order by timestamp desc limit %d offset %d", transactionRows, m.table, pageable.Limit, pageable.Offset)
	err := m.conn.QueryRowsCtx(ctx, &resp, query, address)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return nil, ErrNotFound
	default:
		return nil, err
	}
}

func (m *customTransactionModel) CountEscrowEvents(ctx context.Context, address string) (int64, error) {
	var resp int64
	query := fmt.Sprintf("select count(*) from %s where to_addr = $1 and method in('staking.AddEscrow','staking.ReclaimEscrow') and status=true", m.table)
	err := m.conn.QueryRowCtx(ctx, &resp, query, address)
	switch err {
	case nil:
		return resp, nil
	case sqlc.ErrNotFound:
		return 0, ErrNotFound
	default:
		return 0, err
	}
}
