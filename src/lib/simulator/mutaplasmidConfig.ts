import type { MinerType, MutaplasmidLevel } from '@/types';

export interface MutationRange {
  min: number; // percentage (e.g., -15 means -15%)
  max: number; // percentage (e.g., +20 means +20%)
}

export interface MutaplasmidMutations {
  ActivationCost: MutationRange;
  ActivationTime: MutationRange;
  CPUUsage: MutationRange;
  CriticalSuccessBonusYield: MutationRange;
  CriticalSuccessChance: MutationRange;
  MiningAmount: MutationRange;
  OptimalRange: MutationRange;
  PowergridUsage: MutationRange;
  ResidueProbability?: MutationRange; // Only for Modulated and Ice
  ResidueVolumeMultiplier?: MutationRange; // Only for Modulated and Ice
}

// ORE Mutaplasmid Configurations
const ORE_DECAYED: MutaplasmidMutations = {
  ActivationCost: { min: -15.0, max: 20.0 },
  ActivationTime: { min: -2.5, max: 5.0 },
  CPUUsage: { min: -5.0, max: 25.0 },
  CriticalSuccessBonusYield: { min: -10.0, max: 5.0 },
  CriticalSuccessChance: { min: -15.0, max: 10.0 },
  MiningAmount: { min: -5.0, max: 10.0 },
  OptimalRange: { min: -15.0, max: 20.0 },
  PowergridUsage: { min: -5.0, max: 25.0 },
};

const ORE_GRAVID: MutaplasmidMutations = {
  ActivationCost: { min: -20.0, max: 30.0 },
  ActivationTime: { min: -5.0, max: 7.5 },
  CPUUsage: { min: -15.0, max: 30.0 },
  CriticalSuccessBonusYield: { min: -15.0, max: 10.0 },
  CriticalSuccessChance: { min: -25.0, max: 20.0 },
  MiningAmount: { min: -10.0, max: 20.0 },
  OptimalRange: { min: -20.0, max: 25.0 },
  PowergridUsage: { min: -15.0, max: 30.0 },
};

const ORE_UNSTABLE: MutaplasmidMutations = {
  ActivationCost: { min: -40.0, max: 40.0 },
  ActivationTime: { min: -10.0, max: 10.0 },
  CPUUsage: { min: -20.0, max: 50.0 },
  CriticalSuccessBonusYield: { min: -20.0, max: 15.0 },
  CriticalSuccessChance: { min: -35.0, max: 30.0 },
  MiningAmount: { min: -15.0, max: 30.0 },
  OptimalRange: { min: -25.0, max: 30.0 },
  PowergridUsage: { min: -20.0, max: 50.0 },
};

// Modulated Mutaplasmid Configurations
const MODULATED_DECAYED: MutaplasmidMutations = {
  ActivationCost: { min: -15.0, max: 20.0 },
  ActivationTime: { min: -2.5, max: 5.0 },
  CPUUsage: { min: -5.0, max: 25.0 },
  CriticalSuccessBonusYield: { min: -10.0, max: 5.0 },
  CriticalSuccessChance: { min: -15.0, max: 10.0 },
  MiningAmount: { min: -5.0, max: 10.0 },
  OptimalRange: { min: -15.0, max: 20.0 },
  PowergridUsage: { min: -5.0, max: 25.0 },
  ResidueProbability: { min: -10.0, max: 10.0 },
  ResidueVolumeMultiplier: { min: -5.0, max: 10.0 },
};

const MODULATED_GRAVID: MutaplasmidMutations = {
  ActivationCost: { min: -20.0, max: 30.0 },
  ActivationTime: { min: -5.0, max: 7.5 },
  CPUUsage: { min: -15.0, max: 30.0 },
  CriticalSuccessBonusYield: { min: -15.0, max: 10.0 },
  CriticalSuccessChance: { min: -25.0, max: 20.0 },
  MiningAmount: { min: -10.0, max: 20.0 },
  OptimalRange: { min: -20.0, max: 25.0 },
  PowergridUsage: { min: -15.0, max: 30.0 },
  ResidueProbability: { min: -20.0, max: 20.0 },
  ResidueVolumeMultiplier: { min: -15.0, max: 10.0 },
};

