package com.kood.rentaltracker;

import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.repository.exception.DatabaseOpenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionTest {

    @Test
    void databaseConnectionFailureThrowsTypedException() {
        String originalUrl = System.getProperty(
                "rental.tracker.db.url"
        );

        try {
            System.setProperty(
                    "rental.tracker.db.url",
                    "jdbc:sqlite:/this/path/does/not/exist/database.db"
            );

            DatabaseOpenException exception =
                    assertThrows(
                            DatabaseOpenException.class,
                            Database::connect
                    );

            assertEquals(
                    "Cannot open database",
                    exception.getMessage()
            );

        } finally {
            if (originalUrl == null) {
                System.clearProperty(
                        "rental.tracker.db.url"
                );
            } else {
                System.setProperty(
                        "rental.tracker.db.url",
                        originalUrl
                );
            }
        }
    }
}