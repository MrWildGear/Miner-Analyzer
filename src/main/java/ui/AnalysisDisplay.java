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
import util.ErrorLogger;

/**
 * Handles display and formatting of analysis results
 */
public class AnalysisDisplay {

    private static final String TIER_STYLE_PREFIX = "tier_";
    private static final String STYLE_HEADER = "header";
    private static final String STYLE_DEFAULT = "default";
    private static final String STYLE_GOOD = "good";
    private static final String STYLE_BAD = "bad";

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

    // Percentage calculation constants
    private static final double PERCENTAGE_MULTIPLIER = 100.0; // Convert decimal to percentage
                                                               // (e.g., 0.05 -> 5%)
    private static final double PERCENTAGE_THRESHOLD = 0.1; // Threshold for showing colored
                                                            // percentage (0.1%)
    private static final double PERCENTAGE_DIVISOR = 100.0; // Convert percentage to multiplier
                                                            // (e.g., 5% -> 1.05)
    private static final double DEFAULT_MULTIPLIER = 1.0; // Default multiplier value

    private final StyledDocument doc;
    private Color fgColor;
    private final Map<String, Color> tierColors;
    private final java.util.function.Consumer<Double> sellPriceUpdater;

    // Style cache for frequently accessed styles to improve performance
    // Using ConcurrentHashMap for thread safety (accessed from EDT)
    private final Map<String, Style> styleCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Constructs an AnalysisDisplay with the given document, colors, and sell price updater.
     * 
     * @param doc The styled document to display analysis results in
     * @param fgColor The default foreground (text) color
     * @param tierColors A map of tier letters (S, A, B, C, D, E, F) to their display colors
     * @param sellPriceUpdater A consumer function to update the sell price display
     */
    public AnalysisDisplay(StyledDocument doc, Color fgColor, Map<String, Color> tierColors,
            java.util.function.Consumer<Double> sellPriceUpdater) {
        this.doc = doc;
        this.fgColor = fgColor;
        this.tierColors = new java.util.HashMap<>(tierColors);
        this.sellPriceUpdater = sellPriceUpdater;
        setupTextStyles();
    }

