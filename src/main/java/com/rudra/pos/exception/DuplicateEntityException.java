package com.rudra.pos.exception;

/** Thrown when creating an entity whose unique key already exists. */
public class DuplicateEntityException extends PosException {

    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String message) {
        super(message);
    }
}
