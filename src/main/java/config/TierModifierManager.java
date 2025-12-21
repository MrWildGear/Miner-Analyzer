package config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages tier modifiers for sell price calculation
 */
public class TierModifierManager {

    private static final Map<String, Double> DEFAULT_MODIFIERS = new HashMap<>();

    static {
        DEFAULT_MODIFIERS.put("S", 2.0);
        DEFAULT_MODIFIERS.put("A", 1.8);
        DEFAULT_MODIFIERS.put("B", 1.6);
        DEFAULT_MODIFIERS.put("C", 1.4);
        DEFAULT_MODIFIERS.put("D", 1.2);
        DEFAULT_MODIFIERS.put("E", 1.0);
        DEFAULT_MODIFIERS.put("F", 0.8);
    }

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private TierModifierManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Loads tier modifiers from file, or returns defaults if file doesn't exist
     */
    public static Map<String, Double> loadTierModifiers() {
        File modifiersFile = ConfigManager.getTierModifiersFile();
        Map<String, Double> modifiers = new HashMap<>(DEFAULT_MODIFIERS);

        if (!modifiersFile.exists()) {
            // Create default file
            saveTierModifiers(modifiers);
            return modifiers;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(modifiersFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String tier = parts[0].trim().toUpperCase();
                    parseAndAddModifier(tier, parts[1], modifiers);
                }
            }
        } catch (IOException ignored) {
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
        } catch (NumberFormatException ignored) {
            // Skip invalid lines
        }
    }

    /**
     * Saves tier modifiers to file
     */
    public static void saveTierModifiers(Map<String, Double> modifiers) {
        File modifiersFile = ConfigManager.getTierModifiersFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(modifiersFile))) {
            // Write in tier order: S, A, B, C, D, E, F
            String[] tierOrder = {"S", "A", "B", "C", "D", "E", "F"};
            for (String tier : tierOrder) {
                Double value = modifiers.getOrDefault(tier, DEFAULT_MODIFIERS.get(tier));
                writer.write(tier + "=" + value);
                writer.newLine();
            }
        } catch (IOException ignored) {
            // Silently fail
        }
    }

    /**
     * Gets the modifier for a specific tier
     */
    public static double getModifierForTier(String tier) {
        Map<String, Double> modifiers = loadTierModifiers();
        return modifiers.getOrDefault(tier != null ? tier.toUpperCase() : "F",
                DEFAULT_MODIFIERS.get("F"));
    }

    /**
     * Gets default modifiers
     */
    public static Map<String, Double> getDefaultModifiers() {
        return new HashMap<>(DEFAULT_MODIFIERS);
    }
}

