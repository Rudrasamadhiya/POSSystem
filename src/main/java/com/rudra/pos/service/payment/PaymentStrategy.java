package com.rudra.pos.service.payment;

import com.rudra.pos.model.PaymentMethod;

import java.math.BigDecimal;

/**
 * Strategy abstraction for collecting payment. Each tender type (cash, card,
 * UPI) implements its own {@code collect} step, so adding a new method (e.g.
 * wallet, gift card) means adding one class — not editing the billing engine.
 */
public interface PaymentStrategy {

    PaymentMethod method();

    /**
     * Capture payment for the given amount.
     *
     * @return a short human-readable confirmation line for the receipt
     */
    String collect(BigDecimal amount);
}
