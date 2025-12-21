package ui;

import java.awt.Color;
import java.awt.Toolkit;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import calculator.MiningCalculator;
import config.ConfigManager;
import config.MinerConfig;
import config.OptimalRangeModifierManager;
import config.TierModifierManager;
import model.AnalysisResult;

/**
 * Handles display and formatting of analysis results
 */
public class AnalysisDisplay {

    private static final String TIER_STYLE_PREFIX = "tier_";
    private static final String STYLE_HEADER = "header";

    // Stat key constants
    private static final String KEY_ACTIVATION_TIME = "ActivationTime";
    private static final String KEY_MINING_AMOUNT = "MiningAmount";
    private static final String KEY_CRITICAL_SUCCESS_CHANCE = "CriticalSuccessChance";
    private static final String KEY_CRITICAL_SUCCESS_BONUS_YIELD = "CriticalSuccessBonusYield";
    private static final String KEY_RESIDUE_PROBABILITY = "ResidueProbability";
    private static final String KEY_RESIDUE_VOLUME_MULTIPLIER = "ResidueVolumeMultiplier";
    private static final String KEY_OPTIMAL_RANGE = "OptimalRange";
    private static final String KEY_MIN = "Min";
    private static final String KEY_MAX = "Max";

    // Miner type constants
    private static final String MINER_TYPE_MODULATED = "Modulated";
    private static final String MINER_TYPE_ORE = "ORE";
    private static final String MINER_TYPE_ICE = "Ice";

    // Tier constants
    private static final String TIER_S = "S";
    private static final String TIER_F = "F";

    // Format constants
    private static final String FORMAT_LABEL_COLUMN = "%-20s ";
    private static final String FORMAT_HEADER_ROW = "%-20s %-20s %-20s %-20s%n";
    private static final String FORMAT_PERFORMANCE_VALUE = "%.2f (%.1f)%6s ";
    private static final String FORMAT_COLUMN_WIDTH = "%-20s";
    private static final String FORMAT_COLUMN_WIDTH_NEWLINE = "%-20s%n";

    private final StyledDocument doc;
    private Color fgColor;
    private final Map<String, Color> tierColors;
    private final java.util.function.Consumer<Double> sellPriceUpdater;

    public AnalysisDisplay(StyledDocument doc, Color fgColor, Map<String, Color> tierColors,
            java.util.function.Consumer<Double> sellPriceUpdater) {
        this.doc = doc;
        this.fgColor = fgColor;
        this.tierColors = new java.util.HashMap<>(tierColors);
        this.sellPriceUpdater = sellPriceUpdater;
        setupTextStyles();
    }

    public void updateTheme(Color fgColor, Map<String, Color> tierColors) {
        this.fgColor = fgColor;
        this.tierColors.clear();
        this.tierColors.putAll(tierColors);
        setupTextStyles();
        // Update existing text colors in the document
        updateExistingTextColors();
    }

    private void updateExistingTextColors() {
        if (doc == null) {
            return;
        }
        try {
            int length = doc.getLength();
            if (length == 0) {
                return;
            }

            // Update character attributes for text that uses default/plain styling
            // This will update the foreground color for text inserted with appendText()
            // Special styled text (tier colors, good/bad) uses named styles which are updated in
            // setupTextStyles()
            javax.swing.text.SimpleAttributeSet newAttr = new javax.swing.text.SimpleAttributeSet();
            StyleConstants.setForeground(newAttr, fgColor);

            // Apply to the entire document - this updates the foreground color attribute
            // The 'false' parameter merges attributes rather than replacing all attributes
            doc.setCharacterAttributes(0, length, newAttr, false);

            // Also update the logical style to ensure new text uses the correct color
            Style logicalStyle = doc.getLogicalStyle(0);
            if (logicalStyle != null) {
                StyleConstants.setForeground(logicalStyle, fgColor);
            }
        } catch (Exception ignored) {
            // Ignore errors during text color update
        }
    }

    private void setupTextStyles() {
        if (doc == null) {
            return;
        }

        setupDefaultStyle();

        if (tierColors == null || tierColors.isEmpty()) {
            return;
        }

        try {
            setupTierStyles();
            setupGoodBadStyles();
            setupHeaderStyle();
        } catch (Exception ignored) {
            // Silently handle style errors - don't break the app
        }
    }

