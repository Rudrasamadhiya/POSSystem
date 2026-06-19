package com.rudra.pos.repository;

import com.rudra.pos.model.Entity;
import com.rudra.pos.persistence.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class that turns a CSV file into a thread-safe, auto-incrementing
 * repository. Subclasses only describe how one entity maps to and from a row;
 * loading, id generation, indexing, locking and atomic persistence are handled
 * here (Template Method pattern).
 *
 * @param <T> the entity type
 */
public abstract class AbstractCsvRepository<T extends Entity> implements Repository<T> {

    private final CsvFile file;
    private final Map<Long, T> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected AbstractCsvRepository(Path dataDir, String fileName) {
        this.file = new CsvFile(dataDir.resolve(fileName));
        load();
    }

    /** Column names written as the first line of the file. */
    protected abstract List<String> header();

    /** Serialise an entity to a CSV record. */
    protected abstract List<String> toRow(T entity);

    /** Reconstruct an entity from a CSV record. */
    protected abstract T fromRow(List<String> row);

    /** Hook: load any child tables after the main entities are in memory. */
    protected void onLoaded() {
    }

    /** Hook: persist any child tables. Called while the write lock is held. */
    protected void onPersist() {
    }

    private void load() {
        List<List<String>> rows = file.readAll();
        if (rows.isEmpty()) {
            return; // brand-new table
        }
        for (int i = 1; i < rows.size(); i++) { // row 0 is the header
            T entity = fromRow(rows.get(i));
            store.put(entity.getId(), entity);
            bumpSequence(entity.getId());
        }
        onLoaded();
    }

    private void bumpSequence(Long id) {
        if (id != null) {
            sequence.updateAndGet(prev -> Math.max(prev, id));
        }
    }

    @Override
    public T save(T entity) {
        lock.writeLock().lock();
        try {
            if (entity.getId() == null) {
                entity.setId(sequence.incrementAndGet());
            } else {
                bumpSequence(entity.getId());
            }
            store.put(entity.getId(), entity);
            persist();
            return entity;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<T> findById(Long id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(store.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(store.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void delete(Long id) {
        lock.writeLock().lock();
        try {
            if (store.remove(id) != null) {
                persist();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return store.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Find the first entity matching a predicate-like scan helper for subclasses. */
    protected List<T> snapshot() {
        return new ArrayList<>(store.values());
    }

    /** Persist the full in-memory table to disk atomically. */
    protected void persist() {
        List<List<String>> rows = new ArrayList<>();
        for (T entity : store.values()) {
            rows.add(toRow(entity));
        }
        file.writeAll(header(), rows);
        onPersist();
    }

    /** Directory in which this repository's data files live. */
    protected Path dataDir() {
        return file.getPath().getParent();
    }
}
