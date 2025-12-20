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

    public AnalysisResult() {
        this.setTier("F");
    }

    public double getRealWorldM3PerSec() {
        return realWorldM3PerSec;
    }

    public void setRealWorldM3PerSec(double realWorldM3PerSec) {
        this.realWorldM3PerSec = realWorldM3PerSec;
    }

    public Map<String, Double> getStats() {
        return stats;
    }

    public void setStats(Map<String, Double> stats) {
        this.stats = stats;
    }

    public double getEffectiveM3PerSec() {
        return effectiveM3PerSec;
    }

    public void setEffectiveM3PerSec(double effectiveM3PerSec) {
        this.effectiveM3PerSec = effectiveM3PerSec;
    }

    public double getM3PerSec() {
        return m3PerSec;
    }

    public void setM3PerSec(double m3PerSec) {
        this.m3PerSec = m3PerSec;
    }

    public Double getBasePlusCritsM3PerSec() {
        return basePlusCritsM3PerSec;
    }

    public void setBasePlusCritsM3PerSec(Double basePlusCritsM3PerSec) {
        this.basePlusCritsM3PerSec = basePlusCritsM3PerSec;
    }

    public Double getRealWorldBasePlusCritsM3PerSec() {
        return realWorldBasePlusCritsM3PerSec;
    }

    public void setRealWorldBasePlusCritsM3PerSec(Double realWorldBasePlusCritsM3PerSec) {
        this.realWorldBasePlusCritsM3PerSec = realWorldBasePlusCritsM3PerSec;
    }

    public double getRealWorldEffectiveM3PerSec() {
        return realWorldEffectiveM3PerSec;
    }

    public void setRealWorldEffectiveM3PerSec(double realWorldEffectiveM3PerSec) {
        this.realWorldEffectiveM3PerSec = realWorldEffectiveM3PerSec;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }
}

