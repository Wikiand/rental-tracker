package com.kood.rentaltracker.repository;

import com.kood.rentaltracker.domain.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(int id);

    Optional<User> findByUsername(String username);

    Optional<User> findOwner();
}