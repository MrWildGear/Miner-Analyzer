import { useState, useEffect, useRef, useCallback } from 'react';
import { format } from 'date-fns';
import { FolderOpen, RefreshCw, Copy, Check } from 'lucide-react';
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
import type { LogAnalysisResult } from '@/types';
import { writeText } from '@tauri-apps/plugin-clipboard-manager';

export default function LogView() {
  const [logDirectory, setLogDirectory] = useState<string | null>(() =>
    getLogDirectory(),
  );
  const [analysis, setAnalysis] = useState<LogAnalysisResult | null>(null);
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

  const formatPercentage = (value: number): string => {
    return `${value.toFixed(2)}%`;
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
        lines.push(`${ore.oreName} ${amount}`);
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
                        <th className="text-left p-2">Character</th>
                        <th className="text-right p-2">Total Mined</th>
                        <th className="text-right p-2">Crit Mined</th>
                        <th className="text-right p-2">Residue</th>
                        <th className="text-right p-2">Crit Rate</th>
                        <th className="text-right p-2">Residue %</th>
                        <th className="text-right p-2">Total Cycles</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.values(analysis.characters)
                        .sort(
                          (a, b) => b.totalMined - a.totalMined,
                        )
                        .map((char) => {
                          const critRate =
                            char.totalCycles > 0
                              ? (char.critCycles / char.totalCycles) * 100
                              : 0;
                          const totalPotential =
                            char.totalMined + char.critMined + char.totalResidue;
                          const residueRate =
                            totalPotential > 0
                              ? (char.totalResidue / totalPotential) * 100
                              : 0;

                          return (
                            <tr key={char.characterName} className="border-b">
                              <td className="p-2 font-semibold">
                                {char.characterName}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(char.totalMined)}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(char.critMined)}
                              </td>
                              <td className="text-right p-2">
                                {formatNumber(char.totalResidue)}
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
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-muted-foreground">
                      Total Mined (All Characters)
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatNumber(analysis.overall.totalMined)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Total Crit Mined
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatNumber(analysis.overall.totalCrit)}
                    </p>
                  </div>
                  <div>
                    <Label className="text-muted-foreground">
                      Total Residue (Wasted)
                    </Label>
                    <p className="text-2xl font-bold">
                      {formatNumber(analysis.overall.totalResidue)}
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
                      {formatPercentage(analysis.overall.overallResidueRate)}
                    </p>
                  </div>
                </div>
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
