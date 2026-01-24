import { useState, useEffect, useRef, useCallback } from 'react';
import { format } from 'date-fns';
import { FolderOpen, RefreshCw, Copy, Check, ArrowUp, ArrowDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Input } from '@/components/ui/input';
import { analyzeLogs, getActiveDays, findLogFiles } from '@/lib/parser/logParser';
import {
  getLogDirectory,
  saveLogDirectory,
  selectLogDirectory,
} from '@/lib/config/logConfig';
import type {
  LogAnalysisResult,
  CharacterOreBreakdown,
  CharacterMiningData,
} from '@/types';
import { getOreVolumeM3 } from '@/lib/config/oreVolumes';
import { writeText } from '@tauri-apps/plugin-clipboard-manager';

interface CharM3 {
  totalMinedM3: number;
  critMinedM3: number;
  totalResidueM3: number;
}

function computeCharM3(
  characterOreBreakdown: CharacterOreBreakdown,
  characterName: string,
): CharM3 {
  const ores = characterOreBreakdown[characterName];
  if (!ores) return { totalMinedM3: 0, critMinedM3: 0, totalResidueM3: 0 };
  let totalMinedM3 = 0;
  let critMinedM3 = 0;
  let totalResidueM3 = 0;
  for (const [oreName, data] of Object.entries(ores)) {
    const vol = getOreVolumeM3(oreName);
    totalMinedM3 += (data.nonCrit + data.crit) * vol;
    critMinedM3 += data.crit * vol;
    totalResidueM3 += data.residue * vol;
  }
  return { totalMinedM3, critMinedM3, totalResidueM3 };
}

function compareChar(
  a: CharacterMiningData,
  b: CharacterMiningData,
  breakdown: CharacterOreBreakdown,
  key: CharSortKey,
  dir: 'asc' | 'desc',
): number {
  const m3A = computeCharM3(breakdown, a.characterName);
  const m3B = computeCharM3(breakdown, b.characterName);
  const critRateA =
    a.totalCycles > 0 ? (a.critCycles / a.totalCycles) * 100 : 0;
  const critRateB =
    b.totalCycles > 0 ? (b.critCycles / b.totalCycles) * 100 : 0;
  const potA = m3A.totalMinedM3 + m3A.critMinedM3 + m3A.totalResidueM3;
  const potB = m3B.totalMinedM3 + m3B.critMinedM3 + m3B.totalResidueM3;
  const resA = potA > 0 ? (m3A.totalResidueM3 / potA) * 100 : 0;
  const resB = potB > 0 ? (m3B.totalResidueM3 / potB) * 100 : 0;

  let cmp = 0;
  switch (key) {
    case 'character':
      cmp = a.characterName.localeCompare(b.characterName, undefined, {
        numeric: true,
      });
      break;
    case 'totalMined':
      cmp = m3A.totalMinedM3 - m3B.totalMinedM3;
      break;
    case 'critMined':
      cmp = m3A.critMinedM3 - m3B.critMinedM3;
      break;
    case 'residue':
      cmp = m3A.totalResidueM3 - m3B.totalResidueM3;
      break;
    case 'critRate':
      cmp = critRateA - critRateB;
      break;
    case 'residuePct':
      cmp = resA - resB;
      break;
    case 'totalCycles':
      cmp = a.totalCycles - b.totalCycles;
      break;
  }
  return dir === 'asc' ? cmp : -cmp;
}

type CharSortKey =
  | 'character'
  | 'totalMined'
  | 'critMined'
  | 'residue'
  | 'critRate'
  | 'residuePct'
  | 'totalCycles';

