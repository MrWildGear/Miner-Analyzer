import type { BaseStats, TierRanges, MinerType, SkillLevels } from '@/types';

// Bonus constants
export const SHIP_ROLE_BONUS = 1;
export const MODULE_BONUS = 1;
export const MINING_FOREMAN_BURST_MODULE_STRENGTH_BONUS = 0.25;
export const CAPITAL_INDUSTRIAL_CORE_BURST_STRENGTH_BONUS = 0.4;
export const CAPITAL_INDUSTRIAL_SHIPS_BURST_STRENGTH_BONUS_PER_LEVEL = 0.05;
export const CAPITAL_INDUSTRIAL_SHIPS_SKILL_LEVEL = 5;
export const INDUSTRIAL_CORE_YIELD = 1;
export const INDUSTRIAL_CORE_CYCLE_TIME = 0.75;
export const CALIBRATION_MULTIPLIER = 1;
export const SKILL_YIELD_MULTIPLIER = 1;
export const SKILL_CYCLE_TIME_MULTIPLIER = 1;
export const SKILL_RANGE_MULTIPLIER = 1;
export const HIGHWALL_MINING_YIELD_BONUS = 0.03;
export const IMPLANT_CYCLE_TIME_MULTIPLIER = 1;
export const IMPLANT_RANGE_MULTIPLIER = 1;

export const MINING_FOREMAN_SKILL_LEVEL = 5;
export const MINING_FOREMAN_BURST_STRENGTH_BONUS_PER_LEVEL = 0.1;
export const MINING_DIRECTOR_SKILL_LEVEL = 5;
export const MINING_DIRECTOR_BURST_STRENGTH_BONUS_PER_LEVEL = 0.15;
export const COMMAND_BURST_SPECIALIST_SKILL_LEVEL = 5;
export const COMMAND_BURST_SPECIALIST_STRENGTH_BONUS_PER_LEVEL = 0.1;

export const DEFAULT_SKILL_LEVEL = 5;
export function getDefaultSkillLevels(): SkillLevels {
  return {
    mining: DEFAULT_SKILL_LEVEL,
    astrogeology: DEFAULT_SKILL_LEVEL,
    miningBarge: DEFAULT_SKILL_LEVEL,
    exhumers: DEFAULT_SKILL_LEVEL,
    miningExploitation: DEFAULT_SKILL_LEVEL,
    miningPrecision: DEFAULT_SKILL_LEVEL,
    iceHarvesting: DEFAULT_SKILL_LEVEL,
    iceHarvestingImplant: 3,
    oreMiningImplant: 3,
  };
}

const MINING_SKILL_YIELD_PER_LEVEL = 0.05;
const ASTROGEOLOGY_YIELD_PER_LEVEL = 0.05;
const MINING_BARGE_YIELD_PER_LEVEL = 0.03;
const MINING_BARGE_RANGE_PER_LEVEL = 0.06;
const EXHUMERS_YIELD_PER_LEVEL = 0.06;
const EXHUMERS_STRIP_MINER_DURATION_REDUCTION_PER_LEVEL = 0.03;
const EXHUMERS_ICE_HARVESTER_DURATION_REDUCTION_PER_LEVEL = 0.04;
const MINING_BARGE_ICE_HARVESTER_DURATION_REDUCTION_PER_LEVEL = 0.03;
const ICE_HARVESTING_DURATION_REDUCTION_PER_LEVEL = 0.05;
const MINING_EXPLOITATION_CRIT_BONUS_PER_LEVEL = 0.05;
const MINING_PRECISION_CRIT_CHANCE_PER_LEVEL = 0.1;

const HULK_ROLE_STRIP_MINER_CYCLE_TIME = 0.85;
const HULK_ROLE_ICE_HARVESTER_CYCLE_TIME = 0.7;

const MINING_LASER_UPGRADE_II_YIELD_BONUS = 0.09;
const MINING_LASER_UPGRADE_II_COUNT = 3;
const APPLY_STACKING_PENALTY = false;

