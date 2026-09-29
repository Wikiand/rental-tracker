package com.kood.rentaltracker;

import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import com.kood.rentaltracker.repository.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void initializerCreatesExpectedMultiUserSchema()
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

        DatabaseInitializer.initialize();

        try (var connection = Database.connect()) {

            assertTrue(tableExists(connection, "users"));
            assertTrue(tableExists(connection, "listed_items"));
            assertTrue(tableExists(connection, "rentals"));

            Map<String, Integer> userColumns =
                    getColumnNotNullValues(connection, "users");

            assertEquals(1, userColumns.get("username"));
            assertEquals(1, userColumns.get("password_hash"));

            assertTrue(
                    foreignKeyExists(
                            connection,
                            "listed_items",
                            "users"
                    )
            );

            assertTrue(
                    foreignKeyExists(
                            connection,
                            "rentals",
                            "listed_items"
                    )
            );

            assertTrue(
                    foreignKeyExists(
                            connection,
                            "rentals",
                            "users"
                    )
            );
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


    @Test
    void databaseConnectionLifecycleIsLogged()
            throws Exception {

        database = Files.createTempFile(
                "rental-tracker-logging-",
                ".db"
        );

        Files.deleteIfExists(database);

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + database
        );

        Path logFile = Path.of("rental-tracker.log");
        Files.deleteIfExists(logFile);

        try (var connection = Database.connect()) {
            assertTrue(connection.isValid(1));
        }

        String log = Files.readString(logFile);

        assertTrue(
                log.contains(
                        "event=database.connection.opening"
                )
        );

        assertTrue(
                log.contains(
                        "event=database.connection.opened"
                )
        );

        assertTrue(
                log.contains(
                        "event=database.connection.closing"
                )
        );

        assertTrue(
                log.contains(
                        "event=database.connection.closed"
                )
        );

        assertTrue(
                log.contains(
                        "databaseType=sqlite"
                )
        );

        Files.deleteIfExists(logFile);
    }

    private boolean tableExists(
            java.sql.Connection connection,
            String tableName
    ) throws Exception {

        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master " +
                             "WHERE type='table' AND name='" +
                             tableName + "'"
             )) {

            return resultSet.next();
        }
    }

    private Map<String, Integer> getColumnNotNullValues(
            java.sql.Connection connection,
            String tableName
    ) throws Exception {

        Map<String, Integer> result =
                new HashMap<>();

        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "PRAGMA table_info(" + tableName + ")"
             )) {

            while (resultSet.next()) {
                result.put(
                        resultSet.getString("name"),
                        resultSet.getInt("notnull")
                );
            }
        }

        return result;
    }

    private boolean foreignKeyExists(
            java.sql.Connection connection,
            String tableName,
            String referencedTable
    ) throws Exception {

        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "PRAGMA foreign_key_list(" + tableName + ")"
             )) {

            while (resultSet.next()) {
                if (referencedTable.equals(
                        resultSet.getString("table")
                )) {
                    return true;
                }
            }
        }

        return false;
    }
}
