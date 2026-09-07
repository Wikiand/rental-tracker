package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.repository.UserRepository;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasOwner() {
        return userRepository.findOwner().isPresent();
    }

    public User getOwner() {
        return userRepository.findOwner()
                .orElseThrow(() ->
                        new IllegalStateException("Owner account not found"));
    }

    public User createOwner(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        return userRepository.save(new User(0, username.trim()));
    }
}