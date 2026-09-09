package com.kood.rentaltracker;

import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseInitializerTest {

    private Path database;

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("rental.tracker.db.url");

        if (database != null) {
            Files.deleteIfExists(database);
        }
    }

    @Test
    void initializerMapsSqlFailureToConstraintViolation()
            throws Exception {

        database = Files.createTempFile(
                "rental-tracker-init-",
                ".db"
        );

        Files.deleteIfExists(database);

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + database
        );

        try (var connection = Database.connect();
             var statement = connection.createStatement()) {

            statement.execute(
                    "CREATE TABLE placeholder (id INTEGER)"
            );

            statement.execute(
                    "CREATE INDEX users ON placeholder(id)"
            );
        }

        assertThrows(
                ConstraintViolationException.class,
                DatabaseInitializer::initialize
        );
    }
}
