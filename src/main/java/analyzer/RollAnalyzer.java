package analyzer;

import calculator.MiningCalculator;
import config.MinerConfig;
import model.AnalysisResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Analyzes miner rolls and determines tier assignments
 */
public class RollAnalyzer {
    
    /**
     * Analyzes a miner roll and calculates all performance metrics
     * @param stats Parsed stats from clipboard
     * @param baseStats Base stats for the miner type
     * @param minerType "ORE" or "Modulated"
     * @return AnalysisResult with all calculated metrics and tier
     */
    public static AnalysisResult analyzeRoll(Map<String, Double> stats, Map<String, Double> baseStats, String minerType) {
        AnalysisResult result = new AnalysisResult();
        
        // Fill in defaults from base stats
        Map<String, Double> mutatedStats = new HashMap<>(baseStats);
        mutatedStats.putAll(stats);
        result.stats = mutatedStats;
        
        // Calculate m3/sec
        result.m3PerSec = MiningCalculator.calculateBaseM3PerSec(
            mutatedStats.get("MiningAmount"), 
            mutatedStats.get("ActivationTime")
        );
        
        // Calculate effective m3/sec
        if ("ORE".equals(minerType)) {
            result.effectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                0.0, 0.0
            );
            result.basePlusCritsM3PerSec = null;
        } else {
            result.basePlusCritsM3PerSec = MiningCalculator.calculateBasePlusCritsM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield")
            );
            result.effectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                mutatedStats.get("ResidueProbability"),
                mutatedStats.get("ResidueVolumeMultiplier")
            );
        }
        
        // Calculate real-world values
        result.realWorldM3PerSec = MiningCalculator.calculateRealWorldBaseM3PerSec(
            mutatedStats.get("MiningAmount"),
            mutatedStats.get("ActivationTime")
        );
        
        if ("ORE".equals(minerType)) {
            result.realWorldEffectiveM3PerSec = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                0.0, 0.0
            );
            result.realWorldBasePlusCritsM3PerSec = null;
        } else {
            result.realWorldBasePlusCritsM3PerSec = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                0.0, 0.0
            );
            result.realWorldEffectiveM3PerSec = MiningCalculator.calculateRealWorldEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                mutatedStats.get("ResidueProbability"),
                mutatedStats.get("ResidueVolumeMultiplier")
            );
        }
        
        // Determine tier
        result.tier = determineTier(result.m3PerSec, minerType);
        
        return result;
    }
    
    /**
     * Determines the tier based on m³/sec value
     */
    private static String determineTier(double m3PerSec, String minerType) {
        Map<String, Map<String, Double>> tierRanges = MinerConfig.getTierRanges(minerType);
        String tier = "F";
        
        if (tierRanges != null) {
            Map<String, Double> sRange = tierRanges.get("S");
            Map<String, Double> aRange = tierRanges.get("A");
            Map<String, Double> bRange = tierRanges.get("B");
            Map<String, Double> cRange = tierRanges.get("C");
            Map<String, Double> dRange = tierRanges.get("D");
            Map<String, Double> eRange = tierRanges.get("E");
            Map<String, Double> fRange = tierRanges.get("F");
            
            if (sRange != null && sRange.get("Min") != null && m3PerSec >= sRange.get("Min")) {
                tier = "S";
            } else if (aRange != null && aRange.get("Min") != null && aRange.get("Max") != null && 
                       m3PerSec >= aRange.get("Min") && m3PerSec < aRange.get("Max")) {
                tier = "A";
            } else if (bRange != null && bRange.get("Min") != null && bRange.get("Max") != null && 
                       m3PerSec >= bRange.get("Min") && m3PerSec < bRange.get("Max")) {
                tier = "B";
            } else if (cRange != null && cRange.get("Min") != null && cRange.get("Max") != null && 
                       m3PerSec >= cRange.get("Min") && m3PerSec < cRange.get("Max")) {
                tier = "C";
            } else if (dRange != null && dRange.get("Min") != null && dRange.get("Max") != null && 
                       m3PerSec >= dRange.get("Min") && m3PerSec < dRange.get("Max")) {
                tier = "D";
            } else if (eRange != null && eRange.get("Min") != null && eRange.get("Max") != null && 
                       m3PerSec >= eRange.get("Min") && m3PerSec < eRange.get("Max")) {
                tier = "E";
            } else if (fRange != null && fRange.get("Max") != null && m3PerSec < fRange.get("Max")) {
                tier = "F";
            }
        }
        
        return tier;
    }
}