export default function LogView() {
  const [logDirectory, setLogDirectory] = useState<string | null>(() =>
    getLogDirectory(),
  );
  const [analysis, setAnalysis] = useState<LogAnalysisResult | null>(null);
  const [charSortKey, setCharSortKey] = useState<CharSortKey>('totalMined');
  const [charSortDir, setCharSortDir] = useState<'asc' | 'desc'>('desc');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [realTimeEnabled, setRealTimeEnabled] = useState(false);
  const [dateRange, setDateRange] = useState<{
    start: Date | undefined;
    end: Date | undefined;
  }>({
    start: undefined,
    end: undefined,
  });
  const [activeDays, setActiveDays] = useState<Date[]>([]);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);
  const [copiedSection, setCopiedSection] = useState<string | null>(null);

  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const lastReadTimeRef = useRef<number>(0);
  const hasPerformedInitialAnalysisRef = useRef<boolean>(false);

  // Load log directory on mount
  useEffect(() => {
    const saved = getLogDirectory();
    if (saved) {
      setLogDirectory(saved);
    }
  }, []);

  // Function to perform analysis
  const performAnalysis = useCallback(async () => {
    if (!logDirectory) {
      setError('Please select a log directory');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // Handle partial date ranges: if only start or only end is set, pass it
      // If both are set, pass both. If neither is set, pass undefined.
      const dateRangeToUse =
        dateRange.start || dateRange.end
          ? {
              start: dateRange.start,
              end: dateRange.end,
            }
          : undefined;

      const result = await analyzeLogs(logDirectory, dateRangeToUse);

      if (result) {
        setAnalysis(result);
        setActiveDays(result.activeDays);
        setLastUpdate(new Date());
        lastReadTimeRef.current = Date.now();
        hasPerformedInitialAnalysisRef.current = true;
      } else {
        setError('No log files found or no data to analyze');
        setAnalysis(null);
        hasPerformedInitialAnalysisRef.current = true;
      }
    } catch (err) {
      console.error('Analysis error:', err);
      setError(
        err instanceof Error ? err.message : 'Failed to analyze log files',
      );
      setAnalysis(null);
    } finally {
      setIsLoading(false);
    }
  }, [logDirectory, dateRange.start, dateRange.end]);

  // Real-time reading effect
  useEffect(() => {
    if (realTimeEnabled && logDirectory) {
      // Perform initial analysis
      performAnalysis();

      // Set up interval for 10 seconds
      intervalRef.current = setInterval(() => {
        performAnalysis();
      }, 10000);

      return () => {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
        }
      };
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    }
  }, [realTimeEnabled, logDirectory, performAnalysis]);

  // Auto-update when date range changes (only if real-time is disabled)
  useEffect(() => {
    // Only trigger if:
    // 1. Real-time is disabled (to avoid conflicts with interval)
    // 2. Log directory is set
    // 3. Initial analysis has been performed (to avoid running on mount)
    // 4. Either both dates are set, or both are cleared
    if (
      !realTimeEnabled &&
      logDirectory &&
      hasPerformedInitialAnalysisRef.current &&
      (dateRange.start || dateRange.end || (!dateRange.start && !dateRange.end))
    ) {
      performAnalysis();
    }
  }, [dateRange.start, dateRange.end, realTimeEnabled, logDirectory, performAnalysis]);

  // Load active days when directory changes
  useEffect(() => {
    const loadActiveDays = async () => {
      if (!logDirectory) return;

      try {
        const files = await findLogFiles(logDirectory);
        const days = getActiveDays(files);
        setActiveDays(days);
      } catch (err) {
        console.error('Error loading active days:', err);
      }
    };

    loadActiveDays();
  }, [logDirectory]);

  const handleSelectDirectory = async () => {
    const selected = await selectLogDirectory();
    if (selected) {
      setLogDirectory(selected);
      setAnalysis(null);
      setError(null);
    }
  };

  const formatNumber = (num: number): string => {
    return num.toLocaleString('en-US');
  };

  const formatM3 = (num: number): string => {
    return `${num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} m³`;
  };

  const formatPercentage = (value: number): string => {
    return `${value.toFixed(2)}%`;
  };

  const handleCharSort = (key: CharSortKey) => {
    if (key === charSortKey) {
      setCharSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setCharSortKey(key);
      setCharSortDir(key === 'character' ? 'asc' : 'desc');
    }
  };

  const copyToClipboard = async (text: string, section: string) => {
    try {
      await writeText(text);
      setCopiedSection(section);
      setTimeout(() => setCopiedSection(null), 2000);
    } catch (err) {
      console.error('Error copying to clipboard:', err);
    }
  };

  const generateJaniceFormat = (type: 'total' | 'residue' | 'noncrit' | 'crit') => {
    if (!analysis) return '';

    const sortedOres = Object.values(analysis.oreBreakdown).sort(
      (a, b) => b.nonCrit + b.crit - (a.nonCrit + a.crit),
    );

    const lines: string[] = [];

    for (const ore of sortedOres) {
      let amount = 0;
      if (type === 'total') {
        amount = ore.nonCrit + ore.crit;
      } else if (type === 'residue') {
        amount = ore.residue;
      } else if (type === 'noncrit') {
        amount = ore.nonCrit;
      } else if (type === 'crit') {
        amount = ore.crit;
      }

      if (amount > 0) {
        // Round to nearest 100: divide by 100, round, then multiply by 100
        const roundedAmount = Math.round(amount / 100) * 100;
        // Only include if rounded amount is greater than 0 (skip very small amounts that round to 0)
        if (roundedAmount > 0) {
          lines.push(`${ore.oreName} ${roundedAmount}`);
        }
      }
    }

    return lines.join('\n');
  };

  return (
    <div className="flex flex-col h-screen overflow-auto">
      <Card className="m-4">
        <CardHeader>
          <CardTitle>Log View - Mining Analysis</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Directory Selection */}
          <div className="space-y-2">
            <Label>Log Directory:</Label>
            <div className="flex gap-2">
              <Input
                value={logDirectory || ''}
                placeholder="No directory selected"
                readOnly
                className="flex-1"
              />
              <Button
                variant="outline"
                onClick={handleSelectDirectory}
                disabled={isLoading}
              >
                <FolderOpen className="h-4 w-4 mr-2" />
                Select Directory
              </Button>
            </div>
          </div>

          {/* Date Range Selection */}
          <div className="space-y-2">
            <Label>Date Range (Optional):</Label>
            <div className="flex gap-2">
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="flex-1">
                    {dateRange.start
                      ? format(dateRange.start, 'PPP')
                      : 'Start Date'}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                  <Calendar
                    mode="single"
                    selected={dateRange.start}
                    onSelect={(date) =>
                      setDateRange({ ...dateRange, start: date })
                    }
                    disabled={(date) =>
                      dateRange.end ? date > dateRange.end : false
                    }
                  />
                </PopoverContent>
              </Popover>
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="flex-1">
                    {dateRange.end
                      ? format(dateRange.end, 'PPP')
                      : 'End Date'}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                  <Calendar
                    mode="single"
                    selected={dateRange.end}
                    onSelect={(date) =>
                      setDateRange({ ...dateRange, end: date })
                    }
                    disabled={(date) =>
                      dateRange.start ? date < dateRange.start : false
                    }
                  />
                </PopoverContent>
              </Popover>
              {(dateRange.start || dateRange.end) && (
                <Button
                  variant="outline"
                  onClick={() => setDateRange({ start: undefined, end: undefined })}
                >
                  Clear
                </Button>
              )}
            </div>
          </div>

          {/* Active Days Display */}
          {activeDays.length > 0 && (
            <div className="space-y-2">
              <Label>Active Days:</Label>
              <div className="text-sm text-muted-foreground">
                {activeDays.length} day(s) with activity:{' '}
                {activeDays.map((d) => format(d, 'MMM d')).join(', ')}
              </div>
            </div>
          )}

          {/* Real-time Toggle */}
          <div className="flex items-center justify-between p-4 border rounded-lg bg-muted/50">
            <div className="space-y-0.5">
              <Label htmlFor="realtime" className="text-base font-semibold">
                Real-time Reading
              </Label>
              <p className="text-sm text-muted-foreground">
                Automatically refresh every 10 seconds
              </p>
            </div>
            <Switch
              id="realtime"
              checked={realTimeEnabled}
              onCheckedChange={setRealTimeEnabled}
              disabled={!logDirectory || isLoading}
            />
          </div>

          {/* Controls */}
          <div className="flex gap-2">
            <Button
              onClick={performAnalysis}
              disabled={!logDirectory || isLoading || realTimeEnabled}
            >
              {isLoading ? (
                <>
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  Analyzing...
                </>
              ) : (
                <>
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Analyze Logs
                </>
              )}
            </Button>
          </div>

          {/* Error Display */}
          {error && (
            <div className="p-4 border border-destructive rounded-lg bg-destructive/10 text-destructive">
              {error}
            </div>
          )}

          {/* Last Update */}
          {lastUpdate && (
            <div className="text-sm text-muted-foreground">
              Last updated: {format(lastUpdate, 'PPpp')}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Analysis Results */}
      {analysis && (
        <div className="flex-1 overflow-auto px-4 pb-4">
          <Accordion type="multiple" className="space-y-4">
            {/* Character Breakdown */}
            <AccordionItem value="characters" className="border rounded-lg px-4">
              <AccordionTrigger>Character Breakdown</AccordionTrigger>
              <AccordionContent>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b">
                        <th
                          className="text-left p-2 cursor-pointer select-none hover:bg-muted/50 rounded-tl-lg"
                          onClick={() => handleCharSort('character')}
                        >
                          <span className="inline-flex items-center gap-1">
                            Character
                            {charSortKey === 'character' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                        <th
                          className="text-right p-2 cursor-pointer select-none hover:bg-muted/50"
                          onClick={() => handleCharSort('totalMined')}
                        >
                          <span className="inline-flex items-center justify-end gap-1 w-full">
                            Total Mined
                            {charSortKey === 'totalMined' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                        <th
                          className="text-right p-2 cursor-pointer select-none hover:bg-muted/50"
                          onClick={() => handleCharSort('critMined')}
                        >
                          <span className="inline-flex items-center justify-end gap-1 w-full">
                            Crit Mined
                            {charSortKey === 'critMined' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                        <th
                          className="text-right p-2 cursor-pointer select-none hover:bg-muted/50"
                          onClick={() => handleCharSort('residue')}
                        >
                          <span className="inline-flex items-center justify-end gap-1 w-full">
                            Residue
                            {charSortKey === 'residue' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                        <th
                          className="text-right p-2 cursor-pointer select-none hover:bg-muted/50"
                          onClick={() => handleCharSort('critRate')}
                        >
                          <span className="inline-flex items-center justify-end gap-1 w-full">
                            Crit Rate
                            {charSortKey === 'critRate' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                        <th
                          className="text-right p-2 cursor-pointer select-none hover:bg-muted/50"
                          onClick={() => handleCharSort('residuePct')}
                        >
                          <span className="inline-flex items-center justify-end gap-1 w-full">
                            Residue %
                            {charSortKey === 'residuePct' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                        <th
                          className="text-right p-2 cursor-pointer select-none hover:bg-muted/50 rounded-tr-lg"
                          onClick={() => handleCharSort('totalCycles')}
                        >
                          <span className="inline-flex items-center justify-end gap-1 w-full">
                            Total Cycles
                            {charSortKey === 'totalCycles' &&
                              (charSortDir === 'asc' ? (
                                <ArrowUp className="h-4 w-4" />
                              ) : (
                                <ArrowDown className="h-4 w-4" />
                              ))}
                          </span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.values(analysis.characters)
                        .sort((a, b) =>
                          compareChar(
                            a,
                            b,
                            analysis.characterOreBreakdown,
                            charSortKey,
                            charSortDir,
                          ),
                        )
                        .map((char) => {
                          const m3 = computeCharM3(analysis.characterOreBreakdown, char.characterName);
                          const critRate =
                            char.totalCycles > 0
                              ? (char.critCycles / char.totalCycles) * 100
                              : 0;
                          const totalPotentialM3 =
                            m3.totalMinedM3 + m3.critMinedM3 + m3.totalResidueM3;
                          const residueRate =
                            totalPotentialM3 > 0
                              ? (m3.totalResidueM3 / totalPotentialM3) * 100
                              : 0;

                          return (
                            <tr key={char.characterName} className="border-b">
                              <td className="p-2 font-semibold">
                                {char.characterName}
                              </td>
                              <td className="text-right p-2">
                                {formatM3(m3.totalMinedM3)}
                              </td>
                              <td className="text-right p-2">
                                {formatM3(m3.critMinedM3)}
                              </td>
                              <td className="text-right p-2">
                                {formatM3(m3.totalResidueM3)}
                              </td>
                              <td className="text-right p-2">
                                {formatPercentage(critRate)}
                              </td>
                              <td className="text-right p-2">
                                {formatPercentage(residueRate)}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(char.totalCycles)}
                              </td>
                            </tr>
                          );
                        })}
                    </tbody>
                  </table>
                </div>
              </AccordionContent>
            </AccordionItem>

            {/* Overall Summary */}
            <AccordionItem value="overall" className="border rounded-lg px-4">
              <AccordionTrigger>Overall Summary</AccordionTrigger>
              <AccordionContent>
                {(() => {
                  let overallTotalMinedM3 = 0;
                  let overallTotalCritM3 = 0;
                  let overallTotalResidueM3 = 0;
                  for (const name of Object.keys(analysis.characters)) {
                    const m3 = computeCharM3(analysis.characterOreBreakdown, name);
                    overallTotalMinedM3 += m3.totalMinedM3;
                    overallTotalCritM3 += m3.critMinedM3;
                    overallTotalResidueM3 += m3.totalResidueM3;
                  }
                  const overallResidueRateM3 =
                    overallTotalMinedM3 + overallTotalCritM3 + overallTotalResidueM3 > 0
                      ? (overallTotalResidueM3 /
                          (overallTotalMinedM3 + overallTotalCritM3 + overallTotalResidueM3)) *
                        100
                      : analysis.overall.overallResidueRate;
                  return (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-muted-foreground">
                      Total Mined (All Characters)
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatM3(overallTotalMinedM3)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Total Crit Mined
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatM3(overallTotalCritM3)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Total Residue (Wasted)
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatM3(overallTotalResidueM3)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Total Mining Cycles
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatNumber(analysis.overall.totalCycles)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Total Crit Cycles
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatNumber(analysis.overall.totalCritCycles)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Overall Crit Rate
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatPercentage(analysis.overall.overallCritRate)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Overall Residue Rate
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatPercentage(overallResidueRateM3)}
                    </p>
                  </div>
                </div>
                  );
                })()}
              </AccordionContent>
            </AccordionItem>

            {/* Ore Type Breakdown */}
            <AccordionItem value="ore" className="border rounded-lg px-4">
              <AccordionTrigger>Ore Type Breakdown</AccordionTrigger>
              <AccordionContent>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left p-2">Ore Type</th>
                        <th className="text-right p-2">Non-Crit</th>
                        <th className="text-right p-2">Crit</th>
                        <th className="text-right p-2">Residue</th>
                        <th className="text-right p-2">Total</th>
                        <th className="text-right p-2">Res %</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.values(analysis.oreBreakdown)
                        .sort(
                          (a, b) =>
                            b.nonCrit +
                            b.crit -
                            (a.nonCrit + a.crit),
                        )
                        .map((ore) => {
                          const total = ore.nonCrit + ore.crit;
                          const totalWithResidue = total + ore.residue;
                          const resRate =
                            totalWithResidue > 0
                              ? (ore.residue / totalWithResidue) * 100
                              : 0;

                          return (
                            <tr key={ore.oreName} className="border-b">
                              <td className="p-2 font-semibold">
                                {ore.oreName}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(ore.nonCrit)}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(ore.crit)}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(ore.residue)}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(total)}
                              </td>
                              <td className="text-right p-2">
                                {formatPercentage(resRate)}
                              </td>
                            </tr>
                          );
                        })}
                      {/* Totals Row */}
                      <tr className="border-t-2 font-bold">
                        <td className="p-2">TOTAL</td>
                        <td className="text-right p-2">
                          {formatNumber(
                            Object.values(analysis.oreBreakdown).reduce(
                              (sum, ore) => sum + ore.nonCrit,
                              0,
                            ),
                          )}
                        </td>
                        <td className="text-right p-2">
                          {formatNumber(
                            Object.values(analysis.oreBreakdown).reduce(
                              (sum, ore) => sum + ore.crit,
                              0,
                            ),
                          )}
                        </td>
                        <td className="text-right p-2">
                          {formatNumber(
                            Object.values(analysis.oreBreakdown).reduce(
                              (sum, ore) => sum + ore.residue,
                              0,
                            ),
                          )}
                        </td>
                        <td className="text-right p-2">
                          {formatNumber(
                            Object.values(analysis.oreBreakdown).reduce(
                              (sum, ore) => sum + ore.nonCrit + ore.crit,
                              0,
                            ),
                          )}
                        </td>
                        <td className="text-right p-2">
                          {formatPercentage(
                            analysis.overall.overallResidueRate,
                          )}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </AccordionContent>
            </AccordionItem>

            {/* Janice Export Format */}
            <AccordionItem value="janice" className="border rounded-lg px-4">
              <AccordionTrigger>Janice Appraisal Format</AccordionTrigger>
              <AccordionContent>
                <div className="space-y-4">
                  {/* Total Ore */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <Label className="font-semibold">
                        TOTAL ORE (ACTUAL MINED - paste this)
                      </Label>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          copyToClipboard(generateJaniceFormat('total'), 'total')
                        }
                      >
                        {copiedSection === 'total' ? (
                          <>
                            <Check className="h-4 w-4 mr-2" />
                            Copied!
                          </>
                        ) : (
                          <>
                            <Copy className="h-4 w-4 mr-2" />
                            Copy
                          </>
                        )}
                      </Button>
                    </div>
                    <pre className="p-4 bg-muted rounded-lg overflow-x-auto text-sm">
                      {generateJaniceFormat('total') || 'No ore data'}
                    </pre>
                  </div>

                  {/* Residue */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <Label className="font-semibold">
                        RESIDUE (WASTED/LOST - for reference)
                      </Label>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          copyToClipboard(
                            generateJaniceFormat('residue'),
                            'residue',
                          )
                        }
                      >
                        {copiedSection === 'residue' ? (
                          <>
                            <Check className="h-4 w-4 mr-2" />
                            Copied!
                          </>
                        ) : (
                          <>
                            <Copy className="h-4 w-4 mr-2" />
                            Copy
                          </>
                        )}
                      </Button>
                    </div>
                    <pre className="p-4 bg-muted rounded-lg overflow-x-auto text-sm">
                      {generateJaniceFormat('residue') ||
                        'No residue detected (using T1 miners)'}
                    </pre>
                    {analysis.overall.totalResidue > 0 && (
                      <p className="text-sm text-muted-foreground mt-2">
                        Total Residue: {formatNumber(analysis.overall.totalResidue)}{' '}
                        units (
                        {formatPercentage(analysis.overall.overallResidueRate)}{' '}
                        waste rate)
                      </p>
                    )}
                  </div>

                  {/* Non-Crit Ore */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <Label className="font-semibold">
                        NON-CRIT ORE (paste this)
                      </Label>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          copyToClipboard(
                            generateJaniceFormat('noncrit'),
                            'noncrit',
                          )
                        }
                      >
                        {copiedSection === 'noncrit' ? (
                          <>
                            <Check className="h-4 w-4 mr-2" />
                            Copied!
                          </>
                        ) : (
                          <>
                            <Copy className="h-4 w-4 mr-2" />
                            Copy
                          </>
                        )}
                      </Button>
                    </div>
                    <pre className="p-4 bg-muted rounded-lg overflow-x-auto text-sm">
                      {generateJaniceFormat('noncrit') || 'No non-crit ore data'}
                    </pre>
                  </div>

                  {/* Crit Ore */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <Label className="font-semibold">
                        CRIT ORE (paste this)
                      </Label>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          copyToClipboard(
                            generateJaniceFormat('crit'),
                            'crit',
                          )
                        }
                      >
                        {copiedSection === 'crit' ? (
                          <>
                            <Check className="h-4 w-4 mr-2" />
                            Copied!
                          </>
                        ) : (
                          <>
                            <Copy className="h-4 w-4 mr-2" />
                            Copy
                          </>
                        )}
                      </Button>
                    </div>
                    <pre className="p-4 bg-muted rounded-lg overflow-x-auto text-sm">
                      {generateJaniceFormat('crit') || 'No crit ore data'}
                    </pre>
                  </div>
                </div>
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        </div>
      )}
    </div>
  );
}
