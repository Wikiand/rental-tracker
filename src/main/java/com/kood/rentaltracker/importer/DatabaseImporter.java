package com.kood.rentaltracker.importer;

import com.kood.rentaltracker.infrastructure.Database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DatabaseImporter {

    private final CsvReader csvReader;
    private final Runnable failureHook;

    public DatabaseImporter() {
        this(() -> {});
    }

    public DatabaseImporter(Runnable failureHook) {
        this.csvReader = new CsvReader();
        this.failureHook = failureHook;
    }

    public ImportResult importDirectory(Path directory) {
        try (Connection connection = Database.connect()) {
            boolean previousAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                ImportResult result = new ImportResult();
                ImportContext context = new ImportContext();

                importUsers(
                        connection,
                        directory.resolve("users.csv"),
                        context,
                        result
                );

                failureHook.run();

                importItems(
                        connection,
                        directory.resolve("listed_items.csv"),
                        context,
                        result
                );

                importRentals(
                        connection,
                        directory.resolve("rentals.csv"),
                        context,
                        result
                );

                connection.commit();

                return result;

            } catch (Exception e) {
                connection.rollback();

                throw new RuntimeException(
                        "Import failed",
                        e
                );

            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Import failed",
                    e
            );
        }
    }

    private void importUsers(
            Connection connection,
            Path file,
            ImportContext context,
            ImportResult result
    ) throws Exception {

        requireFile(file);

        List<String[]> rows = csvReader.read(file);

        if (rows.isEmpty()) {
            return;
        }

        requireHeader(
                rows.get(0),
                "id",
                "username",
                "password_hash",
                "created_at"
        );

        for (int i = 1; i < rows.size(); i++) {
            try {
                String[] row = rows.get(i);

                if (row.length != 4) {
                    throw new IllegalArgumentException(
                            "invalid record"
                    );
                }

                // READ
                int sourceId =
                        parseRequiredInt(row[0], "id");

                String username =
                        parseRequiredText(row[1], "username");

                // MATCHED OR NEW
                Integer localId =
                        findUserId(connection, username);

                if (localId != null) {
                    context.mapUser(sourceId, localId);
                    result.incrementMatched();
                    continue;
                }

                // VALIDATE NEW RECORD
                String passwordHash =
                        parseRequiredText(
                                row[2],
                                "password_hash"
                        );

                String createdAt =
                        parseRequiredText(
                                row[3],
                                "created_at"
                        );

                // INSERT
                localId = insertUser(
                        connection,
                        username,
                        passwordHash,
                        createdAt
                );

                context.mapUser(sourceId, localId);
                result.incrementInserted();

            } catch (IllegalArgumentException e) {
                result.addRejection(
                        file.getFileName().toString(),
                        i + 1,
                        e.getMessage() == null
                                ? "invalid record"
                                : e.getMessage()
                );
            }
        }
    }

    private void importItems(
            Connection connection,
            Path file,
            ImportContext context,
            ImportResult result
    ) throws Exception {

        requireFile(file);

        List<String[]> rows = csvReader.read(file);

        if (rows.isEmpty()) {
            return;
        }

        requireHeader(
                rows.get(0),
                "item_id",
                "owner_id",
                "item_name",
                "description",
                "cost_per_day",
                "status",
                "created_at"
        );

        for (int i = 1; i < rows.size(); i++) {
            try {
                String[] row = rows.get(i);

                if (row.length != 7) {
                    throw new IllegalArgumentException(
                            "invalid record"
                    );
                }

                // READ
                int sourceItemId =
                        parseRequiredInt(row[0], "item_id");

                int sourceOwnerId =
                        parseRequiredInt(row[1], "owner_id");

                Integer localOwnerId =
                        context.resolveUser(sourceOwnerId);

                if (localOwnerId == null) {
                    result.addRejection(
                            file.getFileName().toString(),
                            i + 1,
                            "owner_id could not be resolved"
                    );
                    continue;
                }

                String itemName =
                        parseRequiredText(
                                row[2],
                                "item_name"
                        );

                // MATCHED OR NEW
                Integer localItemId =
                        findItemId(
                                connection,
                                localOwnerId,
                                itemName
                        );

                if (localItemId != null) {
                    context.mapItem(
                            sourceItemId,
                            localItemId
                    );
                    result.incrementMatched();
                    continue;
                }

                // VALIDATE NEW RECORD
                String description =
                        parseRequiredText(
                                row[3],
                                "description"
                        );

                double costPerDay =
                        parseRequiredDouble(
                                row[4],
                                "cost_per_day"
                        );

                String status =
                        parseRequiredText(
                                row[5],
                                "status"
                        );

                String createdAt =
                        parseRequiredText(
                                row[6],
                                "created_at"
                        );

                // INSERT
                localItemId = insertItem(
                        connection,
                        localOwnerId,
                        itemName,
                        description,
                        costPerDay,
                        status,
                        createdAt
                );

                context.mapItem(
                        sourceItemId,
                        localItemId
                );

                result.incrementInserted();

            } catch (IllegalArgumentException e) {
                result.addRejection(
                        file.getFileName().toString(),
                        i + 1,
                        e.getMessage() == null
                                ? "invalid record"
                                : e.getMessage()
                );
            }
        }
    }

    private void importRentals(
            Connection connection,
            Path file,
            ImportContext context,
            ImportResult result
    ) throws Exception {

        requireFile(file);

        List<String[]> rows = csvReader.read(file);

        if (rows.isEmpty()) {
            return;
        }

        requireHeader(
                rows.get(0),
                "rental_id",
                "item_id",
                "renter_id",
                "start_time",
                "end_time",
                "returned_at",
                "status"
        );

        for (int i = 1; i < rows.size(); i++) {
            try {
                String[] row = rows.get(i);

                if (row.length != 7) {
                    throw new IllegalArgumentException(
                            "invalid record"
                    );
                }

                // READ
                int sourceRentalId =
                        parseRequiredInt(
                                row[0],
                                "rental_id"
                        );

                int sourceItemId =
                        parseRequiredInt(
                                row[1],
                                "item_id"
                        );

                int sourceRenterId =
                        parseRequiredInt(
                                row[2],
                                "renter_id"
                        );

                Integer localItemId =
                        context.resolveItem(sourceItemId);

                Integer localRenterId =
                        context.resolveUser(sourceRenterId);

                if (localItemId == null
                        || localRenterId == null) {
                    result.addRejection(
                            file.getFileName().toString(),
                            i + 1,
                            "item_id and renter_id could not be resolved"
                    );
                    continue;
                }

                String startTime =
                        parseRequiredText(
                                row[3],
                                "start_time"
                        );

                String endTime =
                        parseRequiredText(
                                row[4],
                                "end_time"
                        );

                // MATCHED OR NEW
                Integer localRentalId =
                        findRentalId(
                                connection,
                                localItemId,
                                localRenterId,
                                startTime,
                                endTime
                        );

                if (localRentalId != null) {
                    result.incrementMatched();
                    continue;
                }

                // VALIDATE NEW RECORD
                String returnedAt =
                        parseOptionalText(row[5]);

                String status =
                        parseRequiredText(
                                row[6],
                                "status"
                        );

                // INSERT
                insertRental(
                        connection,
                        localItemId,
                        localRenterId,
                        startTime,
                        endTime,
                        returnedAt,
                        status
                );

                result.incrementInserted();

            } catch (IllegalArgumentException e) {
                result.addRejection(
                        file.getFileName().toString(),
                        i + 1,
                        e.getMessage() == null
                                ? "invalid record"
                                : e.getMessage()
                );
            }
        }
    }

    private Integer findUserId(
            Connection connection,
            String username
    ) throws SQLException {

        String sql =
                "SELECT id FROM users WHERE username = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next()
                        ? resultSet.getInt("id")
                        : null;
            }
        }
    }

    private int insertUser(
            Connection connection,
            String username,
            String passwordHash,
            String createdAt
    ) throws SQLException {

        String sql =
                "INSERT INTO users "
                        + "(username, password_hash, created_at) "
                        + "VALUES (?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, createdAt);

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (!keys.next()) {
                    throw new SQLException(
                            "No generated user ID"
                    );
                }

                return keys.getInt(1);
            }
        }
    }

    private Integer findItemId(
            Connection connection,
            int ownerId,
            String itemName
    ) throws SQLException {

        String sql =
                "SELECT item_id FROM listed_items "
                        + "WHERE owner_id = ? "
                        + "AND item_name = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, ownerId);
            statement.setString(2, itemName);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next()
                        ? resultSet.getInt("item_id")
                        : null;
            }
        }
    }

    private int insertItem(
            Connection connection,
            int ownerId,
            String itemName,
            String description,
            double costPerDay,
            String status,
            String createdAt
    ) throws SQLException {

        String sql =
                "INSERT INTO listed_items "
                        + "(owner_id, item_name, description, "
                        + "cost_per_day, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setInt(1, ownerId);
            statement.setString(2, itemName);
            statement.setString(3, description);
            statement.setDouble(4, costPerDay);
            statement.setString(5, status);
            statement.setString(6, createdAt);

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (!keys.next()) {
                    throw new SQLException(
                            "No generated item ID"
                    );
                }

                return keys.getInt(1);
            }
        }
    }

    private Integer findRentalId(
            Connection connection,
            int itemId,
            int renterId,
            String startTime,
            String endTime
    ) throws SQLException {

        String sql =
                "SELECT rental_id FROM rentals "
                        + "WHERE item_id = ? "
                        + "AND renter_id = ? "
                        + "AND start_time = ? "
                        + "AND end_time = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, itemId);
            statement.setInt(2, renterId);
            statement.setString(3, startTime);
            statement.setString(4, endTime);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next()
                        ? resultSet.getInt("rental_id")
                        : null;
            }
        }
    }

    private void insertRental(
            Connection connection,
            int itemId,
            int renterId,
            String startTime,
            String endTime,
            String returnedAt,
            String status
    ) throws SQLException {

        String sql =
                "INSERT INTO rentals "
                        + "(item_id, renter_id, start_time, "
                        + "end_time, returned_at, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, itemId);
            statement.setInt(2, renterId);
            statement.setString(3, startTime);
            statement.setString(4, endTime);

            if (returnedAt == null) {
                statement.setNull(
                        5,
                        java.sql.Types.VARCHAR
                );
            } else {
                statement.setString(5, returnedAt);
            }

            statement.setString(6, status);

            statement.executeUpdate();
        }
    }

    private int parseRequiredInt(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    field + " must be an integer"
            );
        }
    }

    private double parseRequiredDouble(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    field + " must be a number"
            );
        }
    }

    private String parseRequiredText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        return value;
    }

    private String parseOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private void requireFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException(
                    "Missing " + file.getFileName()
            );
        }
    }

    private void requireHeader(
            String[] actual,
            String... expected
    ) {

        if (actual.length != expected.length) {
            throw new IllegalArgumentException(
                    "Invalid CSV header"
            );
        }

        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(actual[i])) {
                throw new IllegalArgumentException(
                        "Invalid CSV header"
                );
            }
        }
    }
}
