import { open } from '@tauri-apps/plugin-dialog';

const LOG_DIRECTORY_KEY = 'logDirectory';

/**
 * Get saved log directory path from localStorage
 */
export function getLogDirectory(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(LOG_DIRECTORY_KEY);
}

/**
 * Save log directory preference to localStorage
 */
export function saveLogDirectory(path: string): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem(LOG_DIRECTORY_KEY, path);
}

/**
 * Use Tauri dialog to let user pick directory
 */
export async function selectLogDirectory(): Promise<string | null> {
  try {
    const selected = await open({
      directory: true,
      multiple: false,
      title: 'Select Log Directory',
    });

    if (selected === null) {
      return null;
    }

    // Handle both string and string[] return types
    const directory = Array.isArray(selected) ? selected[0] : selected;
    
    if (directory && typeof directory === 'string') {
      saveLogDirectory(directory);
      return directory;
    }

    return null;
  } catch (error) {
    console.error('Error selecting log directory:', error);
    return null;
  }
}
