package com.rudra.pos.repository;

import com.rudra.pos.model.Store;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Persists {@link Store} tenants to {@code stores.csv}. */
public class StoreRepository extends AbstractCsvRepository<Store> {

    public StoreRepository(Path dataDir) {
        super(dataDir, "stores.csv");
    }

    public Optional<Store> findByCode(String code) {
        for (Store s : snapshot()) {
            if (s.getCode().equalsIgnoreCase(code)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    protected List<String> header() {
        return Arrays.asList("id", "code", "name", "passwordHash", "location", "contact", "createdAt");
    }

    @Override
    protected List<String> toRow(Store s) {
        return Arrays.asList(
                String.valueOf(s.getId()),
                s.getCode(),
                s.getName(),
                s.getPasswordHash(),
                nullToEmpty(s.getLocation()),
                nullToEmpty(s.getContact()),
                s.getCreatedAt().toString());
    }

    @Override
    protected Store fromRow(List<String> r) {
        Store s = new Store();
        s.setId(Long.parseLong(r.get(0)));
        s.setCode(r.get(1));
        s.setName(r.get(2));
        s.setPasswordHash(r.get(3));
        s.setLocation(r.get(4));
        s.setContact(r.get(5));
        s.setCreatedAt(LocalDateTime.parse(r.get(6)));
        return s;
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
