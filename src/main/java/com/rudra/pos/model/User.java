package com.rudra.pos.model;

import java.time.LocalDateTime;

/** A staff account (cashier / manager / admin) belonging to a single store. */
public class User implements Entity {

    private Long id;
    private Long storeId;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long storeId, String username, String passwordHash, Role role) {
        this.storeId = storeId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return username + " [" + role.getLabel() + "]";
    }
}
