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
    }
    
    private void setupTextStyles() {
        if (doc == null) {
            return;
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
            double miningMut = ((stats.get("MiningAmount") / baseStats.get("MiningAmount")) - 1) * 100;
            String tag = getColorTag(miningMut);
            appendText(String.format("%-20s ", "Mining Amount"), fgColor);
            appendText(String.format("%.1f m3%12s ", baseStats.get("MiningAmount"), ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.1f m3%12s ", stats.get("MiningAmount"), ""), tag);
                appendStyledText(formatPercentage(miningMut) + "\n", tag);
            } else {
                appendText(String.format("%.1f m3%12s ", stats.get("MiningAmount"), ""), fgColor);
                appendText(formatPercentage(miningMut) + "\n", fgColor);
            }
            
            // Activation Time
            double timeMut = ((stats.get("ActivationTime") / baseStats.get("ActivationTime")) - 1) * 100;
            tag = getColorTag(-timeMut);
            appendText(String.format("%-20s ", "Activation Time"), fgColor);
            appendText(String.format("%.1f s%13s ", baseStats.get("ActivationTime"), ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.1f s%13s ", stats.get("ActivationTime"), ""), tag);
                appendStyledText(formatPercentage(timeMut) + "\n", tag);
            } else {
                appendText(String.format("%.1f s%13s ", stats.get("ActivationTime"), ""), fgColor);
                appendText(formatPercentage(timeMut) + "\n", fgColor);
            }
            
            // Crit Chance
            double critChanceMut = baseStats.get("CriticalSuccessChance") > 0 ?
                ((stats.get("CriticalSuccessChance") / baseStats.get("CriticalSuccessChance")) - 1) * 100 : 0;
            tag = getColorTag(critChanceMut);
            appendText(String.format("%-20s ", "Crit Chance"), fgColor);
            appendText(String.format("%.2f%%%15s ", baseStats.get("CriticalSuccessChance") * 100, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.2f%%%15s ", stats.get("CriticalSuccessChance") * 100, ""), tag);
                appendStyledText(formatPercentage(critChanceMut) + "\n", tag);
            } else {
                appendText(String.format("%.2f%%%15s ", stats.get("CriticalSuccessChance") * 100, ""), fgColor);
                appendText(formatPercentage(critChanceMut) + "\n", fgColor);
            }
            
            // Crit Bonus
            double critBonusMut = ((stats.get("CriticalSuccessBonusYield") / baseStats.get("CriticalSuccessBonusYield")) - 1) * 100;
            tag = getColorTag(critBonusMut);
            appendText(String.format("%-20s ", "Crit Bonus"), fgColor);
            appendText(String.format("%.0f%%%16s ", baseStats.get("CriticalSuccessBonusYield") * 100, ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.0f%%%16s ", stats.get("CriticalSuccessBonusYield") * 100, ""), tag);
                appendStyledText(formatPercentage(critBonusMut) + "\n", tag);
            } else {
                appendText(String.format("%.0f%%%16s ", stats.get("CriticalSuccessBonusYield") * 100, ""), fgColor);
                appendText(formatPercentage(critBonusMut) + "\n", fgColor);
            }
            
            // Residue (Modulated only)
            if ("Modulated".equals(minerType)) {
                double residueProbMut = ((stats.get("ResidueProbability") / baseStats.get("ResidueProbability")) - 1) * 100;
                tag = getColorTag(-residueProbMut);
                appendText(String.format("%-20s ", "Residue Prob"), fgColor);
                appendText(String.format("%.2f%%%15s ", baseStats.get("ResidueProbability") * 100, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f%%%15s ", stats.get("ResidueProbability") * 100, ""), tag);
                    appendStyledText(formatPercentage(residueProbMut) + "\n", tag);
                } else {
                    appendText(String.format("%.2f%%%15s ", stats.get("ResidueProbability") * 100, ""), fgColor);
                    appendText(formatPercentage(residueProbMut) + "\n", fgColor);
                }
                
                double residueMultMut = ((stats.get("ResidueVolumeMultiplier") / baseStats.get("ResidueVolumeMultiplier")) - 1) * 100;
                tag = getColorTag(-residueMultMut);
                appendText(String.format("%-20s ", "Residue Mult"), fgColor);
                appendText(String.format("%.3f x%14s ", baseStats.get("ResidueVolumeMultiplier"), ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.3f x%14s ", stats.get("ResidueVolumeMultiplier"), ""), tag);
                    appendStyledText(formatPercentage(residueMultMut) + "\n", tag);
                } else {
                    appendText(String.format("%.3f x%14s ", stats.get("ResidueVolumeMultiplier"), ""), fgColor);
                    appendText(formatPercentage(residueMultMut) + "\n", fgColor);
                }
            }
            
            // Optimal Range
            double optimalRangeMut = baseStats.get("OptimalRange") > 0 ?
                ((stats.get("OptimalRange") / baseStats.get("OptimalRange")) - 1) * 100 : 0;
            tag = getColorTag(optimalRangeMut);
            appendText(String.format("%-20s ", "Optimal Range"), fgColor);
            appendText(String.format("%.2f km%14s ", baseStats.get("OptimalRange"), ""), fgColor);
            if (tag != null) {
                appendStyledText(String.format("%.2f km%14s ", stats.get("OptimalRange"), ""), tag);
                appendStyledText(formatPercentage(optimalRangeMut) + "\n", tag);
            } else {
                appendText(String.format("%.2f km%14s ", stats.get("OptimalRange"), ""), fgColor);
                appendText(formatPercentage(optimalRangeMut) + "\n", fgColor);
            }
            
            appendText("\n", fgColor);
            
            // Performance Metrics
            appendStyledText("Performance Metrics:\n", "header");
            appendText(String.format("%-20s %-20s %-20s %-20s\n", "Metric", "Base", "Rolled", "% Change"), fgColor);
            appendText(repeat("-", 76) + "\n", fgColor);
            
            // Calculate base values
            double baseM3PerSec = MiningCalculator.calculateBaseM3PerSec(baseStats.get("MiningAmount"), baseStats.get("ActivationTime"));
            double baseEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
                baseStats.get("MiningAmount"),
                baseStats.get("ActivationTime"),
                baseStats.get("CriticalSuccessChance"),
                baseStats.get("CriticalSuccessBonusYield"),
                baseStats.getOrDefault("ResidueProbability", 0.0),
                baseStats.getOrDefault("ResidueVolumeMultiplier", 0.0)
            );
            double baseRealWorldM3PerSec = MiningCalculator.calculateRealWorldBaseM3PerSec(
                baseStats.get("MiningAmount"),
                baseStats.get("ActivationTime")
            );
            double baseRealWorldEffectiveM3PerSec = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                baseStats.get("MiningAmount"),
                baseStats.get("ActivationTime"),
                baseStats.get("CriticalSuccessChance"),
                baseStats.get("CriticalSuccessBonusYield"),
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
                    baseStats.get("MiningAmount"),
                    baseStats.get("ActivationTime"),
                    baseStats.get("CriticalSuccessChance"),
                    baseStats.get("CriticalSuccessBonusYield")
                );
                double baseRealWorldBasePlusCrits = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                    baseStats.get("MiningAmount"),
                    baseStats.get("ActivationTime"),
                    baseStats.get("CriticalSuccessChance"),
                    baseStats.get("CriticalSuccessBonusYield"),
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
        try {
            Style style = doc.addStyle("temp", null);
            StyleConstants.setForeground(style, color);
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
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

