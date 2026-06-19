package com.rudra.pos.model;

import java.math.BigDecimal;

/**
 * An immutable snapshot of one sold line inside a completed {@link Transaction}.
 * Unlike a {@link CartItem} this stores denormalised product details (name,
 * price) so a historical receipt still reads correctly even if the product is
 * later renamed or deleted.
 */
public class TransactionLine {

    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;

    public TransactionLine() {
    }

    public TransactionLine(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
