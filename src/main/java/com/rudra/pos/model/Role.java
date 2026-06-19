package com.rudra.pos.model;

/**
 * Role-based access control levels. The ordinal ordering encodes privilege:
 * an ADMIN can do anything a MANAGER can, and a MANAGER anything a CASHIER can.
 */
public enum Role {

    CASHIER("Cashier"),
    MANAGER("Manager"),
    ADMIN("Admin");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** @return true if this role is at least as privileged as {@code required}. */
    public boolean satisfies(Role required) {
        return this.ordinal() >= required.ordinal();
    }

    public static Role fromString(String value) {
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(value) || r.label.equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
