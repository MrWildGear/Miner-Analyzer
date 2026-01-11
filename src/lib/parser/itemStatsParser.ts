export interface ParsedStats {
  [key: string]: number;
}

const TIME_PATTERN = /(\d+)\s*m\s*(\d+)\s*s/i;
const COMPACT_TIME_PATTERN = /(\d+)m(\d+)s/i;
const PATTERN_STARTS_WITH_DIGIT = /^\d.*/;
const PERCENTAGE_DIVISOR = 100;
const SECONDS_PER_MINUTE = 60;

function normalizeNumericString(input: string): string {
  // Remove thousand separators (commas, spaces)
  return input.replaceAll(/[, ]/g, '');
}

function parseTimeFormat(timeStr: string): number | null {
  // Try format with spaces: "3m 20s"
  const timeMatcher = TIME_PATTERN.exec(timeStr);
  if (timeMatcher) {
    try {
      const minutes = Number.parseInt(timeMatcher[1], 10);
      const seconds = Number.parseInt(timeMatcher[2], 10);
      return minutes * SECONDS_PER_MINUTE + seconds;
    } catch (e) {
      console.error('Error parsing time format:', timeStr, e);
      return null;
    }
  }

  // Try compact format without spaces: "3m20s"
  const compactMatcher = COMPACT_TIME_PATTERN.exec(timeStr);
  if (compactMatcher) {
    try {
      const minutes = Number.parseInt(compactMatcher[1], 10);
      const seconds = Number.parseInt(compactMatcher[2], 10);
      return minutes * SECONDS_PER_MINUTE + seconds;
    } catch (e) {
      console.error('Error parsing compact time format:', timeStr, e);
      return null;
    }
  }
  return null;
}

function matchStatName(labelLower: string): string | null {
  if (/^activation\s+cost.*/.test(labelLower)) {
    return 'ActivationCost';
  }
  if (
    /.*activation\s+time.*/.test(labelLower) ||
    (/.*duration.*/.test(labelLower) && !labelLower.includes('residue'))
  ) {
    return 'ActivationTime';
  }
  if (/^mining\s+amount.*/.test(labelLower)) {
    return 'MiningAmount';
  }
  if (/.*critical\s+success\s+chance.*/.test(labelLower)) {
    return 'CriticalSuccessChance';
  }
  if (/.*critical\s+success\s+bonus\s+yield.*/.test(labelLower)) {
    return 'CriticalSuccessBonusYield';
  }
  if (/^optimal\s+range.*/.test(labelLower)) {
    return 'OptimalRange';
  }
  if (/.*residue\s+probability.*/.test(labelLower)) {
    return 'ResidueProbability';
  }
  if (/.*residue\s+volume\s+multiplier.*/.test(labelLower)) {
    return 'ResidueVolumeMultiplier';
  }
  return null;
}

function getStatValue(statName: string, numValue: number): number {
  if (
    statName === 'CriticalSuccessChance' ||
    statName === 'CriticalSuccessBonusYield' ||
    statName === 'ResidueProbability'
  ) {
    return numValue / PERCENTAGE_DIVISOR;
  }
  return numValue;
}

function findMatcherForLine(line: string): RegExpMatchArray | null {
  // Try tab-separated first
  const tabMatch = /^(.+?)\t+(.+)$/.exec(line);
  if (tabMatch) {
    const valuePart = tabMatch[2].trim();
    if (
      PATTERN_STARTS_WITH_DIGIT.test(valuePart) ||
      TIME_PATTERN.test(valuePart) ||
      COMPACT_TIME_PATTERN.test(valuePart)
    ) {
      return tabMatch;
    }
  }

  // Try multiple spaces
  const multiSpaceMatch = /^(.+?)\s{2,}(.+)$/.exec(line);
  if (multiSpaceMatch) {
    const valuePart = multiSpaceMatch[2].trim();
    if (
      PATTERN_STARTS_WITH_DIGIT.test(valuePart) ||
      TIME_PATTERN.test(valuePart) ||
      COMPACT_TIME_PATTERN.test(valuePart)
    ) {
      return multiSpaceMatch;
    }
  }

  // Try single space (least specific, try last)
  const singleSpaceMatch = /^(.+?)\s+(.+)$/.exec(line);
  if (singleSpaceMatch) {
    const valuePart = singleSpaceMatch[2].trim();
    if (
      PATTERN_STARTS_WITH_DIGIT.test(valuePart) ||
      TIME_PATTERN.test(valuePart) ||
      COMPACT_TIME_PATTERN.test(valuePart)
    ) {
      return singleSpaceMatch;
    }
  }

  return null;
}

function processStatLine(
  matcher: RegExpMatchArray,
  stats: ParsedStats,
): void {
  try {
    const label = matcher[1].trim();
    let valueStr = matcher[2].trim();
    const statName = matchStatName(label.toLowerCase());

    if (!statName) {
      return;
    }

    // Special handling for ActivationTime - check for time format (Xm Ys)
    if (statName === 'ActivationTime') {
      const timeValue = parseTimeFormat(valueStr);
      if (timeValue !== null) {
        stats[statName] = timeValue;
        return;
      }
    }

    // Extract numeric value from the string
    valueStr = normalizeNumericString(valueStr);

    // Extract number pattern (digits and decimal point)
    const numberPattern = /(\d+(?:\.\d+)?)/;
    const numberMatch = numberPattern.exec(valueStr);

    if (numberMatch) {
      const numberStr = numberMatch[1];
      const numValue = Number.parseFloat(numberStr);
      const statValue = getStatValue(statName, numValue);
      stats[statName] = statValue;
    }
  } catch (e) {
    console.error('Error parsing stat line:', e);
  }
}

export function parseItemStats(clipboardText: string): ParsedStats {
  const stats: ParsedStats = {};
  if (!clipboardText?.trim()) {
    return stats;
  }

  const lines = clipboardText.split('\n');

  for (const line of lines) {
    const trimmedLine = line.trim();
    if (!trimmedLine) {
      continue;
    }

    const matcher = findMatcherForLine(trimmedLine);
    if (matcher) {
      processStatLine(matcher, stats);
    }
  }

  return stats;
}
