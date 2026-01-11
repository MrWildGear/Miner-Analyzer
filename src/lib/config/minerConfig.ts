import type { BaseStats, TierRanges, MinerType } from '@/types';

// Bonus constants
export const SHIP_ROLE_BONUS = 1.75;
export const MODULE_BONUS = 1.15;
export const MINING_FOREMAN_BURST_YIELD = 1.15;
export const INDUSTRIAL_CORE_YIELD = 1.5;
export const INDUSTRIAL_CORE_CYCLE_TIME = 0.75;
export const CALIBRATION_MULTIPLIER = 1.35;

const ORE_BASE_STATS: BaseStats = {
  ActivationCost: 23,
  StructureHitpoints: 40,
  Volume: 5,
  OptimalRange: 18.75,
  ActivationTime: 45,
  MiningAmount: 200,
  CriticalSuccessChance: 0.01,
  ResidueVolumeMultiplier: 0,
  ResidueProbability: 0,
  TechLevel: 1,
  CriticalSuccessBonusYield: 2,
  MetaLevel: 6,
};

const MODULATED_BASE_STATS: BaseStats = {
  ActivationCost: 30,
  StructureHitpoints: 40,
  Volume: 5,
  Capacity: 10,
  OptimalRange: 15,
  ActivationTime: 45,
  MiningAmount: 120,
  CriticalSuccessChance: 0.01,
  ResidueVolumeMultiplier: 1,
  ResidueProbability: 0.34,
  TechLevel: 2,
  CriticalSuccessBonusYield: 2,
  MetaLevel: 5,
};

const ICE_BASE_STATS: BaseStats = {
  ActivationCost: 12,
  StructureHitpoints: 40,
  Volume: 5,
  OptimalRange: 12.5,
  ActivationTime: 200, // 3m 20s = 200s
  MiningAmount: 1000,
  CriticalSuccessChance: 0.01,
  CriticalSuccessBonusYield: 2,
  ResidueProbability: 0,
  ResidueVolumeMultiplier: 0,
  TechLevel: 1,
  MetaLevel: 6,
};

const ORE_TIER_RANGES: TierRanges = {
  S: { min: 6.27, max: 6.61 },
  A: { min: 5.92, max: 6.27 },
  B: { min: 5.57, max: 5.92 },
  C: { min: 5.23, max: 5.57 },
  D: { min: 4.88, max: 5.23 },
  E: { min: 4.44, max: 4.88 },
  F: { min: 0, max: 4.44 },
};

const MODULATED_TIER_RANGES: TierRanges = {
  S: { min: 3.76188, max: 3.97 },
  A: { min: 3.55376, max: 3.76188 },
  B: { min: 3.34564, max: 3.55376 },
  C: { min: 3.13752, max: 3.34564 },
  D: { min: 2.9294, max: 3.13752 },
  E: { min: 2.67, max: 2.9294 },
  F: { min: 0, max: 2.67 },
};

const ICE_TIER_RANGES: TierRanges = {
  S: { min: 7.033, max: 7.44 },
  A: { min: 6.627, max: 7.033 },
  B: { min: 6.22, max: 6.627 },
  C: { min: 5.813, max: 6.22 },
  D: { min: 5.407, max: 5.813 },
  E: { min: 5, max: 5.407 },
  F: { min: 0, max: 5 },
};

export function getBaseStats(minerType: MinerType): BaseStats {
  switch (minerType) {
    case 'ORE':
      return { ...ORE_BASE_STATS };
    case 'Ice':
      return { ...ICE_BASE_STATS };
    case 'Modulated':
    default:
      return { ...MODULATED_BASE_STATS };
  }
}

export function getTierRanges(minerType: MinerType): TierRanges {
  switch (minerType) {
    case 'ORE':
      return { ...ORE_TIER_RANGES };
    case 'Ice':
      return { ...ICE_TIER_RANGES };
    case 'Modulated':
    default:
      return { ...MODULATED_TIER_RANGES };
  }
}