const ICE_HARVESTER_UPGRADE_II_CYCLE_TIME_BONUS = 0.09;
const ICE_HARVESTER_UPGRADE_II_COUNT = 3;
const ICE_HARVESTER_ACCELERATOR_I_CYCLE_TIME_BONUS = 0.12;
const ICE_HARVESTER_ACCELERATOR_I_COUNT = 1;

const MINING_SURVEY_CHIPSET_II_COUNT = 1;
const MINING_SURVEY_CHIPSET_II_CRIT_CHANCE = 1.2;
const MINING_SURVEY_CHIPSET_II_CRIT_BONUS = 1.2;
const MINING_SURVEY_CHIPSET_II_RESIDUE = 0.8;

const MINING_LASER_EFFICIENCY_CRIT_CHANCE_BONUS = 0.5;
const MINING_LASER_EFFICIENCY_RESIDUE_REDUCTION = 0.15;
const MINING_LASER_FIELD_ENHANCEMENT_RANGE_BONUS = 0.4;
const MINING_LASER_OPTIMIZATION_DURATION_REDUCTION = 0.15;
const MINING_EQUIPMENT_PRESERVATION_RESIDUE_REDUCTION = 0.15;
const MINING_FOREMAN_MINDLINK_BONUS = 0.25;
const BURST_STRENGTH_CALIBRATION_MULTIPLIER = 1.07;

const STACKING_PENALTY_FACTORS = [
  1,
  0.86911998,
  0.57058314,
  0.28295515,
  0.10599265,
  0.02999426,
  0.00640399,
  0.001,
];

function applyStackingPenalties(bonuses: number[]): number[] {
  const sorted = [...bonuses].sort(
    (a, b) => Math.abs(b) - Math.abs(a),
  );
  return sorted.map((bonus, index) => {
    const penalty = STACKING_PENALTY_FACTORS[index] ?? 0;
    return bonus * penalty;
  });
}

const MINING_LASER_UPGRADE_II_MULTIPLIERS = (
  APPLY_STACKING_PENALTY
    ? applyStackingPenalties(
        Array.from(
          { length: MINING_LASER_UPGRADE_II_COUNT },
          () => MINING_LASER_UPGRADE_II_YIELD_BONUS,
        ),
      )
    : Array.from(
        { length: MINING_LASER_UPGRADE_II_COUNT },
        () => MINING_LASER_UPGRADE_II_YIELD_BONUS,
      )
).map((bonus) => 1 + bonus);
const ICE_HARVESTER_UPGRADE_II_MULTIPLIERS = (
  APPLY_STACKING_PENALTY
    ? applyStackingPenalties(
        Array.from(
          { length: ICE_HARVESTER_UPGRADE_II_COUNT },
          () => ICE_HARVESTER_UPGRADE_II_CYCLE_TIME_BONUS,
        ),
      )
    : Array.from(
        { length: ICE_HARVESTER_UPGRADE_II_COUNT },
        () => ICE_HARVESTER_UPGRADE_II_CYCLE_TIME_BONUS,
      )
).map((bonus) => 1 - bonus);
const ICE_HARVESTER_ACCELERATOR_I_MULTIPLIERS = Array.from(
  { length: ICE_HARVESTER_ACCELERATOR_I_COUNT },
  () => 1 - ICE_HARVESTER_ACCELERATOR_I_CYCLE_TIME_BONUS,
);
const MINING_SURVEY_CHIPSET_II_CRIT_CHANCE_MULTIPLIERS = Array.from(
  { length: MINING_SURVEY_CHIPSET_II_COUNT },
  () => MINING_SURVEY_CHIPSET_II_CRIT_CHANCE,
);
const MINING_SURVEY_CHIPSET_II_CRIT_BONUS_MULTIPLIERS = Array.from(
  { length: MINING_SURVEY_CHIPSET_II_COUNT },
  () => MINING_SURVEY_CHIPSET_II_CRIT_BONUS,
);
const MINING_SURVEY_CHIPSET_II_RESIDUE_MULTIPLIERS = Array.from(
  { length: MINING_SURVEY_CHIPSET_II_COUNT },
  () => MINING_SURVEY_CHIPSET_II_RESIDUE,
);

