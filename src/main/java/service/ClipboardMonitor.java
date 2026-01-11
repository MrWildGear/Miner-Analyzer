package service;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import analyzer.RollAnalyzer;
import config.MinerConfig;
import model.AnalysisResult;
import parser.ItemStatsParser;
import ui.AnalysisDisplay;
import util.ErrorLogger;

/**
 * Service for monitoring clipboard and triggering analysis
 */
public class ClipboardMonitor implements AutoCloseable {

    private final String minerType;
    private final AnalysisDisplay display;
    private final StatusUpdater statusUpdater;

    private String lastClipboardHash = null;
    private final AtomicReference<Timer> clipboardTimer = new AtomicReference<>();

    // Adaptive polling intervals (in milliseconds)
    private static final long ACTIVE_INTERVAL = 300; // Fast polling when active
    private static final long IDLE_INTERVAL = 1500; // Slower polling when idle
    private static final long IDLE_THRESHOLD = 5000; // Switch to idle after 5 seconds

    private volatile long lastChangeTime = System.currentTimeMillis();
    private volatile boolean isIdle = false;

    // Error throttling to prevent UI spam (show error at most once per 5 seconds)
    private static final long ERROR_THROTTLE_INTERVAL = 5000; // 5 seconds
    private volatile long lastErrorDisplayTime = 0;
    private volatile String lastErrorMessage = null;

    /**
     * Functional interface for updating status messages.
     */
    @FunctionalInterface
    public interface StatusUpdater {
        /**
         * Updates the status message in the UI.
         * 
         * @param message The status message to display
         */
        void updateStatus(String message);
    }

    /**
     * Constructs a ClipboardMonitor for the specified miner type.
     * 
     * @param minerType The miner type to monitor for ("ORE", "Modulated", or "Ice")
     * @param display The AnalysisDisplay to use for showing analysis results
     * @param statusUpdater The callback function to update status messages
     */
    public ClipboardMonitor(String minerType, AnalysisDisplay display,
            StatusUpdater statusUpdater) {
        this.minerType = minerType;
        this.display = display;
        this.statusUpdater = statusUpdater;
    }

    /**
     * Starts the clipboard monitoring with adaptive polling. Uses faster polling (300ms) when
     * active, slower (1500ms) when idle. If already started, stops the existing timer first.
     */
    public void start() {
        synchronized (this) {
            // Stop any existing timer before starting a new one
            Timer oldTimer = clipboardTimer.get();
            if (oldTimer != null) {
                oldTimer.cancel();
                oldTimer.purge();
            }

            lastChangeTime = System.currentTimeMillis();
            isIdle = false;

            clipboardTimer.set(new Timer(true)); // Daemon thread
            scheduleNextCheck(ACTIVE_INTERVAL);
        }
    }

