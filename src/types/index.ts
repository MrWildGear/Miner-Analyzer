export type MinerType = 'ORE' | 'Modulated' | 'Ice';

export interface AnalysisResult {
  stats: Record<string, number>;
  m3PerSec: number;
  basePlusCritsM3PerSec: number | null;
  effectiveM3PerSec: number;
  tier: string;
}

export interface TierRange {
  min: number;
  max: number;
}

export interface BaseStats {
  [key: string]: number;
}

export interface TierRanges {
  [tier: string]: TierRange;
}

export interface TierModifiers {
  S: number;
  A: number;
  B: number;
  C: number;
  D: number;
  E: number;
  F: number;
}

export interface SkillLevels {
  mining: number;
  astrogeology: number;
  miningBarge: number;
  exhumers: number;
  miningExploitation: number;
  miningPrecision: number;
  iceHarvesting: number;
  iceHarvestingImplant: number;
  oreMiningImplant: number;
}

export type MutaplasmidLevel = 'Decayed' | 'Gravid' | 'Unstable';

export interface TierDistribution {
  [tier: string]: number; // Count of rolls for each tier (S, A, B, C, D, E, F, S+, A+, B+, C+, D+, E+)
}

export interface SimulationConfig {
  minerType: MinerType;
  mutaplasmidLevel: MutaplasmidLevel;
  baseItemCost: number;
  mutaplasmidCost: number;
  sampleSize: number;
  skillLevels: SkillLevels;
}

export interface SimulationResult {
  tierDistribution: TierDistribution;
  totalRolls: number;
  statistics: {
    averageEffectiveM3PerSec: number;
    medianEffectiveM3PerSec: number;
    minEffectiveM3PerSec: number;
    maxEffectiveM3PerSec: number;
  };
  costAnalysis: {
    costPerTier: Record<string, number>; // Cost per tier including +tiers
    expectedValue: number;
    roi: number; // Return on investment percentage
  };
}

export interface CharacterMiningData {
  characterName: string;
  totalMined: number;
  critMined: number;
  totalCycles: number;
  critCycles: number;
  totalResidue: number;
  shipsDestroyed: string[];
}

export interface OreData {
  oreName: string;
  nonCrit: number;
  crit: number;
  residue: number;
}

export interface LogAnalysisResult {
  characters: Record<string, CharacterMiningData>;
  oreBreakdown: Record<string, OreData>;
  dateRange: { start: Date; end: Date };
  activeDays: Date[];
  overall: {
    totalMined: number;
    totalCrit: number;
    totalCycles: number;
    totalCritCycles: number;
    totalResidue: number;
    overallCritRate: number;
    overallResidueRate: number;
  };
}
