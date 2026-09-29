package com.kood.rentaltracker.validation;

public class UserValidator {

    public void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters."
            );
        }
    }

    public void validateRegistration(
            String username,
            String password
    ) {
        validateUsername(username);
        validatePassword(password);
    }
}
