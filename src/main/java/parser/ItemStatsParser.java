package parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for extracting item stats from clipboard text
 */
public class ItemStatsParser {
    
    // Patterns for different text formats
    private static final Pattern PATTERN_TAB_SEPARATED = Pattern.compile("^(.+?)\\t+(\\d+\\.?\\d*)");
    private static final Pattern PATTERN_SPACE_SEPARATED = Pattern.compile("^(.+?)\\s+(\\d+\\.?\\d*)");
    private static final Pattern PATTERN_MULTIPLE_SPACES = Pattern.compile("^(.+?)\\s{2,}(\\d+\\.?\\d*)");
    
    /**
     * Parses item stats from clipboard text
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
            if (line.isEmpty()) continue;
            
            Matcher matcher = null;
            boolean found = false;
            
            // Try different patterns in order
            matcher = PATTERN_TAB_SEPARATED.matcher(line);
            if (matcher.find()) {
                found = true;
            } else {
                matcher = PATTERN_SPACE_SEPARATED.matcher(line);
                if (matcher.find()) {
                    found = true;
                } else {
                    matcher = PATTERN_MULTIPLE_SPACES.matcher(line);
                    if (matcher.find()) {
                        found = true;
                    }
                }
            }
            
            if (found && matcher != null) {
                try {
                    String label = matcher.group(1).trim();
                    String valueStr = matcher.group(2).trim();
                    // Remove any trailing units or characters (keep decimal point)
                    valueStr = valueStr.replaceAll("[^0-9.]", "");
                    if (valueStr.isEmpty()) continue;
                    
                    double numValue = Double.parseDouble(valueStr);
                    String labelLower = label.toLowerCase();
                    
                    // Match stat names
                    if (labelLower.matches("^activation\\s+cost.*")) {
                        stats.put("ActivationCost", numValue);
                    } else if (labelLower.matches(".*activation\\s+time.*") || 
                              (labelLower.matches(".*duration.*") && !labelLower.contains("residue"))) {
                        stats.put("ActivationTime", numValue);
                    } else if (labelLower.matches("^mining\\s+amount.*")) {
                        stats.put("MiningAmount", numValue);
                    } else if (labelLower.matches(".*critical\\s+success\\s+chance.*")) {
                        stats.put("CriticalSuccessChance", numValue / 100.0);
                    } else if (labelLower.matches(".*critical\\s+success\\s+bonus\\s+yield.*")) {
                        stats.put("CriticalSuccessBonusYield", numValue / 100.0);
                    } else if (labelLower.matches("^optimal\\s+range.*")) {
                        stats.put("OptimalRange", numValue);
                    } else if (labelLower.matches(".*residue\\s+probability.*")) {
                        stats.put("ResidueProbability", numValue / 100.0);
                    } else if (labelLower.matches(".*residue\\s+volume\\s+multiplier.*")) {
                        stats.put("ResidueVolumeMultiplier", numValue);
                    }
                } catch (NumberFormatException e) {
                    // Skip lines that can't be parsed - this is normal
                    continue;
                }
            }
        }
        
        return stats;
    }
}

