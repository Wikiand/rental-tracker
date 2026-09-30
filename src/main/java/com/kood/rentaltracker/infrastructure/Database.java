package com.kood.rentaltracker.infrastructure;

import com.kood.rentaltracker.logging.AppLogger;
import com.kood.rentaltracker.repository.exception.DatabaseOpenException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String DEFAULT_URL =
            "jdbc:sqlite:rental-tracker.db";

    private static final AppLogger LOGGER =
            new AppLogger();

    private Database() {
    }

    public static Connection connect() {
        String url = System.getenv(
                "RENTAL_TRACKER_DB_URL"
        );

        if (url == null || url.isBlank()) {
            url = System.getProperty(
                    "rental.tracker.db.url",
                    DEFAULT_URL
            );
        }

        LOGGER.info(
                "database.connection.opening",
                "url=" + sanitizeUrl(url)
        );

        try {
            Connection connection =
                    DriverManager.getConnection(url);

            try (Statement statement =
                         connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }

            LOGGER.info(
                    "database.connection.opened",
                    "databaseType=sqlite"
            );

            return createLoggingConnection(connection);

        } catch (SQLException e) {
            LOGGER.error(
                    "database.connection.failed",
                    "databaseType=sqlite",
                    e
            );

            throw new DatabaseOpenException(
                    "Cannot open database",
                    e
            );
        }
    }

    private static Connection createLoggingConnection(
            Connection connection
    ) {
        InvocationHandler handler = new ConnectionCloseLogger(
                connection
        );

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "unknown";
        }

        return url.replace(
                System.lineSeparator(),
                ""
        );
    }

    private static final class ConnectionCloseLogger
            implements InvocationHandler {

        private final Connection delegate;

        private ConnectionCloseLogger(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] args
        ) throws Throwable {

            if ("close".equals(method.getName())
                    && method.getParameterCount() == 0) {

                LOGGER.info(
                        "database.connection.closing",
                        "databaseType=sqlite"
                );

                try {
                    Object result =
                            method.invoke(delegate, args);

                    LOGGER.info(
                            "database.connection.closed",
                            "databaseType=sqlite"
                    );

                    return result;

                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();

                    LOGGER.error(
                            "database.connection.close.failed",
                            "databaseType=sqlite",
                            cause
                    );

                    throw cause;
                }
            }

            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
