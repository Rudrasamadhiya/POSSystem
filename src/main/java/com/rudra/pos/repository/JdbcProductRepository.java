package com.rudra.pos.repository;

import com.rudra.pos.model.Product;
import com.rudra.pos.persistence.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL-backed {@link ProductRepository}. */
public class JdbcProductRepository implements ProductRepository {

    private final ConnectionProvider cp;

    public JdbcProductRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public Product save(Product p) {
        try (Connection c = cp.getConnection()) {
            if (p.getId() == null) {
                String sql = "INSERT INTO products(store_id,barcode,name,price,stock,category,reorder_level) "
                        + "VALUES(?,?,?,?,?,?,?) RETURNING id";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, p);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            p.setId(rs.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE products SET store_id=?,barcode=?,name=?,price=?,stock=?,category=?,reorder_level=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, p);
                    ps.setLong(8, p.getId());
                    ps.executeUpdate();
                }
            }
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("save(product) failed: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, Product p) throws SQLException {
        ps.setLong(1, p.getStoreId());
        ps.setString(2, p.getBarcode());
        ps.setString(3, p.getName());
        ps.setBigDecimal(4, p.getPrice());
        ps.setInt(5, p.getStock());
        ps.setString(6, p.getCategory());
        ps.setInt(7, p.getReorderLevel());
    }

    @Override
    public Optional<Product> findById(Long id) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Product> findByBarcode(Long storeId, String barcode) {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM products WHERE store_id=? AND barcode=?")) {
            ps.setLong(1, storeId);
            ps.setString(2, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByBarcode failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Product> findByStore(Long storeId) {
        List<Product> out = new ArrayList<>();
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE store_id=? ORDER BY id")) {
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
    public List<Product> findAll() {
        List<Product> out = new ArrayList<>();
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM products ORDER BY id");
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
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() {
        try (Connection c = cp.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM products");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("count failed: " + e.getMessage(), e);
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setStoreId(rs.getLong("store_id"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("name"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStock(rs.getInt("stock"));
        p.setCategory(rs.getString("category"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        return p;
    }
}
