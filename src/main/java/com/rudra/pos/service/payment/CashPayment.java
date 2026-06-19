package com.rudra.pos.service.payment;

import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.util.Money;

import java.math.BigDecimal;

/** Cash tender. */
public class CashPayment implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH;
    }

    @Override
    public String collect(BigDecimal amount) {
        return "Cash received: " + Money.format(amount);
    }
}
