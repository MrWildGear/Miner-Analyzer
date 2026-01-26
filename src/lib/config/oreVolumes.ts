/**
 * EVE Online ore volume (m³ per unit) lookup.
 * Used to convert mined units to m³ for Character breakdown and Overall Summary.
 */

const ORE_VOLUMES_M3: Record<string, number> = {
  Arkonor : 16,
  'Arkonor II-Grade': 16,
  'Arkonor III-Grade': 16,
  'Arkonor IV-Grade': 16,

  Bezdnacine: 16,
  'Bezdnacine II-Grade': 16,
  'Bezdnacine III-Grade': 16,

  Bistot: 16,
  'Bistot II-Grade': 16,
  'Bistot III-Grade': 16,
  'Bistot IV-Grade': 16,

  Crokite: 16,
  'Crokite II-Grade': 16,
  'Crokite III-Grade': 16,
  'Crokite IV-Grade': 16,

  'Dark Ochre': 8,
  'Dark Ochre II-Grade': 8,
  'Dark Ochre III-Grade': 8,
  'Dark Ochre IV-Grade': 8,

  Ducinium: 16,
  'Ducinium II-Grade': 16,
  'Ducinium III-Grade': 16,
  'Ducinium IV-Grade': 16,

  Eifyrium: 16,
  'Eifyrium II-Grade': 16,
  'Eifyrium III-Grade': 16,
  'Eifyrium IV-Grade': 16,

  Gneiss: 8,
  'Gneiss II-Grade': 8,
  'Gneiss III-Grade': 8,
  'Gneiss IV-Grade': 8,

  Griemeer: 0.8,
  'Griemeer II-Grade': 0.8,
  'Griemeer III-Grade': 0.8,
  'Griemeer IV-Grade': 0.8,

  Hedbergite: 3,
  'Hedbergite II-Grade': 3,
  'Hedbergite III-Grade': 3,
  'Hedbergite IV-Grade': 3,
  
  Hemorphite: 3,
  'Hemorphite II-Grade': 3,
  'Hemorphite III-Grade': 3,
  'Hemorphite IV-Grade': 3,

  Hezorime: 5,
  'Hezorime II-Grade': 5,
  'Hezorime III-Grade': 5,
  'Hezorime IV-Grade': 5,

  Jaspet: 2,
  'Jaspet II-Grade': 2,
  'Jaspet III-Grade': 2,
  'Jaspet IV-Grade': 2,
 
  Kernite: 1.2,
  'Kernite II-Grade': 1.2,
  'Kernite III-Grade': 1.2,
  'Kernite IV-Grade': 1.2,
 
  Kylixium: 1.2,
  'Kylixium II-Grade': 1.2,
  'Kylixium III-Grade': 1.2,
  'Kylixium IV-Grade': 1.2,
 
  Mercoxit: 40,
  'Mercoxit II-Grade': 40,
  'Mercoxit III-Grade': 40,

  Mordunium: 0.1,
  'Mordunium II-Grade': 0.1,
  'Mordunium III-Grade': 0.1,
  'Mordunium IV-Grade': 0.1,

  Nocxite: 4,
  'Nocxite II-Grade': 4,
  'Nocxite III-Grade': 4,
  'Nocxite IV-Grade': 4,
 
  Omber: 0.6,
  'Omber II-Grade': 0.6,
  'Omber III-Grade': 0.6,
  'Omber IV-Grade': 0.6,
  
  Plagioclase: 0.35,
  'Plagioclase II-Grade': 0.35,
  'Plagioclase III-Grade': 0.35,
  'Plagioclase IV-Grade': 0.35,
 
  Prismaticite: 40,

  Pyroxeres: 0.3,
  'Pyroxeres II-Grade': 0.3,
  'Pyroxeres III-Grade': 0.3,
  'Pyroxeres IV-Grade': 0.3,

  Rakovene: 16,
  'Rakovene II-Grade': 16,
  'Rakovene III-Grade': 16,
 
  Scordite: 0.15,
  'Scordite II-Grade': 0.15,
  'Scordite III-Grade': 0.15,
  'Scordite IV-Grade': 0.15,
 
  Spodumain: 16,
  'Spodumain II-Grade': 16,
  'Spodumain III-Grade': 16,
  'Spodumain IV-Grade': 16,
  
  Talassonite: 16,
  'Talassonite II-Grade': 16,
  'Talassonite III-Grade': 16,
 
  Tyranite: 0.6,

  Ueganite: 5,
  'Ueganite II-Grade': 5,
  'Ueganite III-Grade': 5,
  'Ueganite IV-Grade': 5,
 
  Veldspar: 0.1,
  'Veldspar II-Grade': 0.1,
  'Veldspar III-Grade': 0.1,
  'Veldspar IV-Grade': 0.1,

  Ytirium: 0.6,
  'Ytirium II-Grade': 0.6,
  'Ytirium III-Grade': 0.6,
  'Ytirium IV-Grade': 0.6,

  Cobaltite: 10,
  'Copious Cobaltite': 10,
  'Twinkling Cobaltite': 10,

  Euxenite: 10,
  'Copious Euxenite': 10,
  'Twinkling Euxenite': 10,

  Scheelite: 10,
  'Copious Scheelite': 10,
  'Twinkling Scheelite': 10,

  Titanite: 10,
  'Copious Titanite': 10,
  'Twinkling Titanite': 10,

  Loparite: 10,
  'Bountiful Loparite': 10,
  'Shining Loparite': 10,

  Monazite: 10,
  'Bountiful Monazite': 10,
  'Shining Monazite': 10,

  Xenotime: 10,
  'Bountiful Xenotime': 10,
  'Shining Xenotime': 10,

  Ytterbite: 10,
  'Bountiful Ytterbite': 10,
  'Shining Ytterbite': 10,

  Carnotite: 10,
  'Glowing Carnotite': 10,
  'Replete Carnotite': 10,

  Cinnabar: 10,
  'Glowing Cinnabar': 10,
  'Replete Cinnabar': 10,

  Pollucite : 10,
  'Glowing Pollucite': 10,
  'Replete Pollucite': 10,

  Zircon: 10,
  'Glowing Zircon': 10,
  'Replete Zircon': 10,

  Bitumens: 10,
  'Brimful Bitumens': 10,
  'Glistening Bitumens': 10,

  Coesite: 10,
  'Glowing Coesite': 10,
  'Replete Coesite': 10,

  Sylvite: 10,
  'Glowing Sylvite': 10,
  'Replete Sylvite': 10,

  Zeolites: 10,
  'Glowing Zeolites': 10,
  'Replete Zeolites': 10,

  Chromite : 10,
  'Lavish Chromite': 10,
  'Shimmering Chromite': 10,

  Otavite: 10,
  'Lavish Otavite': 10,
  'Shimmering Otavite': 10,

  Sperrylite: 10,
  'Lavish Sperrylite': 10,
  'Shimmering Sperrylite': 10,

  Vanadinite: 10,
  'Lavish Vanadinite': 10,
  'Shimmering Vanadinite': 10,

  'Blue Ice': 1000,
  'Blue Ice IV-Grade': 1000,

  'Clear Icicle': 1000,
  'Clear Icicle IV-Grade': 1000,

  'Dark Glitter': 1000,

  Gelidus: 1000,

  'Glacial Mass': 1000,
  'Glacial Mass IV-Grade': 1000,

 'Glare Crust': 1000,
  
 Krystallos: 1000,

'White Glaze': 1000,
'White Glaze IV-Grade': 1000,
};

const FALLBACK_VOLUME_M3 = 0;

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
