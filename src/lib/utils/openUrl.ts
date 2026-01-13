import { open } from '@tauri-apps/plugin-shell';

/**
 * Checks if we're running in a Tauri environment
 */
function isTauriEnvironment(): boolean {
  return (
    typeof window !== 'undefined' &&
    (window as any).__TAURI_INTERNALS__ !== undefined ||
    (window as any).__TAURI__ !== undefined
  );
}

/**
 * Opens a URL in the default browser
 * Tries Tauri's shell.open first, falls back to window.open
 */
export async function openUrl(url: string): Promise<void> {
  // Always try Tauri first if available, then fallback to window.open
  if (isTauriEnvironment()) {
    try {
      await open(url);
      console.log('URL opened via Tauri shell:', url);
      return;
    } catch (error) {
      console.warn('Tauri shell.open failed, falling back to window.open:', error);
      // Fall through to window.open fallback
    }
  }
  
  // Fallback to window.open (works in both browser and Tauri if shell fails)
  if (typeof window !== 'undefined') {
    const opened = window.open(url, '_blank', 'noopener,noreferrer');
    if (opened) {
      console.log('URL opened via window.open:', url);
    } else {
      console.error('window.open was blocked or failed');
      throw new Error('Unable to open URL: popup blocked or window.open failed');
    }
  } else {
    throw new Error('Unable to open URL: no window object available');
  }
}
