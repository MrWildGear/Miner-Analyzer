import { readTextFile, readDir, stat, type FileEntry } from '@tauri-apps/plugin-fs';
import type {
  CharacterMiningData,
  OreData,
  LogAnalysisResult,
  CharacterOreBreakdown,
} from '@/types';

const LOG_FILE_PATTERN = /^\d{8}_\d{6}(_\d+)?\.txt$/;

interface LogFileInfo {
  name: string;
  mtime: Date;
}

/**
 * Find all log files in the specified directory matching the pattern YYYYMMDD_HHMMSS[_characterID].txt
 * Returns file info with name and modification date
 */
export async function findLogFiles(
  directory: string,
): Promise<LogFileInfo[]> {
  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:19',message:'findLogFiles entry',data:{directory},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
  // #endregion
  try {
    const entries = await readDir(directory);
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:24',message:'readDir result',data:{entryCount:entries.length,firstFewNames:entries.slice(0,5).map(e=>e.name)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
    // #endregion
    const logFiles: LogFileInfo[] = [];
    let patternMatchCount = 0;
    let statSuccessCount = 0;
    let statFailCount = 0;

    for (const entry of entries) {
      if (entry.name && LOG_FILE_PATTERN.test(entry.name)) {
        patternMatchCount++;
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:30',message:'Pattern match found',data:{fileName:entry.name},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'B'})}).catch(()=>{});
        // #endregion
        try {
          const filePath = `${directory}/${entry.name}`;
          const fileStat = await stat(filePath);
          if (fileStat.mtime) {
            statSuccessCount++;
            logFiles.push({
              name: entry.name,
              mtime: fileStat.mtime,
            });
            // #region agent log
            fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:37',message:'stat success',data:{fileName:entry.name,mtime:fileStat.mtime.toISOString()},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'C'})}).catch(()=>{});
            // #endregion
          }
        } catch (error) {
          statFailCount++;
          console.warn(`Error getting metadata for ${entry.name}:`, error);
          // #region agent log
          fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:44',message:'stat failed',data:{fileName:entry.name,error:error instanceof Error?error.message:String(error)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'C'})}).catch(()=>{});
          // #endregion
          // Still include the file with current date as fallback
          logFiles.push({
            name: entry.name,
            mtime: new Date(),
          });
        }
      }
    }

    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:52',message:'findLogFiles exit',data:{totalEntries:entries.length,patternMatches:patternMatchCount,statSuccess:statSuccessCount,statFail:statFailCount,logFilesCount:logFiles.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
    // #endregion
    // Sort by modification date descending (most recent first)
    return logFiles.sort((a, b) => b.mtime.getTime() - a.mtime.getTime());
  } catch (error) {
    console.error('Error reading log directory:', error);
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:56',message:'findLogFiles error',data:{directory,error:error instanceof Error?error.message:String(error)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
    // #endregion
    return [];
  }
}

/**
 * Extract character name from log file content
 */
async function extractCharacterName(filePath: string): Promise<string | null> {
  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:59',message:'extractCharacterName entry',data:{filePath},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
  // #endregion
  try {
    const content = await readTextFile(filePath);
    const lines = content.split('\n').slice(0, 5); // Read first 5 lines
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:65',message:'File read',data:{filePath,contentLength:content.length,first5Lines:lines},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
    // #endregion
    const listenerLine = lines.find((line) => line.includes('Listener:'));

    if (listenerLine) {
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:70',message:'Listener line found',data:{filePath,listenerLine},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
      // #endregion
      const match = listenerLine.match(/Listener:\s*(.+)/);
      if (match && match[1]) {
        const charName = match[1].trim();
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:75',message:'Character name extracted',data:{filePath,charName},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
        // #endregion
        return charName;
      } else {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:80',message:'Regex match failed',data:{filePath,listenerLine},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
        // #endregion
      }
    } else {
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:84',message:'No listener line found',data:{filePath,first5Lines:lines},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
      // #endregion
    }
    return null;
  } catch (error) {
    console.error(`Error reading file ${filePath}:`, error);
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:90',message:'extractCharacterName error',data:{filePath,error:error instanceof Error?error.message:String(error)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
    // #endregion
    return null;
  }
}

/**
 * Identify session characters from the most recent hour, or from all files in a date range
 */
