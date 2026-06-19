package com.rudra.pos.exception;

/**
 * Thrown by the billing engine when a checkout would drive a product's
 * stock below zero. Carries the product name and the shortfall so the UI
 * can render a helpful message.
 */
public class InsufficientStockException extends PosException {

    private static final long serialVersionUID = 1L;

    private final String productName;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format(
                "Insufficient stock for '%s': requested %d but only %d available",
                productName, requested, available));
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }

    public String getProductName() {
        return productName;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
