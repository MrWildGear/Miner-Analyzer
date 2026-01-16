import type { AnalysisResult, MinerType, SkillLevels } from '@/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';
import { createLiveModifiers } from '@/lib/config/minerConfig';

interface AnalysisDisplayProps {
  analysis: AnalysisResult;
  baseStats: Record<string, number>;
  minerType: MinerType;
  skillLevels: SkillLevels;
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
const MODULATED_ONLY_STATS = ['ResidueProbability', 'ResidueVolumeMultiplier'];

// Stats where lower is better (like ActivationTime)
const LOWER_IS_BETTER_STATS = [
  'ActivationTime',
  'ResidueProbability',
  'ResidueVolumeMultiplier',
];

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
    const normalized = value > 1000 ? value / 1000 : value;
    return normalized.toFixed(2) + ' km';
  }
  if (statName === 'ResidueProbability') {
    return (value * 100).toFixed(2) + '%';
  }
  return value.toFixed(2);
}

function normalizeRangeValue(value: number): number {
  return value > 1000 ? value / 1000 : value;
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

function combineMultipliers(multipliers: number[]): number {
  if (multipliers.length === 0) {
    return 1;
  }
  return multipliers.reduce((acc, value) => acc * value, 1);
}

function getLiveMultiplierForStat(
  statName: string,
  liveModifiers: ReturnType<typeof createLiveModifiers>,
): number {
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
}

function applyLiveModifiersToStats(
  stats: Record<string, number>,
  liveModifiers: ReturnType<typeof createLiveModifiers>,
): Record<string, number> {
  const liveStats: Record<string, number> = { ...stats };
  for (const [statName, value] of Object.entries(stats)) {
    if (typeof value !== 'number') {
      continue;
    }
    const multiplier = getLiveMultiplierForStat(statName, liveModifiers);
    liveStats[statName] = value * multiplier;
  }
  return liveStats;
}

export default function AnalysisDisplay({
  analysis,
  baseStats,
  minerType,
  skillLevels,
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
  const baseEffectiveMiningPct =
    MiningCalculator.calculateEffectiveMiningPercent(
      baseCritChance,
      baseCritBonus,
      baseResidueProb,
      baseResidueMult,
    );
  const rolledEffectiveMiningPct =
    MiningCalculator.calculateEffectiveMiningPercent(
      rolledStats.CriticalSuccessChance ?? 0,
      rolledStats.CriticalSuccessBonusYield ?? 0,
      rolledStats.ResidueProbability ?? 0,
      rolledStats.ResidueVolumeMultiplier ?? 0,
    );

  // Filter stats to display (only those in ROLL_ANALYSIS_STATS that exist)
  const statsToDisplay = [
    ...ROLL_ANALYSIS_STATS,
    ...(minerType === 'Modulated' ? MODULATED_ONLY_STATS : []),
  ].filter((stat) => baseStats[stat] !== undefined);

  // Performance metrics percentage calculations
  const baseM3Pct = calculatePercentage(baseM3PerSec, analysis.m3PerSec);
  const effectiveM3Pct = calculatePercentage(
    baseEffectiveM3PerSec,
    analysis.effectiveM3PerSec,
  );
  const effectiveMiningPct = calculatePercentage(
    baseEffectiveMiningPct,
    rolledEffectiveMiningPct,
  );

  const liveModifiers = createLiveModifiers(minerType, skillLevels);
  const liveBaseStats = applyLiveModifiersToStats(baseStats, liveModifiers);
  const liveRolledStats = applyLiveModifiersToStats(rolledStats, liveModifiers);

  const liveMiningAmount = liveRolledStats.MiningAmount ?? 0;
  const liveActivationTime = liveRolledStats.ActivationTime ?? 0;
  const liveCritChance = liveRolledStats.CriticalSuccessChance ?? 0;
  const liveCritBonus = liveRolledStats.CriticalSuccessBonusYield ?? 0;
  const liveResidueProb = liveRolledStats.ResidueProbability ?? 0;
  const liveResidueMult = liveRolledStats.ResidueVolumeMultiplier ?? 0;

  const liveBaseMiningAmount = liveBaseStats.MiningAmount ?? 0;
  const liveBaseActivationTime = liveBaseStats.ActivationTime ?? 0;
  const liveBaseCritChance = liveBaseStats.CriticalSuccessChance ?? 0;
  const liveBaseCritBonus = liveBaseStats.CriticalSuccessBonusYield ?? 0;
  const liveBaseResidueProb = liveBaseStats.ResidueProbability ?? 0;
  const liveBaseResidueMult = liveBaseStats.ResidueVolumeMultiplier ?? 0;

  const liveBaseM3PerSec = MiningCalculator.calculateBaseM3PerSec(
    liveBaseMiningAmount,
    liveBaseActivationTime,
  );
  const liveM3PerSec = MiningCalculator.calculateBaseM3PerSec(
    liveMiningAmount,
    liveActivationTime,
  );
  const liveBaseEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
    liveBaseMiningAmount,
    liveBaseActivationTime,
    liveBaseCritChance,
    liveBaseCritBonus,
    liveBaseResidueProb,
    liveBaseResidueMult,
  );
  const liveEffectiveM3PerSec = MiningCalculator.calculateEffectiveM3PerSec(
    liveMiningAmount,
    liveActivationTime,
    liveCritChance,
    liveCritBonus,
    liveResidueProb,
    liveResidueMult,
  );
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

  const liveRows = [
    ...statsToDisplay.map((statName) => ({
      key: statName,
      label: formatStatName(statName),
      base: liveBaseStats[statName] ?? 0,
      rolled: liveRolledStats[statName] ?? liveBaseStats[statName] ?? 0,
      format: (value: number) => formatStatValue(value, statName),
      tooltip: null as string | null,
      statName,
    })),
    {
      key: 'LiveBaseM3PerSec',
      label: 'Base M3/sec',
      base: liveBaseM3PerSec,
      rolled: liveM3PerSec,
      format: (value: number) => value.toFixed(2),
      tooltip: 'MiningAmount / ActivationTime',
      statName: null as string | null,
    },
    {
      key: 'LiveEffectiveM3PerSec',
      label: 'Effective M3/sec (no residue)',
      base: liveBaseEffectiveM3PerSec,
      rolled: liveEffectiveM3PerSec,
      format: (value: number) => value.toFixed(2),
      tooltip:
        '(MiningAmount + (MiningAmount × CritBonus × CritChance)) / ActivationTime',
      statName: null as string | null,
    },
    {
      key: 'LiveEffectiveMiningM3PerSec',
      label: 'Effective Mining',
      base: liveBaseEffectiveMiningPct,
      rolled: liveEffectiveMiningPct,
      format: (value: number) => `${value.toFixed(2)}%`,
      tooltip:
        '((1 + (CritBonus × CritChance)) / (1 + (ResidueProbability × ResidueVolumeMultiplier))) × 100',
      statName: null as string | null,
    },
  ];

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
                  const percentage =
                    statName === 'OptimalRange'
                      ? calculatePercentage(
                          normalizeRangeValue(base),
                          normalizeRangeValue(rolled),
                        )
                      : calculatePercentage(base, rolled);
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
          <TooltipProvider>
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
                    <td className="p-2">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span className="cursor-help underline decoration-dotted">
                            Base M3/sec
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p className="font-mono text-xs">
                            MiningAmount / ActivationTime
                          </p>
                        </TooltipContent>
                      </Tooltip>
                    </td>
                    <td className="text-right p-2">
                      {baseM3PerSec.toFixed(2)}
                    </td>
                    <td
                      className={`text-right p-2 ${
                        baseM3Pct > 0 ? 'text-green-500' : 'text-red-500'
                      }`}
                    >
                      {analysis.m3PerSec.toFixed(2)}
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
                    <td className="p-2">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span className="cursor-help underline decoration-dotted">
                            Effective M3/sec (no residue)
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p className="font-mono text-xs">
                            (MiningAmount + (MiningAmount × CritBonus ×
                            CritChance)) / ActivationTime
                          </p>
                        </TooltipContent>
                      </Tooltip>
                    </td>
                    <td className="text-right p-2">
                      {baseEffectiveM3PerSec.toFixed(2)}
                    </td>
                    <td
                      className={`text-right p-2 ${
                        effectiveM3Pct > 0 ? 'text-green-500' : 'text-red-500'
                      }`}
                    >
                      {analysis.effectiveM3PerSec.toFixed(2)}
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
                  <tr className="border-b">
                    <td className="p-2">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span className="cursor-help underline decoration-dotted">
                            Effective Mining
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>
                          <p className="font-mono text-xs">
                            ((1 + (CritBonus × CritChance)) /
                            (1 + (ResidueProbability × ResidueVolumeMultiplier))) × 100
                          </p>
                        </TooltipContent>
                      </Tooltip>
                    </td>
                    <td className="text-right p-2">
                      {baseEffectiveMiningPct.toFixed(2)}%
                    </td>
                    <td
                      className={`text-right p-2 ${
                        effectiveMiningPct > 0
                          ? 'text-green-500'
                          : 'text-red-500'
                      }`}
                    >
                      {rolledEffectiveMiningPct.toFixed(2)}%
                    </td>
                    <td
                      className={`text-right p-2 ${
                        effectiveMiningPct > 0
                          ? 'text-green-500'
                          : 'text-red-500'
                      }`}
                    >
                      {effectiveMiningPct > 0 ? '+' : ''}
                      {effectiveMiningPct.toFixed(1)}
                      %
                    </td>
                  </tr>
                  {minerType === 'Modulated' && (
                    <tr className="border-b">
                      <td className="p-2">Residue Probability</td>
                      <td className="text-right p-2">
                        {formatStatValue(baseResidueProb, 'ResidueProbability')}
                      </td>
                      <td
                        className={`text-right p-2 ${
                          calculatePercentage(
                            baseResidueProb,
                            rolledStats.ResidueProbability ?? baseResidueProb,
                          ) < 0
                            ? 'text-green-500'
                            : 'text-red-500'
                        }`}
                      >
                        {formatStatValue(
                          rolledStats.ResidueProbability ?? baseResidueProb,
                          'ResidueProbability',
                        )}
                      </td>
                      <td
                        className={`text-right p-2 ${
                          calculatePercentage(
                            baseResidueProb,
                            rolledStats.ResidueProbability ?? baseResidueProb,
                          ) < 0
                            ? 'text-green-500'
                            : 'text-red-500'
                        }`}
                      >
                        {calculatePercentage(
                          baseResidueProb,
                          rolledStats.ResidueProbability ?? baseResidueProb,
                        ) > 0
                          ? '+'
                          : ''}
                        {calculatePercentage(
                          baseResidueProb,
                          rolledStats.ResidueProbability ?? baseResidueProb,
                        ).toFixed(2)}
                        %
                      </td>
                    </tr>
                  )}
                  {minerType === 'Modulated' && (
                    <tr className="border-b">
                      <td className="p-2">Residue Volume Multiplier</td>
                      <td className="text-right p-2">
                        {formatStatValue(
                          baseResidueMult,
                          'ResidueVolumeMultiplier',
                        )}
                      </td>
                      <td
                        className={`text-right p-2 ${
                          calculatePercentage(
                            baseResidueMult,
                            rolledStats.ResidueVolumeMultiplier ?? baseResidueMult,
                          ) < 0
                            ? 'text-green-500'
                            : 'text-red-500'
                        }`}
                      >
                        {formatStatValue(
                          rolledStats.ResidueVolumeMultiplier ?? baseResidueMult,
                          'ResidueVolumeMultiplier',
                        )}
                      </td>
                      <td
                        className={`text-right p-2 ${
                          calculatePercentage(
                            baseResidueMult,
                            rolledStats.ResidueVolumeMultiplier ?? baseResidueMult,
                          ) < 0
                            ? 'text-green-500'
                            : 'text-red-500'
                        }`}
                      >
                        {calculatePercentage(
                          baseResidueMult,
                          rolledStats.ResidueVolumeMultiplier ?? baseResidueMult,
                        ) > 0
                          ? '+'
                          : ''}
                        {calculatePercentage(
                          baseResidueMult,
                          rolledStats.ResidueVolumeMultiplier ?? baseResidueMult,
                        ).toFixed(2)}
                        %
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </TooltipProvider>
        </div>

        <div>
          <h3 className="font-semibold mb-2">Live Performance Metrics:</h3>
          <TooltipProvider>
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
                  {liveRows.map((row) => {
                    const percentage =
                      row.statName === 'OptimalRange'
                        ? calculatePercentage(
                            normalizeRangeValue(row.base),
                            normalizeRangeValue(row.rolled),
                          )
                        : calculatePercentage(row.base, row.rolled);
                    const beneficial = row.statName
                      ? isBeneficial(row.statName, percentage)
                      : percentage > 0;

                    return (
                      <tr key={row.key} className="border-b">
                        <td className="p-2">
                          {row.tooltip ? (
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <span className="cursor-help underline decoration-dotted">
                                  {row.label}
                                </span>
                              </TooltipTrigger>
                              <TooltipContent>
                                <p className="font-mono text-xs">
                                  {row.tooltip}
                                </p>
                              </TooltipContent>
                            </Tooltip>
                          ) : (
                            row.label
                          )}
                        </td>
                        <td className="text-right p-2">
                          {row.format(row.base)}
                        </td>
                        <td
                          className={`text-right p-2 ${
                            beneficial ? 'text-green-500' : 'text-red-500'
                          }`}
                        >
                          {row.format(row.rolled)}
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
          </TooltipProvider>
        </div>
      </CardContent>
    </Card>
  );
}
