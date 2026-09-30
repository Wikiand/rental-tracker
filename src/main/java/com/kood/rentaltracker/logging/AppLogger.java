package com.kood.rentaltracker.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class AppLogger {

    private static final Path LOG_FILE =
            resolveLogFile();

    private static Path resolveLogFile() {
        String configuredPath =
                System.getenv("RENTAL_TRACKER_LOG_PATH");

        if (configuredPath == null || configuredPath.isBlank()) {
            return Path.of("rental-tracker.log");
        }

        return Path.of(configuredPath);
    }

    public void info(
            String event,
            String context
    ) {
        write("INFO", event, context);
    }

    public void warn(
            String event,
            String context
    ) {
        write("WARN", event, context);
    }

    public void error(
            String event,
            String context,
            Throwable throwable
    ) {
        String detail =
                throwable == null
                        ? ""
                        : " errorType="
                        + throwable.getClass().getSimpleName();

        write(
                "ERROR",
                event,
                context + detail
        );
    }

    private void write(
            String level,
            String event,
            String context
    ) {
        String timestamp =
                OffsetDateTime
                        .now(ZoneOffset.UTC)
                        .toString();

        String line =
                timestamp
                        + " "
                        + level
                        + " event="
                        + event
                        + " "
                        + context
                        + System.lineSeparator();

        try {
            Files.writeString(
                    LOG_FILE,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            /*
             * Logging must never crash the application.
             */
        }
    }
}
