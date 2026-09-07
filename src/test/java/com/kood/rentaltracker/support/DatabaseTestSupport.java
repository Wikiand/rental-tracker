package com.kood.rentaltracker.support;

import com.kood.rentaltracker.infrastructure.DatabaseInitializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DatabaseTestSupport {

    private DatabaseTestSupport() {
    }

    public static Path createDatabase() throws IOException {
        Path database = Files.createTempFile(
                "rental-tracker-test-",
                ".db"
        );

        Files.deleteIfExists(database);

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + database
        );

        DatabaseInitializer.initialize();

        return database;
    }

    public static void cleanup(Path database)
            throws IOException {

        System.clearProperty(
                "rental.tracker.db.url"
        );

        Files.deleteIfExists(database);
    }
}
