package com.rudra.pos.model;

import java.time.LocalDateTime;

/**
 * A tenant in the multi-store system. Every other entity (users, products,
 * transactions) is scoped to exactly one Store, which is how data isolation
 * between tenants is enforced.
 */
public class Store implements Entity {

    private Long id;
    private String code;          // human-facing unique login id, e.g. "BHOPAL01"
    private String name;
    private String passwordHash;  // admin password (PBKDF2)
    private String location;
    private String contact;
    private LocalDateTime createdAt;

    public Store() {
    }

    public Store(String code, String name, String passwordHash, String location, String contact) {
        this.code = code;
        this.name = name;
        this.passwordHash = passwordHash;
        this.location = location;
        this.contact = contact;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
