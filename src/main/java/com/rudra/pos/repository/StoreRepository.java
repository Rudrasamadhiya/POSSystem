package com.rudra.pos.repository;

import com.rudra.pos.model.Store;

import java.util.Optional;

/** Repository contract for {@link Store} tenants (CSV or JDBC backed). */
public interface StoreRepository extends Repository<Store> {

    Optional<Store> findByCode(String code);
}
