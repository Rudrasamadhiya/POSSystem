package com.rudra.pos.service;

import com.rudra.pos.exception.AuthenticationException;
import com.rudra.pos.exception.DuplicateEntityException;
import com.rudra.pos.exception.ValidationException;
import com.rudra.pos.model.Role;
import com.rudra.pos.model.Store;
import com.rudra.pos.model.User;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.util.PasswordHasher;

/**
 * Handles tenant registration, store-admin login, staff account management and
 * cashier login. Passwords are never stored in plain text — they pass through
 * {@link PasswordHasher} on the way in and are verified by re-hashing on login.
 */
public class AuthService {

    private final Database db;

    public AuthService(Database db) {
        this.db = db;
    }

    /** Register a new store (tenant) with an admin password. */
    public Store registerStore(String code, String name, String password,
                               String location, String contact)
            throws ValidationException, DuplicateEntityException {
        requireText(code, "Store code");
        requireText(name, "Store name");
        requireText(password, "Password");
        if (db.stores().findByCode(code).isPresent()) {
            throw new DuplicateEntityException("Store code '" + code + "' is already taken");
        }
        Store store = new Store(code, name, PasswordHasher.hash(password), location, contact);
        return db.stores().save(store);
    }

    /** Authenticate a store admin by store code + password. */
    public Store authenticateStore(String code, String password) throws AuthenticationException {
        Store store = db.stores().findByCode(code)
                .orElseThrow(() -> new AuthenticationException("Invalid store code or password"));
        if (!PasswordHasher.verify(password, store.getPasswordHash())) {
            throw new AuthenticationException("Invalid store code or password");
        }
        return store;
    }

    /** Create a staff account under a store. */
    public User createUser(Long storeId, String username, String password, Role role)
            throws ValidationException, DuplicateEntityException {
        requireText(username, "Username");
        requireText(password, "Password");
        if (db.users().findByUsername(storeId, username).isPresent()) {
            throw new DuplicateEntityException("Username '" + username + "' already exists in this store");
        }
        User user = new User(storeId, username, PasswordHasher.hash(password), role);
        return db.users().save(user);
    }

    /** Authenticate a staff member by username + password. */
    public User authenticateUser(String username, String password) throws AuthenticationException {
        User user = db.users().findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated");
        }
        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        return user;
    }

    public void setUserActive(Long userId, boolean active) {
        db.users().findById(userId).ifPresent(u -> {
            u.setActive(active);
            db.users().save(u);
        });
    }

    private static void requireText(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required");
        }
    }
}
