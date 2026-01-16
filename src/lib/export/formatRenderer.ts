import type { AnalysisResult, MinerType, SkillLevels } from '@/types';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';
import { createLiveModifiers } from '@/lib/config/minerConfig';

interface FormatContext {
  analysis: AnalysisResult;
  baseStats: Record<string, number>;
  minerType: MinerType;
  skillLevels: SkillLevels;
}

/**
 * Renders the export format by replacing placeholders with actual values
 */
export function renderExportFormat(
  format: string,
  context: FormatContext,
): string {
  const {
    analysis,
    baseStats,
    minerType,
    skillLevels,
  } = context;

  // Calculate base values
  const baseMiningAmount = baseStats.MiningAmount ?? 0;
  const baseActivationTime = baseStats.ActivationTime ?? 0;
  const baseCritChance = baseStats.CriticalSuccessChance ?? 0;
  const baseCritBonus = baseStats.CriticalSuccessBonusYield ?? 0;
  const baseResidueProb = baseStats.ResidueProbability ?? 0;
  const baseResidueMult = baseStats.ResidueVolumeMultiplier ?? 0;
  const baseEffectiveMiningPct =
    MiningCalculator.calculateEffectiveMiningPercent(
      baseCritChance,
      baseCritBonus,
      baseResidueProb,
      baseResidueMult,
    );
  const rolledEffectiveMiningPct =
    MiningCalculator.calculateEffectiveMiningPercent(
      analysis.stats.CriticalSuccessChance ?? 0,
      analysis.stats.CriticalSuccessBonusYield ?? 0,
      analysis.stats.ResidueProbability ?? 0,
      analysis.stats.ResidueVolumeMultiplier ?? 0,
    );

  const baseBaseM3PerSec = MiningCalculator.calculateBaseM3PerSec(
    baseMiningAmount,
    baseActivationTime,
  );
  const baseEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
    baseMiningAmount,
    baseActivationTime,
    baseCritChance,
    baseCritBonus,
    baseResidueProb,
    baseResidueMult,
  );

  // Calculate percentages for both Base and Effective M3/sec
  const baseM3Pct = ((analysis.m3PerSec - baseBaseM3PerSec) / baseBaseM3PerSec) * 100;
  const effectiveM3Pct = ((analysis.effectiveM3PerSec - baseEffectiveM3PerSec) / baseEffectiveM3PerSec) * 100;

  const formatPercentage = (value: number): string => {
    const absValue = Math.abs(value);
    if (absValue < 10) {
      const sign = value >= 0 ? '+' : '-';
      // Ensure 2 digits before decimal point (e.g., "05.2" not "5.2")
      const parts = absValue.toFixed(1).split('.');
      const integerPart = parts[0].padStart(2, '0');
      const padded = `${integerPart}.${parts[1]}`;
      return sign + padded;
    }
    // For values >= 10, show + for positive, - for negative
    const sign = value >= 0 ? '+' : '-';
    return sign + absValue.toFixed(1);
  };

  const calculatePercentage = (base: number, rolled: number): number => {
    if (base === 0) return 0;
    return ((rolled - base) / base) * 100;
  };

  const normalizeRangeValue = (value: number): number =>
    value > 1000 ? value / 1000 : value;

  const combineMultipliers = (multipliers: number[]): number => {
    if (multipliers.length === 0) {
      return 1;
    }
    return multipliers.reduce((acc, value) => acc * value, 1);
  };

  const getLiveMultiplierForStat = (
    statName: string,
    liveModifiers: ReturnType<typeof createLiveModifiers>,
  ): number => {
    switch (statName) {
      case 'MiningAmount':
        return combineMultipliers(liveModifiers.yield);
      case 'ActivationTime':
        return combineMultipliers(liveModifiers.cycleTime);
      case 'OptimalRange':
        return combineMultipliers(liveModifiers.range);
      case 'CriticalSuccessChance':
        return combineMultipliers(liveModifiers.critChance);
      case 'CriticalSuccessBonusYield':
        return combineMultipliers(liveModifiers.critBonus);
      case 'ResidueProbability':
        return combineMultipliers(liveModifiers.residueProbability);
      case 'ResidueVolumeMultiplier':
        return combineMultipliers(liveModifiers.residueVolumeMultiplier);
      default:
        return 1;
    }
  };

  const applyLiveModifiersToStats = (
    stats: Record<string, number>,
    liveModifiers: ReturnType<typeof createLiveModifiers>,
  ): Record<string, number> => {
    const liveStats: Record<string, number> = { ...stats };
    for (const [statName, value] of Object.entries(stats)) {
      if (typeof value !== 'number') {
        continue;
      }
      const multiplier = getLiveMultiplierForStat(statName, liveModifiers);
      liveStats[statName] = value * multiplier;
    }
    return liveStats;
  };

  const liveModifiers = createLiveModifiers(minerType, skillLevels);
  const liveBaseStats = applyLiveModifiersToStats(baseStats, liveModifiers);
  const liveRolledStats = applyLiveModifiersToStats(
    analysis.stats,
    liveModifiers,
  );

  const liveMiningAmount = liveRolledStats.MiningAmount ?? 0;
  const liveActivationTime = liveRolledStats.ActivationTime ?? 0;
  const liveCritChance = liveRolledStats.CriticalSuccessChance ?? 0;
  const liveCritBonus = liveRolledStats.CriticalSuccessBonusYield ?? 0;
  const liveOptimalRange = normalizeRangeValue(
    liveRolledStats.OptimalRange ?? 0,
  );
  const liveResidueProb = liveRolledStats.ResidueProbability ?? 0;
  const liveResidueMult = liveRolledStats.ResidueVolumeMultiplier ?? 0;

  const liveBaseMiningAmount = liveBaseStats.MiningAmount ?? 0;
  const liveBaseActivationTime = liveBaseStats.ActivationTime ?? 0;
  const liveBaseCritChance = liveBaseStats.CriticalSuccessChance ?? 0;
  const liveBaseCritBonus = liveBaseStats.CriticalSuccessBonusYield ?? 0;
  const liveBaseOptimalRange = normalizeRangeValue(
    liveBaseStats.OptimalRange ?? 0,
  );
  const liveBaseResidueProb = liveBaseStats.ResidueProbability ?? 0;
  const liveBaseResidueMult = liveBaseStats.ResidueVolumeMultiplier ?? 0;
  const liveBaseEffectiveMiningPct =
    MiningCalculator.calculateEffectiveMiningPercent(
      liveBaseCritChance,
      liveBaseCritBonus,
      liveBaseResidueProb,
      liveBaseResidueMult,
    );
  const liveEffectiveMiningPct =
    MiningCalculator.calculateEffectiveMiningPercent(
      liveCritChance,
      liveCritBonus,
      liveResidueProb,
      liveResidueMult,
    );

  const liveM3PerSec = MiningCalculator.calculateBaseM3PerSec(
    liveMiningAmount,
    liveActivationTime,
  );
  const liveBaseM3PerSec = MiningCalculator.calculateBaseM3PerSec(
    liveBaseMiningAmount,
    liveBaseActivationTime,
  );
  const liveEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
    liveMiningAmount,
    liveActivationTime,
    liveCritChance,
    liveCritBonus,
    liveResidueProb,
    liveResidueMult,
  );
  const liveBaseEffectiveM3PerSec =
    MiningCalculator.calculateEffectiveM3PerSec(
      liveBaseMiningAmount,
      liveBaseActivationTime,
      liveBaseCritChance,
      liveBaseCritBonus,
      liveBaseResidueProb,
      liveBaseResidueMult,
    );

  // Calculate optimal range percentage if available
  const rolledOptimalRange = analysis.stats.OptimalRange;
  const baseOptimalRange = baseStats.OptimalRange;
  const normalizedRolledOptimalRange =
    rolledOptimalRange !== undefined
      ? normalizeRangeValue(rolledOptimalRange)
      : undefined;
  const normalizedBaseOptimalRange =
    baseOptimalRange !== undefined
      ? normalizeRangeValue(baseOptimalRange)
      : undefined;
  let optimalRangePct: string | null = null;
  if (
    normalizedRolledOptimalRange !== undefined &&
    normalizedBaseOptimalRange !== undefined &&
    normalizedBaseOptimalRange > 0
  ) {
    const optimalRangePctValue =
      ((normalizedRolledOptimalRange - normalizedBaseOptimalRange) /
        normalizedBaseOptimalRange) *
      100;
    const formatted = formatPercentage(optimalRangePctValue);
    optimalRangePct = `${formatted}%`;
  }

  // Build replacement map
  const tier = analysis.tier.trim();
  const spaceAfterTier = tier.endsWith('+') ? '' : ' ';

  const replacements: Record<string, string> = {
    '{tier}': tier,
    '{m3Pct}': formatPercentage(baseM3Pct),
    '{effectiveM3Pct}': formatPercentage(effectiveM3Pct),
    '{m3PerSec}': analysis.m3PerSec.toFixed(2),
    '{effectiveM3PerSec}': analysis.effectiveM3PerSec.toFixed(2),
    '{effectiveMiningPct}': rolledEffectiveMiningPct.toFixed(2),
    '{effectiveMiningPctChange}': formatPercentage(
      calculatePercentage(baseEffectiveMiningPct, rolledEffectiveMiningPct),
    ),
    '{optimalRangePct}': optimalRangePct || '',
    '{optimalRange}': normalizedRolledOptimalRange?.toFixed(2) || '',
    '{minerType}': minerType,
    '{MiningAmount}': analysis.stats.MiningAmount?.toFixed(0) || '',
    '{ActivationTime}': analysis.stats.ActivationTime?.toFixed(1) || '',
    '{CriticalSuccessChance}': (analysis.stats.CriticalSuccessChance ?? 0).toFixed(3),
    '{CriticalSuccessBonusYield}': (analysis.stats.CriticalSuccessBonusYield ?? 0).toFixed(1),
    '{ResidueProbability}': (analysis.stats.ResidueProbability ?? 0).toFixed(3),
    '{ResidueVolumeMultiplier}': (analysis.stats.ResidueVolumeMultiplier ?? 0).toFixed(2),
    '{liveMiningAmount}': liveMiningAmount.toFixed(1),
    '{liveMiningAmountPct}': formatPercentage(
      calculatePercentage(liveBaseMiningAmount, liveMiningAmount),
    ),
    '{liveActivationTime}': liveActivationTime.toFixed(1),
    '{liveActivationTimePct}': formatPercentage(
      calculatePercentage(liveBaseActivationTime, liveActivationTime),
    ),
    '{liveCriticalSuccessChance}': liveCritChance.toFixed(3),
    '{liveCriticalSuccessChancePct}': formatPercentage(
      calculatePercentage(liveBaseCritChance, liveCritChance),
    ),
    '{liveCriticalSuccessBonusYield}': liveCritBonus.toFixed(1),
    '{liveCriticalSuccessBonusYieldPct}': formatPercentage(
      calculatePercentage(liveBaseCritBonus, liveCritBonus),
    ),
    '{liveOptimalRange}': liveOptimalRange.toFixed(2),
    '{liveOptimalRangePct}': formatPercentage(
      calculatePercentage(liveBaseOptimalRange, liveOptimalRange),
    ),
    '{liveM3PerSec}': liveM3PerSec.toFixed(2),
    '{liveM3PerSecPct}': formatPercentage(
      calculatePercentage(liveBaseM3PerSec, liveM3PerSec),
    ),
    '{liveEffectiveM3PerSec}': liveEffectiveM3PerSec.toFixed(2),
    '{liveEffectiveM3PerSecPct}': formatPercentage(
      calculatePercentage(liveBaseEffectiveM3PerSec, liveEffectiveM3PerSec),
    ),
    '{liveEffectiveMiningPct}': liveEffectiveMiningPct.toFixed(2),
    '{liveEffectiveMiningPctChange}': formatPercentage(
      calculatePercentage(liveBaseEffectiveMiningPct, liveEffectiveMiningPct),
    ),
  };

  // Replace all placeholders
  let result = format;
  for (const [placeholder, value] of Object.entries(replacements)) {
    result = result.replace(new RegExp(placeholder.replace(/[{}]/g, '\\$&'), 'g'), value);
  }

  // Truncate to 100 characters if needed
  if (result.length > 100) {
    result = result.substring(0, 100);
  }

  return result;
}
