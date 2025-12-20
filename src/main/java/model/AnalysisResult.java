package model;

import java.util.Map;

/**
 * Data class containing the results of a miner roll analysis
 */
public class AnalysisResult {
    public Map<String, Double> stats;
    public double m3PerSec;
    public Double basePlusCritsM3PerSec;
    public double effectiveM3PerSec;
    public double realWorldM3PerSec;
    public Double realWorldBasePlusCritsM3PerSec;
    public double realWorldEffectiveM3PerSec;
    public String tier;
    
    public AnalysisResult() {
        this.tier = "F";
    }
}

