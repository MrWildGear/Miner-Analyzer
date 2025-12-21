package config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import util.ErrorLogger;

/**
 * Manages tier modifiers for sell price calculation
 */
public class TierModifierManager {

    private static final Map<String, Double> DEFAULT_MODIFIERS = new HashMap<>();

    // Tier string constants
    private static final String TIER_S = "S";
    private static final String TIER_A = "A";
    private static final String TIER_B = "B";
    private static final String TIER_C = "C";
    private static final String TIER_D = "D";
    private static final String TIER_E = "E";
    private static final String TIER_F = "F";

    // Default modifier values
    private static final double MODIFIER_S = 2.0;
    private static final double MODIFIER_A = 1.8;
    private static final double MODIFIER_B = 1.6;
    private static final double MODIFIER_C = 1.4;
    private static final double MODIFIER_D = 1.2;
    private static final double MODIFIER_E = 1.0;
    private static final double MODIFIER_F = 0.8;

    // File format constants
    private static final String COMMENT_PREFIX = "#";
    private static final String KEY_VALUE_SEPARATOR = "=";

    static {
        DEFAULT_MODIFIERS.put(TIER_S, MODIFIER_S);
        DEFAULT_MODIFIERS.put(TIER_A, MODIFIER_A);
        DEFAULT_MODIFIERS.put(TIER_B, MODIFIER_B);
        DEFAULT_MODIFIERS.put(TIER_C, MODIFIER_C);
        DEFAULT_MODIFIERS.put(TIER_D, MODIFIER_D);
        DEFAULT_MODIFIERS.put(TIER_E, MODIFIER_E);
        DEFAULT_MODIFIERS.put(TIER_F, MODIFIER_F);
    }

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private TierModifierManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Loads tier modifiers from file, or returns defaults if file doesn't exist.
     * 
     * @return A map of tier letters (S, A, B, C, D, E, F) to their modifier values
     */
    public static Map<String, Double> loadTierModifiers() {
        File modifiersFile = ConfigManager.getTierModifiersFile();
        Map<String, Double> modifiers = new HashMap<>(DEFAULT_MODIFIERS);

        if (modifiersFile == null) {
            // Path validation failed, return defaults
            return modifiers;
        }

        if (!modifiersFile.exists()) {
            // Create default file
            saveTierModifiers(modifiers);
            return modifiers;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(modifiersFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split(KEY_VALUE_SEPARATOR);
                if (parts.length == 2) {
                    String tier = parts[0].trim().toUpperCase();
                    parseAndAddModifier(tier, parts[1], modifiers);
                }
            }
        } catch (IOException e) {
            ErrorLogger.logError(
                    "Error loading tier modifiers from file: " + modifiersFile.getAbsolutePath(),
                    e);
            // Return defaults on error
        }

        return modifiers;
    }

    /**
     * Parses a modifier value and adds it to the modifiers map if valid
     */
    private static void parseAndAddModifier(String tier, String valueStr,
            Map<String, Double> modifiers) {
        try {
            double value = Double.parseDouble(valueStr.trim());
            if (DEFAULT_MODIFIERS.containsKey(tier)) {
                modifiers.put(tier, value);
            }
        } catch (NumberFormatException e) {
            ErrorLogger.logError("Invalid tier modifier format for tier: " + tier, e);
            // Skip invalid lines
        }
    }

    /**
     * Saves tier modifiers to file.
     * 
     * @param modifiers A map of tier letters to modifier values to save
     */
    public static void saveTierModifiers(Map<String, Double> modifiers) {
        File modifiersFile = ConfigManager.getTierModifiersFile();

        if (modifiersFile == null) {
            // Path validation failed, cannot save
            ErrorLogger.logError("Cannot save tier modifiers: path validation failed",
                    new SecurityException("Path validation failed"));
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(modifiersFile))) {
            // Write in tier order: S, A, B, C, D, E, F
            String[] tierOrder = {TIER_S, TIER_A, TIER_B, TIER_C, TIER_D, TIER_E, TIER_F};
            for (String tier : tierOrder) {
                Double value = modifiers.getOrDefault(tier, DEFAULT_MODIFIERS.get(tier));
                writer.write(tier + KEY_VALUE_SEPARATOR + value);
                writer.newLine();
            }
        } catch (IOException e) {
            ErrorLogger.logError(
                    "Error saving tier modifiers to file: " + modifiersFile.getAbsolutePath(), e);
            // Config saving failure is logged but not critical
        }
    }

    /**
     * Gets the modifier for a specific tier.
     * 
     * @param tier The tier letter (S, A, B, C, D, E, or F)
     * @return The modifier value for the tier, or the default F tier modifier if tier is invalid
     */
    public static double getModifierForTier(String tier) {
        Map<String, Double> modifiers = loadTierModifiers();
        return modifiers.getOrDefault(tier != null ? tier.toUpperCase() : TIER_F,
                DEFAULT_MODIFIERS.get(TIER_F));
    }

    /**
     * Gets default modifiers.
     * 
     * @return A copy of the default tier modifier map
     */
    public static Map<String, Double> getDefaultModifiers() {
        return new HashMap<>(DEFAULT_MODIFIERS);
    }
}

