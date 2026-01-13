import type { AnalysisResult, MinerType, BaseStats, TierRanges } from '@/types';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';
import { getTierRanges } from '@/lib/config/minerConfig';

export function analyzeRoll(
  stats: Record<string, number>,
  baseStats: BaseStats,
  minerType: MinerType,
): AnalysisResult {
  const result: AnalysisResult = {
    stats: { ...baseStats, ...stats },
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
      0,
      0,
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


  // Determine tier
  let tier = determineTier(result.m3PerSec, minerType);

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

function determineTier(m3PerSec: number, minerType: MinerType): string {
  const tierRanges = getTierRanges(minerType);

  // Check tier S (special case: only needs Min)
  const sRange = tierRanges.S;
  if (sRange && isInSRange(m3PerSec, sRange)) {
    return 'S';
  }

  // Check tiers A-E (need both Min and Max)
  const standardTiers: Array<keyof TierRanges> = ['A', 'B', 'C', 'D', 'E'];
  for (const tier of standardTiers) {
    const range = tierRanges[tier];
    if (range && isInStandardRange(m3PerSec, range)) {
      return tier as string;
    }
  }

  // Default to F
  return 'F';
}

function isInSRange(value: number, range: { min: number; max?: number }): boolean {
  return range.min !== undefined && value >= range.min;
}

function isInStandardRange(
  value: number,
  range: { min: number; max: number },
): boolean {
  const min = range.min;
  const max = range.max;
  return min !== undefined && max !== undefined && value >= min && value < max;
}
