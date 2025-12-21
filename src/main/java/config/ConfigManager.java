package config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages configuration file I/O operations
 */
public class ConfigManager {

    private static final String CONFIG_DIR_NAME = "config";
    private static final String ROLL_COST_FILE = "roll_cost.txt";
    private static final String TIER_MODIFIERS_FILE = "tier_modifiers.txt";

    /**
     * Converts a URL location to a File, handling URISyntaxException
     * 
     * @param location The URL location to convert
     * @return The File representation of the URL location
     */
    private static File urlToFile(java.net.URL location) {
        try {
            return new File(location.toURI());
                } catch (java.net.URISyntaxException ignored) {
                    // If URI conversion fails, try getting path directly
            String path = location.getPath();
            // Handle Windows paths: remove leading / if present (e.g., /C:/path -> C:/path)
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")
                    && path.length() > 2 && path.startsWith("/") && path.charAt(2) == ':') {
                path = path.substring(1);
            }
            return new File(path);
        }
    }

    /**
     * Gets base directory from JAR file (directory if IDE, parent if JAR)
     */
    private static File getBaseDirectoryFromJarFile(File jarFile) {
        if (jarFile.isDirectory()) {
            return jarFile;
        }
        return jarFile.getParentFile();
    }

    /**
     * Ensures config directory exists, creating it if necessary
     */
    private static File ensureConfigDirectoryExists(File baseDir, String configDirName) {
        File configDir = new File(baseDir, configDirName);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return configDir;
    }

    /**
     * Tries to get config directory from JAR location
     * 
     * @return Config directory if successful, null otherwise
     */
    private static File tryGetConfigDirectoryFromJar() {
        try {
            java.net.URL location =
                    ConfigManager.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }

            File jarFile = urlToFile(location);
            File baseDir = getBaseDirectoryFromJarFile(jarFile);

            if (baseDir != null && baseDir.exists()) {
                return ensureConfigDirectoryExists(baseDir, CONFIG_DIR_NAME);
            }
        } catch (Exception ignored) {
            // Fallback to current directory
        }
        return null;
    }

    /**
     * Gets the config directory path (relative to JAR location or current directory)
     */
    private static File getConfigDirectory() {
        File configDir = tryGetConfigDirectoryFromJar();
        if (configDir != null) {
            return configDir;
        }

        // Fallback: use current directory
        return ensureConfigDirectoryExists(new File("."), CONFIG_DIR_NAME);
    }

    /**
     * Gets the roll cost from config file
     * 
     * @return Roll cost as double, or 0.0 if file doesn't exist or is invalid
     */
    public static double getRollCost() {
        File configDir = getConfigDirectory();
        File costFile = new File(configDir, ROLL_COST_FILE);

        if (!costFile.exists()) {
            // Create default file with 0
            saveRollCost(0.0);
            return 0.0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(costFile))) {
            String line = reader.readLine();
            if (line != null) {
                line = line.trim();
                return Double.parseDouble(line);
            }
        } catch (IOException | NumberFormatException ignored) {
            // Return default value on error
        }

        return 0.0;
    }

    /**
     * Saves the roll cost to config file
     * 
     * @param cost The cost to save
     */
    public static void saveRollCost(double cost) {
        File configDir = getConfigDirectory();
        File costFile = new File(configDir, ROLL_COST_FILE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(costFile))) {
            writer.write(String.valueOf(cost));
        } catch (IOException ignored) {
            // Silently fail - config saving is not critical
        }
    }

    /**
     * Gets the tier modifiers file path
     */
    public static File getTierModifiersFile() {
        File configDir = getConfigDirectory();
        return new File(configDir, TIER_MODIFIERS_FILE);
    }
}

