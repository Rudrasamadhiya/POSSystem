package com.rudra.pos.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mutable shopping cart used during a billing session. Adding a product
 * that is already present simply increments its quantity, mirroring how a
 * cashier re-scans the same item.
 */
public class Cart {

    private final List<CartItem> items = new ArrayList<>();

    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.addQuantity(quantity);
                return;
            }
        }
        items.add(new CartItem(product, quantity));
    }

    public void setQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            removeProduct(productId);
            return;
        }
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(productId)) {
                item.setQuantity(quantity);
                return;
            }
        }
    }

    public void removeProduct(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** @return the sum of every line total, exact to two decimal places. */
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }
}
