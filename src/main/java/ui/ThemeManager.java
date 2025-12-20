package ui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.UIManager;

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
        } catch (Exception ignored) {
            // If theme detection fails, use default light theme
            isDarkMode = false;
            applyTheme();
        }
    }

    public void detectSystemTheme() {
        // Try to detect system theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
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

    /**
     * Waits for a process to finish with a timeout, compatible with both Java 7 and Java 8+
     * 
     * @param process the process to wait for
     * @param timeoutSeconds the timeout in seconds
     * @return true if the process finished within the timeout, false otherwise
     */
    private boolean waitForProcessWithTimeout(Process process, int timeoutSeconds) {
        try {
            // Try Java 8+ method first
            return process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return false;
        } catch (NoSuchMethodError ignored) {
            // Fallback for Java 7: use simple waitFor with thread interrupt
            Thread waitThread = new Thread(() -> {
                try {
                    process.waitFor();
                } catch (InterruptedException ignored1) {
                    Thread.currentThread().interrupt();
                }
            });
            waitThread.start();
            try {
                waitThread.join((long) timeoutSeconds * 1000); // Wait max timeoutSeconds
                                                               // milliseconds
                return !waitThread.isAlive();
            } catch (InterruptedException ignored2) {
                waitThread.interrupt();
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private boolean detectSystemDarkMode() {
        // Method 1: Try Windows Registry (most reliable for Windows)
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Optional<Boolean> result = detectWindowsRegistryTheme();
            if (result.isPresent()) {
                return result.get();
            }
        }

        // Method 2: Check UI Manager colors (fallback)
        Optional<Boolean> result = detectUIManagerTheme();
        if (result.isPresent()) {
            return result.get();
        }

        // Method 3: Check system property (if available)
        result = detectSystemPropertyTheme();
        if (result.isPresent()) {
            return result.get();
        }

        // Default to light mode
        return false;
    }

    private Optional<Boolean> detectWindowsRegistryTheme() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v",
                    "AppsUseLightTheme");
            Process process = pb.start();

            boolean finished = waitForProcessWithTimeout(process, 2);
            if (!finished) {
                destroyProcess(process);
                return Optional.empty(); // Timeout - fall through to other detection methods
            }

            return parseRegistryOutput(process);
        } catch (Exception ignored) {
            // Registry read failed, try other methods
            return Optional.empty();
        }
    }

    private void destroyProcess(Process process) {
        try {
            process.destroy();
        } catch (Exception ignored) {
            // Ignore destroy errors
        }
    }

    private Optional<Boolean> parseRegistryOutput(Process process) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // If value is 0, dark mode is enabled
                if (line.contains("AppsUseLightTheme")
                        && (line.contains("0x0") || line.matches(".*\\s0\\s*$"))) {
                    return Optional.of(true);
                }
            }
            return Optional.of(false);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Boolean> detectUIManagerTheme() {
        try {
            Color bg = UIManager.getColor("Panel.background");
            if (bg != null) {
                // Dark mode typically has low brightness
                double brightness =
                        (bg.getRed() * 0.299 + bg.getGreen() * 0.587 + bg.getBlue() * 0.114)
                                / 255.0;
                return Optional.of(brightness < 0.5);
            }
            return Optional.of(false);
        } catch (Exception ignored) {
            // Color check failed
            return Optional.empty();
        }
    }

    private Optional<Boolean> detectSystemPropertyTheme() {
        try {
            String theme = System.getProperty("swing.systemTheme");
            return Optional.of(theme != null && theme.toLowerCase().contains("dark"));
        } catch (Exception ignored) {
            // Property check failed
            return Optional.empty();
        }
    }

    private void applyTheme() {
        tierColors = new HashMap<>();
        if (isDarkMode) {
            bgColor = new Color(20, 20, 20); // Soft black - darker than gray but not pure black
            fgColor = new Color(200, 200, 200); // Light gray text (softer than white)
            frameBg = new Color(25, 25, 25); // Slightly lighter than bg for contrast
            menuBgColor = new Color(25, 25, 25); // Match frame background
            menuFgColor = new Color(200, 200, 200); // Light gray text (softer than white)
            borderColor = new Color(30, 30, 30); // Very dark border (matches between bg and
                                                 // frameBg)
            headerBgColor = new Color(25, 25, 25); // Match menu bar color
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
        applyUIManagerTheme();
    }

    private void applyUIManagerTheme() {
        // Apply UIManager properties for popup menus and menu items
        try {
            UIManager.put("PopupMenu.background", menuBgColor);
            UIManager.put("PopupMenu.foreground", menuFgColor);
            UIManager.put("Menu.background", menuBgColor);
            UIManager.put("Menu.foreground", menuFgColor);
            UIManager.put("MenuItem.background", menuBgColor);
            UIManager.put("MenuItem.foreground", menuFgColor);
            UIManager.put("MenuBar.background", menuBgColor);
            UIManager.put("MenuBar.foreground", menuFgColor);

            // Radio button colors
            UIManager.put("RadioButton.background", bgColor);
            UIManager.put("RadioButton.foreground", fgColor);
            UIManager.put("RadioButton.focus", borderColor);

            // Label colors
            UIManager.put("Label.background", bgColor);
            UIManager.put("Label.foreground", fgColor);

            // Panel colors
            UIManager.put("Panel.background", bgColor);
            UIManager.put("Panel.foreground", fgColor);

            // TextPane colors
            UIManager.put("TextPane.background", frameBg);
            UIManager.put("TextPane.foreground", fgColor);
            UIManager.put("TextPane.caretForeground", fgColor);

            // Selected menu item colors
            if (isDarkMode) {
                UIManager.put("MenuItem.selectionBackground", new Color(50, 50, 50));
                UIManager.put("MenuItem.selectionForeground", new Color(200, 200, 200)); // Light
                                                                                         // gray to
                                                                                         // match
                                                                                         // theme
            } else {
                UIManager.put("MenuItem.selectionBackground", new Color(200, 200, 200));
                UIManager.put("MenuItem.selectionForeground", Color.BLACK);
            }
        } catch (Exception ignored) {
            // Ignore UIManager errors
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

