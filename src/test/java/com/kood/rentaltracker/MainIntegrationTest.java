package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.UserRepositoryImpl;
import com.kood.rentaltracker.security.PasswordHasher;
import com.kood.rentaltracker.transport.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainIntegrationTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";

    private Path databaseFile;
    private ByteArrayOutputStream output;
    private PrintStream originalOut;
    private java.io.InputStream originalIn;

    @BeforeEach
    void setUp() throws Exception {
        originalOut = System.out;
        originalIn = System.in;

        databaseFile =
                Files.createTempFile(
                        "rental-tracker-test-",
                        ".db"
                );

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + databaseFile
        );

        DatabaseInitializer.initialize();

        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8
                )
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setIn(originalIn);

        System.clearProperty(
                "rental.tracker.db.url"
        );

        Files.deleteIfExists(databaseFile);
    }

    @Test
    void wrongPasswordIsRejected() {
        createUser();

        String input = """
                2
                testuser
                wrongpassword
                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        System.err.println(
                "===== APP OUTPUT ====="
        );
        System.err.println(appOutput);
        System.err.println(
                "===== END APP OUTPUT ====="
        );

        assertTrue(
                appOutput.contains(
                        "Invalid username or password."
                )
        );
    }

    @Test
    void correctPasswordIsAccepted() {
        createUser();

        String input = """
                2
                testuser
                password123
                5
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Welcome, testuser!"
                )
        );

        assertTrue(
                appOutput.contains(
                        "1) List an item"
                )
        );
    }

    @Test
    void unknownUserIsRejected() {
        String input = """
                2
                unknownuser
                password123
                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Invalid username or password."
                )
        );
    }

    @Test
    void emptyPasswordIsRejected() {
        createUser();

        String input = """
                2
                testuser

                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Password cannot be empty."
                )
        );
    }

    @Test
    void userCanRegisterThenLogin() {
        String input = """
                1
                newuser
                password123
                password123
                2
                newuser
                password123
                5
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Account created successfully for newuser."
                )
        );

        assertTrue(
                appOutput.contains(
                        "Welcome, newuser!"
                )
        );
    }

    @Test
    void registrationRejectsPasswordMismatch() {
        String input = """
                1
                newuser
                password123
                differentpassword
                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Passwords do not match."
                )
        );
    }

    @Test
    void registrationRejectsShortPassword() {
        String input = """
                1
                newuser
                short
                short
                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Password must be at least 8 characters."
                )
        );
    }

    @Test
    void registrationRejectsBlankUsername() {
        String input = """
                1

                password123
                password123
                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Username cannot be empty."
                )
        );
    }

    @Test
    void duplicateUsernameIsRejected() {
        createUser();

        String input = """
                1
                testuser
                password123
                password123
                3
                """;

        runApplication(input);

        String appOutput =
                output.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                appOutput.contains(
                        "Username is already taken."
                )
        );
    }

    private void createUser() {
        UserRepositoryImpl repository =
                new UserRepositoryImpl();

        PasswordHasher passwordHasher =
                new PasswordHasher();

        String passwordHash =
                passwordHasher.hash(PASSWORD);

        repository.save(
                new User(
                        0,
                        USERNAME
                ),
                passwordHash
        );
    }

    private void runApplication(String input) {
        System.setIn(
                new ByteArrayInputStream(
                        input.getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        Main.main(new String[0]);
    }
}
