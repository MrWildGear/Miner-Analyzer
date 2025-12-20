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
import analyzer.RollAnalyzer;
import config.MinerConfig;
import model.AnalysisResult;
import parser.ItemStatsParser;
import ui.AnalysisDisplay;

/**
 * Service for monitoring clipboard and triggering analysis
 */
public class ClipboardMonitor {

    private final String minerType;
    private final AnalysisDisplay display;
    private final StatusUpdater statusUpdater;

    private String lastClipboardHash = null;
    private Timer clipboardTimer;

    @FunctionalInterface
    public interface StatusUpdater {
        void updateStatus(String message);
    }

    public ClipboardMonitor(String minerType, AnalysisDisplay display,
            StatusUpdater statusUpdater) {
        this.minerType = minerType;
        this.display = display;
        this.statusUpdater = statusUpdater;
    }

    public void start() {
        clipboardTimer = new Timer(true);
        clipboardTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkClipboard();
            }
        }, 0, 300); // Check every 300ms
    }

    public void stop() {
        if (clipboardTimer != null) {
            clipboardTimer.cancel();
            clipboardTimer = null;
        }
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
        } catch (HeadlessException _) {
            // Running in headless environment - can't access GUI
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
        } catch (IllegalStateException _) {
            // Clipboard is locked by another application
            return null;
        } catch (SecurityException _) {
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
        } catch (UnsupportedFlavorException | java.io.IOException _) {
            // Clipboard doesn't contain text or IO error - this is normal, ignore
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
        if (currentHash.equals(lastClipboardHash)) {
            return; // Content hasn't changed
        }

        lastClipboardHash = currentHash;
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
            updateStatusOnEDT("Calculation error: " + e.getMessage());
        } catch (NullPointerException _) {
            updateStatusOnEDT("Error: Missing data");
        } catch (Exception _) {
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
     * Handles errors that occur during clipboard monitoring.
     * 
     * @param e The exception that occurred
     */
    private void handleClipboardError(Exception e) {
        // Log error but don't spam the UI - only show occasionally
        if (Math.random() < 0.01) { // Only show 1% of errors to avoid spam
            updateStatusOnEDT("Error reading clipboard: " + e.getClass().getSimpleName());
        }
    }
}

