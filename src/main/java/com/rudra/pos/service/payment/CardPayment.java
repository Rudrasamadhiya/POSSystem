package com.rudra.pos.service.payment;

import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.util.Money;

import java.math.BigDecimal;

/** Credit/debit card tender. */
public class CardPayment implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public String collect(BigDecimal amount) {
        return "Card charged: " + Money.format(amount) + " (approved)";
    }
}
