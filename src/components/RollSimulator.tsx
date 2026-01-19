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
import { Switch } from '@/components/ui/switch';
import { getDefaultSkillLevels } from '@/lib/config/minerConfig';
import SkillLevelsDialog from './SkillLevelsDialog';
import TierRangesDialog from './TierRangesDialog';

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
  const [comparisonMode, setComparisonMode] = useState(false);
  const [minerType, setMinerType] = useState<MinerType>('ORE');
  const [mutaplasmidLevel, setMutaplasmidLevel] = useState<MutaplasmidLevel>('Decayed');
  const [mutaplasmidLevel2, setMutaplasmidLevel2] = useState<MutaplasmidLevel>('Gravid');
  const [baseItemCost, setBaseItemCost] = useState<string>('0');
  const [mutaplasmidCost, setMutaplasmidCost] = useState<string>('0');
  const [mutaplasmidCost2, setMutaplasmidCost2] = useState<string>('0');
  const [sampleSize, setSampleSize] = useState<string>('1000000');
  const [isRunning, setIsRunning] = useState(false);
  const [isRunning2, setIsRunning2] = useState(false);
  const [progress, setProgress] = useState({ completed: 0, total: 0 });
  const [progress2, setProgress2] = useState({ completed: 0, total: 0 });
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [result2, setResult2] = useState<SimulationResult | null>(null);
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
  const [tierRangesOpen, setTierRangesOpen] = useState(false);
  const workerRef = useRef<Worker | null>(null);
  const workerRef2 = useRef<Worker | null>(null);
  const startTimeRef = useRef<number | null>(null);
  const startTimeRef2 = useRef<number | null>(null);
  // Refs to avoid stale closures in event listeners
  const comparisonModeRef = useRef(comparisonMode);
  const isRunning2Ref = useRef(isRunning2);
  const minerTypeRef = useRef(minerType);
  const mutaplasmidLevel2Ref = useRef(mutaplasmidLevel2);
  const baseItemCostRef = useRef(baseItemCost);
  const mutaplasmidCost2Ref = useRef(mutaplasmidCost2);
  const sampleSizeRef = useRef(sampleSize);
  const skillLevelsRef = useRef(skillLevels);
  
  // Keep refs in sync with state
  useEffect(() => {
    comparisonModeRef.current = comparisonMode;
    isRunning2Ref.current = isRunning2;
    minerTypeRef.current = minerType;
    mutaplasmidLevel2Ref.current = mutaplasmidLevel2;
    baseItemCostRef.current = baseItemCost;
    mutaplasmidCost2Ref.current = mutaplasmidCost2;
    sampleSizeRef.current = sampleSize;
    skillLevelsRef.current = skillLevels;
  }, [comparisonMode, isRunning2, minerType, mutaplasmidLevel2, baseItemCost, mutaplasmidCost2, sampleSize, skillLevels]);

  useEffect(() => {
    localStorage.setItem('skillLevels', JSON.stringify(skillLevels));
  }, [skillLevels]);

  useEffect(() => {
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:75',message:'useEffect running - initializing workers',data:{comparisonMode:comparisonModeRef.current,minerType:minerTypeRef.current,mutaplasmidLevel2:mutaplasmidLevel2Ref.current},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
    // #endregion
    // Initialize first worker
    workerRef.current = new Worker(
      new URL('../workers/simulatorWorker.ts', import.meta.url),
      { type: 'module' },
    ) as Worker;

    workerRef.current.addEventListener('message', (event) => {
      const { type, ...data } = event.data;

      if (type === 'progress') {
        setProgress({ completed: data.completed, total: data.total });
      } else if (type === 'result') {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:87',message:'First simulation result received',data:{comparisonMode:comparisonModeRef.current,isRunning2:isRunning2Ref.current,hasWorker2:!!workerRef2.current},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
        // #endregion
        setResult(data.result);
        setIsRunning(false);
        startTimeRef.current = null;
        
        // If in comparison mode and first simulation is done, start second
        // Use refs to get current values (avoid stale closures)
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:93',message:'Checking if should start second simulation',data:{comparisonMode:comparisonModeRef.current,isRunning2:isRunning2Ref.current,hasWorker2:!!workerRef2.current,willStart:comparisonModeRef.current&&!isRunning2Ref.current&&!!workerRef2.current},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'B'})}).catch(()=>{});
        // #endregion
        if (comparisonModeRef.current && !isRunning2Ref.current && workerRef2.current) {
          const config2: SimulationConfig = {
            minerType: minerTypeRef.current,
            mutaplasmidLevel: mutaplasmidLevel2Ref.current,
            baseItemCost: Number.parseFloat(baseItemCostRef.current) || 0,
            mutaplasmidCost: Number.parseFloat(mutaplasmidCost2Ref.current) || 0,
            sampleSize: Number.parseInt(sampleSizeRef.current, 10) || 1_000_000,
            skillLevels: skillLevelsRef.current,
          };
          // #region agent log
          fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:102',message:'Starting second simulation',data:{config2},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'B'})}).catch(()=>{});
          // #endregion
          setIsRunning2(true);
          setProgress2({ completed: 0, total: config2.sampleSize });
          setResult2(null);
          startTimeRef2.current = Date.now();
          workerRef2.current.postMessage({ type: 'start', config: config2 });
          // #region agent log
          fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:106',message:'Second simulation message sent',data:{sent:true},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'B'})}).catch(()=>{});
          // #endregion
        }
      } else if (type === 'error') {
        console.error('Simulation error:', data.error);
        setIsRunning(false);
        startTimeRef.current = null;
      }
    });

    // Initialize second worker for comparison mode
    workerRef2.current = new Worker(
      new URL('../workers/simulatorWorker.ts', import.meta.url),
      { type: 'module' },
    ) as Worker;

    workerRef2.current.addEventListener('message', (event) => {
      const { type, ...data } = event.data;
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:121',message:'Worker2 message received',data:{type,hasData:!!data},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'C'})}).catch(()=>{});
      // #endregion

      if (type === 'progress') {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:125',message:'Worker2 progress update',data:{completed:data.completed,total:data.total},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'C'})}).catch(()=>{});
        // #endregion
        setProgress2({ completed: data.completed, total: data.total });
      } else if (type === 'result') {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:127',message:'Worker2 result received',data:{hasResult:!!data.result},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'C'})}).catch(()=>{});
        // #endregion
        setResult2(data.result);
        setIsRunning2(false);
        startTimeRef2.current = null;
      } else if (type === 'error') {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'RollSimulator.tsx:130',message:'Worker2 error',data:{error:data.error},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
        // #endregion
        console.error('Simulation 2 error:', data.error);
        setIsRunning2(false);
        startTimeRef2.current = null;
      }
    });

    return () => {
      if (workerRef.current) {
        workerRef.current.terminate();
      }
      if (workerRef2.current) {
        workerRef2.current.terminate();
      }
    };
  }, []);

  const handleStart = () => {
    if (isRunning || isRunning2) {
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

    // If comparison mode, clear second result but don't start it yet (will start after first completes)
    if (comparisonMode) {
      setResult2(null);
      setProgress2({ completed: 0, total: config.sampleSize });
    }

    workerRef.current?.postMessage({ type: 'start', config });
  };

  const handleStop = () => {
    workerRef.current?.postMessage({ type: 'stop' });
    workerRef2.current?.postMessage({ type: 'stop' });
    setIsRunning(false);
    setIsRunning2(false);
    startTimeRef.current = null;
    startTimeRef2.current = null;
  };

  const formatNumber = (num: number): string => {
    if (!isFinite(num)) {
      return '∞';
    }
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
    return ((value / total) * 100).toFixed(2) + '%';
  };

  const formatOdds = (value: number, total: number): string => {
    if (total === 0 || value === 0) return '∞:1';
    const percentage = (value / total) * 100;
    if (percentage === 0) return '∞:1';
    if (percentage >= 100) return '1:1';
    const odds = 100 / percentage;
    
    // Round to nearest whole number
    const roundedOdds = Math.round(odds);
    
    // Format odds with B/M/K notation for large numbers
    if (roundedOdds >= 1_000_000_000) {
      return Math.round(roundedOdds / 1_000_000_000) + 'B:1';
    } else if (roundedOdds >= 1_000_000) {
      return Math.round(roundedOdds / 1_000_000) + 'M:1';
    } else if (roundedOdds >= 1_000) {
      return Math.round(roundedOdds / 1_000) + 'K:1';
    } else {
      return roundedOdds + ':1';
    }
  };

  const getETA = (startTime: number | null, currentProgress: { completed: number; total: number }): string => {
    if (!startTime || currentProgress.completed === 0) return 'Calculating...';
    const elapsed = Date.now() - startTime;
    const rate = currentProgress.completed / elapsed; // rolls per ms
    const remaining = currentProgress.total - currentProgress.completed;
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

  // Helper function to render a result section
  const renderResultSection = (
    result: SimulationResult,
    mutaplasmidCostValue: string,
    baseItemCostValue: string,
  ) => {
    return (
      <div className="space-y-4">
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
                    <th className="text-right p-2">Odds</th>
                    <th className="text-right p-2">Total %</th>
                    <th className="text-right p-2">Total Odds</th>
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
                        <td className="text-right p-2">
                          {formatOdds(count, result.totalRolls)}
                        </td>
                        {isBaseTier ? (
                          <>
                            <td className="text-right p-2 font-semibold">
                              {formatPercentage(totalCount, result.totalRolls)}
                            </td>
                            <td className="text-right p-2 font-semibold">
                              {formatOdds(totalCount, result.totalRolls)}
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
                  {formatNumber(result.costAnalysis.expectedValue)}
                </p>
              </div>
              <div>
                <Label className="text-muted-foreground">Total Cost</Label>
                <p className="text-2xl font-bold">
                  {formatNumber(
                    Number.parseFloat(baseItemCostValue) + Number.parseFloat(mutaplasmidCostValue)
                  )}
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
              <Label className="text-muted-foreground mb-2 block">Cost to Get One (Based on Odds)</Label>
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
                      const cost = result.costAnalysis.costPerTier[tier];
                      if (cost === undefined || cost === 0) return null;
                      if (cost === Infinity) {
                        return (
                          <tr key={tier} className="border-b">
                            <td className={`p-2 font-semibold ${getTierColor(tier)}`}>
                              {tier}
                            </td>
                            <td className="text-right p-2 text-muted-foreground">∞</td>
                          </tr>
                        );
                      }
                      return (
                        <tr key={tier} className="border-b">
                          <td className={`p-2 font-semibold ${getTierColor(tier)}`}>
                            {tier}
                          </td>
                          <td className="text-right p-2">{formatNumber(cost)}</td>
                        </tr>
                      );
                    })}
                    {/* Show combined totals */}
                    {['S', 'A', 'B', 'C', 'D', 'E', 'F'].map((baseTier) => {
                      const totalTierKey = `${baseTier} Total`;
                      const cost = result.costAnalysis.costPerTier[totalTierKey];
                      if (cost === undefined || cost === 0) return null;
                      if (cost === Infinity) {
                        return (
                          <tr key={totalTierKey} className="border-b">
                            <td className={`p-2 font-semibold ${getTierColor(baseTier)}`}>
                              {baseTier} Total
                            </td>
                            <td className="text-right p-2 text-muted-foreground">∞</td>
                          </tr>
                        );
                      }
                      return (
                        <tr key={totalTierKey} className="border-b">
                          <td className={`p-2 font-semibold ${getTierColor(baseTier)}`}>
                            {baseTier} Total
                          </td>
                          <td className="text-right p-2">{formatNumber(cost)}</td>
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
    );
  };

  return (
    <div className="flex flex-col h-screen overflow-auto">
      <Card className="m-4">
        <CardHeader>
          <CardTitle>Roll Simulator</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Comparison Mode Toggle */}
          <div className="flex items-center justify-between p-4 border rounded-lg bg-muted/50">
            <div className="space-y-0.5">
              <Label htmlFor="comparison-mode" className="text-base font-semibold">
                Comparison Mode
              </Label>
              <p className="text-sm text-muted-foreground">
                Compare two mutaplasmid levels side-by-side
              </p>
            </div>
            <Switch
              id="comparison-mode"
              checked={comparisonMode}
              onCheckedChange={setComparisonMode}
              disabled={isRunning || isRunning2}
            />
          </div>

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
          {comparisonMode ? (
            <div className="grid grid-cols-2 gap-4">
              {/* Mutaplasmid 1 */}
              <div className="space-y-2">
                <Label className="font-semibold">Mutaplasmid 1:</Label>
                <RadioGroup
                  value={mutaplasmidLevel}
                  onValueChange={(value) => setMutaplasmidLevel(value as MutaplasmidLevel)}
                  className="flex gap-4"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="Decayed" id="sim-decayed-1" />
                    <Label htmlFor="sim-decayed-1">Decayed</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="Gravid" id="sim-gravid-1" />
                    <Label htmlFor="sim-gravid-1">Gravid</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="Unstable" id="sim-unstable-1" />
                    <Label htmlFor="sim-unstable-1">Unstable</Label>
                  </div>
                </RadioGroup>
              </div>
              {/* Mutaplasmid 2 */}
              <div className="space-y-2">
                <Label className="font-semibold">Mutaplasmid 2:</Label>
                <RadioGroup
                  value={mutaplasmidLevel2}
                  onValueChange={(value) => setMutaplasmidLevel2(value as MutaplasmidLevel)}
                  className="flex gap-4"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="Decayed" id="sim-decayed-2" />
                    <Label htmlFor="sim-decayed-2">Decayed</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="Gravid" id="sim-gravid-2" />
                    <Label htmlFor="sim-gravid-2">Gravid</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="Unstable" id="sim-unstable-2" />
                    <Label htmlFor="sim-unstable-2">Unstable</Label>
                  </div>
                </RadioGroup>
              </div>
            </div>
          ) : (
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
          )}

          {/* Cost Inputs */}
          {comparisonMode ? (
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label htmlFor="base-cost">Base Item Cost:</Label>
                <Input
                  id="base-cost"
                  type="number"
                  value={baseItemCost}
                  onChange={(e) => setBaseItemCost(e.target.value)}
                  disabled={isRunning || isRunning2}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="mutaplasmid-cost-1">Mutaplasmid 1 Cost:</Label>
                <Input
                  id="mutaplasmid-cost-1"
                  type="number"
                  value={mutaplasmidCost}
                  onChange={(e) => setMutaplasmidCost(e.target.value)}
                  disabled={isRunning || isRunning2}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="mutaplasmid-cost-2">Mutaplasmid 2 Cost:</Label>
                <Input
                  id="mutaplasmid-cost-2"
                  type="number"
                  value={mutaplasmidCost2}
                  onChange={(e) => setMutaplasmidCost2(e.target.value)}
                  disabled={isRunning || isRunning2}
                />
              </div>
            </div>
          ) : (
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
          )}

          {/* Sample Size */}
          <div className="space-y-2">
            <Label htmlFor="sample-size">Sample Size:</Label>
            <div className="flex gap-2">
              <Input
                id="sample-size"
                type="number"
                value={sampleSize}
                onChange={(e) => setSampleSize(e.target.value)}
                disabled={isRunning || isRunning2}
                className="flex-1"
              />
              <div className="flex gap-1">
                {SAMPLE_SIZE_PRESETS.map((preset) => (
                  <Button
                    key={preset.label}
                    variant="outline"
                    size="sm"
                    onClick={() => setSampleSize(preset.value.toString())}
                    disabled={isRunning || isRunning2}
                  >
                    {preset.label}
                  </Button>
                ))}
              </div>
            </div>
          </div>

          {/* Controls */}
          <div className="flex gap-2">
            <Button onClick={(isRunning || isRunning2) ? handleStop : handleStart} disabled={false}>
              {(isRunning || isRunning2) ? 'Stop' : 'Start Simulation'}
            </Button>
            <Button
              variant="outline"
              onClick={() => setSkillLevelsOpen(true)}
              disabled={isRunning || isRunning2}
            >
              Skill Levels
            </Button>
            <Button
              variant="outline"
              onClick={() => setTierRangesOpen(true)}
              disabled={isRunning || isRunning2}
            >
              Tier Ranges
            </Button>
          </div>

          {/* Progress */}
          {(isRunning || isRunning2) && (
            <div className="space-y-4">
              {comparisonMode ? (
                <>
                  {/* Simulation 1 Progress */}
                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="font-semibold">Mutaplasmid 1 ({mutaplasmidLevel}):</span>
                      <span>
                        {formatNumber(progress.completed)} / {formatNumber(progress.total)} - ETA: {getETA(startTimeRef.current, progress)}
                      </span>
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
                  {/* Simulation 2 Progress */}
                  {isRunning2 && (
                    <div className="space-y-2">
                      <div className="flex justify-between text-sm">
                        <span className="font-semibold">Mutaplasmid 2 ({mutaplasmidLevel2}):</span>
                        <span>
                          {formatNumber(progress2.completed)} / {formatNumber(progress2.total)} - ETA: {getETA(startTimeRef2.current, progress2)}
                        </span>
                      </div>
                      <div className="w-full bg-secondary rounded-full h-2">
                        <div
                          className="bg-primary h-2 rounded-full transition-all duration-300"
                          style={{
                            width: `${(progress2.completed / progress2.total) * 100}%`,
                          }}
                        />
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span>
                      Progress: {formatNumber(progress.completed)} / {formatNumber(progress.total)}
                    </span>
                    <span>ETA: {getETA(startTimeRef.current, progress)}</span>
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
            </div>
          )}
        </CardContent>
      </Card>

      {/* Results */}
      {(result || (comparisonMode && result2)) && (
        <div className="flex-1 overflow-auto px-4 pb-4">
          {comparisonMode && result && result2 ? (
            <>
              {/* Headers side by side */}
              <div className="grid grid-cols-2 gap-4 mb-4 sticky top-0 bg-background z-10 pb-4 border-b">
                <div>
                  <h2 className="text-lg font-semibold">Mutaplasmid 1: {mutaplasmidLevel}</h2>
                  <p className="text-sm text-muted-foreground">
                    Cost: {Number.parseFloat(baseItemCost).toFixed(2)} + {Number.parseFloat(mutaplasmidCost).toFixed(2)} = {(Number.parseFloat(baseItemCost) + Number.parseFloat(mutaplasmidCost)).toFixed(2)}
                  </p>
                </div>
                <div>
                  <h2 className="text-lg font-semibold">Mutaplasmid 2: {mutaplasmidLevel2}</h2>
                  <p className="text-sm text-muted-foreground">
                    Cost: {Number.parseFloat(baseItemCost).toFixed(2)} + {Number.parseFloat(mutaplasmidCost2).toFixed(2)} = {(Number.parseFloat(baseItemCost) + Number.parseFloat(mutaplasmidCost2)).toFixed(2)}
                  </p>
                </div>
              </div>
              {/* Split View - Both Results */}
              <div className="grid grid-cols-2 gap-4">
                {/* First Result */}
                <div className="space-y-4 overflow-auto border-r pr-4">
                  {renderResultSection(result, mutaplasmidCost, baseItemCost)}
                </div>
                {/* Second Result */}
                <div className="space-y-4 overflow-auto pl-4">
                  {renderResultSection(result2, mutaplasmidCost2, baseItemCost)}
                </div>
              </div>
            </>
          ) : result ? (
            /* Single Result View */
            <div className="space-y-4">
              {renderResultSection(result, mutaplasmidCost, baseItemCost)}
            </div>
          ) : comparisonMode && result2 ? (
            /* Show second result if first not done yet */
            <div className="space-y-4">
              <div className="sticky top-0 bg-background z-10 pb-2 border-b mb-4">
                <h2 className="text-lg font-semibold">Mutaplasmid 2: {mutaplasmidLevel2}</h2>
                <p className="text-sm text-muted-foreground">
                  Cost: {Number.parseFloat(baseItemCost).toFixed(2)} + {Number.parseFloat(mutaplasmidCost2).toFixed(2)} = {(Number.parseFloat(baseItemCost) + Number.parseFloat(mutaplasmidCost2)).toFixed(2)}
                </p>
              </div>
              {renderResultSection(result2, mutaplasmidCost2, baseItemCost)}
            </div>
          ) : null}
        </div>
      )}

      <SkillLevelsDialog
        open={skillLevelsOpen}
        onOpenChange={setSkillLevelsOpen}
        skillLevels={skillLevels}
        onSave={setSkillLevels}
      />
      <TierRangesDialog
        open={tierRangesOpen}
        onOpenChange={setTierRangesOpen}
      />
    </div>
  );
}
