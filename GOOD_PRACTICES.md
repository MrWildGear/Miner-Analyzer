# Good Practices Documentation

This document summarizes the good software engineering practices implemented throughout the codebase.

## 1. Try-with-Resources Used Consistently

**Implementation:** All file I/O operations use try-with-resources to ensure proper resource cleanup.

**Examples:**
- `ErrorLogger.java` (line 135): `try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true)))`
- `ErrorLogMonitor.java` (lines 80, 106, 189, 195): Multiple `try (BufferedReader reader = ...)` blocks
- `ConfigManager.java` (lines 206, 236): File reading/writing with try-with-resources

**Benefits:**
- Automatic resource cleanup even if exceptions occur
- Prevents resource leaks
- Cleaner code without manual close() calls

---

## 2. Proper Exception Handling with Logging

**Implementation:** Comprehensive exception handling with structured logging throughout the application.

**Examples:**
- `EveMinerAnalyzer.java`: Exception handlers in main() method (lines 948-949, 952-953, 979-982)
- `ClipboardMonitor.java`: Specific exception handling for different error types (lines 207-211, 220-232, 241-249)
- `ItemStatsParser.java`: NumberFormatException handling with logging (line 151-154)
- `ErrorLogger.java`: Centralized logging utility with different log levels

**Benefits:**
- All errors are logged for debugging
- User-friendly error messages displayed in UI
- Errors don't crash the application
- Error statistics tracking via ErrorLogMonitor

---

## 3. Thread Safety with Volatile and Synchronized

**Implementation:** Proper use of `volatile` for visibility and `synchronized` for atomicity.

**Examples:**

**Volatile fields:**
- `ErrorLogMonitor.java`: `volatile Timer monitorTimer` (line 31), `volatile boolean isRunning` (line 32)
- `ClipboardMonitor.java`: `volatile Timer clipboardTimer` (line 30), `volatile long lastChangeTime` (line 37), `volatile boolean isIdle` (line 38)

**Synchronized blocks/methods:**
- `ErrorLogMonitor.java`: `synchronized` in `start()` (line 134), `stop()` (line 156)
- `ClipboardMonitor.java`: `synchronized` in `start()` (line 77), `checkClipboard()` processing (line 259), error handling (line 366)
- `ErrorLogger.java`: `synchronized` in `log()` method (line 127) and `setMinimumLogLevel()` (line 39)

**Benefits:**
- Prevents race conditions
- Ensures visibility of state changes across threads
- Safe concurrent access to shared resources

---

## 4. Path Validation for Security

**Implementation:** Comprehensive path validation to prevent path traversal attacks.

**Location:** `ConfigManager.java` (lines 29-101)