    private void setupDefaultStyle() {
        if (doc == null) {
            return;
        }
        try {
            Style defaultStyle = getOrCreateStyle("default");
            if (defaultStyle != null) {
                StyleConstants.setForeground(defaultStyle, fgColor);
            }
        } catch (Exception ignored) {
            // Ignore default style setup errors
        }
    }

    private void setupTierStyles() {
        if (doc == null || tierColors == null) {
            return;
        }
        for (Map.Entry<String, Color> entry : tierColors.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String styleName = TIER_STYLE_PREFIX + entry.getKey();
            Style style = getOrCreateStyle(styleName);
            if (style != null) {
                StyleConstants.setForeground(style, entry.getValue());
                StyleConstants.setBold(style, true);
            }
        }
    }

    private void setupGoodBadStyles() {
        if (doc == null || tierColors == null) {
            return;
        }
        Color sColor = tierColors.get(TIER_S);
        if (sColor != null) {
            Style goodStyle = getOrCreateStyle("good");
            if (goodStyle != null) {
                StyleConstants.setForeground(goodStyle, sColor);
            }
        }

        Color fColor = tierColors.get(TIER_F);
        if (fColor != null) {
            Style badStyle = getOrCreateStyle("bad");
            if (badStyle != null) {
                StyleConstants.setForeground(badStyle, fColor);
            }
        }
    }

    private void setupHeaderStyle() {
        if (doc == null) {
            return;
        }
        Style headerStyle = getOrCreateStyle(STYLE_HEADER);
        if (headerStyle != null) {
            StyleConstants.setBold(headerStyle, true);
            StyleConstants.setFontSize(headerStyle, 11);
        }
    }

    private Style getOrCreateStyle(String styleName) {
        if (doc == null || styleName == null) {
            return null;
        }
        Style style = doc.getStyle(styleName);
        if (style == null) {
            style = doc.addStyle(styleName, null);
        }
        return style;
    }

