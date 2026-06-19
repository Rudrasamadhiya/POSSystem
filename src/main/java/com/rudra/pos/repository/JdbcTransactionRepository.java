package com.rudra.pos.repository;

import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.TransactionLine;
import com.rudra.pos.persistence.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL-backed {@link TransactionRepository}. A sale's header and its lines
 * are written inside a single JDBC transaction (commit/rollback) so a sale is
 * persisted atomically.
 */
public class JdbcTransactionRepository implements TransactionRepository {

    private final ConnectionProvider cp;

    public JdbcTransactionRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public Transaction save(Transaction t) {
        try (Connection c = cp.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (t.getId() == null) {
                    insertHeader(c, t);
                } else {
                    updateHeader(c, t);
                    deleteLines(c, t.getId());
                }
                insertLines(c, t);
                c.commit();
                return t;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("save(transaction) failed: " + e.getMessage(), e);
        }
    }

    private void insertHeader(Connection c, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions(store_id,user_id,total,payment_method,customer_name,created_at) "
                + "VALUES(?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindHeader(ps, t);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    t.setId(rs.getLong(1));
                }
            }
        }
    }

    private void updateHeader(Connection c, Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET store_id=?,user_id=?,total=?,payment_method=?,customer_name=?,created_at=? WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindHeader(ps, t);
            ps.setLong(7, t.getId());
            ps.executeUpdate();
        }
    }

    private void bindHeader(PreparedStatement ps, Transaction t) throws SQLException {
        ps.setLong(1, t.getStoreId());
        if (t.getUserId() == null) {
            ps.setNull(2, Types.BIGINT);
        } else {
            ps.setLong(2, t.getUserId());
        }
        ps.setBigDecimal(3, t.getTotal());
        ps.setString(4, t.getPaymentMethod().name());
        ps.setString(5, t.getCustomerName());
        ps.setTimestamp(6, Timestamp.valueOf(t.getCreatedAt()));
    }

    private void insertLines(Connection c, Transaction t) throws SQLException {
        String sql = "INSERT INTO transaction_lines(transaction_id,product_id,product_name,quantity,unit_price) "
                + "VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (TransactionLine line : t.getLines()) {
                ps.setLong(1, t.getId());
                if (line.getProductId() == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, line.getProductId());
                }
                ps.setString(3, line.getProductName());
                ps.setInt(4, line.getQuantity());
                ps.setBigDecimal(5, line.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteLines(Connection c, Long txId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM transaction_lines WHERE transaction_id=?")) {
            ps.setLong(1, txId);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Transaction t = mapHeader(rs);
                loadLines(c, t);
                return Optional.of(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByStore(Long storeId) {
        return query("SELECT * FROM transactions WHERE store_id=? ORDER BY id", storeId);
    }

    @Override
    public List<Transaction> findAll() {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Transaction> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapHeader(rs));
            }
            for (Transaction t : out) {
                loadLines(c, t);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed: " + e.getMessage(), e);
        }
    }

    private List<Transaction> query(String sql, Long param) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, param);
            List<Transaction> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapHeader(rs));
                }
            }
            for (Transaction t : out) {
                loadLines(c, t);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection c = cp.getConnection()) {
            c.setAutoCommit(false);
            try {
                deleteLines(c, id);
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id=?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM transactions");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("count failed: " + e.getMessage(), e);
        }
    }

    private void loadLines(Connection c, Transaction t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM transaction_lines WHERE transaction_id=? ORDER BY id")) {
            ps.setLong(1, t.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("product_id");
                    Long productId = rs.wasNull() ? null : pid;
                    t.addLine(new TransactionLine(
                            productId,
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price")));
                }
            }
        }
    }

    private Transaction mapHeader(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setStoreId(rs.getLong("store_id"));
        long uid = rs.getLong("user_id");
        t.setUserId(rs.wasNull() ? null : uid);
        t.setTotal(rs.getBigDecimal("total"));
        t.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        t.setCustomerName(rs.getString("customer_name"));
        Timestamp ts = rs.getTimestamp("created_at");
        t.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return t;
    }
}
