package com.rudra.pos.repository;

import com.rudra.pos.model.Role;
import com.rudra.pos.model.User;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Persists {@link User} staff accounts to {@code users.csv}. */
public class UserRepository extends AbstractCsvRepository<User> {

    public UserRepository(Path dataDir) {
        super(dataDir, "users.csv");
    }

    public Optional<User> findByUsername(Long storeId, String username) {
        for (User u : snapshot()) {
            if (u.getStoreId().equals(storeId) && u.getUsername().equalsIgnoreCase(username)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    /** Username lookup across all tenants, used by the cashier login screen. */
    public Optional<User> findByUsername(String username) {
        for (User u : snapshot()) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    public List<User> findByStore(Long storeId) {
        List<User> result = new ArrayList<>();
        for (User u : snapshot()) {
            if (u.getStoreId().equals(storeId)) {
                result.add(u);
            }
        }
        return result;
    }

    @Override
    protected List<String> header() {
        return Arrays.asList("id", "storeId", "username", "passwordHash", "role", "active", "createdAt");
    }

    @Override
    protected List<String> toRow(User u) {
        return Arrays.asList(
                String.valueOf(u.getId()),
                String.valueOf(u.getStoreId()),
                u.getUsername(),
                u.getPasswordHash(),
                u.getRole().name(),
                String.valueOf(u.isActive()),
                u.getCreatedAt().toString());
    }

    @Override
    protected User fromRow(List<String> r) {
        User u = new User();
        u.setId(Long.parseLong(r.get(0)));
        u.setStoreId(Long.parseLong(r.get(1)));
        u.setUsername(r.get(2));
        u.setPasswordHash(r.get(3));
        u.setRole(Role.valueOf(r.get(4)));
        u.setActive(Boolean.parseBoolean(r.get(5)));
        u.setCreatedAt(LocalDateTime.parse(r.get(6)));
        return u;
    }
}
