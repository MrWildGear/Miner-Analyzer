package analyzer;

import java.util.HashMap;
import java.util.Map;
import calculator.MiningCalculator;
import config.MinerConfig;
import model.AnalysisResult;

/**
 * Analyzes miner rolls and determines tier assignments
 */
public class RollAnalyzer {

    /**
     * Private constructor to prevent instantiation
     */
    private RollAnalyzer() {
        // Utility class - no instantiation
    }

    /**
     * Analyzes a miner roll and calculates all performance metrics
     * 
     * @param stats Parsed stats from clipboard
     * @param baseStats Base stats for the miner type
     * @param minerType "ORE", "Modulated", or "Ice"
     * @return AnalysisResult with all calculated metrics and tier
     */
    public static AnalysisResult analyzeRoll(Map<String, Double> stats,
            Map<String, Double> baseStats, String minerType) {
        AnalysisResult result = new AnalysisResult();

        // Fill in defaults from base stats
        Map<String, Double> mutatedStats = new HashMap<>(baseStats);
        mutatedStats.putAll(stats);
        result.setStats(mutatedStats);

        // Validate required stats exist and are not null
        Double miningAmount = mutatedStats.get("MiningAmount");
        Double activationTime = mutatedStats.get("ActivationTime");
        if (miningAmount == null || activationTime == null || activationTime <= 0) {
            throw new IllegalArgumentException(
                    "Missing or invalid required stats: MiningAmount or ActivationTime");
        }

        // Calculate m3/sec
        result.setM3PerSec(MiningCalculator.calculateBaseM3PerSec(miningAmount, activationTime));

        // Get stat values with defaults
        Double critChance = mutatedStats.getOrDefault("CriticalSuccessChance", 0.0);
        Double critBonus = mutatedStats.getOrDefault("CriticalSuccessBonusYield", 0.0);
        Double residueProb = mutatedStats.getOrDefault("ResidueProbability", 0.0);
        Double residueMult = mutatedStats.getOrDefault("ResidueVolumeMultiplier", 0.0);

        // Calculate effective m3/sec
        // ORE and Ice don't have residue, Modulated does
        if ("ORE".equals(minerType) || "Ice".equals(minerType)) {
            result.setEffectiveM3PerSec(MiningCalculator.calculateEffectiveM3PerSec(miningAmount,
                    activationTime, critChance, critBonus, 0.0, 0.0));
            result.setBasePlusCritsM3PerSec(null);
        } else {
            result.setBasePlusCritsM3PerSec(MiningCalculator.calculateBasePlusCritsM3PerSec(
                    miningAmount, activationTime, critChance, critBonus));
            result.setEffectiveM3PerSec(MiningCalculator.calculateEffectiveM3PerSec(miningAmount,
                    activationTime, critChance, critBonus, residueProb, residueMult));
        }

        // Calculate real-world values
        result.setRealWorldM3PerSec(
                MiningCalculator.calculateRealWorldBaseM3PerSec(miningAmount, activationTime));

        if ("ORE".equals(minerType) || "Ice".equals(minerType)) {
            result.setRealWorldEffectiveM3PerSec(
                    MiningCalculator.calculateRealWorldEffectiveM3PerSec(miningAmount,
                            activationTime, critChance, critBonus, 0.0, 0.0));
            result.setRealWorldBasePlusCritsM3PerSec(null);
        } else {
            result.setRealWorldBasePlusCritsM3PerSec(
                    MiningCalculator.calculateRealWorldEffectiveM3PerSec(miningAmount,
                            activationTime, critChance, critBonus, 0.0, 0.0));
            result.setRealWorldEffectiveM3PerSec(
                    MiningCalculator.calculateRealWorldEffectiveM3PerSec(miningAmount,
                            activationTime, critChance, critBonus, residueProb, residueMult));
        }

        // Determine tier
        String tier = determineTier(result.getM3PerSec(), minerType);
        
        // Check if optimal range is increased and add "+" suffix for tiers above F
        Double rolledOptimalRange = mutatedStats.get("OptimalRange");
        Double baseOptimalRange = baseStats.get("OptimalRange");
        if (rolledOptimalRange != null && baseOptimalRange != null 
                && rolledOptimalRange > baseOptimalRange && !"F".equals(tier)) {
            tier = tier + "+";
        }
        
        result.setTier(tier);

        return result;
    }

    /**
     * Determines the tier based on m³/sec value
     */
    private static String determineTier(double m3PerSec, String minerType) {
        Map<String, Map<String, Double>> tierRanges = MinerConfig.getTierRanges(minerType);
        if (tierRanges == null) {
            return "F";
        }

        // Check tier S (special case: only needs Min)
        Map<String, Double> sRange = tierRanges.get("S");
        if (isInSRange(m3PerSec, sRange)) {
            return "S";
        }

        // Check tiers A-E (need both Min and Max)
        String[] standardTiers = {"A", "B", "C", "D", "E"};
        for (String tier : standardTiers) {
            Map<String, Double> range = tierRanges.get(tier);
            if (isInStandardRange(m3PerSec, range)) {
                return tier;
            }
        }

        // Default to F
        return "F";
    }

    /**
     * Checks if a value is in the S tier range (only checks Min, no upper bound)
     */
    private static boolean isInSRange(double value, Map<String, Double> range) {
        return range != null && range.get("Min") != null && value >= range.get("Min");
    }

    /**
     * Checks if a value is in a standard tier range (checks both Min and Max)
     */
    private static boolean isInStandardRange(double value, Map<String, Double> range) {
        if (range == null) {
            return false;
        }
        Double min = range.get("Min");
        Double max = range.get("Max");
        return min != null && max != null && value >= min && value < max;
    }
}

