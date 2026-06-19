package com.rudra.pos.exception;

/** Thrown when login credentials are invalid or an account is disabled. */
public class AuthenticationException extends PosException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
