package com.kood.rentaltracker;

import com.kood.rentaltracker.repository.exception.ConstraintViolationException;
import com.kood.rentaltracker.repository.exception.DatabaseExceptionMapper;
import com.kood.rentaltracker.repository.exception.DatabaseOpenException;
import com.kood.rentaltracker.repository.exception.MappingException;
import com.kood.rentaltracker.repository.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void mappingExceptionStoresMessageAndCause() {
        Throwable cause =
                new RuntimeException("mapping failed");

        MappingException exception =
                new MappingException(
                        "Mapping error",
                        cause
                );

        assertEquals(
                "Mapping error",
                exception.getMessage()
        );

        assertSame(
                cause,
                exception.getCause()
        );
    }

    @Test
    void databaseOpenExceptionStoresMessageAndCause() {
        Throwable cause =
                new SQLException("connection failed");

        DatabaseOpenException exception =
                new DatabaseOpenException(
                        "Cannot open database",
                        cause
                );

        assertEquals(
                "Cannot open database",
                exception.getMessage()
        );

        assertSame(
                cause,
                exception.getCause()
        );
    }

    @Test
    void notFoundExceptionStoresMessage() {
        NotFoundException exception =
                new NotFoundException(
                        "Item not found"
                );

        assertEquals(
                "Item not found",
                exception.getMessage()
        );
    }

    @Test
    void mapperHandlesUniqueConstraint() {
        RuntimeException exception =
                DatabaseExceptionMapper.map(
                        new SQLException(
                                "UNIQUE constraint failed"
                        )
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                exception
        );
    }

    @Test
    void mapperHandlesNotNullConstraint() {
        RuntimeException exception =
                DatabaseExceptionMapper.map(
                        new SQLException(
                                "NOT NULL constraint failed"
                        )
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                exception
        );
    }

    @Test
    void mapperHandlesCheckConstraint() {
        RuntimeException exception =
                DatabaseExceptionMapper.map(
                        new SQLException(
                                "CHECK constraint failed"
                        )
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                exception
        );
    }

    @Test
    void mapperHandlesForeignKeyConstraint() {
        RuntimeException exception =
                DatabaseExceptionMapper.map(
                        new SQLException(
                                "FOREIGN KEY constraint failed"
                        )
                );

        assertInstanceOf(
                ConstraintViolationException.class,
                exception
        );
    }

    @Test
    void mapperHandlesGeneralDatabaseError() {
        RuntimeException exception =
                DatabaseExceptionMapper.map(
                        new SQLException(
                                "database is unavailable"
                        )
                );

        assertEquals(
                "Database error",
                exception.getMessage()
        );

        assertEquals(
                "database is unavailable",
                exception.getCause().getMessage()
        );
    }

    @Test
    void constraintViolationExceptionStoresMessageAndCause() {
        Throwable cause =
                new SQLException(
                        "UNIQUE constraint failed"
                );

        ConstraintViolationException exception =
                new ConstraintViolationException(
                        "Database constraint violation",
                        cause
                );

        assertEquals(
                "Database constraint violation",
                exception.getMessage()
        );

        assertSame(
                cause,
                exception.getCause()
        );
    }
}