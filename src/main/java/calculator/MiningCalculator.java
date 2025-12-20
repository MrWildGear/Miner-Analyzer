package calculator;

import config.MinerConfig;

/**
 * Utility class for calculating mining performance metrics
 */
public class MiningCalculator {
    
    /**
     * Calculates skill bonus multiplier
     * Assumes level 5 in Mining, Astrogeology, and Exhumers
     */
    public static double getSkillBonus() {
        double miningBonus = 1 + (5 * 0.05);  // 1.25x
        double astroBonus = 1 + (5 * 0.05);    // 1.25x
        double exhumerBonus = 1 + (5 * 0.05);  // 1.25x
        return miningBonus * astroBonus * exhumerBonus;
    }
    
    /**
     * Calculates base m³/sec from mining amount and activation time
     */
    public static double calculateBaseM3PerSec(double miningAmount, double activationTime) {
        if (activationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        return miningAmount / activationTime;
    }
    
    /**
     * Calculates effective m³/sec including crits and residue
     */
    public static double calculateEffectiveM3PerSec(double miningAmount, double activationTime,
                                                     double critChance, double critBonus,
                                                     double residueProbability, double residueMultiplier) {
        if (activationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        
        double baseM3 = miningAmount;
        double critGain = baseM3 * critBonus * critChance;
        double residueLoss = baseM3 * residueProbability * residueMultiplier;
        double expectedM3PerCycle = baseM3 + critGain - residueLoss;
        return expectedM3PerCycle / activationTime;
    }
    
    /**
     * Calculates m³/sec with base and crits only (no residue)
     */
    public static double calculateBasePlusCritsM3PerSec(double miningAmount, double activationTime,
                                                        double critChance, double critBonus) {
        if (activationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        double baseM3 = miningAmount;
        double critGain = baseM3 * critBonus * critChance;
        double expectedM3PerCycle = baseM3 + critGain;
        return expectedM3PerCycle / activationTime;
    }
    
    /**
     * Calculates real-world base m³/sec with all bonuses applied
     */
    public static double calculateRealWorldBaseM3PerSec(double baseMiningAmount, double baseActivationTime) {
        if (baseActivationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        
        double skillMultiplier = getSkillBonus();
        double bonusedMiningAmount = baseMiningAmount * skillMultiplier;
        bonusedMiningAmount = bonusedMiningAmount * MinerConfig.SHIP_ROLE_BONUS;
        bonusedMiningAmount = bonusedMiningAmount * MinerConfig.MODULE_BONUS;
        
        double boostedMiningAmount = bonusedMiningAmount * MinerConfig.MINING_FOREMAN_BURST_YIELD * MinerConfig.INDUSTRIAL_CORE_YIELD;
        double boostedActivationTime = baseActivationTime * MinerConfig.INDUSTRIAL_CORE_CYCLE_TIME;
        
        if (boostedActivationTime <= 0) {
            throw new IllegalArgumentException("Boosted ActivationTime cannot be zero or negative");
        }
        
        boostedMiningAmount = boostedMiningAmount * MinerConfig.CALIBRATION_MULTIPLIER;
        return boostedMiningAmount / boostedActivationTime;
    }
    
    /**
     * Calculates real-world effective m³/sec with all bonuses and crits/residue
     */
    public static double calculateRealWorldEffectiveM3PerSec(double baseMiningAmount, double baseActivationTime,
                                                              double critChance, double critBonus,
                                                              double residueProbability, double residueMultiplier) {
        if (baseActivationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        
        double skillMultiplier = getSkillBonus();
        double bonusedMiningAmount = baseMiningAmount * skillMultiplier;
        bonusedMiningAmount = bonusedMiningAmount * MinerConfig.SHIP_ROLE_BONUS;
        bonusedMiningAmount = bonusedMiningAmount * MinerConfig.MODULE_BONUS;
        
        double boostedMiningAmount = bonusedMiningAmount * MinerConfig.MINING_FOREMAN_BURST_YIELD * MinerConfig.INDUSTRIAL_CORE_YIELD;
        double boostedActivationTime = baseActivationTime * MinerConfig.INDUSTRIAL_CORE_CYCLE_TIME;
        
        if (boostedActivationTime <= 0) {
            throw new IllegalArgumentException("Boosted ActivationTime cannot be zero or negative");
        }
        
        boostedMiningAmount = boostedMiningAmount * MinerConfig.CALIBRATION_MULTIPLIER;
        
        double baseM3 = boostedMiningAmount;
        double critGain = baseM3 * critBonus * critChance;
        double residueLoss = baseM3 * residueProbability * residueMultiplier;
        double expectedM3PerCycle = baseM3 + critGain - residueLoss;
        return expectedM3PerCycle / boostedActivationTime;
    }
}

