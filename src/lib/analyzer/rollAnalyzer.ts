import type { AnalysisResult, MinerType, BaseStats, TierRanges, SkillLevels } from '@/types';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';
import {
  applyModulatedCrystalModifiers,
  getTierRanges,
  createLiveModifiers,
} from '@/lib/config/minerConfig';

export function analyzeRoll(
  stats: Record<string, number>,
  baseStats: BaseStats,
  minerType: MinerType,
  skillLevels: SkillLevels,
): AnalysisResult {
  const adjustedStats =
    minerType === 'Modulated' ? applyModulatedCrystalModifiers(stats) : stats;
  const result: AnalysisResult = {
    stats: { ...baseStats, ...adjustedStats },
    m3PerSec: 0,
    basePlusCritsM3PerSec: null,
    effectiveM3PerSec: 0,
    tier: 'F',
  };

  // Validate required stats
  const miningAmount = result.stats.MiningAmount;
  const activationTime = result.stats.ActivationTime;
  if (
    miningAmount === undefined ||
    activationTime === undefined ||
    activationTime <= 0
  ) {
    throw new Error(
      'Missing or invalid required stats: MiningAmount or ActivationTime',
    );
  }

  // Calculate m3/sec
  result.m3PerSec = MiningCalculator.calculateBaseM3PerSec(
    miningAmount,
    activationTime,
  );

  // Get stat values with defaults
  const critChance = result.stats.CriticalSuccessChance ?? 0;
  const critBonus = result.stats.CriticalSuccessBonusYield ?? 0;
  const residueProb = result.stats.ResidueProbability ?? 0;
  const residueMult = result.stats.ResidueVolumeMultiplier ?? 0;

  // Calculate effective m3/sec
  // ORE and Ice don't have residue, Modulated does
  if (minerType === 'ORE' || minerType === 'Ice') {
    result.effectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
      miningAmount,
      activationTime,
      critChance,
      critBonus,
      0, // residueProbability (no residue for ORE and Ice)
      0, // residueMultiplier (no residue for ORE and Ice)
    );
    result.basePlusCritsM3PerSec = null;
  } else {
    result.basePlusCritsM3PerSec =
      MiningCalculator.calculateBasePlusCritsM3PerSec(
        miningAmount,
        activationTime,
        critChance,
        critBonus,
      );
    result.effectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
      miningAmount,
      activationTime,
      critChance,
      critBonus,
      residueProb,
      residueMult,
    );
  }


  const liveEffectiveM3PerSec = calculateLiveEffectiveM3PerSec(
    result.stats,
    minerType,
    skillLevels,
  );

  // Determine tier using live effective performance
  let tier = determineTier(liveEffectiveM3PerSec, minerType);

  // Check if optimal range is increased and add "+" suffix for tiers above F
  const rolledOptimalRange = result.stats.OptimalRange;
  const baseOptimalRange = baseStats.OptimalRange;
  if (
    rolledOptimalRange !== undefined &&
    baseOptimalRange !== undefined &&
    rolledOptimalRange > baseOptimalRange &&
    tier !== 'F'
  ) {
    tier = tier + '+';
  }

  result.tier = tier;

  return result;
}

function combineMultipliers(multipliers: number[]): number {
  if (multipliers.length === 0) {
    return 1;
  }
  return multipliers.reduce((acc, value) => acc * value, 1);
}

function calculateLiveEffectiveM3PerSec(
  stats: Record<string, number>,
  minerType: MinerType,
  skillLevels: SkillLevels,
): number {
  const liveModifiers = createLiveModifiers(minerType, skillLevels);
  const miningAmount =
    (stats.MiningAmount ?? 0) * combineMultipliers(liveModifiers.yield);
  const activationTime =
    (stats.ActivationTime ?? 0) * combineMultipliers(liveModifiers.cycleTime);
  const critChance =
    (stats.CriticalSuccessChance ?? 0) *
    combineMultipliers(liveModifiers.critChance);
  const critBonus =
    (stats.CriticalSuccessBonusYield ?? 0) *
    combineMultipliers(liveModifiers.critBonus);
  const residueProb =
    (stats.ResidueProbability ?? 0) *
    combineMultipliers(liveModifiers.residueProbability);
  const residueMult =
    (stats.ResidueVolumeMultiplier ?? 0) *
    combineMultipliers(liveModifiers.residueVolumeMultiplier);

  if (minerType === 'ORE' || minerType === 'Ice') {
    return MiningCalculator.calculateEffectiveM3PerSec(
      miningAmount,
      activationTime,
      critChance,
      critBonus,
      0, // residueProbability (no residue for ORE and Ice)
      0, // residueMultiplier (no residue for ORE and Ice)
    );
  }

  return MiningCalculator.calculateEffectiveM3PerSec(
    miningAmount,
    activationTime,
    critChance,
    critBonus,
    residueProb,
    residueMult,
  );
}

function determineTier(m3PerSec: number, minerType: MinerType): string {
  const tierRanges = getTierRanges(minerType);

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

function isInRange(
  value: number,
  range: { min: number; max?: number },
): boolean {
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
