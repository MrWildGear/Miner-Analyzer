package model;

import java.util.Map;

/**
 * Data class containing the results of a miner roll analysis
 */
public class AnalysisResult {
    private Map<String, Double> stats;
    private double m3PerSec;
    private Double basePlusCritsM3PerSec;
    private double effectiveM3PerSec;
    private double realWorldM3PerSec;
    private Double realWorldBasePlusCritsM3PerSec;
    private double realWorldEffectiveM3PerSec;
    private String tier;

    /**
     * Constructs a new AnalysisResult with default tier set to "F".
     */
    public AnalysisResult() {
        this.setTier("F");
    }

    /**
     * Gets the real-world base m³/sec (with all bonuses applied).
     * 
     * @return The real-world base m³/sec value
     */
    public double getRealWorldM3PerSec() {
        return realWorldM3PerSec;
    }

    /**
     * Sets the real-world base m³/sec value.
     * 
     * @param realWorldM3PerSec The real-world base m³/sec value
     */
    public void setRealWorldM3PerSec(double realWorldM3PerSec) {
        this.realWorldM3PerSec = realWorldM3PerSec;
    }

    /**
     * Gets the parsed stats map containing all item statistics.
     * 
     * @return A map of stat names to values
     */
    public Map<String, Double> getStats() {
        return stats;
    }

    /**
     * Sets the stats map containing all item statistics.
     * 
     * @param stats A map of stat names to values
     */
    public void setStats(Map<String, Double> stats) {
        this.stats = stats;
    }

    /**
     * Gets the effective m³/sec (including crits and residue effects).
     * 
     * @return The effective m³/sec value
     */
    public double getEffectiveM3PerSec() {
        return effectiveM3PerSec;
    }

    /**
     * Sets the effective m³/sec value.
     * 
     * @param effectiveM3PerSec The effective m³/sec value (including crits and residue)
     */
    public void setEffectiveM3PerSec(double effectiveM3PerSec) {
        this.effectiveM3PerSec = effectiveM3PerSec;
    }

    /**
     * Gets the base m³/sec value (mining amount / activation time).
     * 
     * @return The base m³/sec value
     */
    public double getM3PerSec() {
        return m3PerSec;
    }

    /**
     * Sets the base m³/sec value.
     * 
     * @param m3PerSec The base m³/sec value
     */
    public void setM3PerSec(double m3PerSec) {
        this.m3PerSec = m3PerSec;
    }

    /**
     * Gets the base + crits m³/sec value (only for Modulated miners).
     * 
     * @return The base + crits m³/sec value, or null if not applicable
     */
    public Double getBasePlusCritsM3PerSec() {
        return basePlusCritsM3PerSec;
    }

    /**
     * Sets the base + crits m³/sec value.
     * 
     * @param basePlusCritsM3PerSec The base + crits m³/sec value (can be null for non-Modulated
     *        miners)
     */
    public void setBasePlusCritsM3PerSec(Double basePlusCritsM3PerSec) {
        this.basePlusCritsM3PerSec = basePlusCritsM3PerSec;
    }

    /**
     * Gets the real-world base + crits m³/sec value (only for Modulated miners).
     * 
     * @return The real-world base + crits m³/sec value, or null if not applicable
     */
    public Double getRealWorldBasePlusCritsM3PerSec() {
        return realWorldBasePlusCritsM3PerSec;
    }

    /**
     * Sets the real-world base + crits m³/sec value.
     * 
     * @param realWorldBasePlusCritsM3PerSec The real-world base + crits m³/sec value (can be null
     *        for non-Modulated miners)
     */
    public void setRealWorldBasePlusCritsM3PerSec(Double realWorldBasePlusCritsM3PerSec) {
        this.realWorldBasePlusCritsM3PerSec = realWorldBasePlusCritsM3PerSec;
    }

    /**
     * Gets the real-world effective m³/sec (with all bonuses and crits/residue effects).
     * 
     * @return The real-world effective m³/sec value
     */
    public double getRealWorldEffectiveM3PerSec() {
        return realWorldEffectiveM3PerSec;
    }

    /**
     * Sets the real-world effective m³/sec value.
     * 
     * @param realWorldEffectiveM3PerSec The real-world effective m³/sec value
     */
    public void setRealWorldEffectiveM3PerSec(double realWorldEffectiveM3PerSec) {
        this.realWorldEffectiveM3PerSec = realWorldEffectiveM3PerSec;
    }

    /**
     * Gets the tier assignment (S, A, B, C, D, E, or F). May include "+" suffix if optimal range is
     * increased.
     * 
     * @return The tier string
     */
    public String getTier() {
        return tier;
    }

    /**
     * Sets the tier assignment.
     * 
     * @param tier The tier string (S, A, B, C, D, E, or F, optionally with "+" suffix)
     */
    public void setTier(String tier) {
        this.tier = tier;
    }
}

