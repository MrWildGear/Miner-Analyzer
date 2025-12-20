package parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for extracting item stats from clipboard text
 */
public class ItemStatsParser {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private ItemStatsParser() {
        // Utility class - no instantiation
    }

    // Patterns for different text formats
    private static final Pattern PATTERN_TAB_SEPARATED =
            Pattern.compile("^(.+?)\\t+(\\d+\\.?\\d*)");
    private static final Pattern PATTERN_SPACE_SEPARATED =
            Pattern.compile("^(.+?)\\s+(\\d+\\.?\\d*)");
    private static final Pattern PATTERN_MULTIPLE_SPACES =
            Pattern.compile("^(.+?)\\s{2,}(\\d+\\.?\\d*)");

    /**
     * Parses item stats from clipboard text
     * 
     * @param clipboardText The text from clipboard
     * @return Map of stat names to values
     */
    public static Map<String, Double> parseItemStats(String clipboardText) {
        Map<String, Double> stats = new HashMap<>();
        if (clipboardText == null || clipboardText.trim().isEmpty()) {
            return stats;
        }

        String[] lines = clipboardText.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                Matcher matcher = findMatcherForLine(line);
                if (matcher != null) {
                    processStatLine(matcher, stats);
                }
            }
        }

        return stats;
    }

    /**
     * Finds a matching pattern for the given line
     * 
     * @param line The line to match
     * @return Matcher if a pattern matches, null otherwise
     */
    private static Matcher findMatcherForLine(String line) {
        Matcher matcher = PATTERN_TAB_SEPARATED.matcher(line);
        if (matcher.find()) {
            return matcher;
        }

        matcher = PATTERN_SPACE_SEPARATED.matcher(line);
        if (matcher.find()) {
            return matcher;
        }

        matcher = PATTERN_MULTIPLE_SPACES.matcher(line);
        if (matcher.find()) {
            return matcher;
        }

        return null;
    }

    /**
     * Processes a stat line and adds it to the stats map if valid
     * 
     * @param matcher The matcher containing the parsed groups
     * @param stats The map to add the stat to
     */
    private static void processStatLine(Matcher matcher, Map<String, Double> stats) {
        try {
            String label = matcher.group(1).trim();
            String valueStr = matcher.group(2).trim();
            // Remove any trailing units or characters (keep decimal point)
            valueStr = valueStr.replaceAll("[^0-9.]", "");
            if (valueStr.isEmpty()) {
                return;
            }

            double numValue = Double.parseDouble(valueStr);
            String statName = matchStatName(label.toLowerCase());

            if (statName != null) {
                double statValue = getStatValue(statName, numValue);
                stats.put(statName, statValue);
            }
        } catch (NumberFormatException _) {
            // Skip lines that can't be parsed - this is normal
        }
    }

    /**
     * Matches a label to a stat name
     * 
     * @param labelLower The lowercase label to match
     * @return The stat name if matched, null otherwise
     */
    private static String matchStatName(String labelLower) {
        if (labelLower.matches("^activation\\s+cost.*")) {
            return "ActivationCost";
        }
        if (labelLower.matches(".*activation\\s+time.*")
                || (labelLower.matches(".*duration.*") && !labelLower.contains("residue"))) {
            return "ActivationTime";
        }
        if (labelLower.matches("^mining\\s+amount.*")) {
            return "MiningAmount";
        }
        if (labelLower.matches(".*critical\\s+success\\s+chance.*")) {
            return "CriticalSuccessChance";
        }
        if (labelLower.matches(".*critical\\s+success\\s+bonus\\s+yield.*")) {
            return "CriticalSuccessBonusYield";
        }
        if (labelLower.matches("^optimal\\s+range.*")) {
            return "OptimalRange";
        }
        if (labelLower.matches(".*residue\\s+probability.*")) {
            return "ResidueProbability";
        }
        if (labelLower.matches(".*residue\\s+volume\\s+multiplier.*")) {
            return "ResidueVolumeMultiplier";
        }
        return null;
    }

    /**
     * Gets the processed stat value (applies percentage conversion if needed)
     * 
     * @param statName The stat name
     * @param numValue The raw numeric value
     * @return The processed stat value
     */
    private static double getStatValue(String statName, double numValue) {
        if ("CriticalSuccessChance".equals(statName) || "CriticalSuccessBonusYield".equals(statName)
                || "ResidueProbability".equals(statName)) {
            return numValue / 100.0;
        }
        return numValue;
    }
}