    public void displayAnalysis(AnalysisResult analysis, Map<String, Double> baseStats,
            String minerType) {
        try {
            if (doc == null || analysis == null || baseStats == null || minerType == null) {
                return;
            }

            doc.remove(0, doc.getLength());

            Map<String, Double> stats = analysis.getStats();
            if (stats == null) {
                return;
            }

            displayHeader(minerType);
            displayRollAnalysisSection(stats, baseStats, minerType);
            double baseM3Pct = displayPerformanceMetricsSection(analysis, baseStats, minerType);
            displayTierSection(analysis, minerType);
            copyToClipboard(analysis.getTier(), minerType, baseM3Pct);

            // Calculate and display sell price
            if (sellPriceUpdater != null) {
                sellPriceUpdater.accept(calculateSellPrice(analysis, baseM3Pct, minerType));
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void displayHeader(String minerType) {
        appendStyledText(repeat("=", 76) + "\n", STYLE_HEADER);
        String minerTypeLabel = MINER_TYPE_ICE.equals(minerType) ? "Ice Harvester" : "Strip Miner";
        appendStyledText("EVE Online " + minerType + " " + minerTypeLabel + " Roll Analyzer\n", STYLE_HEADER);
        appendStyledText(repeat("=", 76) + "\n\n", STYLE_HEADER);
    }

    private void displayRollAnalysisSection(Map<String, Double> stats,
            Map<String, Double> baseStats, String minerType) {
        appendStyledText("Roll Analysis:\n", STYLE_HEADER);
        appendText(String.format(FORMAT_HEADER_ROW, "Metric", "Base", "Rolled", "% Change"),
                fgColor);
        appendText(repeat("-", 76) + "\n", fgColor);

        displayMetricRow("Mining Amount", stats.get(KEY_MINING_AMOUNT),
                baseStats.get(KEY_MINING_AMOUNT), "%.1f m3", false);
        displayMetricRow("Activation Time", stats.get(KEY_ACTIVATION_TIME),
                baseStats.get(KEY_ACTIVATION_TIME), "%.1f s", true);
        displayPercentageMetricRow("Crit Chance", stats.get(KEY_CRITICAL_SUCCESS_CHANCE),
                baseStats.get(KEY_CRITICAL_SUCCESS_CHANCE), "%.2f%%", false);
        displayPercentageMetricRow("Crit Bonus", stats.get(KEY_CRITICAL_SUCCESS_BONUS_YIELD),
                baseStats.get(KEY_CRITICAL_SUCCESS_BONUS_YIELD), "%.0f%%", false);

        if (MINER_TYPE_MODULATED.equals(minerType)) {
            displayPercentageMetricRow("Residue Prob", stats.get(KEY_RESIDUE_PROBABILITY),
                    baseStats.get(KEY_RESIDUE_PROBABILITY), "%.2f%%", true);
            displayMetricRow("Residue Mult", stats.get(KEY_RESIDUE_VOLUME_MULTIPLIER),
                    baseStats.get(KEY_RESIDUE_VOLUME_MULTIPLIER), "%.3f x", true);
        }

        displayMetricRow("Optimal Range", stats.get(KEY_OPTIMAL_RANGE),
                baseStats.get(KEY_OPTIMAL_RANGE), "%.2f km", false);
        appendText("\n", fgColor);
    }

    private void displayMetricRow(String label, Double rolled, Double base, String format,
            boolean invertColor) {
        double mutation = calculatePercentageChange(rolled, base);
        String tag = getColorTag(invertColor ? -mutation : mutation);
        appendText(String.format(FORMAT_LABEL_COLUMN, label), fgColor);

        // Format base value with unit - pad to exactly 20 characters to match header
        String baseFormatted = String.format(format, base != null ? base : 0.0);
        appendText(String.format(FORMAT_COLUMN_WIDTH, baseFormatted), fgColor);

        // Format rolled value with unit - pad to exactly 20 characters to match header
        String rolledFormatted = String.format(format, rolled != null ? rolled : 0.0);
        if (tag != null) {
            appendStyledText(String.format(FORMAT_COLUMN_WIDTH, rolledFormatted), tag);
        } else {
            appendText(String.format(FORMAT_COLUMN_WIDTH, rolledFormatted), fgColor);
        }

        // Format percentage change - pad to exactly 20 characters to match header
        String percentageStr = formatPercentage(mutation);
        if (tag != null) {
            appendStyledText(String.format(FORMAT_COLUMN_WIDTH_NEWLINE, percentageStr), tag);
        } else {
            appendText(String.format(FORMAT_COLUMN_WIDTH_NEWLINE, percentageStr), fgColor);
        }
    }

    private void displayPercentageMetricRow(String label, Double rolled, Double base, String format,
            boolean invertColor) {
        double mutation = calculatePercentageChange(rolled, base);
        String tag = getColorTag(invertColor ? -mutation : mutation);
        appendText(String.format(FORMAT_LABEL_COLUMN, label), fgColor);

        // Format base value as percentage - pad to exactly 20 characters to match header
        String baseFormatted = String.format(format, (base != null ? base : 0.0) * 100);
        appendText(String.format(FORMAT_COLUMN_WIDTH, baseFormatted), fgColor);

        // Format rolled value as percentage - pad to exactly 20 characters to match header
        String rolledFormatted = String.format(format, (rolled != null ? rolled : 0.0) * 100);
        if (tag != null) {
            appendStyledText(String.format(FORMAT_COLUMN_WIDTH, rolledFormatted), tag);
        } else {
            appendText(String.format(FORMAT_COLUMN_WIDTH, rolledFormatted), fgColor);
        }

        // Format percentage change - pad to exactly 20 characters to match header
        String percentageStr = formatPercentage(mutation);
        if (tag != null) {
            appendStyledText(String.format(FORMAT_COLUMN_WIDTH_NEWLINE, percentageStr), tag);
        } else {
            appendText(String.format(FORMAT_COLUMN_WIDTH_NEWLINE, percentageStr), fgColor);
        }
    }

    private double calculatePercentageChange(Double rolled, Double base) {
        if (rolled != null && base != null && base > 0) {
            return ((rolled / base) - 1) * 100;
        }
        return 0.0;
    }

    private double displayPerformanceMetricsSection(AnalysisResult analysis,
            Map<String, Double> baseStats, String minerType) {
        appendStyledText("Performance Metrics:\n", STYLE_HEADER);
        appendText(String.format(FORMAT_HEADER_ROW, "Metric", "Base", "Rolled", "% Change"),
                fgColor);
        appendText(repeat("-", 76) + "\n", fgColor);

        Double baseMiningAmt = baseStats.get(KEY_MINING_AMOUNT);
        Double baseActTime = baseStats.get(KEY_ACTIVATION_TIME);
        if (baseMiningAmt == null || baseActTime == null || baseActTime <= 0) {
            return 0.0;
        }

        PerformanceMetrics baseMetrics =
                calculateBasePerformanceMetrics(baseStats, baseMiningAmt, baseActTime);
        double baseM3Pct = displayPerformanceMetricRow("Base M3/sec", analysis.getM3PerSec(),
                analysis.getRealWorldM3PerSec(), baseMetrics.baseM3PerSec,
                baseMetrics.realWorldM3PerSec);

        if (MINER_TYPE_MODULATED.equals(minerType) && analysis.getBasePlusCritsM3PerSec() != null) {
            displayBasePlusCritsRow(analysis, baseStats, baseMiningAmt, baseActTime);
        }

        displayPerformanceMetricRow("Effective M3/sec", analysis.getEffectiveM3PerSec(),
                analysis.getRealWorldEffectiveM3PerSec(), baseMetrics.effectiveM3PerSec,
                baseMetrics.realWorldEffectiveM3PerSec);

        appendText("\n", fgColor);
        return baseM3Pct;
    }

    private PerformanceMetrics calculateBasePerformanceMetrics(Map<String, Double> baseStats,
            Double baseMiningAmt, Double baseActTime) {
        double baseM3PerSec = MiningCalculator.calculateBaseM3PerSec(baseMiningAmt, baseActTime);
        double baseEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(baseMiningAmt,
                baseActTime, baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_CHANCE, 0.0),
                baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_BONUS_YIELD, 0.0),
                baseStats.getOrDefault(KEY_RESIDUE_PROBABILITY, 0.0),
                baseStats.getOrDefault(KEY_RESIDUE_VOLUME_MULTIPLIER, 0.0));
        double baseRealWorldM3PerSec =
                MiningCalculator.calculateRealWorldBaseM3PerSec(baseMiningAmt, baseActTime);
        double baseRealWorldEffectiveM3PerSec =
                MiningCalculator.calculateRealWorldEffectiveM3PerSec(baseMiningAmt, baseActTime,
                        baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_CHANCE, 0.0),
                        baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_BONUS_YIELD, 0.0),
                        baseStats.getOrDefault(KEY_RESIDUE_PROBABILITY, 0.0),
                        baseStats.getOrDefault(KEY_RESIDUE_VOLUME_MULTIPLIER, 0.0));
        return new PerformanceMetrics(baseM3PerSec, baseEffectiveM3PerSec, baseRealWorldM3PerSec,
                baseRealWorldEffectiveM3PerSec);
    }

    private double displayPerformanceMetricRow(String label, double rolled, double realWorldRolled,
            double base, double realWorldBase) {
        double pct = base > 0 ? ((rolled / base) - 1) * 100 : 0;
        String tag = getColorTag(pct);
        appendText(String.format(FORMAT_LABEL_COLUMN, label), fgColor);
        appendText(String.format(FORMAT_PERFORMANCE_VALUE, base, realWorldBase, ""), fgColor);
        displayPerformanceValue(rolled, realWorldRolled, pct, tag);
        return pct;
    }

    private void displayPerformanceValue(double rolled, double realWorldRolled, double pct,
            String tag) {
        if (tag != null) {
            appendStyledText(String.format(FORMAT_PERFORMANCE_VALUE, rolled, realWorldRolled, ""),
                    tag);
            appendStyledText(formatPercentage(pct) + "\n", tag);
        } else {
            appendText(String.format(FORMAT_PERFORMANCE_VALUE, rolled, realWorldRolled, ""),
                    fgColor);
            appendText(formatPercentage(pct) + "\n", fgColor);
        }
    }

    private void displayBasePlusCritsRow(AnalysisResult analysis, Map<String, Double> baseStats,
            Double baseMiningAmt, Double baseActTime) {
        double baseBasePlusCrits = MiningCalculator.calculateBasePlusCritsM3PerSec(baseMiningAmt,
                baseActTime, baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_CHANCE, 0.0),
                baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_BONUS_YIELD, 0.0));
        double baseRealWorldBasePlusCrits =
                MiningCalculator.calculateRealWorldEffectiveM3PerSec(baseMiningAmt, baseActTime,
                        baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_CHANCE, 0.0),
                        baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_BONUS_YIELD, 0.0), 0.0, 0.0);
        double basePlusCritsPct = baseBasePlusCrits > 0
                ? ((analysis.getBasePlusCritsM3PerSec() / baseBasePlusCrits) - 1) * 100
                : 0;
        String tag = getColorTag(basePlusCritsPct);
        appendText(String.format(FORMAT_LABEL_COLUMN, "Base + Crits M3/s"), fgColor);
        appendText(String.format(FORMAT_PERFORMANCE_VALUE, baseBasePlusCrits,
                baseRealWorldBasePlusCrits, ""), fgColor);
        displayPerformanceValue(analysis.getBasePlusCritsM3PerSec(),
                analysis.getRealWorldBasePlusCritsM3PerSec(), basePlusCritsPct, tag);
    }

    private void displayTierSection(AnalysisResult analysis, String minerType) {
        String tier = analysis.getTier() != null ? analysis.getTier() : TIER_F;
        // Strip "+" suffix for tier range lookup
        String tierForLookup = tier.endsWith("+") ? tier.substring(0, tier.length() - 1) : tier;
        String tierRangeStr = formatTierRange(tierForLookup, minerType);

        appendStyledText("Tier: ", STYLE_HEADER);
        // Use tierForLookup for style lookup, but display full tier with "+" if present
        appendStyledText(tier + "\n", TIER_STYLE_PREFIX + tierForLookup);
        if (!tierRangeStr.isEmpty()) {
            appendStyledText("(" + tierRangeStr + ")\n", TIER_STYLE_PREFIX + tierForLookup);
        }
        appendText("\n" + repeat("=", 76) + "\n", fgColor);
    }

    private String formatTierRange(String tier, String minerType) {
        Map<String, Map<String, Double>> tierRanges = MinerConfig.getTierRanges(minerType);
        Map<String, Double> tierRange = tierRanges != null ? tierRanges.get(tier) : null;

        if (tierRange == null) {
            return "";
        }

        Double min = tierRange.get(KEY_MIN);
        Double max = tierRange.get(KEY_MAX);
        if (min == null || max == null) {
            return "";
        }

        if (TIER_S.equals(tier)) {
            return String.format("%.2f-%.2f+ m³/s", min, max);
        } else if (TIER_F.equals(tier)) {
            return String.format("<%.2f m³/s", max);
        } else {
            return String.format("%.2f-%.5f m³/s", min, max);
        }
    }

    private void copyToClipboard(String tier, String minerType, double baseM3Pct) {
        String tierDisplay;
        if (tier == null) {
            tierDisplay = TIER_F;
        } else if (TIER_S.equals(tier) || "S+".equals(tier)) {
            // Handle S tier - show as +S in clipboard
            tierDisplay = tier.endsWith("+") ? "+S" : "+S";
        } else {
            // For other tiers, keep the "+" suffix if present
            tierDisplay = tier;
        }
        String minerLabel;
        if (MINER_TYPE_ORE.equals(minerType)) {
            minerLabel = "[ORE]";
        } else if (MINER_TYPE_ICE.equals(minerType)) {
            minerLabel = "[Ice]";
        } else {
            minerLabel = "[Modulated]";
        }
        String clipboardText =
                tierDisplay + ": (" + formatPercentage(baseM3Pct) + ") " + minerLabel;
        try {
            java.awt.datatransfer.StringSelection selection =
                    new java.awt.datatransfer.StringSelection(clipboardText);
            java.awt.datatransfer.Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
        } catch (Exception ignored) {
            // Ignore clipboard errors
        }
    }

    private static class PerformanceMetrics {
        final double baseM3PerSec;
        final double effectiveM3PerSec;
        final double realWorldM3PerSec;
        final double realWorldEffectiveM3PerSec;

        PerformanceMetrics(double baseM3PerSec, double effectiveM3PerSec, double realWorldM3PerSec,
                double realWorldEffectiveM3PerSec) {
            this.baseM3PerSec = baseM3PerSec;
            this.effectiveM3PerSec = effectiveM3PerSec;
            this.realWorldM3PerSec = realWorldM3PerSec;
            this.realWorldEffectiveM3PerSec = realWorldEffectiveM3PerSec;
        }
    }

    public void appendText(String text, Color color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            // Ensure we have a valid color - use fgColor as fallback if color is null or invalid
            Color textColor = color;
            if (textColor == null || (textColor.getRed() == textColor.getGreen()
                    && textColor.getGreen() == textColor.getBlue() && textColor.getRed() > 200)) {
                // If color is null or too light (close to white), use fgColor
                textColor = fgColor;
            }

            // Create a completely fresh attribute set with ONLY the foreground color
            // This ensures no inheritance from parent styles
            javax.swing.text.SimpleAttributeSet attr = new javax.swing.text.SimpleAttributeSet();
            StyleConstants.setForeground(attr, textColor);
            // Explicitly set other attributes to avoid inheritance issues
            StyleConstants.setBold(attr, false);
            StyleConstants.setItalic(attr, false);

            doc.insertString(doc.getLength(), text, attr);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            // Last resort fallback - insert with no attributes and hope component's foreground
            // works
            try {
                doc.insertString(doc.getLength(), text, null);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void appendStyledText(String text, String styleName) {
        try {
            Style style = doc.getStyle(styleName);
            if (style == null) {
                style = doc.addStyle(styleName, null);
            }
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String formatPercentage(double value) {
        // Format percentage - will be padded to 20 chars in the calling method
        if (value > 0.1) {
            if (value < 10.0) {
                return String.format("+%04.1f%%", value); // e.g., +05.3%
            } else {
                return String.format("+%.1f%%", value); // e.g., +22.4% or +123.4%
            }
        } else if (value < -0.1) {
            double absValue = Math.abs(value);
            if (absValue < 10.0) {
                return String.format("-%04.1f%%", absValue); // e.g., -04.1%
            } else {
                return String.format("%.1f%%", value); // e.g., -07.5% or -123.4%
            }
        } else {
            return "+0.0%";
        }
    }

    private String getColorTag(double value) {
        if (value > 0.1) {
            return "good";
        } else if (value < -0.1) {
            return "bad";
        }
        return null;
    }

    /**
     * Calculates sell price based on formula: cost * tier_modifier * (100% + baseM3Pct%) * optimal_range_modifier (if tier has "+")
     */
    private double calculateSellPrice(AnalysisResult analysis, double baseM3Pct, String minerType) {
        double cost = ConfigManager.getRollCost();
        if (cost <= 0) {
            return 0.0;
        }

        String tier = analysis.getTier() != null ? analysis.getTier() : "F";
        // Remove "+" suffix if present for tier modifier lookup
        String tierForModifier = tier.endsWith("+") ? tier.substring(0, tier.length() - 1) : tier;
        double tierModifier = TierModifierManager.getModifierForTier(tierForModifier);

        // Check if tier has "+" suffix (optimal range increased) and apply modifier
        double optimalRangeModifier = 1.0;
        if (tier.endsWith("+")) {
            optimalRangeModifier = OptimalRangeModifierManager.loadOptimalRangeModifier();
        }

        // Formula: cost * tier_modifier * (100% + baseM3Pct%) * optimal_range_modifier (if applicable)
        // baseM3Pct is already a percentage, so convert to multiplier: 1 + (baseM3Pct / 100)
        double m3Multiplier = 1.0 + (baseM3Pct / 100.0);
        return cost * tierModifier * m3Multiplier * optimalRangeModifier;
    }
}

