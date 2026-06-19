package com.rudra.pos.model;

import java.math.BigDecimal;

/**
 * A sellable item in a store's catalogue. Money is modelled with
 * {@link BigDecimal} rather than {@code double} to avoid floating-point
 * rounding errors in billing totals.
 */
public class Product implements Entity {

    private Long id;
    private Long storeId;
    private String barcode;
    private String name;
    private BigDecimal price;
    private int stock;
    private String category;
    private int reorderLevel;   // threshold below which the item is "low stock"

    public Product() {
    }

    public Product(Long storeId, String barcode, String name, BigDecimal price,
                   int stock, String category, int reorderLevel) {
        this.storeId = storeId;
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.reorderLevel = reorderLevel;
    }

    /** @return true when stock has fallen to or below the reorder threshold. */
    public boolean isLowStock() {
        return stock <= reorderLevel;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    @Override
    public String toString() {
        return name + " (" + barcode + ")";
    }
}
