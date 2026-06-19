package com.rudra.pos.persistence;

import com.rudra.pos.repository.CsvProductRepository;
import com.rudra.pos.repository.CsvStoreRepository;
import com.rudra.pos.repository.CsvTransactionRepository;
import com.rudra.pos.repository.CsvUserRepository;
import com.rudra.pos.repository.JdbcProductRepository;
import com.rudra.pos.repository.JdbcStoreRepository;
import com.rudra.pos.repository.JdbcTransactionRepository;
import com.rudra.pos.repository.JdbcUserRepository;
import com.rudra.pos.repository.ProductRepository;
import com.rudra.pos.repository.StoreRepository;
import com.rudra.pos.repository.TransactionRepository;
import com.rudra.pos.repository.UserRepository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persistence facade — a single entry point that owns the four repositories.
 * Storage is <b>pluggable</b>: the repositories are referenced through their
 * interfaces, so the rest of the app is identical whether data lives in CSV
 * files or PostgreSQL.
 *
 * <ul>
 *   <li>{@link #openDefault()} picks the backend from the environment: if
 *       {@code DATABASE_URL} (or {@code JDBC_DATABASE_URL}) is set it uses
 *       PostgreSQL; otherwise it falls back to local CSV files.</li>
 *   <li>{@code new Database(Path)} forces the CSV backend — handy for tests and
 *       offline local use.</li>
 * </ul>
 */
public class Database {

    private final Path dataDir;
    private final StoreRepository stores;
    private final UserRepository users;
    private final ProductRepository products;
    private final TransactionRepository transactions;

    /** CSV-backed database rooted at {@code dataDir}. */
    public Database(Path dataDir) {
        this.dataDir = dataDir;
        this.stores = new CsvStoreRepository(dataDir);
        this.users = new CsvUserRepository(dataDir);
        this.products = new CsvProductRepository(dataDir);
        this.transactions = new CsvTransactionRepository(dataDir);
    }

    private Database(StoreRepository stores, UserRepository users,
                     ProductRepository products, TransactionRepository transactions) {
        this.dataDir = Paths.get("data");
        this.stores = stores;
        this.users = users;
        this.products = products;
        this.transactions = transactions;
    }

    /** Choose a backend from the environment (PostgreSQL if configured, else CSV). */
    public static Database openDefault() {
        String url = firstNonEmpty(System.getenv("DATABASE_URL"), System.getenv("JDBC_DATABASE_URL"));
        if (url != null) {
            return openPostgres(url);
        }
        String dir = System.getenv().getOrDefault("POS_DATA_DIR", "data");
        return new Database(Paths.get(dir));
    }

    /** PostgreSQL-backed database. The connection string may be Render/Heroku or JDBC style. */
    public static Database openPostgres(String url) {
        ConnectionProvider cp = new ConnectionProvider(url);
        PostgresSchema.init(cp);
        System.out.println("Storage backend: PostgreSQL");
        return new Database(
                new JdbcStoreRepository(cp),
                new JdbcUserRepository(cp),
                new JdbcProductRepository(cp),
                new JdbcTransactionRepository(cp));
    }

    public Path getDataDir() {
        return dataDir;
    }

    public StoreRepository stores() {
        return stores;
    }

    public UserRepository users() {
        return users;
    }

    public ProductRepository products() {
        return products;
    }

    public TransactionRepository transactions() {
        return transactions;
    }

    public boolean isEmpty() {
        return stores.count() == 0;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        if (b != null && !b.isEmpty()) {
            return b;
        }
        return null;
    }
}
