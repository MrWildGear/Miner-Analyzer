
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

