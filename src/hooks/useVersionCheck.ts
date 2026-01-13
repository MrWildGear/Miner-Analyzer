import { useState, useEffect } from 'react';
import { checkForUpdates, type VersionCheckResult } from '@/lib/utils/versionCheck';
import { APP_VERSION } from '@/version';

export function useVersionCheck(autoCheck = true) {
  const [result, setResult] = useState<VersionCheckResult | null>(null);
  const [isChecking, setIsChecking] = useState(false);
  const [lastChecked, setLastChecked] = useState<Date | null>(null);

  const performCheck = async () => {
    setIsChecking(true);
    try {
      const checkResult = await checkForUpdates();
      setResult(checkResult);
      setLastChecked(new Date());
      // Cache the result
      localStorage.setItem('versionCheckResult', JSON.stringify(checkResult));
      localStorage.setItem('lastVersionCheck', Date.now().toString());
    } catch (error) {
      console.error('Version check failed:', error);
      const errorResult = {
        isUpToDate: true,
        currentVersion: '',
        latestVersion: null,
        updateUrl: 'https://github.com/MrWildGear/Miner-Analyzer/releases',
        error: error instanceof Error ? error.message : 'Unknown error',
      };
      setResult(errorResult);
      localStorage.setItem('versionCheckResult', JSON.stringify(errorResult));
    } finally {
      setIsChecking(false);
    }
  };

  useEffect(() => {
    if (autoCheck) {
      // Check on mount, but only once per session
      const lastCheckTime = localStorage.getItem('lastVersionCheck');
      const now = Date.now();
      const oneHour = 60 * 60 * 1000; // 1 hour in milliseconds

      // Only auto-check if it's been more than 1 hour since last check
      // In dev mode, check more frequently (every 5 minutes) or force check if cache is cleared
      const isDev = import.meta.env.DEV;
      const cacheTime = isDev ? 5 * 60 * 1000 : oneHour; // 5 minutes in dev, 1 hour in prod
      
      // Check if cache was cleared (for testing)
      const cacheCleared = sessionStorage.getItem('versionCheckCacheCleared') === 'true';
      if (cacheCleared) {
        sessionStorage.removeItem('versionCheckCacheCleared');
      }

      if (!lastCheckTime || now - Number.parseInt(lastCheckTime, 10) > cacheTime || cacheCleared) {
        console.log('Checking for updates...');
        performCheck().then(() => {
          localStorage.setItem('lastVersionCheck', now.toString());
        });
      } else {
        // Load cached result if available
        const cachedResult = localStorage.getItem('versionCheckResult');
        if (cachedResult) {
          try {
            const parsed = JSON.parse(cachedResult);
            // If the cached result's currentVersion doesn't match the actual current version,
            // we need to re-check (version might have changed)
            if (parsed.currentVersion !== APP_VERSION) {
              console.log('Version changed, re-checking for updates...', {
                cached: parsed.currentVersion,
                actual: APP_VERSION,
              });
              performCheck();
            } else {
              setResult(parsed);
              setLastChecked(new Date(Number.parseInt(lastCheckTime, 10)));
              console.log('Using cached version check result:', parsed);
              // In dev mode, still perform a fresh check in the background
              if (isDev) {
                console.log('Dev mode: Performing background version check...');
                performCheck();
              }
            }
          } catch {
            // Ignore parse errors, perform check
            console.log('Error parsing cached result, checking for updates...');
            performCheck();
          }
        } else {
          // No cache, perform check anyway
          console.log('No cached result, checking for updates...');
          performCheck();
        }
      }
    }
  }, [autoCheck]);

  return {
    result,
    isChecking,
    lastChecked,
    checkForUpdates: performCheck,
  };
}
