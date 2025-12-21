package config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class containing base stats, tier ranges, and bonuses for ORE Strip Miner,
 * Modulated Strip Miner II, and ORE Ice Harvester
 */
public class MinerConfig {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MinerConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ============================================================================
    // BASE STATS
    // ============================================================================

    private static final Map<String, Double> ORE_BASE_STATS = new HashMap<>();
    private static final Map<String, Double> MODULATED_BASE_STATS = new HashMap<>();
    private static final Map<String, Double> ICE_BASE_STATS = new HashMap<>();

    // ============================================================================
    // TIER RANGES
    // ============================================================================

    private static final Map<String, Map<String, Double>> ORE_TIER_RANGES = new HashMap<>();
    private static final Map<String, Map<String, Double>> MODULATED_TIER_RANGES = new HashMap<>();
    private static final Map<String, Map<String, Double>> ICE_TIER_RANGES = new HashMap<>();

    // ============================================================================
    // BONUSES
    // ============================================================================

    public static final double SHIP_ROLE_BONUS = 1.75;
    public static final double MODULE_BONUS = 1.15;
    public static final double MINING_FOREMAN_BURST_YIELD = 1.15;
    public static final double INDUSTRIAL_CORE_YIELD = 1.50;
    public static final double INDUSTRIAL_CORE_CYCLE_TIME = 0.75;
    public static final double CALIBRATION_MULTIPLIER = 1.35;

    static {
        initializeOreBaseStats();
        initializeModulatedBaseStats();
        initializeIceBaseStats();
        initializeOreTierRanges();
        initializeModulatedTierRanges();
        initializeIceTierRanges();
    }

    private static void initializeOreBaseStats() {
        ORE_BASE_STATS.put("ActivationCost", 23.0);
        ORE_BASE_STATS.put("StructureHitpoints", 40.0);
        ORE_BASE_STATS.put("Volume", 5.0);
        ORE_BASE_STATS.put("OptimalRange", 18.75);
        ORE_BASE_STATS.put("ActivationTime", 45.0);
        ORE_BASE_STATS.put("MiningAmount", 200.0);
        ORE_BASE_STATS.put("CriticalSuccessChance", 0.01);
        ORE_BASE_STATS.put("ResidueVolumeMultiplier", 0.0);
        ORE_BASE_STATS.put("ResidueProbability", 0.0);
        ORE_BASE_STATS.put("TechLevel", 1.0);
        ORE_BASE_STATS.put("CriticalSuccessBonusYield", 2.0);
        ORE_BASE_STATS.put("MetaLevel", 6.0);
    }

    private static void initializeModulatedBaseStats() {
        MODULATED_BASE_STATS.put("ActivationCost", 30.0);
        MODULATED_BASE_STATS.put("StructureHitpoints", 40.0);
        MODULATED_BASE_STATS.put("Volume", 5.0);
        MODULATED_BASE_STATS.put("Capacity", 10.0);
        MODULATED_BASE_STATS.put("OptimalRange", 15.00);
        MODULATED_BASE_STATS.put("ActivationTime", 45.0);
        MODULATED_BASE_STATS.put("MiningAmount", 120.0);
        MODULATED_BASE_STATS.put("CriticalSuccessChance", 0.01);
        MODULATED_BASE_STATS.put("ResidueVolumeMultiplier", 1.0);
        MODULATED_BASE_STATS.put("ResidueProbability", 0.34);
        MODULATED_BASE_STATS.put("TechLevel", 2.0);
        MODULATED_BASE_STATS.put("CriticalSuccessBonusYield", 2.0);
        MODULATED_BASE_STATS.put("MetaLevel", 5.0);
    }

    private static void initializeIceBaseStats() {
        // ORE Ice Harvester base stats
        // Activation time: 3m 20s = 200 seconds
        ICE_BASE_STATS.put("ActivationCost", 12.0);
        ICE_BASE_STATS.put("StructureHitpoints", 40.0);
        ICE_BASE_STATS.put("Volume", 5.0);
        ICE_BASE_STATS.put("OptimalRange", 12.50);
        ICE_BASE_STATS.put("ActivationTime", 200.0); // 3m 20s = 200s
        ICE_BASE_STATS.put("MiningAmount", 1000.0);
        ICE_BASE_STATS.put("CriticalSuccessChance", 0.01); // 1%
        ICE_BASE_STATS.put("CriticalSuccessBonusYield", 2.0); // 200%
        ICE_BASE_STATS.put("ResidueProbability", 0.0);
        ICE_BASE_STATS.put("ResidueVolumeMultiplier", 0.0);
        ICE_BASE_STATS.put("TechLevel", 1.0);
        ICE_BASE_STATS.put("MetaLevel", 6.0);
    }

