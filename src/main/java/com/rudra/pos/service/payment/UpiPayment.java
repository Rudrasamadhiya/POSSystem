package com.rudra.pos.service.payment;

import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.util.Money;

import java.math.BigDecimal;

/** UPI (PhonePe / GPay / Paytm) tender. */
public class UpiPayment implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public String collect(BigDecimal amount) {
        return "UPI collected: " + Money.format(amount) + " (txn ref auto-generated)";
    }
}
