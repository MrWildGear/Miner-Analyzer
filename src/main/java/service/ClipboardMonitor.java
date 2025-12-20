package service;

import analyzer.RollAnalyzer;
import config.MinerConfig;
import model.AnalysisResult;
import parser.ItemStatsParser;
import ui.AnalysisDisplay;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
    
    public ClipboardMonitor(String minerType, AnalysisDisplay display, StatusUpdater statusUpdater) {
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
            // Check if we're in a headless environment
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard == null) {
                return;
            }
            
            Transferable contents = null;
            try {
                contents = clipboard.getContents(null);
            } catch (IllegalStateException e) {
                // Clipboard is locked by another application
                return;
            } catch (SecurityException e) {
                // Security exception - can't access clipboard
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (statusUpdater != null) {
                        statusUpdater.updateStatus("Clipboard access denied - check permissions");
                    }
                });
                return;
            }
            
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String clipboardText = null;
                try {
                    clipboardText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException e) {
                    // Clipboard doesn't contain text - this is normal, ignore
                    return;
                } catch (java.io.IOException e) {
                    // IO error reading clipboard
                    return;
                }
                
                if (clipboardText != null && !clipboardText.isEmpty()) {
                    String currentHash = String.valueOf(clipboardText.hashCode());
                    
                    if (!currentHash.equals(lastClipboardHash)) {
                        lastClipboardHash = currentHash;
                        
                        // Parse stats
                        Map<String, Double> parsedStats = ItemStatsParser.parseItemStats(clipboardText);
                        
                        if (!parsedStats.isEmpty()) {
                            Map<String, Double> baseStats = MinerConfig.getBaseStats(minerType);
                            
                            if (baseStats == null || baseStats.isEmpty()) {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    if (statusUpdater != null) {
                                        statusUpdater.updateStatus("Error: Base stats not initialized");
                                    }
                                });
                                return;
                            }
                            
                            try {
                                // Analyze
                                AnalysisResult analysis = RollAnalyzer.analyzeRoll(parsedStats, baseStats, minerType);
                                
                                if (analysis != null) {
                                    // Display
                                    display.displayAnalysis(analysis, baseStats, minerType);
                                    if (statusUpdater != null) {
                                        statusUpdater.updateStatus("Analysis complete");
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    if (statusUpdater != null) {
                                        statusUpdater.updateStatus("Calculation error: " + e.getMessage());
                                    }
                                });
                            } catch (NullPointerException e) {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    if (statusUpdater != null) {
                                        statusUpdater.updateStatus("Error: Missing data");
                                    }
                                });
                            } catch (Exception e) {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    if (statusUpdater != null) {
                                        statusUpdater.updateStatus("Error during analysis");
                                    }
                                });
                            }
                        } else {
                            // Clipboard changed but no stats parsed - might be non-miner data
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                if (statusUpdater != null) {
                                    statusUpdater.updateStatus("Clipboard updated but no miner stats found");
                                }
                            });
                        }
                    }
                }
            }
        } catch (HeadlessException e) {
            // Running in headless environment - can't access GUI
            return;
        } catch (Exception e) {
            // Log error but don't spam the UI - only show occasionally
            if (Math.random() < 0.01) { // Only show 1% of errors to avoid spam
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (statusUpdater != null) {
                        statusUpdater.updateStatus("Error reading clipboard: " + e.getClass().getSimpleName());
                    }
                });
            }
        }
    }
}

