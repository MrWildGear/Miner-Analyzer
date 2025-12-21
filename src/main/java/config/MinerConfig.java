package config;

import java.util.Collections;
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

    // Cached unmodifiable versions of tier ranges
    private static final Map<String, Map<String, Double>> UNMODIFIABLE_ORE_TIER_RANGES;
    private static final Map<String, Map<String, Double>> UNMODIFIABLE_MODULATED_TIER_RANGES;
    private static final Map<String, Map<String, Double>> UNMODIFIABLE_ICE_TIER_RANGES;

    // ============================================================================
    // BONUSES
    // ============================================================================

    public static final double SHIP_ROLE_BONUS = 1.75;
    public static final double MODULE_BONUS = 1.15;
    public static final double MINING_FOREMAN_BURST_YIELD = 1.15;
    public static final double INDUSTRIAL_CORE_YIELD = 1.50;
    public static final double INDUSTRIAL_CORE_CYCLE_TIME = 0.75;
    public static final double CALIBRATION_MULTIPLIER = 1.35;

    // Critical success and probability constants
    private static final double CRITICAL_SUCCESS_CHANCE_BASE = 0.01; // 1% base critical success chance
    private static final double MODULATED_RESIDUE_PROBABILITY = 0.34; // 34% residue probability for modulated
    private static final double ICE_MINING_AMOUNT_BASE = 1000.0; // Base mining amount for ice harvester

    // ============================================================================
    // STAT STRING CONSTANTS
    // ============================================================================

    private static final String ACTIVATION_COST = "ActivationCost";
    private static final String STRUCTURE_HITPOINTS = "StructureHitpoints";
    private static final String VOLUME = "Volume";
    private static final String OPTIMAL_RANGE = "OptimalRange";
    private static final String ACTIVATION_TIME = "ActivationTime";
    private static final String MINING_AMOUNT = "MiningAmount";
    private static final String CRITICAL_SUCCESS_CHANCE = "CriticalSuccessChance";
    private static final String RESIDUE_VOLUME_MULTIPLIER = "ResidueVolumeMultiplier";
    private static final String RESIDUE_PROBABILITY = "ResidueProbability";
    private static final String TECH_LEVEL = "TechLevel";
    private static final String CRITICAL_SUCCESS_BONUS_YIELD = "CriticalSuccessBonusYield";
    private static final String META_LEVEL = "MetaLevel";
    private static final String CAPACITY = "Capacity";

    // ============================================================================
    // TIER STRING CONSTANTS
    // ============================================================================

    private static final String MIN = "Min";
    private static final String MAX = "Max";
    private static final String TIER_S = "S";
    private static final String TIER_A = "A";
    private static final String TIER_B = "B";
    private static final String TIER_C = "C";
    private static final String TIER_D = "D";
    private static final String TIER_E = "E";
    private static final String TIER_F = "F";

    // ============================================================================
    // MINER TYPE CONSTANTS
    // ============================================================================

    private static final String MINER_TYPE_ORE = "ORE";
    private static final String MINER_TYPE_ICE = "Ice";

    static {
        initializeOreBaseStats();
        initializeModulatedBaseStats();
        initializeIceBaseStats();
        initializeOreTierRanges();
        initializeModulatedTierRanges();
        initializeIceTierRanges();
        
        // Initialize cached unmodifiable tier ranges
        UNMODIFIABLE_ORE_TIER_RANGES = createUnmodifiableTierRanges(ORE_TIER_RANGES);
        UNMODIFIABLE_MODULATED_TIER_RANGES = createUnmodifiableTierRanges(MODULATED_TIER_RANGES);
        UNMODIFIABLE_ICE_TIER_RANGES = createUnmodifiableTierRanges(ICE_TIER_RANGES);
    }
    
    /**
     * Creates an unmodifiable map with unmodifiable nested maps from a tier ranges map.
     */
    private static Map<String, Map<String, Double>> createUnmodifiableTierRanges(
            Map<String, Map<String, Double>> tierRanges) {
        Map<String, Map<String, Double>> unmodifiable = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : tierRanges.entrySet()) {
            unmodifiable.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(unmodifiable);
    }

    private static void initializeOreBaseStats() {
        ORE_BASE_STATS.put(ACTIVATION_COST, 23.0);
        ORE_BASE_STATS.put(STRUCTURE_HITPOINTS, 40.0);
        ORE_BASE_STATS.put(VOLUME, 5.0);
        ORE_BASE_STATS.put(OPTIMAL_RANGE, 18.75);
        ORE_BASE_STATS.put(ACTIVATION_TIME, 45.0);
        ORE_BASE_STATS.put(MINING_AMOUNT, 200.0);
        ORE_BASE_STATS.put(CRITICAL_SUCCESS_CHANCE, CRITICAL_SUCCESS_CHANCE_BASE);
        ORE_BASE_STATS.put(RESIDUE_VOLUME_MULTIPLIER, 0.0);
        ORE_BASE_STATS.put(RESIDUE_PROBABILITY, 0.0);
        ORE_BASE_STATS.put(TECH_LEVEL, 1.0);
        ORE_BASE_STATS.put(CRITICAL_SUCCESS_BONUS_YIELD, 2.0);
        ORE_BASE_STATS.put(META_LEVEL, 6.0);
    }

    private static void initializeModulatedBaseStats() {
        MODULATED_BASE_STATS.put(ACTIVATION_COST, 30.0);
        MODULATED_BASE_STATS.put(STRUCTURE_HITPOINTS, 40.0);
        MODULATED_BASE_STATS.put(VOLUME, 5.0);
        MODULATED_BASE_STATS.put(CAPACITY, 10.0);
        MODULATED_BASE_STATS.put(OPTIMAL_RANGE, 15.00);
        MODULATED_BASE_STATS.put(ACTIVATION_TIME, 45.0);
        MODULATED_BASE_STATS.put(MINING_AMOUNT, 120.0);
        MODULATED_BASE_STATS.put(CRITICAL_SUCCESS_CHANCE, CRITICAL_SUCCESS_CHANCE_BASE);
        MODULATED_BASE_STATS.put(RESIDUE_VOLUME_MULTIPLIER, 1.0);
        MODULATED_BASE_STATS.put(RESIDUE_PROBABILITY, MODULATED_RESIDUE_PROBABILITY);
        MODULATED_BASE_STATS.put(TECH_LEVEL, 2.0);
        MODULATED_BASE_STATS.put(CRITICAL_SUCCESS_BONUS_YIELD, 2.0);
        MODULATED_BASE_STATS.put(META_LEVEL, 5.0);
    }

    private static void initializeIceBaseStats() {
        // ORE Ice Harvester base stats
        // Activation time: 3m 20s = 200 seconds
        ICE_BASE_STATS.put(ACTIVATION_COST, 12.0);
        ICE_BASE_STATS.put(STRUCTURE_HITPOINTS, 40.0);
        ICE_BASE_STATS.put(VOLUME, 5.0);
        ICE_BASE_STATS.put(OPTIMAL_RANGE, 12.50);
        ICE_BASE_STATS.put(ACTIVATION_TIME, 200.0); // 3m 20s = 200s
        ICE_BASE_STATS.put(MINING_AMOUNT, ICE_MINING_AMOUNT_BASE);
        ICE_BASE_STATS.put(CRITICAL_SUCCESS_CHANCE, CRITICAL_SUCCESS_CHANCE_BASE); // 1%
        ICE_BASE_STATS.put(CRITICAL_SUCCESS_BONUS_YIELD, 2.0); // 200%
        ICE_BASE_STATS.put(RESIDUE_PROBABILITY, 0.0);
        ICE_BASE_STATS.put(RESIDUE_VOLUME_MULTIPLIER, 0.0);
        ICE_BASE_STATS.put(TECH_LEVEL, 1.0);
        ICE_BASE_STATS.put(META_LEVEL, 6.0);
    }

    private static void initializeOreTierRanges() {
        Map<String, Double> sRange = new HashMap<>();
        sRange.put(MIN, 6.27);
        sRange.put(MAX, 6.61);
        ORE_TIER_RANGES.put(TIER_S, sRange);

        Map<String, Double> aRange = new HashMap<>();
        aRange.put(MIN, 5.92);
        aRange.put(MAX, 6.27);
        ORE_TIER_RANGES.put(TIER_A, aRange);

        Map<String, Double> bRange = new HashMap<>();
        bRange.put(MIN, 5.57);
        bRange.put(MAX, 5.92);
        ORE_TIER_RANGES.put(TIER_B, bRange);

        Map<String, Double> cRange = new HashMap<>();
        cRange.put(MIN, 5.23);
        cRange.put(MAX, 5.57);
        ORE_TIER_RANGES.put(TIER_C, cRange);

        Map<String, Double> dRange = new HashMap<>();
        dRange.put(MIN, 4.88);
        dRange.put(MAX, 5.23);
        ORE_TIER_RANGES.put(TIER_D, dRange);

        Map<String, Double> eRange = new HashMap<>();
        eRange.put(MIN, 4.44);
        eRange.put(MAX, 4.88);
        ORE_TIER_RANGES.put(TIER_E, eRange);

        Map<String, Double> fRange = new HashMap<>();
        fRange.put(MIN, 0.0);
        fRange.put(MAX, 4.44);
        ORE_TIER_RANGES.put(TIER_F, fRange);
    }

    private static void initializeModulatedTierRanges() {
        Map<String, Double> modSRange = new HashMap<>();
        modSRange.put(MIN, 3.76188);
        modSRange.put(MAX, 3.97);
        MODULATED_TIER_RANGES.put(TIER_S, modSRange);

        Map<String, Double> modARange = new HashMap<>();
        modARange.put(MIN, 3.55376);
        modARange.put(MAX, 3.76188);
        MODULATED_TIER_RANGES.put(TIER_A, modARange);

        Map<String, Double> modBRange = new HashMap<>();
        modBRange.put(MIN, 3.34564);
        modBRange.put(MAX, 3.55376);
        MODULATED_TIER_RANGES.put(TIER_B, modBRange);

        Map<String, Double> modCRange = new HashMap<>();
        modCRange.put(MIN, 3.13752);
        modCRange.put(MAX, 3.34564);
        MODULATED_TIER_RANGES.put(TIER_C, modCRange);

        Map<String, Double> modDRange = new HashMap<>();
        modDRange.put(MIN, 2.92940);
        modDRange.put(MAX, 3.13752);
        MODULATED_TIER_RANGES.put(TIER_D, modDRange);

        Map<String, Double> modERange = new HashMap<>();
        modERange.put(MIN, 2.67);
        modERange.put(MAX, 2.92940);
        MODULATED_TIER_RANGES.put(TIER_E, modERange);

        Map<String, Double> modFRange = new HashMap<>();
        modFRange.put(MIN, 0.0);
        modFRange.put(MAX, 2.67);
        MODULATED_TIER_RANGES.put(TIER_F, modFRange);
    }

    private static void initializeIceTierRanges() {
        // Ice tier ranges based on m³/sec
        Map<String, Double> sRange = new HashMap<>();
        sRange.put(MIN, 7.033);
        sRange.put(MAX, 7.44);
        ICE_TIER_RANGES.put(TIER_S, sRange);

        Map<String, Double> aRange = new HashMap<>();
        aRange.put(MIN, 6.627);
        aRange.put(MAX, 7.033);
        ICE_TIER_RANGES.put(TIER_A, aRange);

        Map<String, Double> bRange = new HashMap<>();
        bRange.put(MIN, 6.220);
        bRange.put(MAX, 6.627);
        ICE_TIER_RANGES.put(TIER_B, bRange);

        Map<String, Double> cRange = new HashMap<>();
        cRange.put(MIN, 5.813);
        cRange.put(MAX, 6.220);
        ICE_TIER_RANGES.put(TIER_C, cRange);

        Map<String, Double> dRange = new HashMap<>();
        dRange.put(MIN, 5.407);
        dRange.put(MAX, 5.813);
        ICE_TIER_RANGES.put(TIER_D, dRange);

        Map<String, Double> eRange = new HashMap<>();
        eRange.put(MIN, 5.000);
        eRange.put(MAX, 5.407);
        ICE_TIER_RANGES.put(TIER_E, eRange);

        Map<String, Double> fRange = new HashMap<>();
        fRange.put(MIN, 0.0);
        fRange.put(MAX, 5.000);
        ICE_TIER_RANGES.put(TIER_F, fRange);
    }

    // ============================================================================
    // PUBLIC GETTERS
    // ============================================================================

    /**
     * Gets the base stats for ORE Strip Miner.
     * 
     * @return An unmodifiable map of stat names to base values
     */
    public static Map<String, Double> getOreBaseStats() {
        return Collections.unmodifiableMap(ORE_BASE_STATS);
    }

    /**
     * Gets the base stats for Modulated Strip Miner II.
     * 
     * @return An unmodifiable map of stat names to base values
     */
    public static Map<String, Double> getModulatedBaseStats() {
        return Collections.unmodifiableMap(MODULATED_BASE_STATS);
    }

    /**
     * Gets the base stats for ORE Ice Harvester.
     * 
     * @return An unmodifiable map of stat names to base values
     */
    public static Map<String, Double> getIceBaseStats() {
        return Collections.unmodifiableMap(ICE_BASE_STATS);
    }

    /**
     * Gets the base stats for the specified miner type.
     * 
     * @param minerType The miner type ("ORE", "Ice", or anything else for "Modulated")
     * @return An unmodifiable map of stat names to base values
     */
    public static Map<String, Double> getBaseStats(String minerType) {
        if (MINER_TYPE_ORE.equals(minerType)) {
            return getOreBaseStats();
        } else if (MINER_TYPE_ICE.equals(minerType)) {
            return getIceBaseStats();
        } else {
            return getModulatedBaseStats();
        }
    }

    /**
     * Gets the tier ranges for ORE Strip Miner.
     * 
     * @return An unmodifiable map of tier letters (S, A, B, C, D, E, F) to their min/max ranges
     */
    public static Map<String, Map<String, Double>> getOreTierRanges() {
        return UNMODIFIABLE_ORE_TIER_RANGES;
    }

    /**
     * Gets the tier ranges for Modulated Strip Miner II.
     * 
     * @return An unmodifiable map of tier letters (S, A, B, C, D, E, F) to their min/max ranges
     */
    public static Map<String, Map<String, Double>> getModulatedTierRanges() {
        return UNMODIFIABLE_MODULATED_TIER_RANGES;
    }

    /**
     * Gets the tier ranges for ORE Ice Harvester.
     * 
     * @return An unmodifiable map of tier letters (S, A, B, C, D, E, F) to their min/max ranges
     */
    public static Map<String, Map<String, Double>> getIceTierRanges() {
        return UNMODIFIABLE_ICE_TIER_RANGES;
    }

    /**
     * Gets the tier ranges for the specified miner type.
     * 
     * @param minerType The miner type ("ORE", "Ice", or anything else for "Modulated")
     * @return An unmodifiable map of tier letters (S, A, B, C, D, E, F) to their min/max ranges
     */
    public static Map<String, Map<String, Double>> getTierRanges(String minerType) {
        if (MINER_TYPE_ORE.equals(minerType)) {
            return getOreTierRanges();
        } else if (MINER_TYPE_ICE.equals(minerType)) {
            return getIceTierRanges();
        } else {
            return getModulatedTierRanges();
        }
    }
}