async function identifySessionCharacters(
  directory: string,
  logFiles: LogFileInfo[],
  dateRange?: { start?: Date; end?: Date },
): Promise<Set<string>> {
  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:81',message:'identifySessionCharacters entry',data:{logFilesCount:logFiles.length,hasDateRange:!!dateRange},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
  // #endregion
  if (logFiles.length === 0) return new Set();

  let filesToCheck: LogFileInfo[];

  if (dateRange && (dateRange.start || dateRange.end)) {
    // When date range is provided, find characters from ALL files in that range
    const rangeStart = dateRange.start
      ? normalizeToStartOfDay(dateRange.start)
      : null;
    const rangeEnd = dateRange.end
      ? normalizeToEndOfDay(dateRange.end)
      : null;
    filesToCheck = logFiles.filter((file) => {
      const fileDate = normalizeToStartOfDay(file.mtime);
      if (rangeStart && rangeEnd) {
        return fileDate >= rangeStart && fileDate <= rangeEnd;
      } else if (rangeStart) {
        return fileDate >= rangeStart;
      } else if (rangeEnd) {
        return fileDate <= rangeEnd;
      }
      return true;
    });
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:95',message:'Using date range for character identification',data:{filesToCheckCount:filesToCheck.length,rangeStart:rangeStart?.toISOString(),rangeEnd:rangeEnd?.toISOString()},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
    // #endregion
  } else {
    // No date range: use most recent hour (original behavior)
    const mostRecentFile = logFiles[0];
    const sessionHourPrefix = mostRecentFile.name.substring(0, 11); // YYYYMMDD_HH
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:102',message:'Most recent file identified',data:{mostRecentFileName:mostRecentFile.name,sessionHourPrefix},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
    // #endregion
    filesToCheck = logFiles.filter((file) =>
      file.name.startsWith(sessionHourPrefix),
    );
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:107',message:'Recent hour files found',data:{filesToCheckCount:filesToCheck.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
    // #endregion
  }

  const sessionCharacters = new Set<string>();

  for (const file of filesToCheck) {
    const charName = await extractCharacterName(`${directory}/${file.name}`);
    if (charName) {
      sessionCharacters.add(charName);
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:115',message:'Character found',data:{fileName:file.name,charName},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
      // #endregion
    }
  }

  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:122',message:'identifySessionCharacters exit',data:{sessionCharactersCount:sessionCharacters.size,sessionCharacters:Array.from(sessionCharacters)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
  // #endregion
  return sessionCharacters;
}

/**
 * Normalize date to start of day (00:00:00) for comparison
 */
function normalizeToStartOfDay(date: Date): Date {
  const normalized = new Date(date);
  normalized.setHours(0, 0, 0, 0);
  return normalized;
}

/**
 * Normalize date to end of day (23:59:59.999) for comparison
 */
function normalizeToEndOfDay(date: Date): Date {
  const normalized = new Date(date);
  normalized.setHours(23, 59, 59, 999);
  return normalized;
}

/**
 * Get all log files for a specific date range and characters
 */
async function getFilesForDateRange(
  directory: string,
  logFiles: LogFileInfo[],
  sessionCharacters: Set<string>,
  dateRange?: { start?: Date; end?: Date },
): Promise<string[]> {
  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:129',message:'getFilesForDateRange entry',data:{logFilesCount:logFiles.length,sessionCharactersCount:sessionCharacters.size,hasDateRange:!!dateRange,dateRangeStart:dateRange?.start?.toISOString(),dateRangeEnd:dateRange?.end?.toISOString()},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
  // #endregion
  const filteredFiles: string[] = [];

  // Normalize date range to start and end of day for proper comparison
  let rangeStart: Date | null = null;
  let rangeEnd: Date | null = null;
  if (dateRange) {
    if (dateRange.start) {
      rangeStart = normalizeToStartOfDay(dateRange.start);
    }
    if (dateRange.end) {
      rangeEnd = normalizeToEndOfDay(dateRange.end);
    }
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:137',message:'Date range normalized',data:{rangeStart:rangeStart?.toISOString(),rangeEnd:rangeEnd?.toISOString()},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
    // #endregion
  }

  let dateFilteredCount = 0;
  let characterFilteredCount = 0;
  let finalMatchCount = 0;

  for (const file of logFiles) {
    // Filter by date range using file modification date
    if (dateRange && (rangeStart || rangeEnd)) {
      const fileDate = normalizeToStartOfDay(file.mtime);
      let shouldFilter = false;
      
      if (rangeStart && rangeEnd) {
        // Both dates set: filter if outside range
        shouldFilter = fileDate < rangeStart || fileDate > rangeEnd;
      } else if (rangeStart) {
        // Only start date: filter if before start
        shouldFilter = fileDate < rangeStart;
      } else if (rangeEnd) {
        // Only end date: filter if after end
        shouldFilter = fileDate > rangeEnd;
      }
      
      if (shouldFilter) {
        dateFilteredCount++;
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:148',message:'File filtered by date',data:{fileName:file.name,fileDate:fileDate.toISOString(),rangeStart:rangeStart?.toISOString(),rangeEnd:rangeEnd?.toISOString()},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
        // #endregion
        continue;
      }
    }

    // Check if file belongs to session characters
    const charName = await extractCharacterName(`${directory}/${file.name}`);
    if (charName && sessionCharacters.has(charName)) {
      finalMatchCount++;
      filteredFiles.push(file.name);
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:157',message:'File matched',data:{fileName:file.name,charName},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'F'})}).catch(()=>{});
      // #endregion
    } else {
      characterFilteredCount++;
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:162',message:'File filtered by character',data:{fileName:file.name,charName,hasCharName:!!charName,isInSession:charName?sessionCharacters.has(charName):false},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'F'})}).catch(()=>{});
      // #endregion
    }
  }

  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:168',message:'getFilesForDateRange exit',data:{totalFiles:logFiles.length,dateFiltered:dateFilteredCount,characterFiltered:characterFilteredCount,finalMatches:finalMatchCount,filteredFilesCount:filteredFiles.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
  // #endregion
  return filteredFiles;
}

