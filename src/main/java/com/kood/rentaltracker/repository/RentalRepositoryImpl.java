package com.kood.rentaltracker.repository;

import com.kood.rentaltracker.domain.Rental;
import com.kood.rentaltracker.domain.RentalStatus;
import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RentalRepositoryImpl implements RentalRepository {

    @Override
    public Rental save(Rental rental) {
        String sql = """
                INSERT INTO rentals
                (item_id, renter_id, start_time, end_time, returned_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, rental.getItemId());
            statement.setInt(2, rental.getRenterId());
            statement.setString(3, rental.getStartTime().toString());
            statement.setString(4, rental.getEndTime().toString());

            if (rental.getReturnedAt() == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(
                        5,
                        rental.getReturnedAt().toString()
                );
            }

            statement.setString(
                    6,
                    rental.getStatus().name().toLowerCase()
            );

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Rental(
                            keys.getInt(1),
                            rental.getItemId(),
                            rental.getRenterId(),
                            rental.getStartTime(),
                            rental.getEndTime(),
                            rental.getReturnedAt(),
                            rental.getStatus()
                    );
                }
            }

            throw new RuntimeException("Failed to create rental");

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public Optional<Rental> findById(int id) {
        String sql = """
                SELECT rental_id,
                       item_id,
                       renter_id,
                       start_time,
                       end_time,
                       returned_at,
                       status
                FROM rentals
                WHERE rental_id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRental(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public List<Rental> findActiveByOwnerId(int ownerId) {
        String sql = """
                SELECT r.rental_id,
                       r.item_id,
                       r.renter_id,
                       r.start_time,
                       r.end_time,
                       r.returned_at,
                       r.status
                FROM rentals r
                JOIN listed_items i
                    ON r.item_id = i.item_id
                WHERE i.owner_id = ?
                  AND r.status = 'active'
                ORDER BY r.end_time
                """;

        return findRentals(sql, ownerId);
    }

    @Override
    public List<Rental> findByOwnerId(int ownerId) {
        String sql = """
                SELECT r.rental_id,
                       r.item_id,
                       r.renter_id,
                       r.start_time,
                       r.end_time,
                       r.returned_at,
                       r.status
                FROM rentals r
                JOIN listed_items i
                    ON r.item_id = i.item_id
                WHERE i.owner_id = ?
                ORDER BY r.start_time DESC
                """;

        return findRentals(sql, ownerId);
    }

    @Override
    public void closeRental(int rentalId, String returnedAt) {
        String sql = """
                UPDATE rentals
                SET returned_at = ?,
                    status = 'closed'
                WHERE rental_id = ?
                  AND status = 'active'
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, returnedAt);
            statement.setInt(2, rentalId);

            int updated = statement.executeUpdate();

            if (updated == 0) {
                throw new RuntimeException(
                        "Active rental not found: " + rentalId
                );
            }

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    private List<Rental> findRentals(
            String sql,
            int ownerId
    ) {
        List<Rental> rentals = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ownerId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rentals.add(mapRental(rs));
                }
            }

            return rentals;

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    private Rental mapRental(ResultSet rs) throws SQLException {
        LocalDateTime startTime =
                LocalDateTime.parse(rs.getString("start_time"));

        LocalDateTime endTime =
                LocalDateTime.parse(rs.getString("end_time"));

        String returnedValue =
                rs.getString("returned_at");

        LocalDateTime returnedAt =
                returnedValue == null
                        ? null
                        : LocalDateTime.parse(returnedValue);

        RentalStatus status = RentalStatus.valueOf(
                rs.getString("status").toUpperCase()
        );

        return new Rental(
                rs.getInt("rental_id"),
                rs.getInt("item_id"),
                rs.getInt("renter_id"),
                startTime,
                endTime,
                returnedAt,
                status
        );
    }
}