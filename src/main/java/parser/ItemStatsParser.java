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
    // Capture the value part completely (numbers with commas, time formats, etc.)
    private static final Pattern PATTERN_TAB_SEPARATED =
            Pattern.compile("^(.+?)\\t+(.+)$");
    private static final Pattern PATTERN_SPACE_SEPARATED =
            Pattern.compile("^(.+?)\\s+(.+)$");
    private static final Pattern PATTERN_MULTIPLE_SPACES =
            Pattern.compile("^(.+?)\\s{2,}(.+)$");
    
    // Pattern for time format: Xm Ys or XmYs
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*m\\s*(\\d+)\\s*s", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPACT_TIME_PATTERN = Pattern.compile("(\\d+)m(\\d+)s", Pattern.CASE_INSENSITIVE);
    
    // Pattern for validating that a value starts with a digit
    private static final String PATTERN_STARTS_WITH_DIGIT = "^\\d.*";

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
            // Verify the value part looks valid (starts with digit or is time format)
            String valuePart = matcher.group(2).trim();
            if (valuePart.matches(PATTERN_STARTS_WITH_DIGIT) || TIME_PATTERN.matcher(valuePart).find() 
                    || COMPACT_TIME_PATTERN.matcher(valuePart).find()) {
                return matcher;
            }
        }

        matcher = PATTERN_MULTIPLE_SPACES.matcher(line);
        if (matcher.find()) {
            // Verify the value part looks valid (starts with digit or is time format)
            String valuePart = matcher.group(2).trim();
            if (valuePart.matches(PATTERN_STARTS_WITH_DIGIT) || TIME_PATTERN.matcher(valuePart).find() 
                    || COMPACT_TIME_PATTERN.matcher(valuePart).find()) {
                return matcher;
            }
        }

        matcher = PATTERN_SPACE_SEPARATED.matcher(line);
        if (matcher.find()) {
            // Verify the value part looks valid (starts with digit or is time format)
            // This is last because it's the least specific
            String valuePart = matcher.group(2).trim();
            if (valuePart.matches(PATTERN_STARTS_WITH_DIGIT) || TIME_PATTERN.matcher(valuePart).find() 
                    || COMPACT_TIME_PATTERN.matcher(valuePart).find()) {
                return matcher;
            }
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
            String statName = matchStatName(label.toLowerCase());

            if (statName == null) {
                return;
            }

            // Special handling for ActivationTime - check for time format (Xm Ys)
            if ("ActivationTime".equals(statName)) {
                Double timeValue = parseTimeFormat(valueStr);
                if (timeValue != null) {
                    stats.put(statName, timeValue);
                    return;
                }
            }

            // Extract numeric value from the string
            // Handle comma-separated numbers (e.g., "1,000 m3" -> 1000)
            valueStr = valueStr.replace(",", ""); // Remove commas
            
            // Extract number pattern (digits and decimal point)
            // This handles formats like "1000 m3", "12.5 km", etc.
            java.util.regex.Pattern numberPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
            Matcher numberMatcher = numberPattern.matcher(valueStr);
            
            if (numberMatcher.find()) {
                String numberStr = numberMatcher.group(1);
                double numValue = Double.parseDouble(numberStr);
                double statValue = getStatValue(statName, numValue);
                stats.put(statName, statValue);
            }
        } catch (NumberFormatException ignored) {
            // Skip lines that can't be parsed - this is normal
        }
    }

    /**
     * Parses time format like "3m 20s" or "3m20s" and converts to seconds
     * 
     * @param timeStr The time string to parse
     * @return The time in seconds, or null if format doesn't match
     */
    private static Double parseTimeFormat(String timeStr) {
        // Try format with spaces: "3m 20s"
        Matcher timeMatcher = TIME_PATTERN.matcher(timeStr);
        if (timeMatcher.find()) {
            try {
                int minutes = Integer.parseInt(timeMatcher.group(1));
                int seconds = Integer.parseInt(timeMatcher.group(2));
                return (double) (minutes * 60 + seconds);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        
        // Try compact format without spaces: "3m20s"
        Matcher compactMatcher = COMPACT_TIME_PATTERN.matcher(timeStr);
        if (compactMatcher.find()) {
            try {
                int minutes = Integer.parseInt(compactMatcher.group(1));
                int seconds = Integer.parseInt(compactMatcher.group(2));
                return (double) (minutes * 60 + seconds);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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


