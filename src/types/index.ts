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
