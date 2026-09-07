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
                        new IllegalStateException(
                                "Owner account not found"
                        )
                );
    }

    public User createOwner(String username) {
        validateUsername(username);

        if (userRepository.findOwner().isPresent()) {
            return userRepository.findOwner().get();
        }

        return userRepository.save(
                new User(0, username.trim())
        );
    }

    public User findOrCreateRenter(String username) {
        validateUsername(username);

        return userRepository.findByUsername(username.trim())
                .orElseGet(() ->
                        userRepository.save(
                                new User(0, username.trim())
                        )
                );
    }

    public User findById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + id
                        )
                );
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }
    }
}