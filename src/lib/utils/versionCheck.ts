import { APP_VERSION } from '@/version';

const GITHUB_API_URL = 'https://api.github.com/repos/MrWildGear/Miner-Analyzer/releases/latest';
const GITHUB_RELEASES_URL = 'https://github.com/MrWildGear/Miner-Analyzer/releases';

export interface VersionCheckResult {
  isUpToDate: boolean;
  currentVersion: string;
  latestVersion: string | null;
  updateUrl: string;
  error: string | null;
}

/**
 * Fetches the latest version from GitHub releases API
 */
async function fetchLatestVersion(): Promise<string | null> {
  try {
    const response = await fetch(GITHUB_API_URL, {
      headers: {
        Accept: 'application/vnd.github.v3+json',
      },
    });

    if (!response.ok) {
      throw new Error(`GitHub API returned ${response.status}`);
    }

    const data = await response.json();
    // Extract version from tag_name (e.g., "Roll Analyzer v2.0.12", "v2.0.12", or "2.0.12")
    const tagName = data.tag_name || '';
    // Try to extract version number pattern (e.g., "2.0.12" from "v2.0.12" or "Roll Analyzer v2.0.12")
    const versionMatch = tagName.match(/(\d+\.\d+\.\d+)/);
    if (versionMatch) {
      return versionMatch[1];
    }
    // Fallback: remove common prefixes
    const version = tagName.replace(/^Roll Analyzer\s+/i, '').replace(/^v/i, '').trim();
    return version || null;
  } catch (error) {
    console.error('Error fetching latest version:', error);
    return null;
  }
}

/**
 * Compares two version strings (e.g., "2.0.12" vs "2.0.13")
 * Returns: 1 if version1 > version2, -1 if version1 < version2, 0 if equal
 */
function compareVersions(version1: string, version2: string): number {
  const v1Parts = version1.split('.').map(Number);
  const v2Parts = version2.split('.').map(Number);

  const maxLength = Math.max(v1Parts.length, v2Parts.length);

  for (let i = 0; i < maxLength; i++) {
    const v1Part = v1Parts[i] || 0;
    const v2Part = v2Parts[i] || 0;

    if (v1Part > v2Part) return 1;
    if (v1Part < v2Part) return -1;
  }

  return 0;
}

/**
 * Checks if the current version is up to date
 */
export async function checkForUpdates(): Promise<VersionCheckResult> {
  const currentVersion = APP_VERSION;
  const updateUrl = GITHUB_RELEASES_URL;

  try {
    const latestVersion = await fetchLatestVersion();

    if (!latestVersion) {
      return {
        isUpToDate: true, // Assume up to date if we can't check
        currentVersion,
        latestVersion: null,
        updateUrl,
        error: 'Unable to fetch latest version',
      };
    }

    const comparison = compareVersions(currentVersion, latestVersion);
    const isUpToDate = comparison >= 0;

    console.log('Version check:', {
      current: currentVersion,
      latest: latestVersion,
      comparison,
      isUpToDate,
    });

    return {
      isUpToDate,
      currentVersion,
      latestVersion,
      updateUrl,
      error: null,
    };
  } catch (error) {
    return {
      isUpToDate: true, // Assume up to date on error
      currentVersion,
      latestVersion: null,
      updateUrl,
      error: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}
