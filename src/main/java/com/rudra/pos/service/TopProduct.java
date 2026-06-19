package com.rudra.pos.service;

/** Small immutable value object: a product name paired with units sold. */
public class TopProduct {

    private final String name;
    private final int quantitySold;

    public TopProduct(String name, int quantitySold) {
        this.name = name;
        this.quantitySold = quantitySold;
    }

    public String getName() {
        return name;
    }

    public int getQuantitySold() {
        return quantitySold;
    }
}
