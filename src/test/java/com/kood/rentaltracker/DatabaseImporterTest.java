
package com.kood.rentaltracker;

import com.kood.rentaltracker.importer.DatabaseImporter;
import com.kood.rentaltracker.importer.ImportResult;
import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.infrastructure.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseImporterTest {

    private Path databaseFile;
    private Path importDirectory;

    @BeforeEach
    void setUp() throws Exception {
        databaseFile = Files.createTempFile(
                "rental-tracker-import-test",
                ".db"
        );

        Files.deleteIfExists(databaseFile);

        System.setProperty(
                "rental.tracker.db.url",
                "jdbc:sqlite:" + databaseFile
        );

        DatabaseInitializer.initialize();

        importDirectory = Files.createTempDirectory(
                "rental-tracker-import"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
        );

        Files.writeString(
                importDirectory.resolve("rentals.csv"),
                "rental_id,item_id,renter_id,start_time,"
                        + "end_time,returned_at,status\n"
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("rental.tracker.db.url");

        Files.deleteIfExists(databaseFile);

        if (importDirectory != null) {
            try (var files = Files.walk(importDirectory)) {
                files.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                                // Cleanup only.
                            }
                        });
            }
        }
    }

    @Test
    void importsNewUser() throws Exception {
        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00"
        );

        ImportResult result =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(1, result.getInserted());
        assertEquals(0, result.getMatched());
        assertEquals(0, result.getRejected());
        assertTrue(userExists("alice"));
    }

    @Test
    void existingUserIsMatchedWithoutModification()
            throws Exception {

        insertUser(
                "alice",
                "original-hash",
                "2026-09-01T00:00:00"
        );

        writeUsersCsv(
                "50,alice,SHOULD_NOT_REPLACE,"
                        + "2099-01-01T00:00:00"
        );

        ImportResult result =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(0, result.getInserted());
        assertEquals(1, result.getMatched());
        assertEquals(0, result.getRejected());

        assertEquals(
                "original-hash",
                findPasswordHash("alice")
        );
    }

    @Test
    void repeatImportIsIdempotent() throws Exception {
        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00"
        );

        ImportResult first =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        ImportResult second =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(1, first.getInserted());
        assertEquals(0, first.getMatched());

        assertEquals(0, second.getInserted());
        assertEquals(1, second.getMatched());

        assertEquals(1, countUsers());
    }

    @Test
    void blankUsernameIsRejectedWhileValidUserImports()
            throws Exception {

        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00\n"
                        + "2,,missing-username,"
                        + "2026-09-01T00:00:00"
        );

        ImportResult result =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(1, result.getInserted());
        assertEquals(0, result.getMatched());
        assertEquals(1, result.getRejected());
        assertEquals(1, countUsers());
        assertTrue(userExists("alice"));
    }

    @Test
    void importsNewItemWithMappedOwner()
            throws Exception {

        writeUsersCsv(
                "1,alice,hash,"
                        + "2026-09-01T00:00:00"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,1,Camera,Test camera,10.0,"
                        + "available,2026-09-01T00:00:00\n"
        );

        ImportResult result =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(2, result.getInserted());
        assertEquals(0, result.getRejected());
        assertTrue(itemExists("Camera"));
    }

    @Test
    void existingItemIsMatchedWithoutModification()
            throws Exception {

        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,1,Camera,Original description,"
                        + "10.0,available,2026-09-01T00:00:00\n"
        );

        ImportResult first =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(2, first.getInserted());

        ItemSnapshot before = findItem("Camera");

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,1,Camera,Changed,99.0,"
                        + "rented,2099-01-01T00:00:00\n"
        );

        ImportResult second =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        ItemSnapshot after = findItem("Camera");

        assertEquals(2, second.getMatched());
        assertEquals(0, second.getInserted());

        assertEquals(before.ownerId(), after.ownerId());
        assertEquals(before.description(), after.description());
        assertEquals(before.costPerDay(), after.costPerDay());
        assertEquals(before.status(), after.status());
    }

    @Test
    void unresolvableItemOwnerIsRejected()
            throws Exception {

        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,999,Camera,Test camera,10.0,"
                        + "available,2026-09-01T00:00:00\n"
        );

        ImportResult result =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(1, result.getInserted());
        assertEquals(0, result.getMatched());
        assertEquals(1, result.getRejected());
        assertEquals(0, countItems());
    }

    @Test
    void unresolvableRentalIsRejectedWhileValidRentalImports()
            throws Exception {

        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,1,Camera,Test camera,10.0,"
                        + "available,2026-09-01T00:00:00\n"
        );

        Files.writeString(
                importDirectory.resolve("rentals.csv"),
                "rental_id,item_id,renter_id,start_time,"
                        + "end_time,returned_at,status\n"
                        + "1,10,1,2026-09-10 10:00:00,"
                        + "2026-09-12 10:00:00,,active\n"
                        + "2,999,999,2026-09-15 10:00:00,"
                        + "2026-09-17 10:00:00,,active\n"
        );

        ImportResult result =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(3, result.getInserted());
        assertEquals(0, result.getMatched());
        assertEquals(1, result.getRejected());
        assertEquals(1, countRentals());
    }

    @Test
    void repeatRentalImportIsIdempotent()
            throws Exception {

        writeUsersCsv(
                "1,alice,hash,2026-09-01T00:00:00"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,1,Camera,Test camera,10.0,"
                        + "available,2026-09-01T00:00:00\n"
        );

        Files.writeString(
                importDirectory.resolve("rentals.csv"),
                "rental_id,item_id,renter_id,start_time,"
                        + "end_time,returned_at,status\n"
                        + "1,10,1,2026-09-10 10:00:00,"
                        + "2026-09-12 10:00:00,,active\n"
        );

        ImportResult first =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        ImportResult second =
                new DatabaseImporter()
                        .importDirectory(importDirectory);

        assertEquals(3, first.getInserted());
        assertEquals(0, first.getMatched());

        assertEquals(0, second.getInserted());
        assertEquals(3, second.getMatched());

        assertEquals(1, countRentals());
    }

    @Test
    void failedImportRollsBackAllChanges()
            throws Exception {

        writeUsersCsv(
                "1,rollback-user,hash,"
                        + "2026-09-01T00:00:00"
        );

        Files.writeString(
                importDirectory.resolve("listed_items.csv"),
                "item_id,owner_id,item_name,description,"
                        + "cost_per_day,status,created_at\n"
                        + "10,1,Rollback Item,Test item,10.0,"
                        + "available,2026-09-01T00:00:00\n"
        );

        Files.writeString(
                importDirectory.resolve("rentals.csv"),
                "rental_id,item_id,renter_id,start_time,"
                        + "end_time,returned_at,status\n"
                        + "1,10,1,2026-09-10 10:00:00,"
                        + "2026-09-12 10:00:00,,active\n"
        );

        DatabaseImporter importer =
                new DatabaseImporter(() -> {
                    throw new RuntimeException("forced failure");
                });

        assertThrows(
                RuntimeException.class,
                () -> importer.importDirectory(importDirectory)
        );

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT COUNT(*) FROM users "
                                     + "WHERE username = ?"
                     )) {

            statement.setString(1, "rollback-user");

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1));
            }
        }

        assertEquals(0, countItems());
        assertEquals(0, countRentals());
    }

    private void writeUsersCsv(String rows)
            throws Exception {

        Files.writeString(
                importDirectory.resolve("users.csv"),
                "id,username,password_hash,created_at\n"
                        + rows + "\n"
        );
    }

    private void insertUser(
            String username,
            String passwordHash,
            String createdAt
    ) throws Exception {

        try (var connection = Database.connect();
             var statement = connection.prepareStatement(
                     "INSERT INTO users "
                             + "(username, password_hash, created_at) "
                             + "VALUES (?, ?, ?)"
             )) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, createdAt);
            statement.executeUpdate();
        }
    }

    private boolean userExists(String username) {
        return countUsers(username) == 1;
    }

    private boolean itemExists(String itemName) {
        try (var connection = Database.connect();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM listed_items "
                             + "WHERE item_name = ?"
             )) {

            statement.setString(1, itemName);

            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getInt(1) == 1;
            }

        } catch (Exception e) {
            fail(e);
            return false;
        }
    }

    private int countUsers() {
        return countUsers(null);
    }

    private int countUsers(String username) {
        String sql =
                username == null
                        ? "SELECT COUNT(*) FROM users"
                        : "SELECT COUNT(*) FROM users WHERE username = ?";

        try (var connection = Database.connect();
             var statement = connection.prepareStatement(sql)) {

            if (username != null) {
                statement.setString(1, username);
            }

            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getInt(1);
            }

        } catch (Exception e) {
            fail(e);
            return -1;
        }
    }

    private int countItems() {
        try (var connection = Database.connect();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM listed_items"
             );
             var resultSet = statement.executeQuery()) {

            assertTrue(resultSet.next());
            return resultSet.getInt(1);

        } catch (Exception e) {
            fail(e);
            return -1;
        }
    }

    private int countRentals() {
        try (var connection = Database.connect();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM rentals"
             );
             var resultSet = statement.executeQuery()) {

            assertTrue(resultSet.next());
            return resultSet.getInt(1);

        } catch (Exception e) {
            fail(e);
            return -1;
        }
    }

    private ItemSnapshot findItem(String itemName) {
        try (var connection = Database.connect();
             var statement = connection.prepareStatement(
                     "SELECT owner_id, description, "
                             + "cost_per_day, status "
                             + "FROM listed_items "
                             + "WHERE item_name = ?"
             )) {

            statement.setString(1, itemName);

            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());

                return new ItemSnapshot(
                        resultSet.getInt("owner_id"),
                        resultSet.getString("description"),
                        resultSet.getDouble("cost_per_day"),
                        resultSet.getString("status")
                );
            }

        } catch (Exception e) {
            fail(e);
            return null;
        }
    }

    private String findPasswordHash(String username) {
        try (var connection = Database.connect();
             var statement = connection.prepareStatement(
                     "SELECT password_hash FROM users "
                             + "WHERE username = ?"
             )) {

            statement.setString(1, username);

            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }

        } catch (Exception e) {
            fail(e);
            return null;
        }
    }

    private record ItemSnapshot(
            int ownerId,
            String description,
            double costPerDay,
            String status
    ) {}
}
