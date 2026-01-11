import type { AnalysisResult, MinerType } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';

interface AnalysisDisplayProps {
  analysis: AnalysisResult;
  baseStats: Record<string, number>;
  minerType: MinerType;
}

const TIER_COLORS: Record<string, string> = {
  S: 'text-green-500',
  A: 'text-blue-500',
  B: 'text-cyan-500',
  C: 'text-yellow-500',
  D: 'text-orange-500',
  E: 'text-red-400',
  F: 'text-red-600',
};

// Key stats to display in Roll Analysis
const ROLL_ANALYSIS_STATS = [
  'MiningAmount',
  'ActivationTime',
  'CriticalSuccessChance',
  'CriticalSuccessBonusYield',
  'OptimalRange',
];

// Stats where lower is better (like ActivationTime)
const LOWER_IS_BETTER_STATS = ['ActivationTime'];

// Format stat name for display
function formatStatName(statName: string): string {
  const nameMap: Record<string, string> = {
    MiningAmount: 'Mining Amount',
    ActivationTime: 'Activation Time',
    CriticalSuccessChance: 'Crit Chance',
    CriticalSuccessBonusYield: 'Crit Bonus',
    OptimalRange: 'Optimal Range',
    ResidueProbability: 'Residue Probability',
    ResidueVolumeMultiplier: 'Residue Volume Multiplier',
  };
  return (
    nameMap[statName] ||
    statName
      .replace(/([A-Z])/g, ' $1')
      .trim()
      .split(' ')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ')
  );
}

// Format stat value based on type
function formatStatValue(value: number, statName: string): string {
  if (statName === 'CriticalSuccessChance') {
    return (value * 100).toFixed(2) + '%';
  }
  if (statName === 'CriticalSuccessBonusYield') {
    return (value * 100).toFixed(0) + '%';
  }
  if (statName === 'ActivationTime') {
    return value.toFixed(1) + ' s';
  }
  if (statName === 'MiningAmount') {
    return value.toFixed(1) + ' m³';
  }
  if (statName === 'OptimalRange') {
    return value.toFixed(2) + ' km';
  }
  if (statName === 'ResidueProbability') {
    return (value * 100).toFixed(2) + '%';
  }
  return value.toFixed(2);
}

// Calculate percentage difference
function calculatePercentage(base: number, rolled: number): number {
  if (base === 0) return 0;
  return ((rolled - base) / base) * 100;
}

// Determine if a change is beneficial
function isBeneficial(
  statName: string,
  percentage: number,
): boolean {
  if (LOWER_IS_BETTER_STATS.includes(statName)) {
    return percentage < 0; // Lower is better
  }
  return percentage > 0; // Higher is better
}

