package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging messages to a file and console with different log levels. Supports
 * INFO, WARN, and ERROR levels with optional filtering. Supports log rotation when the log file
 * grows too large.
 */
public class ErrorLogger {
    /**
     * Enumeration of available log levels.
     */
    public enum LogLevel {
        INFO, WARN, ERROR
    }

    private static final String LOG_FILE = "error.log";
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_ROTATED_LOGS = 5; // Keep up to 5 rotated log files
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Minimum log level - only messages at or above this level will be logged
    private static LogLevel minimumLogLevel = LogLevel.INFO;

    /**
     * Sets the minimum log level. Messages below this level will not be logged.
     * 
     * @param level The minimum log level (INFO, WARN, or ERROR)
     */
    public static synchronized void setMinimumLogLevel(LogLevel level) {
        if (level != null) {
            minimumLogLevel = level;
        }
    }

    /**
     * Gets the current minimum log level.
     * 
     * @return The current minimum log level
     */
    public static LogLevel getMinimumLogLevel() {
        return minimumLogLevel;
    }

    /**
     * Checks if a log level should be logged based on the minimum log level setting.
     * 
     * @param level The log level to check
     * @return true if the level should be logged, false otherwise
     */
    private static boolean shouldLog(LogLevel level) {
        if (level == null) {
            return false;
        }

        // Order: INFO < WARN < ERROR
        switch (minimumLogLevel) {
            case INFO:
                return true; // Log everything
            case WARN:
                return level == LogLevel.WARN || level == LogLevel.ERROR;
            case ERROR:
                return level == LogLevel.ERROR;
            default:
                return true;
        }
    }

    /**
     * Checks if the log file needs rotation and performs it if necessary. Rotates logs by renaming
     * them with numbered suffixes (error.log.1, error.log.2, etc.)
     */
    private static void rotateLogIfNeeded() {
        try {
            File logFile = new File(LOG_FILE);

            // Check if file exists and exceeds size limit
            if (logFile.exists() && logFile.length() >= MAX_LOG_SIZE) {
                // Rotate existing logs: error.log.4 -> error.log.5, error.log.3 -> error.log.4,
                // etc.
                for (int i = MAX_ROTATED_LOGS - 1; i >= 1; i--) {
                    File oldLog = new File(LOG_FILE + "." + i);
                    File newLog = new File(LOG_FILE + "." + (i + 1));

                    if (oldLog.exists()) {
                        if (i == MAX_ROTATED_LOGS - 1) {
                            // Delete the oldest rotated log if we've reached the limit
                            oldLog.delete();
                        } else {
                            // Move to next number
                            Files.move(oldLog.toPath(), newLog.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

                // Move current log to error.log.1
                File rotatedLog = new File(LOG_FILE + ".1");
                Files.move(logFile.toPath(), rotatedLog.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // If rotation fails, log to stderr but don't throw - we'll still try to write to the
            // log
            String timestamp = LocalDateTime.now().format(FORMATTER);
            System.err.println(
                    "[" + timestamp + "] WARNING: Failed to rotate log file: " + e.getMessage());
        }
    }

    /**
     * Logs a message with the specified log level and optional exception.
     * 
     * @param level The log level (INFO, WARN, or ERROR)
     * @param message The message to log
     * @param throwable The exception associated with the message (can be null)
     */
    private static synchronized void log(LogLevel level, String message, Throwable throwable) {
        if (!shouldLog(level)) {
            return;
        }

        // Check and rotate log if needed before writing
        rotateLogIfNeeded();

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(
                    "[" + LocalDateTime.now().format(FORMATTER) + "] " + level + ": " + message);
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
            writer.println("---");
        } catch (IOException e) {
            // Fallback to stderr if file writing fails
            String timestamp = LocalDateTime.now().format(FORMATTER);
            System.err.println("[" + timestamp + "] " + level + ": Failed to write to error log: "
                    + e.getMessage());
            System.err.println("[" + timestamp + "] " + level + ": Original message: " + message);
            if (throwable != null) {
                System.err.println("[" + timestamp + "] " + level + ": Stack trace:");
                throwable.printStackTrace(System.err);
            }
            System.err.println("[" + timestamp + "] Log file write failure stack trace:");
            e.printStackTrace(System.err);
            System.err.println("---");
        }
    }

    /**
     * Logs an INFO level message.
     * 
     * @param message The message to log
     */
    public static void logInfo(String message) {
        log(LogLevel.INFO, message, null);
    }

    /**
     * Logs an INFO level message with an associated exception.
     * 
     * @param message The message to log
     * @param throwable The exception associated with the message (can be null)
     */
    public static void logInfo(String message, Throwable throwable) {
        log(LogLevel.INFO, message, throwable);
    }

    /**
     * Logs a WARN level message.
     * 
     * @param message The message to log
     */
    public static void logWarn(String message) {
        log(LogLevel.WARN, message, null);
    }

    /**
     * Logs a WARN level message with an associated exception.
     * 
     * @param message The message to log
     * @param throwable The exception associated with the message (can be null)
     */
    public static void logWarn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }

    /**
     * Logs an ERROR level message.
     * 
     * @param message The error message to log
     */
    public static synchronized void logError(String message) {
        log(LogLevel.ERROR, message, null);
    }

    /**
     * Logs an ERROR level message with an associated exception.
     * 
     * @param message The error message to log
     * @param throwable The exception associated with the error (can be null)
     */
    public static synchronized void logError(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
}

