package com.rudra.pos.exception;

/**
 * Base type for all checked exceptions thrown by the POS domain.
 *
 * <p>Having a single root lets the application layer catch every
 * business-rule violation with one {@code catch (PosException e)} while
 * still allowing callers to handle specific subtypes when they care.</p>
 */
public class PosException extends Exception {

    private static final long serialVersionUID = 1L;

    public PosException(String message) {
        super(message);
    }

    public PosException(String message, Throwable cause) {
        super(message, cause);
    }
}