function ensureCharOre(
  characterOreBreakdown: CharacterOreBreakdown,
  characterName: string,
  oreName: string,
): { nonCrit: number; crit: number; residue: number } {
  if (!characterOreBreakdown[characterName]) {
    characterOreBreakdown[characterName] = {};
  }
  if (!characterOreBreakdown[characterName][oreName]) {
    characterOreBreakdown[characterName][oreName] = {
      nonCrit: 0,
      crit: 0,
      residue: 0,
    };
  }
  return characterOreBreakdown[characterName][oreName];
}

/**
 * Parse a single log file and extract mining data
 */
async function parseLogFile(
  directory: string,
  fileName: string,
  characterData: Record<string, CharacterMiningData>,
  oreData: Record<string, OreData>,
  characterOreBreakdown: CharacterOreBreakdown,
): Promise<void> {
  try {
    const filePath = `${directory}/${fileName}`;
    const content = await readTextFile(filePath);

    // Extract character name
    const listenerMatch = content.match(/Listener:\s*(.+)/);
    if (!listenerMatch || !listenerMatch[1]) {
      console.warn(`Could not find character name in ${fileName}`);
      return;
    }

    const characterName = listenerMatch[1].trim();

    // Initialize character data if not exists
    if (!characterData[characterName]) {
      characterData[characterName] = {
        characterName,
        totalMined: 0,
        critMined: 0,
        totalCycles: 0,
        critCycles: 0,
        totalResidue: 0,
      };
    }

    // Regular mining pattern: (mining).*?You mined.*?<color=#ff8dc169>(\d+)<.*?units of.*?<font size=12>([^<\r\n]+)
    // Updated to handle <color> tag between "units of" and <font size=12>
    const regularMiningPattern =
      /\(mining\).*?You mined.*?<color=#ff8dc169>(\d+)<.*?units of.*?<font size=12>([^<\r\n]+)/g;
    let match;
    let miningMatchCount = 0;

    while ((match = regularMiningPattern.exec(content)) !== null) {
      miningMatchCount++;
      if (match[1] && match[2]) {
        const amount = Number.parseInt(match[1], 10);
        const oreName = match[2].trim();

        characterData[characterName].totalMined += amount;
        characterData[characterName].totalCycles += 1;

        // Track ore data
        if (!oreData[oreName]) {
          oreData[oreName] = {
            oreName,
            nonCrit: 0,
            crit: 0,
            residue: 0,
          };
        }
        oreData[oreName].nonCrit += amount;

        const charOre = ensureCharOre(characterOreBreakdown, characterName, oreName);
        charOre.nonCrit += amount;
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:303',message:'Regular mining match',data:{fileName,characterName,amount,oreName},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'F'})}).catch(()=>{});
        // #endregion
      }
    }
    // #region agent log
    if (miningMatchCount === 0) {
      // Check if file has mining content at all
      const hasMiningTag = content.includes('(mining)');
      const hasYouMined = content.includes('You mined');
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:311',message:'No regular mining matches',data:{fileName,characterName,hasMiningTag,hasYouMined,contentLength:content.length,firstMiningLine:content.split('\n').find(l=>l.includes('(mining)')&&l.includes('You mined'))?.substring(0,200)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'F'})}).catch(()=>{});
    }
    // #endregion

    // Critical mining pattern: (mining).*?Critical mining success!.*?You mined an additional.*?<color=#fff0ff45><font size=12>(\d+)<.*?units of.*?<font size=12>([^<\r\n]+)
    const critMiningPattern =
      /\(mining\).*?Critical mining success!.*?You mined an additional.*?<color=#fff0ff45><font size=12>(\d+)<.*?units of.*?<font size=12>([^<\r\n]+)/g;

    while ((match = critMiningPattern.exec(content)) !== null) {
      if (match[1] && match[2]) {
        const amount = Number.parseInt(match[1], 10);
        const oreName = match[2].trim();

        characterData[characterName].critMined += amount;
        characterData[characterName].critCycles += 1;

        // Track ore data
        if (!oreData[oreName]) {
          oreData[oreName] = {
            oreName,
            nonCrit: 0,
            crit: 0,
            residue: 0,
          };
        }
        oreData[oreName].crit += amount;

        const charOre = ensureCharOre(characterOreBreakdown, characterName, oreName);
        charOre.crit += amount;
      }
    }

    // Residue pattern: (mining).*?Additional\s+<font size=12><color=#ffff454b>(\d+)<.*?units depleted from asteroid as residue
    // Residue appears after mining lines, so we track the last mined ore
    const lines = content.split(/\r?\n/);
    let lastMinedOre: string | null = null;

    for (const line of lines) {
      // Track last mined ore from regular mining
      const regularMatch = line.match(
        /\(mining\).*?You mined.*?<color=#ff8dc169>\d+<.*?units of.*?<font size=12>([^<\r\n]+)/,
      );
      if (regularMatch && regularMatch[1]) {
        lastMinedOre = regularMatch[1].trim();
      }

      // Also track from crit mining
      const critMatch = line.match(
        /\(mining\).*?Critical mining success!.*?You mined an additional.*?<color=#fff0ff45><font size=12>\d+<.*?units of.*?<font size=12>([^<\r\n]+)/,
      );
      if (critMatch && critMatch[1]) {
        lastMinedOre = critMatch[1].trim();
      }

      // Check for residue line
      const residueMatch = line.match(
        /\(mining\).*?Additional\s+<font size=12><color=#ffff454b>(\d+)<.*?units depleted from asteroid as residue/,
      );
      if (residueMatch && residueMatch[1] && lastMinedOre) {
        const amount = Number.parseInt(residueMatch[1], 10);
        characterData[characterName].totalResidue += amount;

        // Attribute residue to last mined ore
        if (!oreData[lastMinedOre]) {
          oreData[lastMinedOre] = {
            oreName: lastMinedOre,
            nonCrit: 0,
            crit: 0,
            residue: 0,
          };
        }
        oreData[lastMinedOre].residue += amount;

        const charOre = ensureCharOre(characterOreBreakdown, characterName, lastMinedOre);
        charOre.residue += amount;
      }
    }
  } catch (error) {
    console.error(`Error parsing log file ${fileName}:`, error);
  }
}

