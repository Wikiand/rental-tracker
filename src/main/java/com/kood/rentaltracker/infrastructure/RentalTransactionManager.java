package com.kood.rentaltracker.infrastructure;

import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;

import java.sql.Connection;
import java.sql.SQLException;

public final class RentalTransactionManager {

    @FunctionalInterface
    public interface Transaction<T> {
        T execute(Connection connection) throws SQLException;
    }

    public <T> T execute(Transaction<T> transaction) {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                T result = transaction.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }

                if (exception instanceof SQLException sqlException) {
                    throw DatabaseExceptionMapper.map(sqlException);
                }

                throw exception;
            }
        } catch (SQLException exception) {
            throw DatabaseExceptionMapper.map(exception);
        }
    }
}
