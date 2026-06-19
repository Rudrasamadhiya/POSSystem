package com.rudra.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Helpers for formatting and normalising monetary {@link BigDecimal} values. */
public final class Money {

    private Money() {
    }

    /** Round to two decimal places (half-up), the canonical money scale. */
    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Format as an INR amount, e.g. {@code ₹1,250.00}. */
    public static String format(BigDecimal amount) {
        return "₹" + String.format("%,.2f", normalize(amount));
    }
}
