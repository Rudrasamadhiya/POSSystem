package com.rudra.pos.model;

/**
 * Marker contract for anything persisted by a repository. A {@code null} id
 * means the entity is new and has not yet been assigned a primary key.
 */
public interface Entity {

    Long getId();

    void setId(Long id);
}
