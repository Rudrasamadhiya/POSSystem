package com.rudra.pos.repository;

import com.rudra.pos.model.Transaction;

import java.util.List;

/** Repository contract for {@link Transaction} sales (CSV or JDBC backed). */
public interface TransactionRepository extends Repository<Transaction> {

    List<Transaction> findByStore(Long storeId);
}
