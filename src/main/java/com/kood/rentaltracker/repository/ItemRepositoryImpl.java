package com.kood.rentaltracker.repository;

import com.kood.rentaltracker.domain.Item;
import com.kood.rentaltracker.domain.ItemStatus;
import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemRepositoryImpl implements ItemRepository {

    @Override
    public Item save(Item item) {
        String sql = """
                INSERT INTO listed_items
                (owner_id, item_name, description, cost_per_day, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setInt(1, item.getOwnerId());
            statement.setString(2, item.getName());
            statement.setString(3, item.getDescription());
            statement.setDouble(4, item.getCostPerDay());
            statement.setString(
                    5,
                    item.getStatus().name().toLowerCase()
            );

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1))
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Failed to load created item"
                                    )
                            );
                }
            }

            throw new RuntimeException(
                    "Failed to create item"
            );

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public Optional<Item> findById(int id) {
        String sql = """
                SELECT item_id,
                       owner_id,
                       item_name,
                       description,
                       cost_per_day,
                       status,
                       created_at
                FROM listed_items
                WHERE item_id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapItem(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public List<Item> findByOwnerId(int ownerId) {
        String sql = """
                SELECT item_id,
                       owner_id,
                       item_name,
                       description,
                       cost_per_day,
                       status,
                       created_at
                FROM listed_items
                WHERE owner_id = ?
                ORDER BY item_id
                """;

        return findItems(sql, ownerId);
    }

    @Override
    public List<Item> findByOwnerIdAndStatus(
            int ownerId,
            ItemStatus status
    ) {
        String sql = """
                SELECT item_id,
                       owner_id,
                       item_name,
                       description,
                       cost_per_day,
                       status,
                       created_at
                FROM listed_items
                WHERE owner_id = ?
                  AND status = ?
                ORDER BY item_id
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, ownerId);
            statement.setString(
                    2,
                    status.name().toLowerCase()
            );

            List<Item> items = new ArrayList<>();

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
            }

            return items;

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public void updateStatus(
            int itemId,
            ItemStatus status
    ) {
        updateStatusValue(
                itemId,
                status.name().toLowerCase()
        );
    }

    @Override
    public void updateStatusValue(
            int itemId,
            String status
    ) {
        String sql = """
                UPDATE listed_items
                SET status = ?
                WHERE item_id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, itemId);

            int updated = statement.executeUpdate();

            if (updated == 0) {
                throw new RuntimeException(
                        "Item not found: " + itemId
                );
            }

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    private List<Item> findItems(
            String sql,
            int ownerId
    ) {
        List<Item> items = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, ownerId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
            }

            return items;

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        ItemStatus status = ItemStatus.valueOf(
                rs.getString("status").toUpperCase()
        );

        LocalDateTime listingDate =
                parseDateTime(
                        rs.getString("created_at")
                );

        return new Item(
                rs.getInt("item_id"),
                rs.getInt("owner_id"),
                rs.getString("item_name"),
                rs.getString("description"),
                rs.getDouble("cost_per_day"),
                listingDate,
                status
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(
                value.replace(' ', 'T')
        );
    }
}