export default function AnalysisDisplay({
  analysis,
  baseStats,
  minerType,
}: Readonly<AnalysisDisplayProps>) {
  const getTierColor = (tier: string) => {
    const tierLetter = tier.replace('+', '');
    return TIER_COLORS[tierLetter] || 'text-foreground';
  };

  const rolledStats = analysis.stats;

  // Calculate base performance metrics
  const baseMiningAmount = baseStats.MiningAmount ?? 0;
  const baseActivationTime = baseStats.ActivationTime ?? 0;
  const baseCritChance = baseStats.CriticalSuccessChance ?? 0;
  const baseCritBonus = baseStats.CriticalSuccessBonusYield ?? 0;
  const baseResidueProb = baseStats.ResidueProbability ?? 0;
  const baseResidueMult = baseStats.ResidueVolumeMultiplier ?? 0;

  const baseM3PerSec =
    MiningCalculator.calculateBaseM3PerSec(
      baseMiningAmount,
      baseActivationTime,
    );
  const baseEffectiveM3PerSec =
    MiningCalculator.calculateEffectiveM3PerSec(
      baseMiningAmount,
      baseActivationTime,
      baseCritChance,
      baseCritBonus,
      baseResidueProb,
      baseResidueMult,
    );
  const baseRealWorldM3PerSec =
    MiningCalculator.calculateRealWorldBaseM3PerSec(
      baseMiningAmount,
      baseActivationTime,
    );
  const baseRealWorldEffectiveM3PerSec =
    MiningCalculator.calculateRealWorldEffectiveM3PerSec(
      baseMiningAmount,
      baseActivationTime,
      baseCritChance,
      baseCritBonus,
      baseResidueProb,
      baseResidueMult,
    );

  // Filter stats to display (only those in ROLL_ANALYSIS_STATS that exist)
  const statsToDisplay = ROLL_ANALYSIS_STATS.filter(
    (stat) => baseStats[stat] !== undefined,
  );

  // Performance metrics percentage calculations
  const baseM3Pct = calculatePercentage(baseM3PerSec, analysis.m3PerSec);
  const effectiveM3Pct = calculatePercentage(
    baseEffectiveM3PerSec,
    analysis.effectiveM3PerSec,
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <span>Analysis Results</span>
          <span className={`font-bold ${getTierColor(analysis.tier)}`}>
            Tier {analysis.tier}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 font-mono text-sm">
        <div>
          <h3 className="font-semibold mb-2">Roll Analysis:</h3>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-2">Metric</th>
                  <th className="text-right p-2">Base</th>
                  <th className="text-right p-2">Rolled</th>
                  <th className="text-right p-2">% Change</th>
                </tr>
              </thead>
              <tbody>
                {statsToDisplay.map((statName) => {
                  const base = baseStats[statName] ?? 0;
                  const rolled = rolledStats[statName] ?? base;
                  const percentage = calculatePercentage(base, rolled);
                  const beneficial = isBeneficial(statName, percentage);

                  return (
                    <tr key={statName} className="border-b">
                      <td className="p-2">{formatStatName(statName)}</td>
                      <td className="text-right p-2">
                        {formatStatValue(base, statName)}
                      </td>
                      <td
                        className={`text-right p-2 ${
                          beneficial ? 'text-green-500' : 'text-red-500'
                        }`}
                      >
                        {formatStatValue(rolled, statName)}
                      </td>
                      <td
                        className={`text-right p-2 ${
                          beneficial ? 'text-green-500' : 'text-red-500'
                        }`}
                      >
                        {percentage > 0 ? '+' : ''}
                        {percentage.toFixed(2)}%
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        <div>
          <h3 className="font-semibold mb-2">Performance Metrics:</h3>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-2">Metric</th>
                  <th className="text-right p-2">Base</th>
                  <th className="text-right p-2">Rolled</th>
                  <th className="text-right p-2">% Change</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b">
                  <td className="p-2">Base M3/sec</td>
                  <td className="text-right p-2">
                    {baseM3PerSec.toFixed(2)} (
                    {baseRealWorldM3PerSec.toFixed(1)})
                  </td>
                  <td
                    className={`text-right p-2 ${
                      baseM3Pct > 0 ? 'text-green-500' : 'text-red-500'
                    }`}
                  >
                    {analysis.m3PerSec.toFixed(2)} (
                    {analysis.realWorldM3PerSec.toFixed(1)})
                  </td>
                  <td
                    className={`text-right p-2 ${
                      baseM3Pct > 0 ? 'text-green-500' : 'text-red-500'
                    }`}
                  >
                    {baseM3Pct > 0 ? '+' : ''}
                    {baseM3Pct.toFixed(1)}%
                  </td>
                </tr>
                <tr className="border-b">
                  <td className="p-2">Effective M3/sec</td>
                  <td className="text-right p-2">
                    {baseEffectiveM3PerSec.toFixed(2)} (
                    {baseRealWorldEffectiveM3PerSec.toFixed(1)})
                  </td>
                  <td
                    className={`text-right p-2 ${
                      effectiveM3Pct > 0 ? 'text-green-500' : 'text-red-500'
                    }`}
                  >
                    {analysis.effectiveM3PerSec.toFixed(2)} (
                    {analysis.realWorldEffectiveM3PerSec.toFixed(1)})
                  </td>
                  <td
                    className={`text-right p-2 ${
                      effectiveM3Pct > 0 ? 'text-green-500' : 'text-red-500'
                    }`}
                  >
                    {effectiveM3Pct > 0 ? '+' : ''}
                    {effectiveM3Pct.toFixed(1)}%
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