const MINING_FOREMAN_BURST_STRENGTH_BONUS =
  (MINING_FOREMAN_BURST_MODULE_STRENGTH_BONUS +
    MINING_FOREMAN_MINDLINK_BONUS +
    CAPITAL_INDUSTRIAL_CORE_BURST_STRENGTH_BONUS +
    CAPITAL_INDUSTRIAL_SHIPS_SKILL_LEVEL *
      CAPITAL_INDUSTRIAL_SHIPS_BURST_STRENGTH_BONUS_PER_LEVEL +
    MINING_FOREMAN_SKILL_LEVEL * MINING_FOREMAN_BURST_STRENGTH_BONUS_PER_LEVEL +
    MINING_DIRECTOR_SKILL_LEVEL *
      MINING_DIRECTOR_BURST_STRENGTH_BONUS_PER_LEVEL +
    COMMAND_BURST_SPECIALIST_SKILL_LEVEL *
      COMMAND_BURST_SPECIALIST_STRENGTH_BONUS_PER_LEVEL) *
  BURST_STRENGTH_CALIBRATION_MULTIPLIER;

const MINING_FOREMAN_BURST_STRENGTH_MULTIPLIER =
  1 + MINING_FOREMAN_BURST_STRENGTH_BONUS;

export const MINING_FOREMAN_BURST_CYCLE_TIME = Math.max(
  0,
  1 -
    MINING_LASER_OPTIMIZATION_DURATION_REDUCTION *
      MINING_FOREMAN_BURST_STRENGTH_MULTIPLIER,
);
export const MINING_FOREMAN_BURST_RANGE =
  1 +
  MINING_LASER_FIELD_ENHANCEMENT_RANGE_BONUS *
    MINING_FOREMAN_BURST_STRENGTH_MULTIPLIER;
export const MINING_FOREMAN_BURST_RESIDUE = Math.max(
  0,
  1 -
    MINING_LASER_EFFICIENCY_RESIDUE_REDUCTION *
      MINING_FOREMAN_BURST_STRENGTH_MULTIPLIER,
);
export const MINING_FOREMAN_BURST_CRYSTAL_VOLATILITY = Math.max(
  0,
  1 -
    MINING_EQUIPMENT_PRESERVATION_RESIDUE_REDUCTION *
      MINING_FOREMAN_BURST_STRENGTH_MULTIPLIER,
);
export const MINING_FOREMAN_BURST_CRIT_CHANCE =
  1 +
  MINING_LASER_EFFICIENCY_CRIT_CHANCE_BONUS *
    MINING_FOREMAN_BURST_STRENGTH_MULTIPLIER;

const MODULATED_CRYSTAL_YIELD_MULTIPLIER = 1.8;
const MODULATED_CRYSTAL_DURATION_MULTIPLIER = 0.8;
const MODULATED_CRYSTAL_RESIDUE_PROBABILITY_BONUS = 0.3;
const MODULATED_CRYSTAL_RESIDUE_VOLUME_MULTIPLIER_BONUS = 0;

export function applyModulatedCrystalModifiers(
  stats: Record<string, number>,
): Record<string, number> {
  const updated = { ...stats };
  if (updated.MiningAmount !== undefined) {
    updated.MiningAmount *= MODULATED_CRYSTAL_YIELD_MULTIPLIER;
  }
  if (updated.ActivationTime !== undefined) {
    updated.ActivationTime *= MODULATED_CRYSTAL_DURATION_MULTIPLIER;
  }
  if (updated.ResidueProbability !== undefined) {
    updated.ResidueProbability += MODULATED_CRYSTAL_RESIDUE_PROBABILITY_BONUS;
  }
  if (updated.ResidueVolumeMultiplier !== undefined) {
    updated.ResidueVolumeMultiplier +=
      MODULATED_CRYSTAL_RESIDUE_VOLUME_MULTIPLIER_BONUS;
  }
  return updated;
}

