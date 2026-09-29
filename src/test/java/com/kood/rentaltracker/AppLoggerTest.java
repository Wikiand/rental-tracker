
package com.kood.rentaltracker;

import com.kood.rentaltracker.logging.AppLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppLoggerTest {

    private static final Path LOG_FILE =
            Path.of("rental-tracker.log");

    @BeforeEach
    void setUp() throws Exception {
        Files.deleteIfExists(LOG_FILE);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(LOG_FILE);
    }

    @Test
    void infoLoggingWritesStructuredEvent() throws Exception {
        AppLogger logger = new AppLogger();

        logger.info(
                "test.info",
                "userId=1 action=test"
        );

        String log = Files.readString(LOG_FILE);

        assertTrue(log.contains("INFO event=test.info"));
        assertTrue(log.contains("userId=1"));
        assertTrue(log.contains("action=test"));
    }

    @Test
    void warningLoggingWritesStructuredEvent() throws Exception {
        AppLogger logger = new AppLogger();

        logger.warn(
                "test.warning",
                "reason=invalid_input"
        );

        String log = Files.readString(LOG_FILE);

        assertTrue(log.contains("WARN event=test.warning"));
        assertTrue(log.contains("reason=invalid_input"));
    }

    @Test
    void errorLoggingWritesExceptionTypeWithoutSensitiveData()
            throws Exception {

        AppLogger logger = new AppLogger();

        String password = "SecretPassword123!";
        String passwordHash = "$argon2id$secret-hash";

        logger.error(
                "test.error",
                "userId=1 action=test",
                new IllegalArgumentException("test failure")
        );

        String log = Files.readString(LOG_FILE);

        assertTrue(log.contains("ERROR event=test.error"));
        assertTrue(log.contains("userId=1"));
        assertTrue(log.contains("action=test"));
        assertTrue(log.contains(
                "errorType=IllegalArgumentException"
        ));

        assertFalse(log.contains(password));
        assertFalse(log.contains(passwordHash));
    }

    @Test
    void loggerDoesNotWritePasswordOrHash()
            throws Exception {

        AppLogger logger = new AppLogger();

        String password = "SuperSecret123!";
        String passwordHash = "$argon2id$v=19$examplehash";

        logger.info(
                "auth.login.success",
                "userId=1 username=christine"
        );

        String log = Files.readString(LOG_FILE);

        assertFalse(log.contains(password));
        assertFalse(log.contains(passwordHash));
    }

    @Test
    void errorLoggingHandlesNullThrowable()
            throws Exception {

        AppLogger logger = new AppLogger();

        logger.error(
                "test.error.null",
                "context=test",
                null
        );

        String log = Files.readString(LOG_FILE);

        assertTrue(log.contains(
                "ERROR event=test.error.null"
        ));
        assertTrue(log.contains(
                "context=test"
        ));
        assertFalse(log.contains(
                "errorType="
        ));
    }
}
