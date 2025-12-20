package ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages theme detection and application
 */
public class ThemeManager {
    
    private boolean isDarkMode;
    private Boolean manualThemeOverride = null; // null = auto, true = dark, false = light
    
    private Color bgColor;
    private Color fgColor;
    private Color frameBg;
    private Color menuBgColor;
    private Color menuFgColor;
    private Color borderColor;
    private Color headerBgColor;
    private Map<String, Color> tierColors;
    
    public ThemeManager() {
        try {
            detectSystemTheme();
        } catch (Exception e) {
            // If theme detection fails, use default light theme
            isDarkMode = false;
            applyTheme();
        }
    }
    
    public void detectSystemTheme() {
        // Try to detect system theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default if detection fails
        }
        
        // Check if manual override is set
        if (manualThemeOverride != null) {
            isDarkMode = manualThemeOverride;
        } else {
            // Auto-detect system theme
            isDarkMode = detectSystemDarkMode();
        }
        
        applyTheme();
    }
    
    private boolean detectSystemDarkMode() {
        // Method 1: Try Windows Registry (most reliable for Windows)
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"
                );
                Process process = pb.start();
                
                // Wait with timeout (2 seconds max) - compatible with Java 7+
                boolean finished = false;
                try {
                    // Try Java 8+ method first
                    finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (NoSuchMethodError e) {
                    // Fallback for Java 7: use simple waitFor with thread interrupt
                    Thread waitThread = new Thread(() -> {
                        try {
                            process.waitFor();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    waitThread.start();
                    try {
                        waitThread.join(2000); // Wait max 2 seconds
                        finished = !waitThread.isAlive();
                    } catch (InterruptedException ie) {
                        waitThread.interrupt();
                        Thread.currentThread().interrupt();
                    }
                }
                
                if (!finished) {
                    try {
                        process.destroy();
                    } catch (Exception e) {
                        // Ignore destroy errors
                    }
                    // Timeout - fall through to other detection methods
                } else {
                    java.io.BufferedReader reader = null;
                    try {
                        reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream())
                        );
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("AppsUseLightTheme")) {
                                // If value is 0, dark mode is enabled
                                if (line.contains("0x0") || line.matches(".*\\s0\\s*$")) {
                                    return true;
                                }
                            }
                        }
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e) {
                                // Ignore close errors
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Registry read failed, try other methods
            }
        }
        
        // Method 2: Check UI Manager colors (fallback)
        try {
            Color bg = UIManager.getColor("Panel.background");
            if (bg != null) {
                // Dark mode typically has low brightness
                double brightness = (bg.getRed() * 0.299 + bg.getGreen() * 0.587 + bg.getBlue() * 0.114) / 255.0;
                if (brightness < 0.5) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Color check failed
        }
        
        // Method 3: Check system property (if available)
        try {
            String theme = System.getProperty("swing.systemTheme");
            if (theme != null && theme.toLowerCase().contains("dark")) {
                return true;
            }
        } catch (Exception e) {
            // Property check failed
        }
        
        // Default to light mode
        return false;
    }
    
    private void applyTheme() {
        tierColors = new HashMap<>();
        if (isDarkMode) {
            bgColor = new Color(30, 30, 30);
            fgColor = Color.WHITE;
            frameBg = new Color(45, 45, 45);
            menuBgColor = new Color(45, 45, 45);
            menuFgColor = Color.WHITE;
            borderColor = new Color(60, 60, 60); // Darker border color for dark mode
            headerBgColor = new Color(35, 35, 35); // Slightly lighter than bg for header
            tierColors.put("S", new Color(0, 255, 0));
            tierColors.put("A", new Color(0, 255, 255));
            tierColors.put("B", new Color(0, 128, 255));
            tierColors.put("C", Color.YELLOW);
            tierColors.put("D", new Color(255, 0, 255));
            tierColors.put("E", new Color(255, 165, 0));
            tierColors.put("F", Color.RED);
        } else {
            bgColor = Color.WHITE;
            fgColor = Color.BLACK;
            frameBg = new Color(240, 240, 240);
            menuBgColor = new Color(240, 240, 240);
            menuFgColor = Color.BLACK;
            borderColor = new Color(200, 200, 200); // Light border color for light mode
            headerBgColor = new Color(245, 245, 245); // Slightly lighter than bg for header
            tierColors.put("S", new Color(0, 170, 0));
            tierColors.put("A", new Color(0, 170, 170));
            tierColors.put("B", new Color(0, 102, 204));
            tierColors.put("C", new Color(204, 170, 0));
            tierColors.put("D", new Color(170, 0, 170));
            tierColors.put("E", new Color(204, 102, 0));
            tierColors.put("F", new Color(204, 0, 0));
        }
    }
    
    public void toggleTheme() {
        if (manualThemeOverride == null) {
            // Currently auto, switch to opposite of current
            manualThemeOverride = !isDarkMode;
        } else {
            // Currently manual, toggle
            manualThemeOverride = !manualThemeOverride;
        }
        detectSystemTheme();
    }
    
    public void setThemeAuto() {
        manualThemeOverride = null;
        detectSystemTheme();
    }
    
    public void setThemeLight() {
        manualThemeOverride = false;
        detectSystemTheme();
    }
    
    public void setThemeDark() {
        manualThemeOverride = true;
        detectSystemTheme();
    }
    
    // Getters
    public boolean isDarkMode() {
        return isDarkMode;
    }
    
    public Color getBgColor() {
        return bgColor;
    }
    
    public Color getFgColor() {
        return fgColor;
    }
    
    public Color getFrameBg() {
        return frameBg;
    }
    
    public Map<String, Color> getTierColors() {
        return new HashMap<>(tierColors);
    }
    
    public Color getMenuBgColor() {
        return menuBgColor;
    }
    
    public Color getMenuFgColor() {
        return menuFgColor;
    }
    
    public Color getBorderColor() {
        return borderColor;
    }
    
    public Color getHeaderBgColor() {
        return headerBgColor;
    }
}

