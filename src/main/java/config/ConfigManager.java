package config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import util.ErrorLogger;

/**
 * Manages configuration file I/O operations
 */
public class ConfigManager {

    private static final String CONFIG_DIR_NAME = "config";
    private static final String ROLL_COST_FILE = "roll_cost.txt";
    private static final String TIER_MODIFIERS_FILE = "tier_modifiers.txt";
    private static final String OPTIMAL_RANGE_MODIFIER_FILE = "optimal_range_modifier.txt";

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private ConfigManager() {
        // Utility class - no instantiation
    }

    /**
     * Validates a filename to prevent path traversal attacks
     * 
     * @param filename The filename to validate
     * @return true if the filename is safe, false otherwise
     */
    private static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        // Check for path traversal sequences
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // Check for null bytes (potential injection)
        if (filename.contains("\0")) {
            return false;
        }

        // Check for Windows reserved characters
        String reservedChars = "<>:\"|?*";
        for (char c : reservedChars.toCharArray()) {
            if (filename.indexOf(c) >= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Safely resolves a file path within the config directory, preventing path traversal
     * 
     * @param configDir The base config directory
     * @param filename The filename to resolve
     * @return A validated File object within the config directory, or null if validation fails
     */
    private static File resolveConfigFile(File configDir, String filename) {
        // Validate filename
        if (!isValidFilename(filename)) {
            ErrorLogger.logError(
                    "Invalid filename detected (potential path traversal): " + filename,
                    new SecurityException("Path traversal attempt blocked"));
            return null;
        }

        try {
            File file = new File(configDir, filename);

            // Normalize the path to resolve any remaining issues
            File canonicalFile = file.getCanonicalFile();
            File canonicalConfigDir = configDir.getCanonicalFile();

            // Ensure the resolved file is within the config directory
            String canonicalFilePath = canonicalFile.getPath();
            String canonicalConfigPath = canonicalConfigDir.getPath();

            // Check if the file path starts with the config directory path
            if (!canonicalFilePath.startsWith(canonicalConfigPath + File.separator)
                    && !canonicalFilePath.equals(canonicalConfigPath)) {
                ErrorLogger.logError(
                        "Path traversal detected: resolved path outside config directory",
                        new SecurityException("Path traversal attempt blocked"));
                return null;
            }

            return canonicalFile;
        } catch (IOException e) {
            ErrorLogger.logError("Error resolving config file path: " + filename, e);
            return null;
        }
    }

    /**
     * Converts a URL location to a File, handling URISyntaxException
     * 
     * @param location The URL location to convert
     * @return The File representation of the URL location
     */
    private static File urlToFile(java.net.URL location) {
        try {
            return new File(location.toURI());
        } catch (java.net.URISyntaxException e) {
            ErrorLogger.logError("Failed to convert URL to URI for config directory", e);
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
        } catch (Exception e) {
            ErrorLogger.logError("Error getting config directory from JAR location", e);
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
        File costFile = resolveConfigFile(configDir, ROLL_COST_FILE);

        if (costFile == null) {
            ErrorLogger.logError("Failed to resolve roll cost file path due to validation failure",
                    new SecurityException("Path validation failed"));
            return 0.0;
        }

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
        } catch (IOException | NumberFormatException e) {
            ErrorLogger.logError(
                    "Error reading roll cost from config file: " + costFile.getAbsolutePath(), e);
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
        File costFile = resolveConfigFile(configDir, ROLL_COST_FILE);

        if (costFile == null) {
            ErrorLogger.logError("Failed to resolve roll cost file path due to validation failure",
                    new SecurityException("Path validation failed"));
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(costFile))) {
            writer.write(String.valueOf(cost));
        } catch (IOException e) {
            ErrorLogger.logError(
                    "Error saving roll cost to config file: " + costFile.getAbsolutePath(), e);
            // Config saving failure is logged but not critical
        }
    }

    /**
     * Gets the tier modifiers file path
     * 
     * @return The tier modifiers file, or null if path validation fails
     */
    public static File getTierModifiersFile() {
        File configDir = getConfigDirectory();
        File file = resolveConfigFile(configDir, TIER_MODIFIERS_FILE);

        if (file == null) {
            ErrorLogger.logError(
                    "Failed to resolve tier modifiers file path due to validation failure",
                    new SecurityException("Path validation failed"));
        }

        return file;
    }

    /**
     * Gets the optimal range modifier file path
     * 
     * @return The optimal range modifier file, or null if path validation fails
     */
    public static File getOptimalRangeModifiersFile() {
        File configDir = getConfigDirectory();
        File file = resolveConfigFile(configDir, OPTIMAL_RANGE_MODIFIER_FILE);

        if (file == null) {
            ErrorLogger.logError(
                    "Failed to resolve optimal range modifier file path due to validation failure",
                    new SecurityException("Path validation failed"));
        }

        return file;
    }
}

