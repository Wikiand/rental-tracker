package com.kood.rentaltracker.repository;

import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.infrastructure.Database;
import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users (username, password)
                VALUES (?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getUsername());
            statement.setNull(2, Types.VARCHAR);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(
                            keys.getInt(1),
                            user.getUsername()
                    );
                }
            }

            throw new RuntimeException("Failed to create user");

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = """
                SELECT id, username
                FROM users
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(
                            new User(
                                    rs.getInt("id"),
                                    rs.getString("username")
                            )
                    );
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT id, username
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(
                            new User(
                                    rs.getInt("id"),
                                    rs.getString("username")
                            )
                    );
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }

    @Override
    public Optional<User> findOwner() {
        String sql = """
                SELECT id, username
                FROM users
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return Optional.of(
                        new User(
                                rs.getInt("id"),
                                rs.getString("username")
                        )
                );
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw DatabaseExceptionMapper.map(e);
        }
    }
}