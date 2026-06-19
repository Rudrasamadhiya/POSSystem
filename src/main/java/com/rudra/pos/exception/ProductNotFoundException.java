package com.rudra.pos.exception;

/** Thrown when a product cannot be located by id or barcode. */
public class ProductNotFoundException extends PosException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(String message) {
        super(message);
    }
}
