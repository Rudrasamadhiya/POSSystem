package com.rudra.pos.model;

/** Supported tender types at checkout. */
public enum PaymentMethod {

    CASH("Cash"),
    CARD("Card"),
    UPI("UPI");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PaymentMethod fromString(String value) {
        for (PaymentMethod p : values()) {
            if (p.name().equalsIgnoreCase(value) || p.label.equalsIgnoreCase(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown payment method: " + value);
    }
}
