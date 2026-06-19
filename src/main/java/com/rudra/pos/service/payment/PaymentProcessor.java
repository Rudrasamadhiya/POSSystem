package com.rudra.pos.service.payment;

import com.rudra.pos.model.PaymentMethod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that maps a {@link PaymentMethod} to its {@link PaymentStrategy}.
 * Centralising the wiring here keeps the billing engine decoupled from concrete
 * tender implementations.
 */
public class PaymentProcessor {

    private final Map<PaymentMethod, PaymentStrategy> strategies = new EnumMap<>(PaymentMethod.class);

    public PaymentProcessor() {
        register(new CashPayment());
        register(new CardPayment());
        register(new UpiPayment());
    }

    private void register(PaymentStrategy strategy) {
        strategies.put(strategy.method(), strategy);
    }

    public PaymentStrategy strategyFor(PaymentMethod method) {
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("No payment strategy for " + method);
        }
        return strategy;
    }
}
