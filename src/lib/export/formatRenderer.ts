import type { AnalysisResult, MinerType } from '@/types';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';

interface FormatContext {
  analysis: AnalysisResult;
  baseStats: Record<string, number>;
  minerType: MinerType;
  useEffectiveM3: boolean;
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
    useEffectiveM3,
  } = context;

  // Calculate base values
  const baseMiningAmount = baseStats.MiningAmount ?? 0;
  const baseActivationTime = baseStats.ActivationTime ?? 0;
  const baseCritChance = baseStats.CriticalSuccessChance ?? 0;
  const baseCritBonus = baseStats.CriticalSuccessBonusYield ?? 0;
  const baseResidueProb = baseStats.ResidueProbability ?? 0;
  const baseResidueMult = baseStats.ResidueVolumeMultiplier ?? 0;

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

  // Calculate percentages
  const rolledValue = useEffectiveM3 ? analysis.effectiveM3PerSec : analysis.m3PerSec;
  const baseValue = useEffectiveM3 ? baseEffectiveM3PerSec : baseBaseM3PerSec;
  const percentageChange = ((rolledValue - baseValue) / baseValue) * 100;

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

  // Calculate optimal range percentage if available
  const rolledOptimalRange = analysis.stats.OptimalRange;
  const baseOptimalRange = baseStats.OptimalRange;
  let optimalRangePct: string | null = null;
  if (
    rolledOptimalRange !== undefined &&
    baseOptimalRange !== undefined &&
    baseOptimalRange > 0
  ) {
    const optimalRangePctValue = ((rolledOptimalRange - baseOptimalRange) / baseOptimalRange) * 100;
    const formatted = formatPercentage(optimalRangePctValue);
    optimalRangePct = `${formatted}%`;
  }

  // Build replacement map
  const tier = analysis.tier.trim();
  const spaceAfterTier = tier.endsWith('+') ? '' : ' ';

  const replacements: Record<string, string> = {
    '{tier}': tier,
    '{m3Pct}': formatPercentage(((analysis.m3PerSec - baseBaseM3PerSec) / baseBaseM3PerSec) * 100),
    '{effectiveM3Pct}': formatPercentage(percentageChange),
    '{m3PerSec}': analysis.m3PerSec.toFixed(2),
    '{effectiveM3PerSec}': analysis.effectiveM3PerSec.toFixed(2),
    '{optimalRangePct}': optimalRangePct || '',
    '{optimalRange}': rolledOptimalRange?.toFixed(2) || '',
    '{minerType}': minerType,
    '{MiningAmount}': analysis.stats.MiningAmount?.toFixed(0) || '',
    '{ActivationTime}': analysis.stats.ActivationTime?.toFixed(1) || '',
    '{CriticalSuccessChance}': (analysis.stats.CriticalSuccessChance ?? 0).toFixed(3),
    '{CriticalSuccessBonusYield}': (analysis.stats.CriticalSuccessBonusYield ?? 0).toFixed(1),
    '{ResidueProbability}': (analysis.stats.ResidueProbability ?? 0).toFixed(3),
    '{ResidueVolumeMultiplier}': (analysis.stats.ResidueVolumeMultiplier ?? 0).toFixed(2),
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
