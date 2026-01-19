/// <reference lib="webworker" />

import type {
  MinerType,
  MutaplasmidLevel,
  SkillLevels,
  SimulationConfig,
  SimulationResult,
  TierDistribution,
} from '@/types';
import { generateRoll } from '@/lib/simulator/rollSimulator';

declare const self: DedicatedWorkerGlobalScope;

// Fast seeded PRNG for performance (xorshift32)
class SeededRNG {
  private state: number;

  constructor(seed: number = Date.now()) {
    this.state = seed || 1;
  }

  next(): number {
    this.state ^= this.state << 13;
    this.state ^= this.state >>> 17;
    this.state ^= this.state << 5;
    return (this.state >>> 0) / 0xffffffff;
  }

  randomInRange(min: number, max: number): number {
    return this.next() * (max - min) + min;
  }
}

// Override Math.random for the worker to use seeded RNG
let rng: SeededRNG | null = null;

function initRNG(seed?: number) {
  rng = new SeededRNG(seed);
  // Override Math.random in this worker context
  Math.random = () => rng!.next();
}

// Initialize with a seed
initRNG();

interface WorkerMessage {
  type: 'start' | 'stop';
  config?: SimulationConfig;
}

interface ProgressMessage {
  type: 'progress';
  completed: number;
  total: number;
}

interface ResultMessage {
  type: 'result';
  result: SimulationResult;
}

interface ErrorMessage {
  type: 'error';
  error: string;
}

type WorkerResponse = ProgressMessage | ResultMessage | ErrorMessage;

let isRunning = false;
let shouldStop = false;

