package com.kood.rentaltracker.service;

import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.logging.AppLogger;
import com.kood.rentaltracker.repository.UserRepository;
import com.kood.rentaltracker.security.PasswordHasher;
import com.kood.rentaltracker.validation.UserValidator;

public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserValidator userValidator;
    private final AppLogger logger;

    public UserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher
    ) {
        this(
                userRepository,
                passwordHasher,
                new UserValidator(),
                new AppLogger()
        );
    }

    public UserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            UserValidator userValidator
    ) {
        this(
                userRepository,
                passwordHasher,
                userValidator,
                new AppLogger()
        );
    }

    public UserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            UserValidator userValidator,
            AppLogger logger
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.userValidator = userValidator;
        this.logger = logger;
    }

    public User register(
            String username,
            String password
    ) {
        try {
            userValidator.validateRegistration(
                    username,
                    password
            );
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "user.registration.validation.failed",
                    "reason=" + safeMessage(e)
            );
            throw e;
        }

        String normalizedUsername =
                username.trim();

        if (userRepository
                .findByUsername(normalizedUsername)
                .isPresent()) {

            logger.warn(
                    "user.registration.failed",
                    "reason=username_taken"
            );

            throw new IllegalArgumentException(
                    "Username is already taken."
            );
        }

        String passwordHash =
                passwordHasher.hash(password);

        User user = userRepository.save(
                new User(
                        0,
                        normalizedUsername
                ),
                passwordHash
        );

        logger.info(
                "user.registered",
                "userId=" + user.getId()
                        + " username=" + user.getUsername()
        );

        return user;
    }

    public User authenticate(
            String username,
            String password
    ) {
        try {
            userValidator.validateUsername(username);

            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException(
                        "Password cannot be empty."
                );
            }
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "auth.validation.failed",
                    "reason=" + safeMessage(e)
            );
            throw e;
        }

        String normalizedUsername =
                username.trim();

        User user =
                userRepository
                        .findByUsername(
                                normalizedUsername
                        )
                        .orElseThrow(() -> {
                            logger.warn(
                                    "auth.login.failed",
                                    "reason=invalid_credentials"
                            );

                            return new IllegalArgumentException(
                                    "Invalid username or password."
                            );
                        });

        String passwordHash =
                userRepository
                        .findPasswordHashByUsername(
                                normalizedUsername
                        )
                        .orElseThrow(() -> {
                            logger.warn(
                                    "auth.login.failed",
                                    "reason=invalid_credentials"
                            );

                            return new IllegalArgumentException(
                                    "Invalid username or password."
                            );
                        });

        if (!passwordHasher.matches(
                password,
                passwordHash
        )) {
            logger.warn(
                    "auth.login.failed",
                    "reason=invalid_credentials"
            );

            throw new IllegalArgumentException(
                    "Invalid username or password."
            );
        }

        logger.info(
                "auth.login.success",
                "userId=" + user.getId()
                        + " username=" + user.getUsername()
        );

        return user;
    }

    public User findById(int id) {
        return userRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        )
                );
    }

    private String safeMessage(
            IllegalArgumentException exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "validation_failed";
        }

        return message
                .replace(" ", "_")
                .replace(System.lineSeparator(), "_");
    }
}
