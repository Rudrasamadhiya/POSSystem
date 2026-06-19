package com.rudra.pos.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates the PostgreSQL schema on first run (idempotent). */
public final class PostgresSchema {

    private PostgresSchema() {
    }

    private static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS stores ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "code TEXT UNIQUE NOT NULL,"
            + "name TEXT,"
            + "password_hash TEXT,"
            + "location TEXT,"
            + "contact TEXT,"
            + "created_at TIMESTAMP)",
        "CREATE TABLE IF NOT EXISTS users ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "store_id BIGINT,"
            + "username TEXT,"
            + "password_hash TEXT,"
            + "role TEXT,"
            + "active BOOLEAN,"
            + "created_at TIMESTAMP)",
        "CREATE TABLE IF NOT EXISTS products ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "store_id BIGINT,"
            + "barcode TEXT,"
            + "name TEXT,"
            + "price NUMERIC(12,2),"
            + "stock INTEGER,"
            + "category TEXT,"
            + "reorder_level INTEGER)",
        "CREATE TABLE IF NOT EXISTS transactions ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "store_id BIGINT,"
            + "user_id BIGINT,"
            + "total NUMERIC(12,2),"
            + "payment_method TEXT,"
            + "customer_name TEXT,"
            + "created_at TIMESTAMP)",
        "CREATE TABLE IF NOT EXISTS transaction_lines ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "transaction_id BIGINT,"
            + "product_id BIGINT,"
            + "product_name TEXT,"
            + "quantity INTEGER,"
            + "unit_price NUMERIC(12,2))"
    };

    public static void init(ConnectionProvider provider) {
        try (Connection conn = provider.getConnection();
             Statement st = conn.createStatement()) {
            for (String ddl : DDL) {
                st.execute(ddl);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialise PostgreSQL schema: " + e.getMessage(), e);
        }
    }
}