**Features:**
- `isValidFilename()` method checks for:
  - Path traversal sequences (`..`, `/`, `\`)
  - Null bytes (`\0`)
  - Windows reserved characters (`<>:"|?*`)
- `resolveConfigFile()` method:
  - Validates filename before processing
  - Uses `getCanonicalFile()` to normalize paths
  - Verifies resolved path is within config directory
  - Blocks any attempts to access files outside the config directory

**Example:**
```java
// Validates filename and prevents path traversal
if (!canonicalFilePath.startsWith(canonicalConfigPath + File.separator)
        && !canonicalFilePath.equals(canonicalConfigPath)) {
    ErrorLogger.logError("Path traversal detected...", ...);
    return null;
}
```

**Benefits:**
- Prevents directory traversal attacks
- Ensures files are only accessed within intended directories
- Security logging for attempted attacks

---

## 5. Input Validation with Bounds Checking

**Implementation:** Input validation with reasonable bounds to prevent invalid or malicious input.

**Examples:**
- `EveMinerAnalyzer.java`:
  - `MAX_ROLL_COST` constant (line 59): Maximum allowed roll cost (1 trillion ISK)
  - `showRollCostDialog()`: Validates negative values, infinity, NaN, and upper bounds (lines 606-624)
  - `validateTierModifiers()`: Validates tier modifier inputs (lines 763-810)
- `ItemStatsParser.java`: Validates stat values and formats before parsing

**Benefits:**
- Prevents invalid data from causing errors
- Protects against overflow/underflow issues
- Provides user-friendly error messages for invalid input

---

## 6. Resource Cleanup with AutoCloseable

**Implementation:** Services implement `AutoCloseable` interface for proper resource management.

**Examples:**
- `ErrorLogMonitor.java`: Implements `AutoCloseable` (line 20), `close()` method calls `stop()` (line 342)
- `ClipboardMonitor.java`: Implements `AutoCloseable` (line 23), `close()` method calls `stop()` (line 168)
- Both services properly clean up timers and threads in `stop()` methods

**Usage:**
```java
// Can be used with try-with-resources
try (ErrorLogMonitor monitor = new ErrorLogMonitor()) {
    monitor.start();
    // ... use monitor
} // Automatically cleaned up
```

**Benefits:**
- Ensures resources are always cleaned up
- Can be used with try-with-resources
- Prevents resource leaks
- Graceful shutdown handling

---

## 7. Log Rotation and Levels Implemented

**Implementation:** Comprehensive logging system with rotation and level filtering.

**Location:** `ErrorLogger.java`

**Features:**

**Log Levels:**
- `LogLevel` enum: INFO, WARN, ERROR (lines 21-23)
- `setMinimumLogLevel()`: Configurable minimum log level (line 39)
- `shouldLog()`: Filters messages based on level (line 60)

**Log Rotation:**
- `rotateLogIfNeeded()`: Automatically rotates logs when file exceeds 10 MB (line 82)
- Keeps up to 5 rotated log files (`error.log.1` through `error.log.5`)
- Oldest logs are deleted when limit is reached

**Benefits:**
- Prevents log files from growing indefinitely
- Configurable verbosity (can filter to only errors in production)
- Organized log history with numbered rotation
- Automatic management without manual intervention

---

## 8. Error Monitoring Service

**Implementation:** Dedicated service for monitoring error logs in production.

**Location:** `ErrorLogMonitor.java`

**Features:**
- Monitors `error.log` file for new errors
- Polls every 5 seconds for new content
- Tracks statistics:
  - Total error count (all time)
  - Session error count (current run)
  - Last error timestamp
- Callback interface for error notifications
- Thread-safe with atomic counters
- Implements `AutoCloseable` for proper cleanup

**Usage:**
```java
ErrorLogMonitor monitor = new ErrorLogMonitor((timestamp, message, errorNumber) -> {
    // Handle new error
});
monitor.start();
```

**Benefits:**
- Real-time error detection
- Error statistics for monitoring
- Can trigger notifications or alerts
- Helps with production debugging
- Non-intrusive monitoring (daemon thread)

---

## Additional Good Practices

### Graceful Shutdown
- `EveMinerAnalyzer.java`: Shutdown hook registered (line 99)
- Cleanup methods called on window close (line 105)
- Proper cleanup of monitors and threads

### Error Throttling
- `ClipboardMonitor.java`: Error messages throttled to prevent UI spam (lines 40-43, 362-377)
- Shows errors at most once per 5 seconds

### Adaptive Polling
- `ClipboardMonitor.java`: Adaptive polling intervals (300ms active, 1500ms idle)
- Reduces CPU usage when clipboard is not changing

### EDT Safety
- UI updates properly dispatched to Event Dispatch Thread
- `SwingUtilities.invokeLater()` used throughout for thread-safe UI updates

---

## Summary

The codebase demonstrates excellent software engineering practices:
- ✅ Resource management (try-with-resources, AutoCloseable)
- ✅ Exception handling and logging
- ✅ Thread safety (volatile, synchronized)
- ✅ Security (path validation)
- ✅ Input validation
- ✅ Log management (rotation, levels)
- ✅ Monitoring and observability
- ✅ Graceful shutdown and cleanup

These practices contribute to a robust, maintainable, and secure application.

