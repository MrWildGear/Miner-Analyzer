import {
  SHIP_ROLE_BONUS,
  MODULE_BONUS,
  MINING_FOREMAN_BURST_YIELD,
  INDUSTRIAL_CORE_YIELD,
  INDUSTRIAL_CORE_CYCLE_TIME,
  CALIBRATION_MULTIPLIER,
} from '@/lib/config/minerConfig';

const MAX_SKILL_LEVEL = 5;
const SKILL_BONUS_PER_LEVEL = 0.05;

function getSkillBonus(): number {
  const skillMultiplier = 1 + MAX_SKILL_LEVEL * SKILL_BONUS_PER_LEVEL; // 1.25x per skill
  const miningBonus = skillMultiplier;
  const astroBonus = skillMultiplier;
  const exhumerBonus = skillMultiplier;
  return miningBonus * astroBonus * exhumerBonus;
}

export function calculateBaseM3PerSec(
  miningAmount: number,
  activationTime: number,
): number {
  if (activationTime <= 0) {
    throw new Error('ActivationTime cannot be zero or negative');
  }
  return miningAmount / activationTime;
}

export function calculateEffectiveM3PerSec(
  miningAmount: number,
  activationTime: number,
  critChance: number,
  critBonus: number,
  residueProbability: number,
  residueMultiplier: number,
): number {
  if (activationTime <= 0) {
    throw new Error('ActivationTime cannot be zero or negative');
  }

  const baseM3 = miningAmount;
  const critGain = baseM3 * critBonus * critChance;
  const expectedM3PerCycle = baseM3 + critGain;
  return expectedM3PerCycle / activationTime;
}

export function calculateBasePlusCritsM3PerSec(
  miningAmount: number,
  activationTime: number,
  critChance: number,
  critBonus: number,
): number {
  if (activationTime <= 0) {
    throw new Error('ActivationTime cannot be zero or negative');
  }
  const baseM3 = miningAmount;
  const critGain = baseM3 * critBonus * critChance;
  const expectedM3PerCycle = baseM3 + critGain;
  return expectedM3PerCycle / activationTime;
}

export function calculateRealWorldBaseM3PerSec(
  baseMiningAmount: number,
  baseActivationTime: number,
): number {
  if (baseActivationTime <= 0) {
    throw new Error('ActivationTime cannot be zero or negative');
  }

  const skillMultiplier = getSkillBonus();
  let bonusedMiningAmount = baseMiningAmount * skillMultiplier;
  bonusedMiningAmount = bonusedMiningAmount * SHIP_ROLE_BONUS;
  bonusedMiningAmount = bonusedMiningAmount * MODULE_BONUS;

  let boostedMiningAmount =
    bonusedMiningAmount * MINING_FOREMAN_BURST_YIELD * INDUSTRIAL_CORE_YIELD;
  const boostedActivationTime =
    baseActivationTime * INDUSTRIAL_CORE_CYCLE_TIME;

  if (boostedActivationTime <= 0) {
    throw new Error('Boosted ActivationTime cannot be zero or negative');
  }

  boostedMiningAmount = boostedMiningAmount * CALIBRATION_MULTIPLIER;
  return boostedMiningAmount / boostedActivationTime;
}

export function calculateRealWorldEffectiveM3PerSec(
  baseMiningAmount: number,
  baseActivationTime: number,
  critChance: number,
  critBonus: number,
  residueProbability: number,
  residueMultiplier: number,
): number {
  if (baseActivationTime <= 0) {
    throw new Error('ActivationTime cannot be zero or negative');
  }

  const skillMultiplier = getSkillBonus();
  let bonusedMiningAmount = baseMiningAmount * skillMultiplier;
  bonusedMiningAmount = bonusedMiningAmount * SHIP_ROLE_BONUS;
  bonusedMiningAmount = bonusedMiningAmount * MODULE_BONUS;

  let boostedMiningAmount =
    bonusedMiningAmount * MINING_FOREMAN_BURST_YIELD * INDUSTRIAL_CORE_YIELD;
  const boostedActivationTime =
    baseActivationTime * INDUSTRIAL_CORE_CYCLE_TIME;

  if (boostedActivationTime <= 0) {
    throw new Error('Boosted ActivationTime cannot be zero or negative');
  }

  boostedMiningAmount = boostedMiningAmount * CALIBRATION_MULTIPLIER;

  const baseM3 = boostedMiningAmount;
  const critGain = baseM3 * critBonus * critChance;
  const expectedM3PerCycle = baseM3 + critGain;
  return expectedM3PerCycle / boostedActivationTime;
}
