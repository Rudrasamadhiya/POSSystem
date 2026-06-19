package com.rudra.pos.service;

import com.rudra.pos.exception.DuplicateEntityException;
import com.rudra.pos.exception.ProductNotFoundException;
import com.rudra.pos.exception.ValidationException;
import com.rudra.pos.model.Product;
import com.rudra.pos.persistence.Database;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue and stock management for a single store: add/update/delete
 * products, restock, barcode lookup, free-text search and low-stock alerts.
 */
public class InventoryService {

    private final Database db;

    public InventoryService(Database db) {
        this.db = db;
    }

    public Product addProduct(Long storeId, String barcode, String name, BigDecimal price,
                              int stock, String category, int reorderLevel)
            throws ValidationException, DuplicateEntityException {
        requireText(barcode, "Barcode");
        requireText(name, "Name");
        if (price == null || price.signum() < 0) {
            throw new ValidationException("Price must be zero or positive");
        }
        if (stock < 0) {
            throw new ValidationException("Stock cannot be negative");
        }
        if (db.products().findByBarcode(storeId, barcode).isPresent()) {
            throw new DuplicateEntityException("A product with barcode '" + barcode + "' already exists");
        }
        Product product = new Product(storeId, barcode, name, price, stock, category, reorderLevel);
        return db.products().save(product);
    }

    public Product updateProduct(Long productId, String name, BigDecimal price,
                                 String category, int reorderLevel)
            throws ProductNotFoundException, ValidationException {
        Product product = require(productId);
        requireText(name, "Name");
        if (price == null || price.signum() < 0) {
            throw new ValidationException("Price must be zero or positive");
        }
        product.setName(name);
        product.setPrice(price);
        product.setCategory(category);
        product.setReorderLevel(reorderLevel);
        return db.products().save(product);
    }

    /** Add (positive) or remove (negative) units of stock. */
    public Product adjustStock(Long productId, int delta)
            throws ProductNotFoundException, ValidationException {
        Product product = require(productId);
        int updated = product.getStock() + delta;
        if (updated < 0) {
            throw new ValidationException("Adjustment would make stock negative");
        }
        product.setStock(updated);
        return db.products().save(product);
    }

    public void deleteProduct(Long productId) throws ProductNotFoundException {
        require(productId);
        db.products().delete(productId);
    }

    public Product findByBarcode(Long storeId, String barcode) throws ProductNotFoundException {
        return db.products().findByBarcode(storeId, barcode)
                .orElseThrow(() -> new ProductNotFoundException("No product with barcode '" + barcode + "'"));
    }

    public List<Product> listProducts(Long storeId) {
        return db.products().findByStore(storeId);
    }

    /** Case-insensitive search across name, barcode and category. */
    public List<Product> search(Long storeId, String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Product> result = new ArrayList<>();
        for (Product p : db.products().findByStore(storeId)) {
            if (q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getBarcode().toLowerCase().contains(q)
                    || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q))) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Product> lowStockProducts(Long storeId) {
        List<Product> result = new ArrayList<>();
        for (Product p : db.products().findByStore(storeId)) {
            if (p.isLowStock()) {
                result.add(p);
            }
        }
        return result;
    }

    private Product require(Long productId) throws ProductNotFoundException {
        return db.products().findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("No product with id " + productId));
    }

    private static void requireText(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required");
        }
    }
}
