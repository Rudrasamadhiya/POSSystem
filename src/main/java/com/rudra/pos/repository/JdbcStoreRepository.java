package com.rudra.pos.repository;

import com.rudra.pos.model.Store;
import com.rudra.pos.persistence.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL-backed {@link StoreRepository}. */
public class JdbcStoreRepository implements StoreRepository {

    private final ConnectionProvider cp;

    public JdbcStoreRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public Store save(Store s) {
        try (Connection c = cp.getConnection()) {
            if (s.getId() == null) {
                String sql = "INSERT INTO stores(code,name,password_hash,location,contact,created_at) "
                        + "VALUES(?,?,?,?,?,?) RETURNING id";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, s);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            s.setId(rs.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE stores SET code=?,name=?,password_hash=?,location=?,contact=?,created_at=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, s);
                    ps.setLong(7, s.getId());
                    ps.executeUpdate();
                }
            }
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("save(store) failed: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, Store s) throws SQLException {
        ps.setString(1, s.getCode());
        ps.setString(2, s.getName());
        ps.setString(3, s.getPasswordHash());
        ps.setString(4, s.getLocation());
        ps.setString(5, s.getContact());
        ps.setTimestamp(6, Timestamp.valueOf(s.getCreatedAt()));
    }

    @Override
    public Optional<Store> findById(Long id) {
        return queryOne("SELECT * FROM stores WHERE id=?", id);
    }

    @Override
    public Optional<Store> findByCode(String code) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM stores WHERE LOWER(code)=LOWER(?)")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByCode failed: " + e.getMessage(), e);
        }
    }

    private Optional<Store> queryOne(String sql, Long id) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Store> findAll() {
        List<Store> out = new ArrayList<>();
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM stores ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed: " + e.getMessage(), e);
        }
        return out;
    }

    @Override
    public void delete(Long id) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM stores WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM stores");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("count failed: " + e.getMessage(), e);
        }
    }

    private Store map(ResultSet rs) throws SQLException {
        Store s = new Store();
        s.setId(rs.getLong("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setLocation(rs.getString("location"));
        s.setContact(rs.getString("contact"));
        Timestamp ts = rs.getTimestamp("created_at");
        s.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return s;
    }
}
