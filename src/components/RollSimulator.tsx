import { useState, useEffect, useRef } from 'react';
import type {
  MinerType,
  MutaplasmidLevel,
  SimulationConfig,
  SimulationResult,
  SkillLevels,
} from '@/types';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { getDefaultSkillLevels } from '@/lib/config/minerConfig';
import SkillLevelsDialog from './SkillLevelsDialog';

const SAMPLE_SIZE_PRESETS = [
  { label: '1M', value: 1_000_000 },
  { label: '10M', value: 10_000_000 },
  { label: '100M', value: 100_000_000 },
  { label: '1B', value: 1_000_000_000 },
];

const TIER_COLORS: Record<string, string> = {
  S: 'text-green-500',
  A: 'text-blue-500',
  B: 'text-cyan-500',
  C: 'text-yellow-500',
  D: 'text-orange-500',
  E: 'text-red-400',
  F: 'text-red-600',
};

export default function RollSimulator() {
  const [minerType, setMinerType] = useState<MinerType>('ORE');
  const [mutaplasmidLevel, setMutaplasmidLevel] = useState<MutaplasmidLevel>('Decayed');
  const [baseItemCost, setBaseItemCost] = useState<string>('0');
  const [mutaplasmidCost, setMutaplasmidCost] = useState<string>('0');
  const [sampleSize, setSampleSize] = useState<string>('1000000');
  const [isRunning, setIsRunning] = useState(false);
  const [progress, setProgress] = useState({ completed: 0, total: 0 });
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [skillLevels, setSkillLevels] = useState<SkillLevels>(() => {
    const saved = localStorage.getItem('skillLevels');
    if (saved) {
      try {
        const parsed = JSON.parse(saved) as SkillLevels;
        return {
          ...getDefaultSkillLevels(),
          ...parsed,
        };
      } catch {
        return getDefaultSkillLevels();
      }
    }
    return getDefaultSkillLevels();
  });
  const [skillLevelsOpen, setSkillLevelsOpen] = useState(false);
  const workerRef = useRef<Worker | null>(null);
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    localStorage.setItem('skillLevels', JSON.stringify(skillLevels));
  }, [skillLevels]);

  useEffect(() => {
    // Initialize worker
    workerRef.current = new Worker(
      new URL('../workers/simulatorWorker.ts', import.meta.url),
      { type: 'module' },
    ) as Worker;

    workerRef.current.addEventListener('message', (event) => {
      const { type, ...data } = event.data;

      if (type === 'progress') {
        setProgress({ completed: data.completed, total: data.total });
      } else if (type === 'result') {
        setResult(data.result);
        setIsRunning(false);
        startTimeRef.current = null;
      } else if (type === 'error') {
        console.error('Simulation error:', data.error);
        setIsRunning(false);
        startTimeRef.current = null;
      }
    });

    return () => {
      if (workerRef.current) {
        workerRef.current.terminate();
      }
    };
  }, []);

  const handleStart = () => {
    if (isRunning) {
      return;
    }

    const config: SimulationConfig = {
      minerType,
      mutaplasmidLevel,
      baseItemCost: Number.parseFloat(baseItemCost) || 0,
      mutaplasmidCost: Number.parseFloat(mutaplasmidCost) || 0,
      sampleSize: Number.parseInt(sampleSize, 10) || 1_000_000,
      skillLevels,
    };

    if (config.sampleSize <= 0) {
      alert('Sample size must be greater than 0');
      return;
    }

    setIsRunning(true);
    setProgress({ completed: 0, total: config.sampleSize });
    setResult(null);
    startTimeRef.current = Date.now();

    workerRef.current?.postMessage({ type: 'start', config });
  };

  const handleStop = () => {
    workerRef.current?.postMessage({ type: 'stop' });
    setIsRunning(false);
    startTimeRef.current = null;
  };

  const formatNumber = (num: number): string => {
    if (num >= 1_000_000_000) {
      return (num / 1_000_000_000).toFixed(2) + 'B';
    }
    if (num >= 1_000_000) {
      return (num / 1_000_000).toFixed(2) + 'M';
    }
    if (num >= 1_000) {
      return (num / 1_000).toFixed(2) + 'K';
    }
    return num.toFixed(0);
  };

  const formatPercentage = (value: number, total: number): string => {
    if (total === 0) return '0.00%';
    return ((value / total) * 100).toFixed(4) + '%';
  };

  const getETA = (): string => {
    if (!startTimeRef.current || progress.completed === 0) return 'Calculating...';
    const elapsed = Date.now() - startTimeRef.current;
    const rate = progress.completed / elapsed; // rolls per ms
    const remaining = progress.total - progress.completed;
    const etaMs = remaining / rate;
    const etaSeconds = Math.ceil(etaMs / 1000);

    if (etaSeconds < 60) {
      return `${etaSeconds}s`;
    }
    const minutes = Math.floor(etaSeconds / 60);
    const seconds = etaSeconds % 60;
    return `${minutes}m ${seconds}s`;
  };

  const getTierColor = (tier: string) => {
    const tierLetter = tier.replace('+', '');
    return TIER_COLORS[tierLetter] || 'text-foreground';
  };

  // Get all possible tiers in order
  const allTiers = ['S', 'S+', 'A', 'A+', 'B', 'B+', 'C', 'C+', 'D', 'D+', 'E', 'E+', 'F'];

  return (
    <div className="flex flex-col h-screen overflow-auto">
      <Card className="m-4">
        <CardHeader>
          <CardTitle>Roll Simulator</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Miner Type Selection */}
          <div className="space-y-2">
            <Label>Miner Type:</Label>
            <RadioGroup
              value={minerType}
              onValueChange={(value) => setMinerType(value as MinerType)}
              className="flex gap-4"
            >
              <div className="flex items-center gap-2">
                <RadioGroupItem value="ORE" id="sim-ore" />
                <Label htmlFor="sim-ore">ORE</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Modulated" id="sim-modulated" />
                <Label htmlFor="sim-modulated">Modulated</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Ice" id="sim-ice" />
                <Label htmlFor="sim-ice">Ice</Label>
              </div>
            </RadioGroup>
          </div>

          {/* Mutaplasmid Level Selection */}
          <div className="space-y-2">
            <Label>Mutaplasmid Level:</Label>
            <RadioGroup
              value={mutaplasmidLevel}
              onValueChange={(value) => setMutaplasmidLevel(value as MutaplasmidLevel)}
              className="flex gap-4"
            >
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Decayed" id="sim-decayed" />
                <Label htmlFor="sim-decayed">Decayed</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Gravid" id="sim-gravid" />
                <Label htmlFor="sim-gravid">Gravid</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Unstable" id="sim-unstable" />
                <Label htmlFor="sim-unstable">Unstable</Label>
              </div>
            </RadioGroup>
          </div>

          {/* Cost Inputs */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="base-cost">Base Item Cost:</Label>
              <Input
                id="base-cost"
                type="number"
                value={baseItemCost}
                onChange={(e) => setBaseItemCost(e.target.value)}
                disabled={isRunning}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="mutaplasmid-cost">Mutaplasmid Cost:</Label>
              <Input
                id="mutaplasmid-cost"
                type="number"
                value={mutaplasmidCost}
                onChange={(e) => setMutaplasmidCost(e.target.value)}
                disabled={isRunning}
              />
            </div>
          </div>

          {/* Sample Size */}
          <div className="space-y-2">
            <Label htmlFor="sample-size">Sample Size:</Label>
            <div className="flex gap-2">
              <Input
                id="sample-size"
                type="number"
                value={sampleSize}
                onChange={(e) => setSampleSize(e.target.value)}
                disabled={isRunning}
                className="flex-1"
              />
              <div className="flex gap-1">
                {SAMPLE_SIZE_PRESETS.map((preset) => (
                  <Button
                    key={preset.label}
                    variant="outline"
                    size="sm"
                    onClick={() => setSampleSize(preset.value.toString())}
                    disabled={isRunning}
                  >
                    {preset.label}
                  </Button>
                ))}
              </div>
            </div>
          </div>

          {/* Controls */}
          <div className="flex gap-2">
            <Button onClick={isRunning ? handleStop : handleStart} disabled={false}>
              {isRunning ? 'Stop' : 'Start Simulation'}
            </Button>
            <Button
              variant="outline"
              onClick={() => setSkillLevelsOpen(true)}
              disabled={isRunning}
            >
              Skill Levels
            </Button>
          </div>

          {/* Progress */}
          {isRunning && (
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span>
                  Progress: {formatNumber(progress.completed)} / {formatNumber(progress.total)}
                </span>
                <span>ETA: {getETA()}</span>
              </div>
              <div className="w-full bg-secondary rounded-full h-2">
                <div
                  className="bg-primary h-2 rounded-full transition-all duration-300"
                  style={{
                    width: `${(progress.completed / progress.total) * 100}%`,
                  }}
                />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Results */}
      {result && (
        <div className="flex-1 overflow-auto px-4 pb-4 space-y-4">
          {/* Tier Distribution */}
          <Card>
            <CardHeader>
              <CardTitle>Tier Distribution</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-2">Tier</th>
                      <th className="text-right p-2">Count</th>
                      <th className="text-right p-2">Percentage</th>
                      <th className="text-right p-2">Total (Base + Plus)</th>
                      <th className="text-right p-2">Total %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {allTiers.map((tier) => {
                      const count = result.tierDistribution[tier] || 0;
                      const baseTier = tier.replace('+', '');
                      const plusTier = tier + '+';
                      const baseCount = result.tierDistribution[baseTier] || 0;
                      const plusCount = result.tierDistribution[plusTier] || 0;
                      const totalCount = baseCount + plusCount;
                      const isBaseTier = !tier.includes('+');
                      
                      return (
                        <tr key={tier} className="border-b">
                          <td className={`p-2 font-semibold ${getTierColor(tier)}`}>
                            {tier}
                          </td>
                          <td className="text-right p-2">{formatNumber(count)}</td>
                          <td className="text-right p-2">
                            {formatPercentage(count, result.totalRolls)}
                          </td>
                          {isBaseTier ? (
                            <>
                              <td className="text-right p-2 font-semibold">
                                {formatNumber(totalCount)}
                              </td>
                              <td className="text-right p-2 font-semibold">
                                {formatPercentage(totalCount, result.totalRolls)}
                              </td>
                            </>
                          ) : (
                            <>
                              <td className="text-right p-2 text-muted-foreground">-</td>
                              <td className="text-right p-2 text-muted-foreground">-</td>
                            </>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          {/* Statistics */}
          <Card>
            <CardHeader>
              <CardTitle>Statistics</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-muted-foreground">Average Effective M3/sec</Label>
                  <p className="text-2xl font-bold">
                    {result.statistics.averageEffectiveM3PerSec.toFixed(2)}
                  </p>
                </div>
                <div>
                  <Label className="text-muted-foreground">Median Effective M3/sec</Label>
                  <p className="text-2xl font-bold">
                    {result.statistics.medianEffectiveM3PerSec.toFixed(2)}
                  </p>
                </div>
                <div>
                  <Label className="text-muted-foreground">Min Effective M3/sec</Label>
                  <p className="text-2xl font-bold">
                    {result.statistics.minEffectiveM3PerSec.toFixed(2)}
                  </p>
                </div>
                <div>
                  <Label className="text-muted-foreground">Max Effective M3/sec</Label>
                  <p className="text-2xl font-bold">
                    {result.statistics.maxEffectiveM3PerSec.toFixed(2)}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Cost Analysis */}
          <Card>
            <CardHeader>
              <CardTitle>Cost Analysis</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <Label className="text-muted-foreground">Expected Value</Label>
                  <p className="text-2xl font-bold">
                    {result.costAnalysis.expectedValue.toFixed(2)}
                  </p>
                </div>
                <div>
                  <Label className="text-muted-foreground">Total Cost</Label>
                  <p className="text-2xl font-bold">
                    {(
                      Number.parseFloat(baseItemCost) + Number.parseFloat(mutaplasmidCost)
                    ).toFixed(2)}
                  </p>
                </div>
                <div>
                  <Label className="text-muted-foreground">ROI</Label>
                  <p
                    className={`text-2xl font-bold ${
                      result.costAnalysis.roi >= 0 ? 'text-green-500' : 'text-red-500'
                    }`}
                  >
                    {result.costAnalysis.roi.toFixed(2)}%
                  </p>
                </div>
              </div>
              <div>
                <Label className="text-muted-foreground mb-2 block">Cost per Tier</Label>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left p-2">Tier</th>
                        <th className="text-right p-2">Cost</th>
                      </tr>
                    </thead>
                    <tbody>
                      {allTiers.map((tier) => {
                        const cost = result.costAnalysis.costPerTier[tier] || 0;
                        if (cost === 0) return null;
                        return (
                          <tr key={tier} className="border-b">
                            <td className={`p-2 font-semibold ${getTierColor(tier)}`}>
                              {tier}
                            </td>
                            <td className="text-right p-2">{cost.toFixed(2)}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <SkillLevelsDialog
        open={skillLevelsOpen}
        onOpenChange={setSkillLevelsOpen}
        skillLevels={skillLevels}
        onSave={setSkillLevels}
      />
    </div>
  );
}
