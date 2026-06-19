package com.rudra.pos.repository;

import com.rudra.pos.model.User;

import java.util.List;
import java.util.Optional;

/** Repository contract for {@link User} staff accounts (CSV or JDBC backed). */
public interface UserRepository extends Repository<User> {

    Optional<User> findByUsername(Long storeId, String username);

    Optional<User> findByUsername(String username);

    List<User> findByStore(Long storeId);
}
