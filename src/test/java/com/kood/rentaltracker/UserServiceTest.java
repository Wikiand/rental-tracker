
package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.logging.AppLogger;
import com.kood.rentaltracker.repository.UserRepository;
import com.kood.rentaltracker.security.PasswordHasher;
import com.kood.rentaltracker.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private static final String PASSWORD = "securePassword123";

    private static final String HASH =
            "$argon2id$v=19$m=1024,t=3,p=2$test-salt$test-hash";

    private static final Path LOG_FILE =
            Path.of("rental-tracker.log");

    @BeforeEach
    void setUpLog() throws Exception {
        Files.deleteIfExists(LOG_FILE);
    }

    @AfterEach
    void tearDownLog() throws Exception {
        Files.deleteIfExists(LOG_FILE);
    }

    @Test
    void registersNewUser() {
        FakeUserRepository repository =
                new FakeUserRepository();

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        User user =
                service.register(
                        "christine",
                        PASSWORD
                );

        assertNotNull(user);

        assertEquals(
                "christine",
                user.getUsername()
        );

        assertEquals(
                1,
                user.getId()
        );

        assertNotNull(
                repository.passwordHash
        );

        assertNotEquals(
                PASSWORD,
                repository.passwordHash
        );
    }

    @Test
    void trimsUsernameDuringRegistration() {
        FakeUserRepository repository =
                new FakeUserRepository();

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        User user =
                service.register(
                        "  christine  ",
                        PASSWORD
                );

        assertEquals(
                "christine",
                user.getUsername()
        );
    }

    @Test
    void rejectsDuplicateUsername() {
        FakeUserRepository repository =
                new FakeUserRepository();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(
                        "christine",
                        PASSWORD
                )
        );
    }

    @Test
    void rejectsBlankUsername() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(
                        " ",
                        PASSWORD
                )
        );
    }

    @Test
    void rejectsNullUsername() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(
                        null,
                        PASSWORD
                )
        );
    }

    @Test
    void rejectsBlankPassword() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(
                        "christine",
                        ""
                )
        );
    }

    @Test
    void rejectsNullPassword() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(
                        "christine",
                        null
                )
        );
    }

    @Test
    void rejectsShortPassword() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(
                        "christine",
                        "short"
                )
        );
    }

    @Test
    void authenticatesWithCorrectPassword() {
        FakeUserRepository repository =
                new FakeUserRepository();

        PasswordHasher hasher =
                new PasswordHasher();

        String passwordHash =
                hasher.hash(PASSWORD);

        repository.user =
                new User(
                        1,
                        "christine"
                );

        repository.passwordHash =
                passwordHash;

        UserService service =
                new UserService(
                        repository,
                        hasher
                );

        User authenticated =
                service.authenticate(
                        "christine",
                        PASSWORD
                );

        assertEquals(
                1,
                authenticated.getId()
        );

        assertEquals(
                "christine",
                authenticated.getUsername()
        );
    }

    @Test
    void rejectsWrongPassword() {
        FakeUserRepository repository =
                new FakeUserRepository();

        PasswordHasher hasher =
                new PasswordHasher();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        repository.passwordHash =
                hasher.hash(PASSWORD);

        UserService service =
                new UserService(
                        repository,
                        hasher
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(
                        "christine",
                        "wrongPassword"
                )
        );
    }

    @Test
    void rejectsUnknownUsername() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(
                        "unknown",
                        PASSWORD
                )
        );
    }

    @Test
    void rejectsBlankPasswordDuringAuthentication() {
        FakeUserRepository repository =
                new FakeUserRepository();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(
                        "christine",
                        ""
                )
        );
    }

    @Test
    void rejectsNullPasswordDuringAuthentication() {
        FakeUserRepository repository =
                new FakeUserRepository();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(
                        "christine",
                        null
                )
        );
    }

    @Test
    void rejectsUserWithoutPasswordHash() {
        FakeUserRepository repository =
                new FakeUserRepository();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        repository.passwordHash = null;

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(
                        "christine",
                        PASSWORD
                )
        );
    }

    @Test
    void findByIdReturnsUser() {
        FakeUserRepository repository =
                new FakeUserRepository();

        repository.user =
                new User(
                        7,
                        "alice"
                );

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        User user =
                service.findById(7);

        assertEquals(
                7,
                user.getId()
        );

        assertEquals(
                "alice",
                user.getUsername()
        );
    }

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        UserService service =
                new UserService(
                        new FakeUserRepository(),
                        new PasswordHasher()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findById(999)
        );
    }

    @Test
    void successfulAuthenticationIsLoggedWithoutPassword()
            throws Exception {

        FakeUserRepository repository =
                new FakeUserRepository();

        PasswordHasher hasher =
                new PasswordHasher();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        repository.passwordHash =
                hasher.hash(PASSWORD);

        UserService service =
                new UserService(
                        repository,
                        hasher
                );

        service.authenticate(
                "christine",
                PASSWORD
        );

        String log =
                Files.readString(LOG_FILE);

        assertTrue(
                log.contains("INFO")
        );

        assertTrue(
                log.contains(
                        "event=auth.login.success"
                )
        );

        assertTrue(
                log.contains("userId=1")
        );

        assertTrue(
                log.contains(
                        "username=christine"
                )
        );

        assertFalse(
                log.contains(PASSWORD)
        );

        assertFalse(
                log.contains(
                        repository.passwordHash
                )
        );

        assertFalse(
                log.toLowerCase().contains(
                        "password"
                )
        );
    }

    @Test
    void failedAuthenticationIsLoggedWithoutPassword()
            throws Exception {

        FakeUserRepository repository =
                new FakeUserRepository();

        PasswordHasher hasher =
                new PasswordHasher();

        repository.user =
                new User(
                        1,
                        "christine"
                );

        repository.passwordHash =
                hasher.hash(PASSWORD);

        UserService service =
                new UserService(
                        repository,
                        hasher
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(
                        "christine",
                        "wrongPassword"
                )
        );

        String log =
                Files.readString(LOG_FILE);

        assertTrue(
                log.contains("WARN")
        );

        assertTrue(
                log.contains(
                        "event=auth.login.failed"
                )
        );

        assertTrue(
                log.contains(
                        "reason=invalid_credentials"
                )
        );

        assertFalse(
                log.contains(
                        "wrongPassword"
                )
        );

        assertFalse(
                log.contains(
                        repository.passwordHash
                )
        );

        assertFalse(
                log.toLowerCase().contains(
                        "password"
                )
        );
    }

    @Test
    void successfulRegistrationIsLoggedWithoutPassword()
            throws Exception {

        FakeUserRepository repository =
                new FakeUserRepository();

        UserService service =
                new UserService(
                        repository,
                        new PasswordHasher()
                );

        service.register(
                "newuser",
                PASSWORD
        );

        String log =
                Files.readString(LOG_FILE);

        assertTrue(
                log.contains("INFO")
        );

        assertTrue(
                log.contains(
                        "event=user.registered"
                )
        );

        assertTrue(
                log.contains("userId=1")
        );

        assertTrue(
                log.contains(
                        "username=newuser"
                )
        );

        assertFalse(
                log.contains(PASSWORD)
        );

        assertFalse(
                log.contains(
                        repository.passwordHash
                )
        );

        assertFalse(
                log.toLowerCase().contains(
                        "password"
                )
        );
    }

    private static class FakeUserRepository
            implements UserRepository {

        private User user;
        private String passwordHash;

        @Override
        public User save(
                User user,
                String passwordHash
        ) {
            this.user =
                    new User(
                            1,
                            user.getUsername()
                    );

            this.passwordHash =
                    passwordHash;

            return this.user;
        }

        @Override
        public Optional<User> findById(
                int id
        ) {
            if (user != null
                    && user.getId() == id) {

                return Optional.of(user);
            }

            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsername(
                String username
        ) {
            if (user != null
                    && user.getUsername().equals(username)) {

                return Optional.of(user);
            }

            return Optional.empty();
        }

        @Override
        public Optional<String> findPasswordHashByUsername(
                String username
        ) {
            if (user != null
                    && user.getUsername().equals(username)
                    && passwordHash != null) {

                return Optional.of(passwordHash);
            }

            return Optional.empty();
        }
    }
}
