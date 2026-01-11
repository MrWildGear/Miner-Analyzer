import { useState, useEffect } from 'react';
import { readText, writeText } from '@tauri-apps/plugin-clipboard-manager';
import { Moon, Sun } from 'lucide-react';
import type { MinerType, AnalysisResult } from '@/types';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { parseItemStats } from '@/lib/parser/itemStatsParser';
import { analyzeRoll } from '@/lib/analyzer/rollAnalyzer';
import { getBaseStats } from '@/lib/config/minerConfig';
import {
  getRollCost,
  saveRollCost,
  loadTierModifiers,
  saveTierModifiers,
  loadOptimalRangeModifier,
  saveOptimalRangeModifier,
  calculateSellPrice,
} from '@/lib/config/configManager';
import AnalysisDisplay from './AnalysisDisplay';
import SettingsDialog from './SettingsDialog';
import { APP_VERSION } from '@/version';

export default function MainAnalyzer() {
  const [minerType, setMinerType] = useState<MinerType>('ORE');
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [baseStats, setBaseStats] = useState<Record<string, number>>({});
  const [status, setStatus] = useState('Monitoring clipboard...');
  const [sellPrice, setSellPrice] = useState<number>(0);
  const [rollCost, setRollCost] = useState<number>(0);
  const [tierModifiers, setTierModifiers] = useState({
    S: 2,
    A: 1.8,
    B: 1.6,
    C: 1.4,
    D: 1.2,
    E: 1,
    F: 0.8,
  });
  const [optimalRangeModifier, setOptimalRangeModifier] = useState<number>(1);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [lastClipboardHash, setLastClipboardHash] = useState<string>('');
  const [isDarkMode, setIsDarkMode] = useState<boolean>(() => {
    // Initialize from localStorage or default to true (dark mode)
    const saved = localStorage.getItem('theme');
    if (saved === 'dark') return true;
    if (saved === 'light') return false;
    // Default to dark mode if no preference saved
    return true;
  });

  useEffect(() => {
    // Apply theme to document
    const html = document.documentElement;
    if (isDarkMode) {
      html.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      html.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [isDarkMode]);

  useEffect(() => {
    // Load config on mount
    const loadConfig = async () => {
      const cost = await getRollCost();
      const modifiers = await loadTierModifiers();
      const optimalRange = await loadOptimalRangeModifier();
      setRollCost(cost);
      setTierModifiers(modifiers);
      setOptimalRangeModifier(optimalRange);
    };
    loadConfig();
  }, []);

  useEffect(() => {
    // Clipboard monitoring
    const interval = setInterval(async () => {
      try {
        const clipboardText = await readText();
        if (!clipboardText) return;

        const hash = `${clipboardText.length}-${clipboardText.slice(0, 50)}`;
        if (hash === lastClipboardHash) return;

        const parsedStats = parseItemStats(clipboardText);
        if (Object.keys(parsedStats).length === 0) {
          // Don't update hash or clear analysis if no valid stats found
          // This keeps the current analysis visible until a new valid module is scanned
          return;
        }

        // Only update hash when we've confirmed valid stats were found
        setLastClipboardHash(hash);

        const baseStats = getBaseStats(minerType);
        setBaseStats(baseStats);
        try {
          const result = analyzeRoll(parsedStats, baseStats, minerType);
          setAnalysis(result);

          // Calculate sell price
          const baseM3 = result.m3PerSec;
          const baseBaseM3 = baseStats.MiningAmount / baseStats.ActivationTime;
          const baseM3Pct = ((baseM3 - baseBaseM3) / baseBaseM3) * 100;

          const price = calculateSellPrice(
            rollCost,
            result.tier,
            baseM3Pct,
            tierModifiers,
            optimalRangeModifier,
          );
          setSellPrice(price);

          // Copy tier to clipboard
          let tierText = `${result.tier}: (+${baseM3Pct.toFixed(1)}%)`;
          
          // Add optimal range percentage if optimal range is increased
          // Use the same logic as rollAnalyzer.ts to check if optimal range is increased
          const rolledOptimalRange = parsedStats.OptimalRange;
          const baseOptimalRange = baseStats.OptimalRange;
          if (
            rolledOptimalRange !== undefined &&
            baseOptimalRange !== undefined &&
            rolledOptimalRange > baseOptimalRange
          ) {
            const optimalRangePct = ((rolledOptimalRange - baseOptimalRange) / baseOptimalRange) * 100;
            tierText += ` {+${optimalRangePct.toFixed(1)}%}`;
          }
          
          tierText += ` [${minerType}]`;
          await writeText(tierText);

          setStatus('Analysis complete');
        } catch (error) {
          console.error('Analysis error:', error);
          setStatus(`Error: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
      } catch (error) {
        // Clipboard access errors are normal, ignore them
        if (error) {
          // Expected error, intentionally ignored
        }
      }
    }, 300);

    return () => clearInterval(interval);
  }, [minerType, lastClipboardHash, rollCost, tierModifiers, optimalRangeModifier]);

  const handleMinerTypeChange = (value: MinerType) => {
    setMinerType(value);
    setAnalysis(null);
    setBaseStats({});
    setStatus('Miner type changed. Waiting for clipboard update...');
    setSellPrice(0);
  };

  const handleCopySellPrice = async () => {
    if (sellPrice > 0) {
      await writeText(Math.floor(sellPrice).toString());
      setStatus('Sell price copied to clipboard');
    }
  };

  const handleSettingsSave = async (
    newRollCost: number,
    newTierModifiers: typeof tierModifiers,
    newOptimalRangeModifier: number,
  ) => {
    await saveRollCost(newRollCost);
    await saveTierModifiers(newTierModifiers);
    await saveOptimalRangeModifier(newOptimalRangeModifier);
    setRollCost(newRollCost);
    setTierModifiers(newTierModifiers);
    setOptimalRangeModifier(newOptimalRangeModifier);
    setSettingsOpen(false);
  };

  return (
    <div className="flex flex-col h-screen">
      <Card className="m-4">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>EVE Online Strip Miner Roll Analyzer</CardTitle>
            <span className="text-sm text-muted-foreground">v{APP_VERSION}</span>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <Label>Miner Type:</Label>
            <RadioGroup
              value={minerType}
              onValueChange={(value) =>
                handleMinerTypeChange(value as MinerType)
              }
              className="flex gap-4"
            >
              <div className="flex items-center gap-2">
                <RadioGroupItem value="ORE" id="ore" />
                <Label htmlFor="ore">ORE</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Modulated" id="modulated" />
                <Label htmlFor="modulated">Modulated</Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="Ice" id="ice" />
                <Label htmlFor="ice">Ice</Label>
              </div>
            </RadioGroup>
            <div className="ml-auto flex gap-2">
              <Button
                variant="outline"
                size="icon"
                onClick={() => setIsDarkMode(!isDarkMode)}
                aria-label="Toggle theme"
              >
                {isDarkMode ? (
                  <Sun className="h-4 w-4" />
                ) : (
                  <Moon className="h-4 w-4" />
                )}
              </Button>
              <Button
                variant="outline"
                onClick={() => setSettingsOpen(true)}
              >
                Settings
              </Button>
            </div>
          </div>

          {sellPrice > 0 && (
            <div className="flex items-center gap-2">
              <Label>
                Sell Price: {new Intl.NumberFormat().format(sellPrice)} ISK
              </Label>
              <Button size="sm" onClick={handleCopySellPrice}>
                Copy
              </Button>
            </div>
          )}

          <div className="text-sm text-muted-foreground">{status}</div>
        </CardContent>
      </Card>

      {analysis && (
        <div className="flex-1 overflow-auto px-4 pb-4">
          <AnalysisDisplay
            analysis={analysis}
            baseStats={baseStats}
            minerType={minerType}
          />
        </div>
      )}

      <SettingsDialog
        open={settingsOpen}
        onOpenChange={setSettingsOpen}
        rollCost={rollCost}
        tierModifiers={tierModifiers}
        optimalRangeModifier={optimalRangeModifier}
        onSave={handleSettingsSave}
      />
    </div>
  );
}