export function createLiveModifiers(
  minerType: MinerType,
  skillLevels: SkillLevels,
) {
  const miningSkillYieldMultiplier =
    1 + skillLevels.mining * MINING_SKILL_YIELD_PER_LEVEL;
  const astrogeologyYieldMultiplier =
    1 + skillLevels.astrogeology * ASTROGEOLOGY_YIELD_PER_LEVEL;
  const miningBargeYieldMultiplier =
    1 + skillLevels.miningBarge * MINING_BARGE_YIELD_PER_LEVEL;
  const miningBargeRangeMultiplier =
    1 + skillLevels.miningBarge * MINING_BARGE_RANGE_PER_LEVEL;
  const exhumersYieldMultiplier =
    1 + skillLevels.exhumers * EXHUMERS_YIELD_PER_LEVEL;
  const miningBargeIceHarvesterDurationMultiplier = Math.max(
    0,
    1 -
      skillLevels.miningBarge *
        MINING_BARGE_ICE_HARVESTER_DURATION_REDUCTION_PER_LEVEL,
  );
  const exhumersIceHarvesterDurationMultiplier = Math.max(
    0,
    1 -
      skillLevels.exhumers *
        EXHUMERS_ICE_HARVESTER_DURATION_REDUCTION_PER_LEVEL,
  );
  const iceHarvestingDurationMultiplier = Math.max(
    0,
    1 - skillLevels.iceHarvesting * ICE_HARVESTING_DURATION_REDUCTION_PER_LEVEL,
  );
  const exhumersStripMinerDurationMultiplier = Math.max(
    0,
    1 -
      skillLevels.exhumers *
        EXHUMERS_STRIP_MINER_DURATION_REDUCTION_PER_LEVEL,
  );
  const miningExploitationCritBonusMultiplier =
    1 +
    skillLevels.miningExploitation * MINING_EXPLOITATION_CRIT_BONUS_PER_LEVEL;
  const miningPrecisionCritChanceMultiplier =
    1 + skillLevels.miningPrecision * MINING_PRECISION_CRIT_CHANCE_PER_LEVEL;

  const iceHarvestingImplantMultiplier = Math.max(
    0,
    1 - Math.min(5, Math.max(0, skillLevels.iceHarvestingImplant)) / 100,
  );
  const oreMiningImplantMultiplier =
    1 + Math.min(5, Math.max(0, skillLevels.oreMiningImplant)) / 100;
  const unifiedRangeBoostMultiplier = MINING_FOREMAN_BURST_RANGE;

  if (minerType === 'Ice') {
    return {
      yield: [SHIP_ROLE_BONUS],
      cycleTime: [
        HULK_ROLE_ICE_HARVESTER_CYCLE_TIME,
        miningBargeIceHarvesterDurationMultiplier,
        exhumersIceHarvesterDurationMultiplier,
        iceHarvestingDurationMultiplier,
        MINING_FOREMAN_BURST_CYCLE_TIME,
        ...ICE_HARVESTER_UPGRADE_II_MULTIPLIERS,
        ...ICE_HARVESTER_ACCELERATOR_I_MULTIPLIERS,
        SKILL_CYCLE_TIME_MULTIPLIER,
        iceHarvestingImplantMultiplier,
      ],
      range: [
        miningBargeRangeMultiplier,
        unifiedRangeBoostMultiplier,
        SKILL_RANGE_MULTIPLIER,
      ],
      critChance: [
        MINING_FOREMAN_BURST_CRIT_CHANCE,
        miningPrecisionCritChanceMultiplier,
        ...MINING_SURVEY_CHIPSET_II_CRIT_CHANCE_MULTIPLIERS,
      ],
      critBonus: [
        miningExploitationCritBonusMultiplier,
        ...MINING_SURVEY_CHIPSET_II_CRIT_BONUS_MULTIPLIERS,
      ],
      residueProbability: [
        MINING_FOREMAN_BURST_RESIDUE,
        ...MINING_SURVEY_CHIPSET_II_RESIDUE_MULTIPLIERS,
      ],
      residueVolumeMultiplier: [],
    };
  }

  return {
    yield: [
      SHIP_ROLE_BONUS,
      MODULE_BONUS,
      INDUSTRIAL_CORE_YIELD,
      CALIBRATION_MULTIPLIER,
      miningSkillYieldMultiplier,
      astrogeologyYieldMultiplier,
      miningBargeYieldMultiplier,
      exhumersYieldMultiplier,
      ...MINING_LASER_UPGRADE_II_MULTIPLIERS,
      SKILL_YIELD_MULTIPLIER,
      oreMiningImplantMultiplier,
    ],
    cycleTime: [
      HULK_ROLE_STRIP_MINER_CYCLE_TIME,
      exhumersStripMinerDurationMultiplier,
      MINING_FOREMAN_BURST_CYCLE_TIME,
      SKILL_CYCLE_TIME_MULTIPLIER,
      IMPLANT_CYCLE_TIME_MULTIPLIER,
    ],
    range: [
      MINING_FOREMAN_BURST_RANGE,
      miningBargeRangeMultiplier,
      SKILL_RANGE_MULTIPLIER,
      IMPLANT_RANGE_MULTIPLIER,
    ],
    critChance: [
      MINING_FOREMAN_BURST_CRIT_CHANCE,
      miningPrecisionCritChanceMultiplier,
      ...MINING_SURVEY_CHIPSET_II_CRIT_CHANCE_MULTIPLIERS,
    ],
    critBonus: [
      miningExploitationCritBonusMultiplier,
      ...MINING_SURVEY_CHIPSET_II_CRIT_BONUS_MULTIPLIERS,
    ],
    residueProbability: [
      MINING_FOREMAN_BURST_RESIDUE,
      ...MINING_SURVEY_CHIPSET_II_RESIDUE_MULTIPLIERS,
    ],
    residueVolumeMultiplier: [],
  };
}

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
  S: { min: 84.5, max: 89.7 },
  A: { min: 79.2, max: 84.5 },
  B: { min: 73.9, max: 79.2 },
  C: { min: 68.6, max: 73.9 },
  D: { min: 63.4, max: 68.6 },
  E: { min: 58.0, max: 63.4 },
  F: { min: 0, max: 58.0 },
};

const MODULATED_TIER_RANGES: TierRanges = {
  S: { min: 109.4, max: 121.0 },
  A: { min: 97.8, max: 109.4 },
  B: { min: 86.2, max: 97.8 },
  C: { min: 74.6, max: 86.2 },
  D: { min: 63.1, max: 74.6 },
  E: { min: 51.5, max: 63.1 },
  F: { min: 0, max: 51.5 },
};

const ICE_TIER_RANGES: TierRanges = {
  S: { min: 86.0, max: 91.4 },
  A: { min: 80.6, max: 86.0 },
  B: { min: 75.2, max: 80.6 },
  C: { min: 70.0, max: 75.2 },
  D: { min: 64.6, max: 70.0 },
  E: { min: 59.1, max: 64.6 },
  F: { min: 0, max: 59.1 },
};

export function getBaseStats(minerType: MinerType): BaseStats {
  switch (minerType) {
    case 'ORE':
      return { ...ORE_BASE_STATS };
    case 'Ice':
      return { ...ICE_BASE_STATS };
    case 'Modulated':
    default:
      return applyModulatedCrystalModifiers({ ...MODULATED_BASE_STATS });
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