/**
 * Get active days (days with log files) from the log files
 */
export function getActiveDays(logFiles: LogFileInfo[]): Date[] {
  const activeDaysSet = new Set<string>();

  for (const file of logFiles) {
    // Extract date from file modification date
    const fileDate = normalizeToStartOfDay(file.mtime);
    const dateKey = `${fileDate.getFullYear()}${String(fileDate.getMonth() + 1).padStart(2, '0')}${String(fileDate.getDate()).padStart(2, '0')}`;
    activeDaysSet.add(dateKey);
  }

  const activeDays: Date[] = [];
  for (const dateStr of activeDaysSet) {
    const year = Number.parseInt(dateStr.substring(0, 4), 10);
    const month = Number.parseInt(dateStr.substring(4, 6), 10) - 1;
    const day = Number.parseInt(dateStr.substring(6, 8), 10);
    activeDays.push(new Date(year, month, day));
  }

  return activeDays.sort((a, b) => a.getTime() - b.getTime());
}

/**
 * Analyze log files and return aggregated mining data
 */
export async function analyzeLogs(
  directory: string,
  dateRange?: { start?: Date; end?: Date },
): Promise<LogAnalysisResult | null> {
  // #region agent log
  fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:335',message:'analyzeLogs entry',data:{directory,hasDateRange:!!dateRange,dateRangeStart:dateRange?.start?.toISOString(),dateRangeEnd:dateRange?.end?.toISOString()},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
  // #endregion
  try {
    // Find all log files
    const allLogFiles = await findLogFiles(directory);
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:342',message:'After findLogFiles',data:{allLogFilesCount:allLogFiles.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
    // #endregion
    if (allLogFiles.length === 0) {
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:345',message:'No log files found',data:{},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'A'})}).catch(()=>{});
      // #endregion
      return null;
    }

    // Identify session characters (from date range if provided, otherwise from most recent hour)
    const sessionCharacters = await identifySessionCharacters(
      directory,
      allLogFiles,
      dateRange,
    );
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:352',message:'After identifySessionCharacters',data:{sessionCharactersCount:sessionCharacters.size},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
    // #endregion
    if (sessionCharacters.size === 0) {
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:355',message:'No session characters found',data:{},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'D'})}).catch(()=>{});
      // #endregion
      return null;
    }

    // Get files for date range
    const filesToProcess = await getFilesForDateRange(
      directory,
      allLogFiles,
      sessionCharacters,
      dateRange,
    );
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:363',message:'After getFilesForDateRange',data:{filesToProcessCount:filesToProcess.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
    // #endregion

    if (filesToProcess.length === 0) {
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:366',message:'No files to process after filtering',data:{},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
      // #endregion
      return null;
    }

    // Parse all files
    const characterData: Record<string, CharacterMiningData> = {};
    const oreData: Record<string, OreData> = {};
    const characterOreBreakdown: CharacterOreBreakdown = {};
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:484',message:'Starting to parse files',data:{filesToProcessCount:filesToProcess.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
    // #endregion

    for (const file of filesToProcess) {
      await parseLogFile(directory, file, characterData, oreData, characterOreBreakdown);
    }
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:490',message:'Finished parsing files',data:{charactersCount:Object.keys(characterData).length,oresCount:Object.keys(oreData).length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
    // #endregion

    // Calculate overall statistics
    let totalMined = 0;
    let totalCrit = 0;
    let totalCycles = 0;
    let totalCritCycles = 0;
    let totalResidue = 0;

    for (const char of Object.values(characterData)) {
      totalMined += char.totalMined;
      totalCrit += char.critMined;
      totalCycles += char.totalCycles;
      totalCritCycles += char.critCycles;
      totalResidue += char.totalResidue;
    }

    const overallCritRate =
      totalCycles > 0 ? (totalCritCycles / totalCycles) * 100 : 0;
    const totalPotential = totalMined + totalCrit + totalResidue;
    const overallResidueRate =
      totalPotential > 0 ? (totalResidue / totalPotential) * 100 : 0;

    // Get active days from the original file list (filtered by the files we're processing)
    const processedFileInfos = allLogFiles.filter((file) =>
      filesToProcess.includes(file.name),
    );
    const activeDays = getActiveDays(processedFileInfos);

    // Determine date range from files if not provided or partially provided
    let finalDateRange: { start: Date; end: Date };
    if (dateRange && dateRange.start && dateRange.end) {
      // Both dates provided
      finalDateRange = {
        start: dateRange.start,
        end: dateRange.end,
      };
    } else if (dateRange && dateRange.start) {
      // Only start date provided - use last active day as end
      finalDateRange = {
        start: dateRange.start,
        end: activeDays.length > 0 ? activeDays[activeDays.length - 1] : dateRange.start,
      };
    } else if (dateRange && dateRange.end) {
      // Only end date provided - use first active day as start
      finalDateRange = {
        start: activeDays.length > 0 ? activeDays[0] : dateRange.end,
        end: dateRange.end,
      };
    } else if (activeDays.length > 0) {
      // No date range provided - use all active days
      finalDateRange = {
        start: activeDays[0],
        end: activeDays[activeDays.length - 1],
      };
    } else {
      // Fallback to current date
      const today = new Date();
      finalDateRange = {
        start: today,
        end: today,
      };
    }

    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:526',message:'analyzeLogs returning result',data:{charactersCount:Object.keys(characterData).length,oresCount:Object.keys(oreData).length,totalMined,activeDaysCount:activeDays.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
    // #endregion
    return {
      characters: characterData,
      oreBreakdown: oreData,
      characterOreBreakdown,
      dateRange: finalDateRange,
      activeDays,
      overall: {
        totalMined,
        totalCrit,
        totalCycles,
        totalCritCycles,
        totalResidue,
        overallCritRate,
        overallResidueRate,
      },
    };
  } catch (error) {
    console.error('Error analyzing logs:', error);
    // #region agent log
    fetch('http://127.0.0.1:7242/ingest/512e6178-7b24-4d54-9a68-76b71265f9bc',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'logParser.ts:543',message:'analyzeLogs error',data:{error:error instanceof Error?error.message:String(error)},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
    // #endregion
    return null;
  }
}
