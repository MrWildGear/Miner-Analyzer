/**
 * EVE Online ore volume (m³ per unit) lookup.
 * Used to convert mined units to m³ for Character breakdown and Overall Summary.
 */

const ORE_VOLUMES_M3: Record<string, number> = {
  Veldspar: 0.1,
  Scordite: 0.15,
  Pyroxeres: 0.3,
  Plagioclase: 0.35,
  Omber: 0.6,
  Kernite: 1.2,
  Jaspet: 2,
  Hemorphite: 3,
  Hedbergite: 3,
  Gneiss: 5,
  'Dark Ochre': 8,
  Crokite: 16,
  Spodumain: 16,
  Arkonor: 16,
  Bistot: 16,
  Mercoxit: 40,
  Ducinium: 16,
  Eifyrium: 16,
  Mordunium: 0.1,
  Ytirium: 0.6,
  Bezdnacine: 16,
  Rakovene: 16,
  Talassonite: 16,
  Griemeer: 0.8,
  Hezorime: 5,
  Kylixium: 1.2,
  Nocxite: 4,
  Ueganite: 5,
  Prismaticite: 40,
};

const FALLBACK_VOLUME_M3 = 0.1;

/**
 * Normalize ore name for lookup: trim and use title case to match known keys.
 */
function normalizeOreName(name: string): string {
  const trimmed = name.trim();
  const lower = trimmed.toLowerCase();
  const known = Object.keys(ORE_VOLUMES_M3);
  const match = known.find((k) => k.toLowerCase() === lower);
  return match ?? trimmed;
}

/**
 * Get volume in m³ per unit for an ore. Case-insensitive.
 * Returns fallback (0.1) for unknown ores.
 */
export function getOreVolumeM3(oreName: string): number {
  const key = normalizeOreName(oreName);
  return ORE_VOLUMES_M3[key] ?? FALLBACK_VOLUME_M3;
}
