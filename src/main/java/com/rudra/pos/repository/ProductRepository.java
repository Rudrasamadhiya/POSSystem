package com.rudra.pos.repository;

import com.rudra.pos.model.Product;

import java.util.List;
import java.util.Optional;

/** Repository contract for {@link Product} catalogue items (CSV or JDBC backed). */
public interface ProductRepository extends Repository<Product> {

    Optional<Product> findByBarcode(Long storeId, String barcode);

    List<Product> findByStore(Long storeId);
}
