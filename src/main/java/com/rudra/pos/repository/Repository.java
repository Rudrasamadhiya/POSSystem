package com.rudra.pos.repository;

import com.rudra.pos.model.Entity;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract for an aggregate root. Implementations are expected to
 * be thread-safe and to persist changes durably.
 *
 * @param <T> the entity type managed by this repository
 */
public interface Repository<T extends Entity> {

    /** Insert (when id is null) or update an entity, returning the saved instance. */
    T save(T entity);

    Optional<T> findById(Long id);

    List<T> findAll();

    void delete(Long id);

    int count();
}
