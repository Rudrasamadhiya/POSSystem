package com.rudra.pos.exception;

/** Thrown when user-supplied input fails a business validation rule. */
public class ValidationException extends PosException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
