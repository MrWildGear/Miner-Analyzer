import { useState, useEffect } from 'react';
import { readText, writeText } from '@tauri-apps/plugin-clipboard-manager';
import { Moon, Sun, Download, CheckCircle2, AlertCircle } from 'lucide-react';
import type { MinerType, AnalysisResult, SkillLevels } from '@/types';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { parseItemStats } from '@/lib/parser/itemStatsParser';
import { analyzeRoll } from '@/lib/analyzer/rollAnalyzer';
import { getBaseStats, getDefaultSkillLevels } from '@/lib/config/minerConfig';
import * as MiningCalculator from '@/lib/calculator/miningCalculator';
import AnalysisDisplay from './AnalysisDisplay';
import TierRangesDialog from './TierRangesDialog';
import ExportFormatDialog from './ExportFormatDialog';
import UpdateAvailableDialog from './UpdateAvailableDialog';
import SkillLevelsDialog from './SkillLevelsDialog';
import { renderExportFormat } from '@/lib/export/formatRenderer';
import { APP_VERSION } from '@/version';
import { useVersionCheck } from '@/hooks/useVersionCheck';
import { openUrl } from '@/lib/utils/openUrl';

export default function MainAnalyzer() {
  const [minerType, setMinerType] = useState<MinerType>('ORE');
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [baseStats, setBaseStats] = useState<Record<string, number>>({});
  const [status, setStatus] = useState('Monitoring clipboard...');
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
  const [tierRangesOpen, setTierRangesOpen] = useState(false);
  const [exportFormatOpen, setExportFormatOpen] = useState(false);
  const [skillLevelsOpen, setSkillLevelsOpen] = useState(false);
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

  const { result: versionCheck, isChecking: isCheckingVersion, checkForUpdates } = useVersionCheck(true);
  const [updateDialogOpen, setUpdateDialogOpen] = useState(false);

  // Show update dialog when an update is available
  useEffect(() => {
    if (versionCheck && !versionCheck.isUpToDate && versionCheck.latestVersion) {
      // Only show dialog if we haven't shown it for this version yet
      const lastShownVersion = localStorage.getItem('lastShownUpdateVersion');
      const lastShownTime = localStorage.getItem('lastShownUpdateTime');
      const now = Date.now();
      const oneDay = 24 * 60 * 60 * 1000; // 24 hours
      
      // Show dialog if:
      // 1. We haven't shown it for this version yet, OR
      // 2. It's been more than 24 hours since we last showed it
      const shouldShow = 
        lastShownVersion !== versionCheck.latestVersion ||
        !lastShownTime ||
        (now - Number.parseInt(lastShownTime, 10)) > oneDay;
      
      if (shouldShow) {
        console.log('Showing update available dialog for version:', versionCheck.latestVersion);
        setUpdateDialogOpen(true);
        localStorage.setItem('lastShownUpdateVersion', versionCheck.latestVersion);
        localStorage.setItem('lastShownUpdateTime', now.toString());
      } else {
        console.log('Update dialog already shown for this version, skipping');
      }
    } else if (versionCheck && versionCheck.isUpToDate) {
      // If we're up to date, close the dialog if it's open
      if (updateDialogOpen) {
        setUpdateDialogOpen(false);
      }
    }
  }, [versionCheck, updateDialogOpen]);

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
    localStorage.setItem('skillLevels', JSON.stringify(skillLevels));
  }, [skillLevels]);

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
          const result = analyzeRoll(
            parsedStats,
            baseStats,
            minerType,
            skillLevels,
          );
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
            skillLevels,
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
  }, [minerType, lastClipboardHash, lastExportText, exportFormat, skillLevels]);

  useEffect(() => {
    if (!analysis || Object.keys(baseStats).length === 0) {
      return;
    }
    const updated = analyzeRoll(analysis.stats, baseStats, minerType, skillLevels);
    setAnalysis(updated);
  }, [skillLevels]);

  // Recalculate export when format, toggle, or analysis changes
  useEffect(() => {
    if (analysis && Object.keys(baseStats).length > 0) {
      const generateExportText = async () => {
        const tierText = renderExportFormat(exportFormat, {
          analysis,
          baseStats,
          minerType,
          skillLevels,
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
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">v{APP_VERSION}</span>
              {versionCheck && !versionCheck.isUpToDate && versionCheck.latestVersion && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={async () => {
                    if (versionCheck.updateUrl) {
                      await openUrl(versionCheck.updateUrl);
                    }
                  }}
                  className="text-xs"
                  title={`Update available: v${versionCheck.latestVersion}`}
                >
                  <Download className="h-3 w-3 mr-1" />
                  Update Available
                </Button>
              )}
              {versionCheck && versionCheck.isUpToDate && versionCheck.latestVersion && (
                <span className="text-xs text-muted-foreground flex items-center gap-1" title="You're using the latest version">
                  <CheckCircle2 className="h-3 w-3" />
                  Up to date
                </span>
              )}
              {isCheckingVersion && (
                <span className="text-xs text-muted-foreground">Checking...</span>
              )}
            </div>
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
                onClick={() => setSkillLevelsOpen(true)}
              >
                Skill Levels
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
            skillLevels={skillLevels}
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

      <SkillLevelsDialog
        open={skillLevelsOpen}
        onOpenChange={setSkillLevelsOpen}
        skillLevels={skillLevels}
        onSave={setSkillLevels}
      />

      {versionCheck && !versionCheck.isUpToDate && versionCheck.latestVersion && (
        <UpdateAvailableDialog
          open={updateDialogOpen}
          onOpenChange={setUpdateDialogOpen}
          versionCheck={versionCheck}
        />
      )}
    </div>
  );
}