    private static void initializeOreTierRanges() {
        Map<String, Double> sRange = new HashMap<>();
        sRange.put("Min", 6.27);
        sRange.put("Max", 6.61);
        ORE_TIER_RANGES.put("S", sRange);

        Map<String, Double> aRange = new HashMap<>();
        aRange.put("Min", 5.92);
        aRange.put("Max", 6.27);
        ORE_TIER_RANGES.put("A", aRange);

        Map<String, Double> bRange = new HashMap<>();
        bRange.put("Min", 5.57);
        bRange.put("Max", 5.92);
        ORE_TIER_RANGES.put("B", bRange);

        Map<String, Double> cRange = new HashMap<>();
        cRange.put("Min", 5.23);
        cRange.put("Max", 5.57);
        ORE_TIER_RANGES.put("C", cRange);

        Map<String, Double> dRange = new HashMap<>();
        dRange.put("Min", 4.88);
        dRange.put("Max", 5.23);
        ORE_TIER_RANGES.put("D", dRange);

        Map<String, Double> eRange = new HashMap<>();
        eRange.put("Min", 4.44);
        eRange.put("Max", 4.88);
        ORE_TIER_RANGES.put("E", eRange);

        Map<String, Double> fRange = new HashMap<>();
        fRange.put("Min", 0.0);
        fRange.put("Max", 4.44);
        ORE_TIER_RANGES.put("F", fRange);
    }

    private static void initializeModulatedTierRanges() {
        Map<String, Double> modSRange = new HashMap<>();
        modSRange.put("Min", 3.76188);
        modSRange.put("Max", 3.97);
        MODULATED_TIER_RANGES.put("S", modSRange);

        Map<String, Double> modARange = new HashMap<>();
        modARange.put("Min", 3.55376);
        modARange.put("Max", 3.76188);
        MODULATED_TIER_RANGES.put("A", modARange);

        Map<String, Double> modBRange = new HashMap<>();
        modBRange.put("Min", 3.34564);
        modBRange.put("Max", 3.55376);
        MODULATED_TIER_RANGES.put("B", modBRange);

        Map<String, Double> modCRange = new HashMap<>();
        modCRange.put("Min", 3.13752);
        modCRange.put("Max", 3.34564);
        MODULATED_TIER_RANGES.put("C", modCRange);

        Map<String, Double> modDRange = new HashMap<>();
        modDRange.put("Min", 2.92940);
        modDRange.put("Max", 3.13752);
        MODULATED_TIER_RANGES.put("D", modDRange);

        Map<String, Double> modERange = new HashMap<>();
        modERange.put("Min", 2.67);
        modERange.put("Max", 2.92940);
        MODULATED_TIER_RANGES.put("E", modERange);

        Map<String, Double> modFRange = new HashMap<>();
        modFRange.put("Min", 0.0);
        modFRange.put("Max", 2.67);
        MODULATED_TIER_RANGES.put("F", modFRange);
    }

    private static void initializeIceTierRanges() {
        // Ice tier ranges based on m³/sec
        Map<String, Double> sRange = new HashMap<>();
        sRange.put("Min", 7.033);
        sRange.put("Max", 7.44);
        ICE_TIER_RANGES.put("S", sRange);

        Map<String, Double> aRange = new HashMap<>();
        aRange.put("Min", 6.627);
        aRange.put("Max", 7.033);
        ICE_TIER_RANGES.put("A", aRange);

        Map<String, Double> bRange = new HashMap<>();
        bRange.put("Min", 6.220);
        bRange.put("Max", 6.627);
        ICE_TIER_RANGES.put("B", bRange);

        Map<String, Double> cRange = new HashMap<>();
        cRange.put("Min", 5.813);
        cRange.put("Max", 6.220);
        ICE_TIER_RANGES.put("C", cRange);

        Map<String, Double> dRange = new HashMap<>();
        dRange.put("Min", 5.407);
        dRange.put("Max", 5.813);
        ICE_TIER_RANGES.put("D", dRange);

        Map<String, Double> eRange = new HashMap<>();
        eRange.put("Min", 5.000);
        eRange.put("Max", 5.407);
        ICE_TIER_RANGES.put("E", eRange);

        Map<String, Double> fRange = new HashMap<>();
        fRange.put("Min", 0.0);
        fRange.put("Max", 5.000);
        ICE_TIER_RANGES.put("F", fRange);
    }

    // ============================================================================
    // PUBLIC GETTERS
    // ============================================================================

    public static Map<String, Double> getOreBaseStats() {
        return new HashMap<>(ORE_BASE_STATS);
    }

    public static Map<String, Double> getModulatedBaseStats() {
        return new HashMap<>(MODULATED_BASE_STATS);
    }

    public static Map<String, Double> getIceBaseStats() {
        return new HashMap<>(ICE_BASE_STATS);
    }

    public static Map<String, Double> getBaseStats(String minerType) {
        if ("ORE".equals(minerType)) {
            return getOreBaseStats();
        } else if ("Ice".equals(minerType)) {
            return getIceBaseStats();
        } else {
            return getModulatedBaseStats();
        }
    }

    public static Map<String, Map<String, Double>> getOreTierRanges() {
        // Return deep copy to prevent modification
        Map<String, Map<String, Double>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : ORE_TIER_RANGES.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public static Map<String, Map<String, Double>> getModulatedTierRanges() {
        // Return deep copy to prevent modification
        Map<String, Map<String, Double>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : MODULATED_TIER_RANGES.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public static Map<String, Map<String, Double>> getIceTierRanges() {
        // Return deep copy to prevent modification
        Map<String, Map<String, Double>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : ICE_TIER_RANGES.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public static Map<String, Map<String, Double>> getTierRanges(String minerType) {
        if ("ORE".equals(minerType)) {
            return getOreTierRanges();
        } else if ("Ice".equals(minerType)) {
            return getIceTierRanges();
        } else {
            return getModulatedTierRanges();
        }
    }
}

