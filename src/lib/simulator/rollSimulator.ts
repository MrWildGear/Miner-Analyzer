import type {
  BaseStats,
  MinerType,
  SkillLevels,
  MutaplasmidLevel,
  TierRanges,
} from '@/types';
import { getMutaplasmidMutations, type MutationRange } from './mutaplasmidConfig';
import { getBaseStats, getTierRanges, createLiveModifiers } from '@/lib/config/minerConfig';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';

/**
 * Generate a random number between min and max (inclusive)
 */
function randomInRange(min: number, max: number): number {
  return Math.random() * (max - min) + min;
}

/**
 * Apply a mutation percentage to a base stat value
 */
function applyMutation(baseValue: number, mutationPercent: number): number {
  return baseValue * (1 + mutationPercent / 100);
}

/**
 * Combine multipliers (same as in rollAnalyzer.ts)
 */
function combineMultipliers(multipliers: number[]): number {
  if (multipliers.length === 0) {
    return 1;
  }
  return multipliers.reduce((acc, value) => acc * value, 1);
}

/**
 * Apply live modifiers to stats (same logic as rollAnalyzer.ts)
 */
function applyLiveModifiersToStat(
  statName: string,
  value: number,
  liveModifiers: ReturnType<typeof createLiveModifiers>,
): number {
  switch (statName) {
    case 'MiningAmount':
      return value * combineMultipliers(liveModifiers.yield);
    case 'ActivationTime':
      return value * combineMultipliers(liveModifiers.cycleTime);
    case 'OptimalRange':
      return value * combineMultipliers(liveModifiers.range);
    case 'CriticalSuccessChance':
      return value * combineMultipliers(liveModifiers.critChance);
    case 'CriticalSuccessBonusYield':
      return value * combineMultipliers(liveModifiers.critBonus);
    case 'ResidueProbability':
      return value * combineMultipliers(liveModifiers.residueProbability);
    case 'ResidueVolumeMultiplier':
      return value * combineMultipliers(liveModifiers.residueVolumeMultiplier);
    default:
      return value;
  }
}

/**
 * Check if a value is within a tier range
 */
function isInRange(value: number, range: { min: number; max?: number }): boolean {
  const min = range.min;
  const max = range.max;
  if (min === undefined) {
    return false;
  }
  if (max === undefined) {
    return value >= min;
  }
  return value >= min && value < max;
}

/**
 * Determine tier based on live effective m3/sec (no residue)
 */
function determineTier(m3PerSec: number, tierRanges: TierRanges): string {
  // Check tier S
  const sRange = tierRanges.S;
  if (sRange && isInRange(m3PerSec, sRange)) {
    return 'S';
  }

  // Check tiers A-E
  const standardTiers: Array<keyof TierRanges> = ['A', 'B', 'C', 'D', 'E'];
  for (const tier of standardTiers) {
    const range = tierRanges[tier];
    if (range && isInRange(m3PerSec, range)) {
      return tier as string;
    }
  }

  // Default to F
  return 'F';
}

/**
 * Generate a single roll with mutations applied
 */
export function generateRoll(
  minerType: MinerType,
  mutaplasmidLevel: MutaplasmidLevel,
  skillLevels: SkillLevels,
): {
  stats: Record<string, number>;
  liveEffectiveM3PerSec: number;
  tier: string;
} {
  // Get base stats and mutation ranges
  const baseStats = getBaseStats(minerType);
  const mutations = getMutaplasmidMutations(minerType, mutaplasmidLevel);
  const tierRanges = getTierRanges(minerType);

  // Apply mutations to base stats
  const mutatedStats: Record<string, number> = { ...baseStats };

  // Apply mutations for each stat
  for (const [statName, range] of Object.entries(mutations)) {
    if (range && typeof range === 'object' && 'min' in range && 'max' in range) {
      const mutationRange = range as MutationRange;
      const baseValue = baseStats[statName];
      if (baseValue !== undefined) {
        const mutationPercent = randomInRange(mutationRange.min, mutationRange.max);
        mutatedStats[statName] = applyMutation(baseValue, mutationPercent);
      }
    }
  }

  // Apply live modifiers (skill levels)
  const liveModifiers = createLiveModifiers(minerType, skillLevels);
  const liveStats: Record<string, number> = {};
  for (const [statName, value] of Object.entries(mutatedStats)) {
    liveStats[statName] = applyLiveModifiersToStat(statName, value, liveModifiers);
  }

  // Calculate Live Effective M3/sec (no residue) using calculateBasePlusCritsM3PerSec
  const liveMiningAmount = liveStats.MiningAmount ?? 0;
  const liveActivationTime = liveStats.ActivationTime ?? 0;
  const liveCritChance = liveStats.CriticalSuccessChance ?? 0;
  const liveCritBonus = liveStats.CriticalSuccessBonusYield ?? 0;

  const liveEffectiveM3PerSec = MiningCalculator.calculateBasePlusCritsM3PerSec(
    liveMiningAmount,
    liveActivationTime,
    liveCritChance,
    liveCritBonus,
  );

  // Determine tier
  let tier = determineTier(liveEffectiveM3PerSec, tierRanges);

  // Check if optimal range is increased and add "+" suffix for tiers above F
  // Compare raw mutated stats vs base stats (before live modifiers), same as analyzer
  const rolledOptimalRange = mutatedStats.OptimalRange;
  const baseOptimalRange = baseStats.OptimalRange;
  if (
    rolledOptimalRange !== undefined &&
    baseOptimalRange !== undefined &&
    rolledOptimalRange > baseOptimalRange &&
    tier !== 'F'
  ) {
    tier = tier + '+';
  }

  return {
    stats: mutatedStats,
    liveEffectiveM3PerSec,
    tier,
  };
}

/**
 * Simulate multiple rolls and aggregate results
 */
export function simulateRolls(
  minerType: MinerType,
  mutaplasmidLevel: MutaplasmidLevel,
  skillLevels: SkillLevels,
  sampleSize: number,
  onProgress?: (completed: number, total: number) => void,
): {
  tierDistribution: Record<string, number>;
  effectiveM3PerSecValues: number[];
} {
  const tierDistribution: Record<string, number> = {};
  const effectiveM3PerSecValues: number[] = [];

  const BATCH_SIZE = 100000; // Process in batches for progress updates
  let completed = 0;

  for (let i = 0; i < sampleSize; i++) {
    const roll = generateRoll(minerType, mutaplasmidLevel, skillLevels);

    // Track tier distribution
    tierDistribution[roll.tier] = (tierDistribution[roll.tier] || 0) + 1;

    // Track effective m3/sec values for statistics
    effectiveM3PerSecValues.push(roll.liveEffectiveM3PerSec);

    completed++;

    // Report progress periodically
    if (onProgress && completed % BATCH_SIZE === 0) {
      onProgress(completed, sampleSize);
    }
  }

  if (onProgress) {
    onProgress(completed, sampleSize);
  }

  return {
    tierDistribution,
    effectiveM3PerSecValues,
  };
}