    /**
     * Updates the theme colors and refreshes the display styles.
     * 
     * @param fgColor The new foreground (text) color
     * @param tierColors A map of tier letters to their new display colors
     */
    public void updateTheme(Color fgColor, Map<String, Color> tierColors) {
        this.fgColor = fgColor;
        this.tierColors.clear();
        this.tierColors.putAll(tierColors);
        // Clear style cache when theme changes
        styleCache.clear();
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
        } catch (Exception e) {
            ErrorLogger.logError("Error updating existing text colors", e);
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
        } catch (Exception e) {
            ErrorLogger.logError("Error setting up text styles", e);
        }
    }

    private void setupDefaultStyle() {
        if (doc == null) {
            return;
        }
        try {
            Style defaultStyle = getOrCreateStyle(STYLE_DEFAULT);
            if (defaultStyle != null) {
                StyleConstants.setForeground(defaultStyle, fgColor);
            }
        } catch (Exception e) {
            ErrorLogger.logError("Error setting up default style", e);
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
            Style goodStyle = getOrCreateStyle(STYLE_GOOD);
            if (goodStyle != null) {
                StyleConstants.setForeground(goodStyle, sColor);
            }
        }

        Color fColor = tierColors.get(TIER_F);
        if (fColor != null) {
            Style badStyle = getOrCreateStyle(STYLE_BAD);
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
        // Check cache first for performance
        Style style = styleCache.get(styleName);
        if (style != null) {
            return style;
        }
        // If not in cache, get or create from document
        style = doc.getStyle(styleName);
        if (style == null) {
            style = doc.addStyle(styleName, null);
        }
        // Cache the style for future access
        if (style != null) {
            styleCache.put(styleName, style);
        }
        return style;
    }

    /**
     * Displays the analysis results in the document. Formats and shows roll analysis, performance
     * metrics, and tier information. Also calculates and updates the sell price.
     * 
     * @param analysis The analysis result to display
     * @param baseStats The base stats for the miner type for comparison
     * @param minerType The miner type ("ORE", "Modulated", or "Ice")
     */
    public void displayAnalysis(AnalysisResult analysis, Map<String, Double> baseStats,
            String minerType) {
        try {
            if (doc == null || analysis == null || baseStats == null || minerType == null) {
                return;
            }

            doc.remove(0, doc.getLength());

            Map<String, Double> stats = analysis.getStats();
            if (stats == null || stats.isEmpty()) {
                return;
            }

            displayHeader(minerType);
            displayRollAnalysisSection(stats, baseStats, minerType);
            double baseM3Pct = displayPerformanceMetricsSection(analysis, baseStats, minerType);
            displayTierSection(analysis, minerType);

            String tier = analysis.getTier();
            if (tier != null) {
                copyToClipboard(tier, minerType, baseM3Pct, stats, baseStats);
            }

            // Calculate and display sell price
            if (sellPriceUpdater != null) {
                sellPriceUpdater.accept(calculateSellPrice(analysis, baseM3Pct));
            }

        } catch (BadLocationException e) {
            ErrorLogger.logError("Error displaying analysis results", e);
        }
    }

    private void displayHeader(String minerType) {
        appendStyledText(repeat("=", 76) + "\n", STYLE_HEADER);
        String minerTypeLabel = MINER_TYPE_ICE.equals(minerType) ? "Ice Harvester" : "Strip Miner";
        appendStyledText("EVE Online " + minerType + " " + minerTypeLabel + " Roll Analyzer\n",
                STYLE_HEADER);
        appendStyledText(repeat("=", 76) + "\n\n", STYLE_HEADER);
    }

    private void displayRollAnalysisSection(Map<String, Double> stats,
            Map<String, Double> baseStats, String minerType) {
        if (stats == null || baseStats == null) {
            return;
        }

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
        String baseFormatted =
                String.format(format, (base != null ? base : 0.0) * PERCENTAGE_MULTIPLIER);
        appendText(String.format(FORMAT_COLUMN_WIDTH, baseFormatted), fgColor);

        // Format rolled value as percentage - pad to exactly 20 characters to match header
        String rolledFormatted =
                String.format(format, (rolled != null ? rolled : 0.0) * PERCENTAGE_MULTIPLIER);
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
            return ((rolled / base) - 1) * PERCENTAGE_MULTIPLIER;
        }
        return 0.0;
    }

    private double displayPerformanceMetricsSection(AnalysisResult analysis,
            Map<String, Double> baseStats, String minerType) {
        if (analysis == null || baseStats == null) {
            return 0.0;
        }

        appendStyledText("Performance Metrics:\n", STYLE_HEADER);
        appendText(String.format(FORMAT_HEADER_ROW, "Metric", "Base", "Rolled", "% Change"),
                fgColor);
        appendText(repeat("-", 76) + "\n", fgColor);

        // Extract base values fresh each time to avoid any potential state issues
        Double baseMiningAmt = baseStats.get(KEY_MINING_AMOUNT);
        Double baseActTime = baseStats.get(KEY_ACTIVATION_TIME);
        if (baseMiningAmt == null || baseActTime == null || baseActTime <= 0) {
            return 0.0;
        }

        // Calculate base metrics fresh each time - ensure no stale values
        PerformanceMetrics baseMetrics =
                calculateBasePerformanceMetrics(baseStats, baseMiningAmt, baseActTime);
        
        // Get rolled values from current analysis
        double rolledM3PerSec = analysis.getM3PerSec();
        double rolledRealWorldM3PerSec = analysis.getRealWorldM3PerSec();
        
        // Validate values before calculation to prevent incorrect baseM3Pct
        // Note: getM3PerSec() returns a primitive double, so null checks are not applicable
        // Instead, validate that the values are positive and finite
        if (rolledM3PerSec <= 0 || !Double.isFinite(rolledM3PerSec) || baseMetrics.baseM3PerSec <= 0) {
            ErrorLogger.logError("Invalid values for baseM3Pct calculation: rolledM3PerSec=" + 
                    rolledM3PerSec + ", baseM3PerSec=" + baseMetrics.baseM3PerSec, null);
            return 0.0;
        }
        
        // Calculate baseM3Pct using fresh values from current analysis
        double baseM3Pct = displayPerformanceMetricRow("Base M3/sec", 
                rolledM3PerSec,
                rolledRealWorldM3PerSec, 
                baseMetrics.baseM3PerSec,
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
        double pct = base > 0 ? ((rolled / base) - 1) * PERCENTAGE_MULTIPLIER : 0;
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
        if (analysis == null || baseStats == null || baseMiningAmt == null || baseActTime == null) {
            return;
        }

        Double basePlusCritsM3PerSec = analysis.getBasePlusCritsM3PerSec();
        Double realWorldBasePlusCritsM3PerSec = analysis.getRealWorldBasePlusCritsM3PerSec();
        if (basePlusCritsM3PerSec == null || realWorldBasePlusCritsM3PerSec == null) {
            return;
        }

        double baseBasePlusCrits = MiningCalculator.calculateBasePlusCritsM3PerSec(baseMiningAmt,
                baseActTime, baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_CHANCE, 0.0),
                baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_BONUS_YIELD, 0.0));
        double baseRealWorldBasePlusCrits =
                MiningCalculator.calculateRealWorldEffectiveM3PerSec(baseMiningAmt, baseActTime,
                        baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_CHANCE, 0.0),
                        baseStats.getOrDefault(KEY_CRITICAL_SUCCESS_BONUS_YIELD, 0.0), 0.0, 0.0);
        double basePlusCritsPct = baseBasePlusCrits > 0
                ? ((basePlusCritsM3PerSec / baseBasePlusCrits) - 1) * PERCENTAGE_MULTIPLIER
                : 0;
        String tag = getColorTag(basePlusCritsPct);
        appendText(String.format(FORMAT_LABEL_COLUMN, "Base + Crits M3/s"), fgColor);
        appendText(String.format(FORMAT_PERFORMANCE_VALUE, baseBasePlusCrits,
                baseRealWorldBasePlusCrits, ""), fgColor);
        displayPerformanceValue(basePlusCritsM3PerSec, realWorldBasePlusCritsM3PerSec,
                basePlusCritsPct, tag);
    }

    private void displayTierSection(AnalysisResult analysis, String minerType) {
        if (analysis == null) {
            return;
        }

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

    private void copyToClipboard(String tier, String minerType, double baseM3Pct,
            Map<String, Double> stats, Map<String, Double> baseStats) {
        String tierDisplay;
        if (tier == null) {
            tierDisplay = TIER_F;
        } else if (TIER_S.equals(tier) || "S+".equals(tier)) {
            // Handle S tier - show as +S in clipboard
            tierDisplay = "+S";
        } else {
            // For other tiers, keep the "+" suffix if present
            tierDisplay = tier;
        }
        
        // Add space after tier if it doesn't end with "+" (positive range)
        // Check original tier value, not tierDisplay (since S tier is displayed as +S)
        String tierSeparator = (tier != null && tier.endsWith("+")) ? ":" : " :";
        
        String minerLabel;
        if (MINER_TYPE_ORE.equals(minerType)) {
            minerLabel = "[ORE]";
        } else if (MINER_TYPE_ICE.equals(minerType)) {
            minerLabel = "[Ice]";
        } else {
            minerLabel = "[Modulated]";
        }
        
        // Calculate Optimal Range percentage change
        double optimalRangePct = 0.0;
        if (stats != null && baseStats != null) {
            Double rolledOptimalRange = stats.get(KEY_OPTIMAL_RANGE);
            Double baseOptimalRange = baseStats.get(KEY_OPTIMAL_RANGE);
            if (rolledOptimalRange != null && baseOptimalRange != null && baseOptimalRange > 0) {
                optimalRangePct = calculatePercentageChange(rolledOptimalRange, baseOptimalRange);
            }
        }
        
        String clipboardText = tierDisplay + tierSeparator + " (" + formatPercentage(baseM3Pct)
                + ") {" + formatPercentage(optimalRangePct) + "} " + minerLabel;
        try {
            java.awt.datatransfer.StringSelection selection =
                    new java.awt.datatransfer.StringSelection(clipboardText);
            java.awt.datatransfer.Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
        } catch (Exception e) {
            ErrorLogger.logError("Error copying tier to clipboard", e);
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

    /**
     * Appends text to the document with the specified color.
     * 
     * @param text The text to append
     * @param color The color to use for the text (uses default foreground color if null or invalid)
     */
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
            ErrorLogger.logError("Error appending text to document (BadLocationException)", e);
        } catch (Exception e) {
            ErrorLogger.logError("Error appending text to document", e);
            // Last resort fallback - insert with no attributes and hope component's foreground
            // works
            try {
                doc.insertString(doc.getLength(), text, null);
            } catch (BadLocationException ex) {
                ErrorLogger.logError("Error in fallback text append", ex);
            }
        }
    }

    private void appendStyledText(String text, String styleName) {
        try {
            // Use cached style lookup for better performance
            Style style = getOrCreateStyle(styleName);
            if (style != null) {
                doc.insertString(doc.getLength(), text, style);
            } else {
                // Fallback: insert without style if style creation failed
                doc.insertString(doc.getLength(), text, null);
            }
        } catch (BadLocationException e) {
            ErrorLogger.logError("Error appending styled text to document", e);
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
        if (value > PERCENTAGE_THRESHOLD) {
            if (value < 10.0) {
                return String.format("+%04.1f%%", value); // e.g., +05.3%
            } else {
                return String.format("+%.1f%%", value); // e.g., +22.4% or +123.4%
            }
        } else if (value < -PERCENTAGE_THRESHOLD) {
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
            return STYLE_GOOD;
        } else if (value < -0.1) {
            return STYLE_BAD;
        }
        return null;
    }

    /**
     * Calculates sell price based on formula: cost * tier_modifier * (100% + baseM3Pct%) *
     * optimal_range_modifier (if tier has "+")
     */
    private double calculateSellPrice(AnalysisResult analysis, double baseM3Pct) {
        if (analysis == null) {
            return 0.0;
        }

        double cost = ConfigManager.getRollCost();
        if (cost <= 0) {
            return 0.0;
        }

        String tier = analysis.getTier() != null ? analysis.getTier() : "F";
        // Remove "+" suffix if present for tier modifier lookup
        String tierForModifier = tier.endsWith("+") ? tier.substring(0, tier.length() - 1) : tier;
        double tierModifier = TierModifierManager.getModifierForTier(tierForModifier);

        // Check if tier has "+" suffix (optimal range increased) and apply modifier
        double optimalRangeModifier = DEFAULT_MULTIPLIER;
        if (tier.endsWith("+")) {
            optimalRangeModifier = OptimalRangeModifierManager.loadOptimalRangeModifier();
        }

        // Formula: cost * tier_modifier * (100% + baseM3Pct%) * optimal_range_modifier (if
        // applicable)
        // baseM3Pct is already a percentage, so convert to multiplier: 1 + (baseM3Pct / 100)
        double m3Multiplier = DEFAULT_MULTIPLIER + (baseM3Pct / PERCENTAGE_DIVISOR);
        return cost * tierModifier * m3Multiplier * optimalRangeModifier;
    }
}

