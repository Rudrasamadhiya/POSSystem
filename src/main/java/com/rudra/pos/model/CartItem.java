package com.rudra.pos.model;

import java.math.BigDecimal;

/**
 * A line in an in-progress sale: a reference to a product plus the quantity
 * the customer is buying. The unit price is snapshotted at the moment the
 * item is added so later catalogue price changes don't alter an open cart.
 */
public class CartItem {

    private final Product product;
    private int quantity;
    private final BigDecimal unitPrice;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(int delta) {
        this.quantity += delta;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** @return unitPrice * quantity. */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return String.format("%s x%d", product.getName(), quantity);
    }
}
