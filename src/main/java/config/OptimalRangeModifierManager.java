package config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages optimal range modifier for sell price calculation
 * Applies when tier has "+" suffix (optimal range is increased)
 */
public class OptimalRangeModifierManager {

    private static final double DEFAULT_MODIFIER = 1.0;

    /**
     * Gets the modifier file path
     */
    private static File getModifiersFile() {
        return ConfigManager.getOptimalRangeModifiersFile();
    }

    /**
     * Loads optimal range modifier from file, or returns default if file doesn't exist
     */
    public static double loadOptimalRangeModifier() {
        File modifiersFile = getModifiersFile();

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
                    try {
                        double value = Double.parseDouble(line);
                        if (value < 0) {
                            value = 0.0; // Don't allow negative modifiers
                        }
                        return value;
                    } catch (NumberFormatException ignored) {
                        // Return default on parse error
                    }
                }
            }
        } catch (IOException ignored) {
            // Return default on error
        }

        return DEFAULT_MODIFIER;
    }

    /**
     * Saves optimal range modifier to file
     */
    public static void saveOptimalRangeModifier(double modifier) {
        File modifiersFile = getModifiersFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(modifiersFile))) {
            writer.write(String.valueOf(modifier));
        } catch (IOException ignored) {
            // Silently fail
        }
    }

    /**
     * Gets the default modifier
     */
    public static double getDefaultModifier() {
        return DEFAULT_MODIFIER;
    }
}