const MODULATED_UNSTABLE: MutaplasmidMutations = {
  ActivationCost: { min: -40.0, max: 40.0 },
  ActivationTime: { min: -10.0, max: 10.0 },
  CPUUsage: { min: -20.0, max: 50.0 },
  CriticalSuccessBonusYield: { min: -20.0, max: 15.0 },
  CriticalSuccessChance: { min: -35.0, max: 30.0 },
  MiningAmount: { min: -15.0, max: 30.0 },
  OptimalRange: { min: -25.0, max: 30.0 },
  PowergridUsage: { min: -20.0, max: 50.0 },
  ResidueProbability: { min: -30.0, max: 30.0 },
  ResidueVolumeMultiplier: { min: -20.0, max: 15.0 },
};

// Ice Mutaplasmid Configurations
const ICE_DECAYED: MutaplasmidMutations = {
  ActivationCost: { min: -15.0, max: 20.0 },
  ActivationTime: { min: -2.5, max: 5.0 },
  CPUUsage: { min: -5.0, max: 25.0 },
  CriticalSuccessBonusYield: { min: -10.0, max: 5.0 },
  CriticalSuccessChance: { min: -15.0, max: 10.0 },
  MiningAmount: { min: -5.0, max: 10.0 },
  OptimalRange: { min: -15.0, max: 20.0 },
  PowergridUsage: { min: -5.0, max: 25.0 },
  ResidueProbability: { min: -10.0, max: 10.0 },
  ResidueVolumeMultiplier: { min: -5.0, max: 10.0 },
};

const ICE_GRAVID: MutaplasmidMutations = {
  ActivationCost: { min: -20.0, max: 30.0 },
  ActivationTime: { min: -5.0, max: 7.5 },
  CPUUsage: { min: -15.0, max: 30.0 },
  CriticalSuccessBonusYield: { min: -15.0, max: 10.0 },
  CriticalSuccessChance: { min: -25.0, max: 20.0 },
  MiningAmount: { min: -10.0, max: 20.0 },
  OptimalRange: { min: -20.0, max: 25.0 },
  PowergridUsage: { min: -15.0, max: 30.0 },
  ResidueProbability: { min: -20.0, max: 20.0 },
  ResidueVolumeMultiplier: { min: -15.0, max: 10.0 },
};

const ICE_UNSTABLE: MutaplasmidMutations = {
  ActivationCost: { min: -40.0, max: 40.0 },
  ActivationTime: { min: -10.0, max: 10.0 },
  CPUUsage: { min: -20.0, max: 50.0 },
  CriticalSuccessBonusYield: { min: -20.0, max: 15.0 },
  CriticalSuccessChance: { min: -35.0, max: 30.0 },
  MiningAmount: { min: -15.0, max: 30.0 },
  OptimalRange: { min: -25.0, max: 30.0 },
  PowergridUsage: { min: -20.0, max: 50.0 },
  ResidueProbability: { min: -30.0, max: 30.0 },
  ResidueVolumeMultiplier: { min: -20.0, max: 15.0 },
};

// Lookup maps for efficient access
const ORE_MUTAPLASMIDS: Record<MutaplasmidLevel, MutaplasmidMutations> = {
  Decayed: ORE_DECAYED,
  Gravid: ORE_GRAVID,
  Unstable: ORE_UNSTABLE,
};

const MODULATED_MUTAPLASMIDS: Record<MutaplasmidLevel, MutaplasmidMutations> = {
  Decayed: MODULATED_DECAYED,
  Gravid: MODULATED_GRAVID,
  Unstable: MODULATED_UNSTABLE,
};

const ICE_MUTAPLASMIDS: Record<MutaplasmidLevel, MutaplasmidMutations> = {
  Decayed: ICE_DECAYED,
  Gravid: ICE_GRAVID,
  Unstable: ICE_UNSTABLE,
};

const MUTAPLASMID_MAP: Record<MinerType, Record<MutaplasmidLevel, MutaplasmidMutations>> = {
  ORE: ORE_MUTAPLASMIDS,
  Modulated: MODULATED_MUTAPLASMIDS,
  Ice: ICE_MUTAPLASMIDS,
};

/**
 * Get mutation ranges for a specific miner type and mutaplasmid level
 */
export function getMutaplasmidMutations(
  minerType: MinerType,
  level: MutaplasmidLevel,
): MutaplasmidMutations {
  return MUTAPLASMID_MAP[minerType][level];
}