    /**
     * Schedules the next clipboard check with adaptive interval. Adjusts interval based on idle
     * state to reduce CPU usage.
     * 
     * @param interval The interval in milliseconds for the next check
     */
    private void scheduleNextCheck(long interval) {
        Timer timer = clipboardTimer.get();
        if (timer == null) {
            return;
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkClipboard();

                // Determine next interval based on idle state
                long nextInterval = calculateNextInterval();

                // Schedule next check with adaptive interval
                synchronized (ClipboardMonitor.this) {
                    Timer currentTimer = clipboardTimer.get();
                    if (currentTimer != null && currentTimer == timer) {
                        scheduleNextCheck(nextInterval);
                    }
                }
            }
        }, interval);
    }

    /**
     * Calculates the next polling interval based on idle state. Switches to idle mode after
     * IDLE_THRESHOLD milliseconds of no changes.
     * 
     * @return The next polling interval in milliseconds
     */
    private long calculateNextInterval() {
        long timeSinceLastChange = System.currentTimeMillis() - lastChangeTime;

        if (timeSinceLastChange >= IDLE_THRESHOLD && !isIdle) {
            // Switch to idle mode
            isIdle = true;
            return IDLE_INTERVAL;
        } else if (timeSinceLastChange < IDLE_THRESHOLD && isIdle) {
            // Switch back to active mode
            isIdle = false;
            return ACTIVE_INTERVAL;
        }

        // Return current interval based on idle state
        return isIdle ? IDLE_INTERVAL : ACTIVE_INTERVAL;
    }

    /**
     * Stops the clipboard monitoring and cleans up resources. Safe to call multiple times.
     */
    public void stop() {
        Timer timer = clipboardTimer.get();
        if (timer != null) {
            synchronized (this) {
                timer = clipboardTimer.get();
                if (timer != null) {
                    timer.cancel();
                    timer.purge(); // Remove cancelled tasks
                    clipboardTimer.set(null);
                }
            }
        }
    }

    /**
     * Closes the clipboard monitor and cleans up resources. Implements AutoCloseable for
     * try-with-resources support.
     */
    @Override
    public void close() {
        stop();
    }

    private void checkClipboard() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard == null) {
                return;
            }

            Transferable contents = getClipboardContents(clipboard);
            if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return;
            }

            String clipboardText = getClipboardText(contents);
            if (clipboardText != null && !clipboardText.isEmpty()) {
                processClipboardText(clipboardText);
            }
        } catch (HeadlessException e) {
            ErrorLogger.logError("Headless environment detected - cannot access clipboard", e);
        } catch (Exception e) {
            handleClipboardError(e);
        }
    }

    /**
     * Gets clipboard contents, handling exceptions gracefully.
     * 
     * @param clipboard The system clipboard
     * @return Transferable contents or null if unavailable
     */
    private Transferable getClipboardContents(Clipboard clipboard) {
        try {
            return clipboard.getContents(null);
        } catch (IllegalStateException e) {
            ErrorLogger.logError("Clipboard is locked by another application", e);
            // Clipboard is locked by another application
            return null;
        } catch (SecurityException e) {
            ErrorLogger.logError("Security exception accessing clipboard", e);
            // Security exception - can't access clipboard
            updateStatusOnEDT("Clipboard access denied - check permissions");
            return null;
        }
    }

    /**
     * Gets text from clipboard contents, handling exceptions gracefully.
     * 
     * @param contents The clipboard transferable contents
     * @return Clipboard text or null if unavailable
     */
    private String getClipboardText(Transferable contents) {
        try {
            return (String) contents.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | java.io.IOException e) {
            ErrorLogger.logError("Error getting clipboard text (unsupported flavor or IO error)",
                    e);
            // Clipboard doesn't contain text or IO error - this is normal, but log for debugging
            return null;
        }
    }

    /**
     * Processes clipboard text if it has changed.
     * 
     * @param clipboardText The text from the clipboard
     */
    private void processClipboardText(String clipboardText) {
        String currentHash = String.valueOf(clipboardText.hashCode());
        synchronized (this) {
            if (currentHash.equals(lastClipboardHash)) {
                return; // Content hasn't changed
            }

            lastClipboardHash = currentHash;
            // Reset idle state when clipboard changes to switch to active polling
            lastChangeTime = System.currentTimeMillis();
            isIdle = false;
        }
        handleClipboardChange(clipboardText);
    }

    /**
     * Handles processing when clipboard content has changed.
     * 
     * @param clipboardText The new clipboard text
     */
    private void handleClipboardChange(String clipboardText) {
        Map<String, Double> parsedStats = ItemStatsParser.parseItemStats(clipboardText);

        if (parsedStats.isEmpty()) {
            updateStatusOnEDT("Clipboard updated but no miner stats found");
            return;
        }

        processParsedStats(parsedStats);
    }

    /**
     * Processes parsed stats and performs analysis if base stats are available.
     * 
     * @param parsedStats The parsed item stats
     */
    private void processParsedStats(Map<String, Double> parsedStats) {
        Map<String, Double> baseStats = MinerConfig.getBaseStats(minerType);

        if (baseStats == null || baseStats.isEmpty()) {
            updateStatusOnEDT("Error: Base stats not initialized");
            return;
        }

        performAnalysis(parsedStats, baseStats);
    }

    /**
     * Performs analysis on parsed stats and displays the results.
     * 
     * @param parsedStats The parsed item stats from clipboard
     * @param baseStats The base stats for the miner type
     */
    private void performAnalysis(Map<String, Double> parsedStats, Map<String, Double> baseStats) {
        try {
            AnalysisResult analysis = RollAnalyzer.analyzeRoll(parsedStats, baseStats, minerType);

            if (analysis != null) {
                // Display - must be on EDT
                final AnalysisResult finalAnalysis = analysis;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    display.displayAnalysis(finalAnalysis, baseStats, minerType);
                    if (statusUpdater != null) {
                        statusUpdater.updateStatus("Analysis complete");
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            ErrorLogger.logError("Calculation error during analysis", e);
            updateStatusOnEDT("Calculation error: " + e.getMessage());
        } catch (NullPointerException e) {
            ErrorLogger.logError("Null pointer exception during analysis - missing data", e);
            updateStatusOnEDT("Error: Missing data");
        } catch (Exception e) {
            ErrorLogger.logError("Unexpected error during analysis", e);
            updateStatusOnEDT("Error during analysis");
        }
    }

    /**
     * Updates status message on the Event Dispatch Thread.
     * 
     * @param message The status message to display
     */
    private void updateStatusOnEDT(String message) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (statusUpdater != null) {
                statusUpdater.updateStatus(message);
            }
        });
    }

    /**
     * Handles errors that occur during clipboard monitoring. Logs all errors and shows
     * user-friendly messages with throttling to prevent UI spam.
     * 
     * @param e The exception that occurred
     */
    private void handleClipboardError(Exception e) {
        // Always log all errors to file
        ErrorLogger.logError("Clipboard monitoring error", e);

        // Generate user-friendly error message based on exception type
        String userMessage = getUserFriendlyErrorMessage(e);

        // Throttle UI updates to prevent spam (show at most once per ERROR_THROTTLE_INTERVAL)
        long currentTime = System.currentTimeMillis();
        boolean shouldShow = false;

        synchronized (this) {
            if (currentTime - lastErrorDisplayTime >= ERROR_THROTTLE_INTERVAL
                    || !userMessage.equals(lastErrorMessage)) {
                shouldShow = true;
                lastErrorDisplayTime = currentTime;
                lastErrorMessage = userMessage;
            }
        }

        if (shouldShow) {
            updateStatusOnEDT(userMessage);
        }
    }

    /**
     * Generates a user-friendly error message based on the exception type.
     * 
     * @param e The exception that occurred
     * @return A user-friendly error message
     */
    private String getUserFriendlyErrorMessage(Exception e) {
        if (e instanceof IllegalStateException) {
            return "Clipboard temporarily unavailable - another application may be using it";
        } else if (e instanceof SecurityException) {
            return "Clipboard access denied - please check application permissions";
        } else if (e instanceof UnsupportedFlavorException) {
            return "Clipboard content format not supported";
        } else if (e instanceof java.io.IOException) {
            return "Error reading clipboard - please try again";
        } else if (e instanceof HeadlessException) {
            return "Clipboard not available in this environment";
        } else {
            return "Error accessing clipboard: " + e.getClass().getSimpleName();
        }
    }
}

