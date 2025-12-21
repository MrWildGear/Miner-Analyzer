package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for monitoring error.log file in production to track issues. Monitors the log file for
 * new errors and provides statistics.
 */
public class ErrorLogMonitor implements AutoCloseable {

    private static final String LOG_FILE = "error.log";
    private static final Pattern ERROR_PATTERN =
            Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\] ERROR: (.+)");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Polling interval in milliseconds (check every 5 seconds)
    private static final long POLL_INTERVAL = 5000;

    private volatile Timer monitorTimer;
    private volatile boolean isRunning = false;
    private long lastFilePosition = 0;
    private final File logFile;

    // Error statistics
    private final AtomicInteger totalErrorCount = new AtomicInteger(0);
    private final AtomicInteger sessionErrorCount = new AtomicInteger(0);
    private final AtomicLong lastErrorTime = new AtomicLong(0);

    // Callback interface for error notifications
    @FunctionalInterface
    public interface ErrorCallback {
        /**
         * Called when a new error is detected in the log file.
         * 
         * @param timestamp The timestamp of the error
         * @param message The error message
         * @param errorNumber The error number (1-based, session-specific)
         */
        void onErrorDetected(LocalDateTime timestamp, String message, int errorNumber);
    }

    private ErrorCallback errorCallback;

    /**
     * Constructs an ErrorLogMonitor.
     */
    public ErrorLogMonitor() {
        this.logFile = new File(LOG_FILE);
        initializeLastPosition();
    }

    /**
     * Constructs an ErrorLogMonitor with an error callback.
     * 
     * @param errorCallback Callback to invoke when new errors are detected
     */
    public ErrorLogMonitor(ErrorCallback errorCallback) {
        this();
        this.errorCallback = errorCallback;
    }

    /**
     * Initializes the last file position to the end of the file. This prevents reading old errors
     * when monitoring starts.
     */
    private void initializeLastPosition() {
        if (logFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                long position = 0;
                while (reader.readLine() != null) {
                    position = getFileSize();
                }
                lastFilePosition = position;

                // Count existing errors for statistics
                countExistingErrors();
            } catch (IOException e) {
                // If we can't read, start from beginning
                lastFilePosition = 0;
            }
        } else {
            lastFilePosition = 0;
        }
    }

    /**
     * Counts existing errors in the log file for statistics.
     */
    private void countExistingErrors() {
        if (!logFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = ERROR_PATTERN.matcher(line);
                if (matcher.find()) {
                    count++;
                    try {
                        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), FORMATTER);
                        long timestampMs = timestamp.atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli();
                        lastErrorTime.set(Math.max(lastErrorTime.get(), timestampMs));
                    } catch (Exception e) {
                        // Ignore parsing errors for timestamps
                    }
                }
            }
            totalErrorCount.set(count);
        } catch (IOException e) {
            // Ignore errors during initialization
        }
    }

    /**
     * Starts monitoring the error log file. Safe to call multiple times (will restart monitoring if
     * already running).
     */
    public void start() {
        synchronized (this) {
            if (isRunning) {
                stop();
            }

            isRunning = true;
            monitorTimer = new Timer(true); // Daemon thread
            monitorTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isRunning) {
                        checkForNewErrors();
                    }
                }
            }, 0, POLL_INTERVAL);
        }
    }

    /**
     * Stops monitoring the error log file. Safe to call multiple times.
     */
    public void stop() {
        synchronized (this) {
            isRunning = false;
            if (monitorTimer != null) {
                monitorTimer.cancel();
                monitorTimer.purge();
                monitorTimer = null;
            }
        }
    }

    /**
     * Checks the log file for new errors since the last check.
     */
    private void checkForNewErrors() {
        if (!logFile.exists()) {
            // File doesn't exist yet, reset position
            lastFilePosition = 0;
            return;
        }

        long currentSize = getFileSize();

        // File was truncated or deleted, reset position
        if (currentSize < lastFilePosition) {
            lastFilePosition = 0;
        }

        // No new content
        if (currentSize <= lastFilePosition) {
            return;
        }

        // Read new content
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            // Skip to last known position
            long skipped = reader.skip(lastFilePosition);
            if (skipped != lastFilePosition) {
                // Couldn't skip properly, read from beginning
                reader.close();
                try (BufferedReader newReader = new BufferedReader(new FileReader(logFile))) {
                    processNewLines(newReader);
                }
            } else {
                processNewLines(reader);
            }

            lastFilePosition = currentSize;
        } catch (IOException e) {
            // Don't log monitoring errors to avoid recursion
            // Just silently fail and try again next poll
        }
    }

    /**
     * Processes new lines from the log file and detects errors.
     * 
     * @param reader BufferedReader positioned at the new content
     */
    private void processNewLines(BufferedReader reader) throws IOException {
        String line;
        StringBuilder errorBlock = new StringBuilder();
        LocalDateTime errorTimestamp = null;
        String errorMessage = null;

        while ((line = reader.readLine()) != null) {
            Matcher matcher = ERROR_PATTERN.matcher(line);
            if (matcher.find()) {
                // Found a new error entry
                if (errorMessage != null) {
                    // Process previous error block
                    processError(errorTimestamp, errorMessage);
                }

                // Start new error block
                errorTimestamp = LocalDateTime.parse(matcher.group(1), FORMATTER);
                errorMessage = matcher.group(2);
                errorBlock = new StringBuilder(line);
            } else if (errorMessage != null) {
                // Continuation of error block (stack trace, etc.)
                errorBlock.append("\n").append(line);

                // Check if this is the end of error block
                if (line.equals("---")) {
                    processError(errorTimestamp, errorMessage);
                    errorTimestamp = null;
                    errorMessage = null;
                    errorBlock = new StringBuilder();
                }
            }
        }

        // Process any remaining error
        if (errorMessage != null) {
            processError(errorTimestamp, errorMessage);
        }
    }

    /**
     * Processes a detected error: updates statistics and invokes callback.
     * 
     * @param timestamp The timestamp of the error
     * @param message The error message
     */
    private void processError(LocalDateTime timestamp, String message) {
        totalErrorCount.incrementAndGet();
        int sessionCount = sessionErrorCount.incrementAndGet();
        lastErrorTime
                .set(timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());

        // Invoke callback if set
        if (errorCallback != null) {
            try {
                errorCallback.onErrorDetected(timestamp, message, sessionCount);
            } catch (Exception e) {
                // Don't let callback errors break monitoring
                // Could log to System.err, but avoid recursion with ErrorLogger
                System.err.println("Error in error callback: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the current size of the log file.
     * 
     * @return File size in bytes, or 0 if file doesn't exist
     */
    private long getFileSize() {
        if (logFile.exists()) {
            return logFile.length();
        }
        return 0;
    }

    /**
     * Gets the total number of errors detected (including before monitoring started).
     * 
     * @return Total error count
     */
    public int getTotalErrorCount() {
        return totalErrorCount.get();
    }

    /**
     * Gets the number of errors detected since monitoring started.
     * 
     * @return Session error count
     */
    public int getSessionErrorCount() {
        return sessionErrorCount.get();
    }

    /**
     * Gets the timestamp of the last error detected (epoch milliseconds).
     * 
     * @return Last error timestamp, or 0 if no errors detected
     */
    public long getLastErrorTime() {
        return lastErrorTime.get();
    }

    /**
     * Resets the session error count.
     */
    public void resetSessionCount() {
        sessionErrorCount.set(0);
    }

    /**
     * Sets the error callback.
     * 
     * @param errorCallback Callback to invoke when new errors are detected
     */
    public void setErrorCallback(ErrorCallback errorCallback) {
        this.errorCallback = errorCallback;
    }

    /**
     * Checks if monitoring is currently running.
     * 
     * @return true if monitoring is active
     */
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void close() {
        stop();
    }
}

