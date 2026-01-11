import { readTextFile, writeTextFile, exists, BaseDirectory } from '@tauri-apps/plugin-fs';
import type { TierModifiers } from '@/types';

const CONFIG_DIR = 'config';
const ROLL_COST_FILE = 'roll_cost.txt';
const TIER_MODIFIERS_FILE = 'tier_modifiers.txt';
const OPTIMAL_RANGE_MODIFIER_FILE = 'optimal_range_modifier.txt';

async function ensureConfigDir(): Promise<void> {
  // Directory will be created automatically by writeTextFile if it doesn't exist
  // No explicit directory creation needed
}

function getConfigPath(filename: string): string {
  return `${CONFIG_DIR}/${filename}`;
}

export async function getRollCost(): Promise<number> {
  await ensureConfigDir();
  const filePath = getConfigPath(ROLL_COST_FILE);
  
  try {
    const existsFile = await exists(filePath, { baseDir: BaseDirectory.AppData });
    if (!existsFile) {
      return 0;
    }
    
    const content = await readTextFile(filePath, { baseDir: BaseDirectory.AppData });
    const trimmed = content.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      return 0;
    }
    
    const value = Number.parseFloat(trimmed);
    return Number.isNaN(value) || value < 0 ? 0 : value;
  } catch (error) {
    console.error('Error reading roll cost:', error);
    return 0;
  }
}

export async function saveRollCost(cost: number): Promise<void> {
  await ensureConfigDir();
  const filePath = getConfigPath(ROLL_COST_FILE);
  
  try {
    await writeTextFile(filePath, cost.toString(), { baseDir: BaseDirectory.AppData });
  } catch (error) {
    console.error('Error saving roll cost:', error);
    throw error;
  }
}

const DEFAULT_TIER_MODIFIERS: TierModifiers = {
  S: 2,
  A: 1.8,
  B: 1.6,
  C: 1.4,
  D: 1.2,
  E: 1,
  F: 0.8,
};

export async function loadTierModifiers(): Promise<TierModifiers> {
  await ensureConfigDir();
  const filePath = getConfigPath(TIER_MODIFIERS_FILE);
  
  const modifiers: TierModifiers = { ...DEFAULT_TIER_MODIFIERS };
  
  try {
    const existsFile = await exists(filePath, { baseDir: BaseDirectory.AppData });
    if (!existsFile) {
      await saveTierModifiers(modifiers);
      return modifiers;
    }
    
    const content = await readTextFile(filePath, { baseDir: BaseDirectory.AppData });
    const lines = content.split('\n');
    
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) {
        continue;
      }
      
      const [tier, value] = trimmed.split('=');
      if (tier && value) {
        const tierKey = tier.trim().toUpperCase() as keyof TierModifiers;
        const numValue = Number.parseFloat(value.trim());
        if (!Number.isNaN(numValue) && tierKey in DEFAULT_TIER_MODIFIERS) {
          modifiers[tierKey] = numValue;
        }
      }
    }
  } catch (error) {
    console.error('Error loading tier modifiers:', error);
  }
  
  return modifiers;
}

export async function saveTierModifiers(modifiers: TierModifiers): Promise<void> {
  await ensureConfigDir();
  const filePath = getConfigPath(TIER_MODIFIERS_FILE);
  
  try {
    const lines: string[] = [];
    const tiers: Array<keyof TierModifiers> = ['S', 'A', 'B', 'C', 'D', 'E', 'F'];
    for (const tier of tiers) {
      lines.push(`${tier}=${modifiers[tier]}`);
    }
    await writeTextFile(filePath, lines.join('\n'), { baseDir: BaseDirectory.AppData });
  } catch (error) {
    console.error('Error saving tier modifiers:', error);
    throw error;
  }
}

export async function loadOptimalRangeModifier(): Promise<number> {
  await ensureConfigDir();
  const filePath = getConfigPath(OPTIMAL_RANGE_MODIFIER_FILE);
  const DEFAULT_MODIFIER = 1;
  
  try {
    const existsFile = await exists(filePath, { baseDir: BaseDirectory.AppData });
    if (!existsFile) {
      await saveOptimalRangeModifier(DEFAULT_MODIFIER);
      return DEFAULT_MODIFIER;
    }
    
    const content = await readTextFile(filePath, { baseDir: BaseDirectory.AppData });
    const trimmed = content.trim().split('\n')[0]?.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      return DEFAULT_MODIFIER;
    }
    
    const value = Number.parseFloat(trimmed);
    return Number.isNaN(value) || value < 0 ? DEFAULT_MODIFIER : value;
  } catch (error) {
    console.error('Error loading optimal range modifier:', error);
    return DEFAULT_MODIFIER;
  }
}

export async function saveOptimalRangeModifier(modifier: number): Promise<void> {
  await ensureConfigDir();
  const filePath = getConfigPath(OPTIMAL_RANGE_MODIFIER_FILE);
  
  try {
    await writeTextFile(filePath, modifier.toString(), { baseDir: BaseDirectory.AppData });
  } catch (error) {
    console.error('Error saving optimal range modifier:', error);
    throw error;
  }
}

export function getModifierForTier(
  tier: string,
  modifiers: TierModifiers,
): number {
  const tierKey = tier.toUpperCase() as keyof TierModifiers;
  return modifiers[tierKey] ?? modifiers.F;
}

export function calculateSellPrice(
  rollCost: number,
  tier: string,
  baseM3Pct: number,
  tierModifiers: TierModifiers,
  optimalRangeModifier: number,
): number {
  if (rollCost <= 0) {
    return 0;
  }
  
  // Remove "+" suffix if present for tier modifier lookup
  const tierForModifier = tier.endsWith('+') ? tier.slice(0, -1) : tier;
  const tierModifier = getModifierForTier(tierForModifier, tierModifiers);
  
  // Check if tier has "+" suffix (optimal range increased) and apply modifier
  const optimalModifier = tier.endsWith('+') ? optimalRangeModifier : 1;
  
  // Formula: cost * tier_modifier * (100% + baseM3Pct%) * optimal_range_modifier (if applicable)
  // baseM3Pct is already a percentage, so convert to multiplier: 1 + (baseM3Pct / 100)
  const m3Multiplier = 1 + baseM3Pct / 100;
  return rollCost * tierModifier * m3Multiplier * optimalModifier;
}
