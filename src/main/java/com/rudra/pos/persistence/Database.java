package com.rudra.pos.persistence;

import com.rudra.pos.repository.ProductRepository;
import com.rudra.pos.repository.StoreRepository;
import com.rudra.pos.repository.TransactionRepository;
import com.rudra.pos.repository.UserRepository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persistence facade — a single entry point that owns the four repositories and
 * the directory their CSV tables live in. Acts as a lightweight "unit of work":
 * pass one {@code Database} around and every service shares the same in-memory
 * state and on-disk files.
 *
 * <p>Construct it with an explicit directory (handy for tests, which point it at
 * a temp folder) or use {@link #openDefault()} for the standard {@code ./data}
 * location used by the CLI.</p>
 */
public class Database {

    private final Path dataDir;
    private final StoreRepository stores;
    private final UserRepository users;
    private final ProductRepository products;
    private final TransactionRepository transactions;

    public Database(Path dataDir) {
        this.dataDir = dataDir;
        this.stores = new StoreRepository(dataDir);
        this.users = new UserRepository(dataDir);
        this.products = new ProductRepository(dataDir);
        this.transactions = new TransactionRepository(dataDir);
    }

    public static Database openDefault() {
        String dir = System.getenv().getOrDefault("POS_DATA_DIR", "data");
        return new Database(Paths.get(dir));
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
}
