package com.rudra.pos.repository;

import com.rudra.pos.model.Role;
import com.rudra.pos.model.User;
import com.rudra.pos.persistence.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL-backed {@link UserRepository}. */
public class JdbcUserRepository implements UserRepository {

    private final ConnectionProvider cp;

    public JdbcUserRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public User save(User u) {
        try (Connection c = cp.getConnection()) {
            if (u.getId() == null) {
                String sql = "INSERT INTO users(store_id,username,password_hash,role,active,created_at) "
                        + "VALUES(?,?,?,?,?,?) RETURNING id";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, u);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            u.setId(rs.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE users SET store_id=?,username=?,password_hash=?,role=?,active=?,created_at=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, u);
                    ps.setLong(7, u.getId());
                    ps.executeUpdate();
                }
            }
            return u;
        } catch (SQLException e) {
            throw new RuntimeException("save(user) failed: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, User u) throws SQLException {
        ps.setLong(1, u.getStoreId());
        ps.setString(2, u.getUsername());
        ps.setString(3, u.getPasswordHash());
        ps.setString(4, u.getRole().name());
        ps.setBoolean(5, u.isActive());
        ps.setTimestamp(6, Timestamp.valueOf(u.getCreatedAt()));
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(Long storeId, String username) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM users WHERE store_id=? AND LOWER(username)=LOWER(?)")) {
            ps.setLong(1, storeId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM users WHERE LOWER(username)=LOWER(?) ORDER BY id LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findByStore(Long storeId) {
        List<User> out = new ArrayList<>();
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE store_id=? ORDER BY id")) {
            ps.setLong(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByStore failed: " + e.getMessage(), e);
        }
        return out;
    }

    @Override
    public List<User> findAll() {
        List<User> out = new ArrayList<>();
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY id");
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
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("count failed: " + e.getMessage(), e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setStoreId(rs.getLong("store_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        u.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return u;
    }
}
