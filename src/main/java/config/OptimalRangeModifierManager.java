package config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import util.ErrorLogger;

/**
 * Manages optimal range modifier for sell price calculation Applies when tier has "+" suffix
 * (optimal range is increased)
 */
public class OptimalRangeModifierManager {

    private static final double DEFAULT_MODIFIER = 1.0;

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private OptimalRangeModifierManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the modifier file path
     */
    private static File getModifiersFile() {
        return ConfigManager.getOptimalRangeModifiersFile();
    }

    /**
     * Parses a modifier value from a string line
     * 
     * @param line the line to parse
     * @return the parsed modifier value, or null if parsing fails
     */
    private static Double parseModifierValue(String line) {
        try {
            double value = Double.parseDouble(line);
            if (value < 0) {
                value = 0.0; // Don't allow negative modifiers
            }
            return value;
        } catch (NumberFormatException e) {
            ErrorLogger.logError("Invalid optimal range modifier format: " + line, e);
            // Return null on parse error
            return null;
        }
    }

    /**
     * Loads optimal range modifier from file, or returns default if file doesn't exist
     */
    public static double loadOptimalRangeModifier() {
        File modifiersFile = getModifiersFile();

        if (modifiersFile == null) {
            // Path validation failed, return default
            return DEFAULT_MODIFIER;
        }

        if (!modifiersFile.exists()) {
            // Create default file
            saveOptimalRangeModifier(DEFAULT_MODIFIER);
            return DEFAULT_MODIFIER;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(modifiersFile))) {
            String line = reader.readLine();
            if (line != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    Double value = parseModifierValue(line);
                    if (value != null) {
                        return value;
                    }
                }
            }
        } catch (IOException e) {
            ErrorLogger.logError("Error loading optimal range modifier from file: "
                    + modifiersFile.getAbsolutePath(), e);
            // Return default on error
        }

        return DEFAULT_MODIFIER;
    }

    /**
     * Saves optimal range modifier to file
     */
    public static void saveOptimalRangeModifier(double modifier) {
        File modifiersFile = getModifiersFile();

        if (modifiersFile == null) {
            // Path validation failed, cannot save
            ErrorLogger.logError("Cannot save optimal range modifier: path validation failed",
                    new SecurityException("Path validation failed"));
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(modifiersFile))) {
            writer.write(String.valueOf(modifier));
        } catch (IOException e) {
            ErrorLogger.logError("Error saving optimal range modifier to file: "
                    + modifiersFile.getAbsolutePath(), e);
            // Config saving failure is logged but not critical
        }
    }

    /**
     * Gets the default modifier
     */
    public static double getDefaultModifier() {
        return DEFAULT_MODIFIER;
    }
}

