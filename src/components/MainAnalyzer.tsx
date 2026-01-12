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
import * as MiningCalculator from '@/lib/calculator/miningCalculator';
import AnalysisDisplay from './AnalysisDisplay';
import TierRangesDialog from './TierRangesDialog';
import ExportFormatDialog from './ExportFormatDialog';
import { renderExportFormat } from '@/lib/export/formatRenderer';
import { APP_VERSION } from '@/version';

export default function MainAnalyzer() {
  const [minerType, setMinerType] = useState<MinerType>('ORE');
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [baseStats, setBaseStats] = useState<Record<string, number>>({});
  const [status, setStatus] = useState('Monitoring clipboard...');
  const [tierRangesOpen, setTierRangesOpen] = useState(false);
  const [exportFormatOpen, setExportFormatOpen] = useState(false);
  const [lastClipboardHash, setLastClipboardHash] = useState<string>('');
  const [lastExportText, setLastExportText] = useState<string>('');
  const [exportFormat, setExportFormat] = useState<string>(() => {
    // Default format: {tier} : {m3Pct}% {optimalRangePct} {minerType}
    const saved = localStorage.getItem('exportFormat');
    return saved || '{tier} : {m3Pct}% {optimalRangePct} {minerType}';
  });
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
    // Save export format
    localStorage.setItem('exportFormat', exportFormat);
  }, [exportFormat]);

  useEffect(() => {
    // Clipboard monitoring
    const interval = setInterval(async () => {
      try {
        const clipboardText = await readText();
        if (!clipboardText) return;

        const hash = `${clipboardText.length}-${clipboardText.slice(0, 50)}`;
        // Allow re-processing if clipboard text is different from our last export
        // This handles the case where user copies the same item again
        const isDifferentFromExport = clipboardText !== lastExportText;
        if (hash === lastClipboardHash && !isDifferentFromExport) return;

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

          // Calculate base values for both metrics
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

          // Generate export text using custom format
          const tierText = renderExportFormat(exportFormat, {
            analysis: result,
            baseStats,
            minerType,
          });
          await writeText(tierText);
          setLastExportText(tierText);

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
  }, [minerType, lastClipboardHash, lastExportText, exportFormat]);

  // Recalculate export when format, toggle, or analysis changes
  useEffect(() => {
    if (analysis && Object.keys(baseStats).length > 0) {
      const generateExportText = async () => {
        const tierText = renderExportFormat(exportFormat, {
          analysis,
          baseStats,
          minerType,
        });
        await writeText(tierText);
        setLastExportText(tierText);
      };

      generateExportText().catch((error) => {
        console.error('Error regenerating export:', error);
      });
    }
  }, [analysis, baseStats, minerType, exportFormat]);

  const handleMinerTypeChange = (value: MinerType) => {
    setMinerType(value);
    setAnalysis(null);
    setBaseStats({});
    setStatus('Miner type changed. Waiting for clipboard update...');
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
                onClick={() => setTierRangesOpen(true)}
              >
                Tier Ranges
              </Button>
              <Button
                variant="outline"
                onClick={() => setExportFormatOpen(true)}
              >
                Export Format
              </Button>
            </div>
          </div>

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

      <TierRangesDialog
        open={tierRangesOpen}
        onOpenChange={setTierRangesOpen}
      />

      <ExportFormatDialog
        open={exportFormatOpen}
        onOpenChange={setExportFormatOpen}
        format={exportFormat}
        onSave={setExportFormat}
      />
    </div>
  );
}