function calculateStatistics(values: number[]): {
  averageEffectiveM3PerSec: number;
  medianEffectiveM3PerSec: number;
  minEffectiveM3PerSec: number;
  maxEffectiveM3PerSec: number;
} {
  if (values.length === 0) {
    return { averageEffectiveM3PerSec: 0, medianEffectiveM3PerSec: 0, minEffectiveM3PerSec: 0, maxEffectiveM3PerSec: 0 };
  }

  const sorted = [...values].sort((a, b) => a - b);
  const sum = values.reduce((acc, val) => acc + val, 0);
  const average = sum / values.length;
  const median =
    sorted.length % 2 === 0
      ? (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2
      : sorted[Math.floor(sorted.length / 2)];
  const min = sorted[0];
  const max = sorted[sorted.length - 1];

  return { averageEffectiveM3PerSec: average, medianEffectiveM3PerSec: median, minEffectiveM3PerSec: min, maxEffectiveM3PerSec: max };
}

function calculateCostAnalysis(
  tierDistribution: TierDistribution,
  totalRolls: number,
  baseItemCost: number,
  mutaplasmidCost: number,
): {
  costPerTier: Record<string, number>;
  expectedValue: number;
  roi: number;
} {
  const costPerTier: Record<string, number> = {};
  const totalCost = baseItemCost + mutaplasmidCost;
  let totalValue = 0;

  // Calculate cost per tier based on actual odds
  // Cost to get one = totalCostPerRun * odds
  // Odds = 100 / percentage
  for (const [tier, count] of Object.entries(tierDistribution)) {
    if (count === 0 || totalRolls === 0) {
      // Infinite cost for impossible tiers
      costPerTier[tier] = Infinity;
      continue;
    }
    
    const percentage = (count / totalRolls) * 100;
    if (percentage === 0) {
      costPerTier[tier] = Infinity;
      continue;
    }
    
    const odds = 100 / percentage;
    const costToGetOne = totalCost * odds;
    costPerTier[tier] = costToGetOne;
    
    // For expected value, we still need some value estimation
    // Using a simple model where higher tiers are worth more
    const tierValueMultipliers: Record<string, number> = {
      S: 10.0,
      'S+': 12.0,
      A: 5.0,
      'A+': 6.0,
      B: 2.5,
      'B+': 3.0,
      C: 1.5,
      'C+': 1.8,
      D: 1.2,
      'D+': 1.4,
      E: 1.0,
      'E+': 1.1,
      F: 0.5,
    };
    const multiplier = tierValueMultipliers[tier] || 1.0;
    const tierValue = totalCost * multiplier;
    totalValue += tierValue * (count / totalRolls);
  }

  // Also calculate costs for combined tiers (S total, A total, etc.)
  const baseTiers = ['S', 'A', 'B', 'C', 'D', 'E', 'F'];
  for (const baseTier of baseTiers) {
    const plusTier = baseTier + '+';
    const baseCount = tierDistribution[baseTier] || 0;
    const plusCount = tierDistribution[plusTier] || 0;
    const totalCount = baseCount + plusCount;
    
    if (totalCount > 0 && totalRolls > 0) {
      const percentage = (totalCount / totalRolls) * 100;
      const odds = 100 / percentage;
      const costToGetOne = totalCost * odds;
      costPerTier[`${baseTier} Total`] = costToGetOne;
    }
  }

  const expectedValue = totalValue;
  const roi = totalCost > 0 ? ((expectedValue - totalCost) / totalCost) * 100 : 0;

  return { costPerTier, expectedValue, roi };
}

async function runSimulation(config: SimulationConfig) {
  isRunning = true;
  shouldStop = false;

  const {
    minerType,
    mutaplasmidLevel,
    skillLevels,
    sampleSize,
    baseItemCost,
    mutaplasmidCost,
  } = config;

  const tierDistribution: TierDistribution = {};
  const effectiveM3PerSecValues: number[] = [];

  const BATCH_SIZE = 100000; // Process in batches for progress updates
  let completed = 0;

  try {
    // Process in batches to allow progress updates and cancellation
    while (completed < sampleSize && !shouldStop) {
      const batchSize = Math.min(BATCH_SIZE, sampleSize - completed);

      for (let i = 0; i < batchSize; i++) {
        if (shouldStop) break;

        const roll = generateRoll(minerType, mutaplasmidLevel, skillLevels);

        // #region agent log
        if (completed < 5 || completed % 10000 === 0) {
          fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'simulatorWorker.ts:201',message:'Simulator roll generated',data:{tier:roll.tier,liveEffectiveM3PerSec:roll.liveEffectiveM3PerSec,minerType,mutaplasmidLevel,completed},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
        }
        // #endregion

        // Track tier distribution
        tierDistribution[roll.tier] = (tierDistribution[roll.tier] || 0) + 1;

        // Track effective m3/sec values for statistics
        effectiveM3PerSecValues.push(roll.liveEffectiveM3PerSec);

        completed++;
      }

      // Send progress update
      const progressMessage: ProgressMessage = {
        type: 'progress',
        completed,
        total: sampleSize,
      };
      self.postMessage(progressMessage);

      // Yield to allow other operations
      await new Promise((resolve) => setTimeout(resolve, 0));
    }

    if (shouldStop) {
      return;
    }

    // Calculate statistics
    const statistics = calculateStatistics(effectiveM3PerSecValues);

    // Calculate cost analysis
    const costAnalysis = calculateCostAnalysis(
      tierDistribution,
      completed,
      baseItemCost,
      mutaplasmidCost,
    );

    // Send final result
    const result: SimulationResult = {
      tierDistribution,
      totalRolls: completed,
      statistics,
      costAnalysis,
    };

    const resultMessage: ResultMessage = {
      type: 'result',
      result,
    };

    self.postMessage(resultMessage);
  } catch (error) {
    const errorMessage: ErrorMessage = {
      type: 'error',
      error: error instanceof Error ? error.message : 'Unknown error',
    };
    self.postMessage(errorMessage);
  } finally {
    isRunning = false;
  }
}

// Handle messages from main thread
self.addEventListener('message', (event: MessageEvent<WorkerMessage>) => {
  const { type, config } = event.data;

  if (type === 'start') {
    if (isRunning) {
      const errorMessage: ErrorMessage = {
        type: 'error',
        error: 'Simulation already running',
      };
      self.postMessage(errorMessage);
      return;
    }

    if (!config) {
      const errorMessage: ErrorMessage = {
        type: 'error',
        error: 'No configuration provided',
      };
      self.postMessage(errorMessage);
      return;
    }

    // Reinitialize RNG with new seed for this simulation
    initRNG(Date.now() + Math.random() * 1000000);
    runSimulation(config);
  } else if (type === 'stop') {
    shouldStop = true;
  }
});
