package ui;

import calculator.MiningCalculator;
import config.MinerConfig;
import model.AnalysisResult;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.Map;

/**
 * Handles display and formatting of analysis results
 */
public class AnalysisDisplay {
    
    private final StyledDocument doc;
    private Color fgColor;
    private final Map<String, Color> tierColors;
    
    public AnalysisDisplay(StyledDocument doc, Color fgColor, Map<String, Color> tierColors) {
        this.doc = doc;
        this.fgColor = fgColor;
        this.tierColors = new java.util.HashMap<>(tierColors);
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
            // Special styled text (tier colors, good/bad) uses named styles which are updated in setupTextStyles()
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
            // Ignore errors during text color update
        }
    }
    
    private void setupTextStyles() {
        if (doc == null) {
            return;
        }
        
        try {
            // Ensure the default style has the correct foreground color
            // This affects text inserted without explicit styling
            Style defaultStyle = doc.getStyle("default");
            if (defaultStyle == null) {
                defaultStyle = doc.addStyle("default", null);
            }
            if (defaultStyle != null) {
                StyleConstants.setForeground(defaultStyle, fgColor);
            }
        } catch (Exception e) {
            // Ignore default style setup errors
        }
        
        if (tierColors == null || tierColors.isEmpty()) {
            return;
        }
        
        try {
            // Tier colors - get or create styles
            for (Map.Entry<String, Color> entry : tierColors.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String styleName = "tier_" + entry.getKey();
                Style style = doc.getStyle(styleName);
                if (style == null) {
                    style = doc.addStyle(styleName, null);
                }
                if (style != null) {
                    StyleConstants.setForeground(style, entry.getValue());
                    StyleConstants.setBold(style, true);
                }
            }
            
            // Good/Bad colors - get or create styles
            Color sColor = tierColors.get("S");
            if (sColor != null) {
                Style goodStyle = doc.getStyle("good");
                if (goodStyle == null) {
                    goodStyle = doc.addStyle("good", null);
                }
                if (goodStyle != null) {
                    StyleConstants.setForeground(goodStyle, sColor);
                }
            }
            
            Color fColor = tierColors.get("F");
            if (fColor != null) {
                Style badStyle = doc.getStyle("bad");
                if (badStyle == null) {
                    badStyle = doc.addStyle("bad", null);
                }
                if (badStyle != null) {
                    StyleConstants.setForeground(badStyle, fColor);
                }
            }
            
            // Header style - get or create
            Style headerStyle = doc.getStyle("header");
            if (headerStyle == null) {
                headerStyle = doc.addStyle("header", null);
            }
            if (headerStyle != null) {
                StyleConstants.setBold(headerStyle, true);
                StyleConstants.setFontSize(headerStyle, 11);
            }
        } catch (Exception e) {
            // Silently handle style errors - don't break the app
        }
    }
    
    public void displayAnalysis(AnalysisResult analysis, Map<String, Double> baseStats, String minerType) {
        try {
            if (doc == null || analysis == null || baseStats == null || minerType == null) {
                return;
            }
            
            doc.remove(0, doc.getLength());
            
            Map<String, Double> stats = analysis.stats;
            if (stats == null) {
                return;
            }
            
            // Header
            appendStyledText(repeat("=", 76) + "\n", "header");
            appendStyledText("EVE Online " + minerType + " Strip Miner Roll Analyzer\n", "header");
            appendStyledText(repeat("=", 76) + "\n\n", "header");
            
            // Roll Analysis
            appendStyledText("Roll Analysis:\n", "header");
            appendText(String.format("%-20s %-20s %-20s %-20s\n", "Metric", "Base", "Rolled", "% Change"), fgColor);
            appendText(repeat("-", 76) + "\n", fgColor);
            
            // Mining Amount
            Double miningAmount = stats.get("MiningAmount");
            Double baseMiningAmount = baseStats.get("MiningAmount");
            double miningMut = 0.0;
            if (miningAmount != null && baseMiningAmount != null && baseMiningAmount > 0) {
                miningMut = ((miningAmount / baseMiningAmount) - 1) * 100;
            }
            String tag = getColorTag(miningMut);
            appendText(String.format("%-20s ", "Mining Amount"), fgColor);
            appendText(String.format("%.1f m3%12s ", baseMiningAmount != null ? baseMiningAmount : 0.0, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.1f m3%12s ", miningAmount != null ? miningAmount : 0.0, ""), tag);
                appendStyledText(formatPercentage(miningMut) + "\n", tag);
            } else {
                appendText(String.format("%.1f m3%12s ", miningAmount != null ? miningAmount : 0.0, ""), fgColor);
                appendText(formatPercentage(miningMut) + "\n", fgColor);
            }
            
            // Activation Time
            Double activationTime = stats.get("ActivationTime");
            Double baseActivationTime = baseStats.get("ActivationTime");
            double timeMut = 0.0;
            if (activationTime != null && baseActivationTime != null && baseActivationTime > 0) {
                timeMut = ((activationTime / baseActivationTime) - 1) * 100;
            }
            tag = getColorTag(-timeMut);
            appendText(String.format("%-20s ", "Activation Time"), fgColor);
            appendText(String.format("%.1f s%13s ", baseActivationTime != null ? baseActivationTime : 0.0, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.1f s%13s ", activationTime != null ? activationTime : 0.0, ""), tag);
                appendStyledText(formatPercentage(timeMut) + "\n", tag);
            } else {
                appendText(String.format("%.1f s%13s ", activationTime != null ? activationTime : 0.0, ""), fgColor);
                appendText(formatPercentage(timeMut) + "\n", fgColor);
            }
            
            // Crit Chance
            Double critChance = stats.get("CriticalSuccessChance");
            Double baseCritChance = baseStats.get("CriticalSuccessChance");
            double critChanceMut = 0.0;
            if (critChance != null && baseCritChance != null && baseCritChance > 0) {
                critChanceMut = ((critChance / baseCritChance) - 1) * 100;
            }
            tag = getColorTag(critChanceMut);
            appendText(String.format("%-20s ", "Crit Chance"), fgColor);
            appendText(String.format("%.2f%%%15s ", (baseCritChance != null ? baseCritChance : 0.0) * 100, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.2f%%%15s ", (critChance != null ? critChance : 0.0) * 100, ""), tag);
                appendStyledText(formatPercentage(critChanceMut) + "\n", tag);
            } else {
                appendText(String.format("%.2f%%%15s ", (critChance != null ? critChance : 0.0) * 100, ""), fgColor);
                appendText(formatPercentage(critChanceMut) + "\n", fgColor);
            }
            
            // Crit Bonus
            Double critBonus = stats.get("CriticalSuccessBonusYield");
            Double baseCritBonus = baseStats.get("CriticalSuccessBonusYield");
            double critBonusMut = 0.0;
            if (critBonus != null && baseCritBonus != null && baseCritBonus > 0) {
                critBonusMut = ((critBonus / baseCritBonus) - 1) * 100;
            }
            tag = getColorTag(critBonusMut);
            appendText(String.format("%-20s ", "Crit Bonus"), fgColor);
            appendText(String.format("%.0f%%%16s ", (baseCritBonus != null ? baseCritBonus : 0.0) * 100, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.0f%%%16s ", (critBonus != null ? critBonus : 0.0) * 100, ""), tag);
                appendStyledText(formatPercentage(critBonusMut) + "\n", tag);
            } else {
                appendText(String.format("%.0f%%%16s ", (critBonus != null ? critBonus : 0.0) * 100, ""), fgColor);
                appendText(formatPercentage(critBonusMut) + "\n", fgColor);
            }
            
            // Residue (Modulated only)
            if ("Modulated".equals(minerType)) {
                Double residueProb = stats.get("ResidueProbability");
                Double baseResidueProb = baseStats.get("ResidueProbability");
                double residueProbMut = 0.0;
                if (residueProb != null && baseResidueProb != null && baseResidueProb > 0) {
                    residueProbMut = ((residueProb / baseResidueProb) - 1) * 100;
                }
                tag = getColorTag(-residueProbMut);
                appendText(String.format("%-20s ", "Residue Prob"), fgColor);
                appendText(String.format("%.2f%%%15s ", (baseResidueProb != null ? baseResidueProb : 0.0) * 100, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f%%%15s ", (residueProb != null ? residueProb : 0.0) * 100, ""), tag);
                    appendStyledText(formatPercentage(residueProbMut) + "\n", tag);
                } else {
                    appendText(String.format("%.2f%%%15s ", (residueProb != null ? residueProb : 0.0) * 100, ""), fgColor);
                    appendText(formatPercentage(residueProbMut) + "\n", fgColor);
                }
                
                Double residueMult = stats.get("ResidueVolumeMultiplier");
                Double baseResidueMult = baseStats.get("ResidueVolumeMultiplier");
                double residueMultMut = 0.0;
                if (residueMult != null && baseResidueMult != null && baseResidueMult > 0) {
                    residueMultMut = ((residueMult / baseResidueMult) - 1) * 100;
                }
                tag = getColorTag(-residueMultMut);
                appendText(String.format("%-20s ", "Residue Mult"), fgColor);
                appendText(String.format("%.3f x%14s ", baseResidueMult != null ? baseResidueMult : 0.0, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.3f x%14s ", residueMult != null ? residueMult : 0.0, ""), tag);
                    appendStyledText(formatPercentage(residueMultMut) + "\n", tag);
                } else {
                    appendText(String.format("%.3f x%14s ", residueMult != null ? residueMult : 0.0, ""), fgColor);
                    appendText(formatPercentage(residueMultMut) + "\n", fgColor);
                }
            }
            
            // Optimal Range
            Double optimalRange = stats.get("OptimalRange");
            Double baseOptimalRange = baseStats.get("OptimalRange");
            double optimalRangeMut = 0.0;
            if (optimalRange != null && baseOptimalRange != null && baseOptimalRange > 0) {
                optimalRangeMut = ((optimalRange / baseOptimalRange) - 1) * 100;
            }
            tag = getColorTag(optimalRangeMut);
            appendText(String.format("%-20s ", "Optimal Range"), fgColor);
            appendText(String.format("%.2f km%14s ", baseOptimalRange != null ? baseOptimalRange : 0.0, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.2f km%14s ", optimalRange != null ? optimalRange : 0.0, ""), tag);
                appendStyledText(formatPercentage(optimalRangeMut) + "\n", tag);
            } else {
                appendText(String.format("%.2f km%14s ", optimalRange != null ? optimalRange : 0.0, ""), fgColor);
                appendText(formatPercentage(optimalRangeMut) + "\n", fgColor);
            }
            
            appendText("\n", fgColor);
            
            // Performance Metrics
            appendStyledText("Performance Metrics:\n", "header");
            appendText(String.format("%-20s %-20s %-20s %-20s\n", "Metric", "Base", "Rolled", "% Change"), fgColor);
            appendText(repeat("-", 76) + "\n", fgColor);
            
            // Calculate base values - use getOrDefault to handle nulls
            Double baseMiningAmt = baseStats.get("MiningAmount");
            Double baseActTime = baseStats.get("ActivationTime");
            if (baseMiningAmt == null || baseActTime == null || baseActTime <= 0) {
                return; // Cannot calculate without required base stats
            }
            
            double baseM3PerSec = MiningCalculator.calculateBaseM3PerSec(baseMiningAmt, baseActTime);
            double baseEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
                baseMiningAmt,
                baseActTime,
                baseStats.getOrDefault("CriticalSuccessChance", 0.0),
                baseStats.getOrDefault("CriticalSuccessBonusYield", 0.0),
                baseStats.getOrDefault("ResidueProbability", 0.0),
                baseStats.getOrDefault("ResidueVolumeMultiplier", 0.0)
            );
            double baseRealWorldM3PerSec = MiningCalculator.calculateRealWorldBaseM3PerSec(
                baseMiningAmt,
                baseActTime
            );
            double baseRealWorldEffectiveM3PerSec = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                baseMiningAmt,
                baseActTime,
                baseStats.getOrDefault("CriticalSuccessChance", 0.0),
                baseStats.getOrDefault("CriticalSuccessBonusYield", 0.0),
                baseStats.getOrDefault("ResidueProbability", 0.0),
                baseStats.getOrDefault("ResidueVolumeMultiplier", 0.0)
            );
            
            // Base M3/sec
            double baseM3Pct = baseM3PerSec > 0 ? ((analysis.m3PerSec / baseM3PerSec) - 1) * 100 : 0;
            tag = getColorTag(baseM3Pct);
            appendText(String.format("%-20s ", "Base M3/sec"), fgColor);
            appendText(String.format("%.2f (%.1f)%6s ", baseM3PerSec, baseRealWorldM3PerSec, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.2f (%.1f)%6s ", analysis.m3PerSec, analysis.realWorldM3PerSec, ""), tag);
                appendStyledText(formatPercentage(baseM3Pct) + "\n", tag);
            } else {
                appendText(String.format("%.2f (%.1f)%6s ", analysis.m3PerSec, analysis.realWorldM3PerSec, ""), fgColor);
                appendText(formatPercentage(baseM3Pct) + "\n", fgColor);
            }
            
            // Base + Crits (Modulated only)
            if ("Modulated".equals(minerType) && analysis.basePlusCritsM3PerSec != null) {
                double baseBasePlusCrits = MiningCalculator.calculateBasePlusCritsM3PerSec(
                    baseMiningAmt,
                    baseActTime,
                    baseStats.getOrDefault("CriticalSuccessChance", 0.0),
                    baseStats.getOrDefault("CriticalSuccessBonusYield", 0.0)
                );
                double baseRealWorldBasePlusCrits = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                    baseMiningAmt,
                    baseActTime,
                    baseStats.getOrDefault("CriticalSuccessChance", 0.0),
                    baseStats.getOrDefault("CriticalSuccessBonusYield", 0.0),
                    0.0, 0.0
                );
                double basePlusCritsPct = baseBasePlusCrits > 0 ?
                    ((analysis.basePlusCritsM3PerSec / baseBasePlusCrits) - 1) * 100 : 0;
                tag = getColorTag(basePlusCritsPct);
                appendText(String.format("%-20s ", "Base + Crits M3/s"), fgColor);
                appendText(String.format("%.2f (%.1f)%6s ", baseBasePlusCrits, baseRealWorldBasePlusCrits, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f (%.1f)%6s ", analysis.basePlusCritsM3PerSec, 
                        analysis.realWorldBasePlusCritsM3PerSec, ""), tag);
                    appendStyledText(formatPercentage(basePlusCritsPct) + "\n", tag);
                } else {
                    appendText(String.format("%.2f (%.1f)%6s ", analysis.basePlusCritsM3PerSec, 
                        analysis.realWorldBasePlusCritsM3PerSec, ""), fgColor);
                    appendText(formatPercentage(basePlusCritsPct) + "\n", fgColor);
                }
            }
            
            // Effective M3/sec
            double effM3Pct = baseEffectiveM3PerSec > 0 ?
                ((analysis.effectiveM3PerSec / baseEffectiveM3PerSec) - 1) * 100 : 0;
            tag = getColorTag(effM3Pct);
            appendText(String.format("%-20s ", "Effective M3/sec"), fgColor);
            appendText(String.format("%.2f (%.1f)%6s ", baseEffectiveM3PerSec, baseRealWorldEffectiveM3PerSec, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.2f (%.1f)%6s ", analysis.effectiveM3PerSec, 
                    analysis.realWorldEffectiveM3PerSec, ""), tag);
                appendStyledText(formatPercentage(effM3Pct) + "\n", tag);
            } else {
                appendText(String.format("%.2f (%.1f)%6s ", analysis.effectiveM3PerSec, 
                    analysis.realWorldEffectiveM3PerSec, ""), fgColor);
                appendText(formatPercentage(effM3Pct) + "\n", fgColor);
            }
            
            appendText("\n", fgColor);
            
            // Tier
            String tier = analysis.tier;
            if (tier == null) {
                tier = "F";
            }
            Map<String, Map<String, Double>> tierRanges = MinerConfig.getTierRanges(minerType);
            Map<String, Double> tierRange = tierRanges != null ? tierRanges.get(tier) : null;
            
            String tierRangeStr = "";
            if (tierRange != null) {
                Double min = tierRange.get("Min");
                Double max = tierRange.get("Max");
                if (min != null && max != null) {
                    if ("S".equals(tier)) {
                        tierRangeStr = String.format("%.2f-%.2f+ m³/s", min, max);
                    } else if ("F".equals(tier)) {
                        tierRangeStr = String.format("<%.2f m³/s", max);
                    } else {
                        tierRangeStr = String.format("%.2f-%.5f m³/s", min, max);
                    }
                }
            }
            
            appendStyledText("Tier: ", "header");
            appendStyledText(tier + "\n", "tier_" + tier);
            if (!tierRangeStr.isEmpty()) {
                appendStyledText("(" + tierRangeStr + ")\n", "tier_" + tier);
            }
            
            appendText("\n" + repeat("=", 76) + "\n", fgColor);
            
            // Copy to clipboard
            String tierDisplay = "S".equals(tier) ? "+S" : tier;
            String minerLabel = "ORE".equals(minerType) ? "[ORE]" : "[Modulated]";
            String clipboardText = tierDisplay + ": (" + formatPercentage(baseM3Pct) + ") " + minerLabel;
            try {
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(clipboardText);
                java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
            } catch (Exception e) {
                // Ignore clipboard errors
            }
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public void appendText(String text, Color color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            // Ensure we have a valid color - use fgColor as fallback if color is null or invalid
            Color textColor = color;
            if (textColor == null || (textColor.getRed() == textColor.getGreen() && textColor.getGreen() == textColor.getBlue() && textColor.getRed() > 200)) {
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
            // Last resort fallback - insert with no attributes and hope component's foreground works
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
        if (value > 0.1) {
            return String.format("+%.1f%%", value);
        } else if (value < -0.1) {
            return String.format("%.1f%%", value);
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
}

