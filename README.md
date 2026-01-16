# EVE Online Strip Miner Roll Analyzer

A Tauri + React + TypeScript desktop application that analyzes EVE Online Strip Miner rolls by monitoring your clipboard. Supports ORE Strip Miner, Modulated Strip Miner II, and ORE Ice Harvester.

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ and npm/pnpm
- Rust (latest stable version)
- Tauri CLI: `npm install -g @tauri-apps/cli` or `cargo install tauri-cli`

### Installation

1. Install dependencies:
   ```bash
   npm install
   # or
   pnpm install
   ```

2. Add app icons (see `src-tauri/icons/README.md` for details)

3. Run in development mode:
   ```bash
   npm run tauri:dev
   # or
   pnpm tauri:dev
   ```

### Build

Build the application:
```bash
npm run tauri:build
# or
pnpm tauri:build
```

The built application will be in `src-tauri/target/release/`

## Features

- **Real-time clipboard monitoring** - Automatically analyzes items when you copy stats from EVE Online
- **Triple miner support** - Switch between ORE, Modulated, and Ice Harvester analysis
- **Theme support** - Light/Dark mode (via system preference)
- **Tier assignment** - Automatically assigns tiers (S, A, B, C, D, E, F) based on base m³/s
- **Comprehensive metrics** - Shows base, effective, and real-world m³/s values
- **Sell price calculation** - Calculates and displays recommended sell price based on roll cost and tier modifiers
- **Customizable settings** - Configure roll cost and tier modifiers via Settings dialog

## Project Structure

```
Rolled Mods/
├── src/
│   ├── components/
│   │   ├── ui/           # shadcn/ui components
│   │   ├── MainAnalyzer.tsx
│   │   ├── AnalysisDisplay.tsx
│   │   └── SettingsDialog.tsx
│   ├── lib/
│   │   ├── analyzer/     # Roll analysis logic
│   │   ├── calculator/   # Mining calculations
│   │   ├── config/       # Configuration management
│   │   └── parser/       # Clipboard parsing
│   ├── types/            # TypeScript types
│   ├── App.tsx
│   └── main.tsx
├── src-tauri/            # Tauri Rust backend
│   ├── src/
│   │   ├── main.rs
│   │   └── lib.rs
│   ├── icons/            # App icon files
│   └── Cargo.toml
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## Configuration

Configuration files are stored in the app data directory:
- `roll_cost.txt` - Cost per roll (used for sell price calculations)
- `tier_modifiers.txt` - Tier modifier multipliers (S, A, B, C, D, E, F)
- `optimal_range_modifier.txt` - Optimal range modifier (applies when tier has '+')

You can edit these files directly or use the Settings dialog in the application.

## Tier Ranges

| Tier | ORE Strip Miner      | Modulated Strip Miner II | ORE Ice Harvester |
|:-----|:---------------------|:-------------------------|:------------------|
| **S** | 84.5 - 89.7 m³/s    | 109.4 - 121.0+ m³/s       | 86.0 - 91.4 m³/s  |
| **A** | 79.2 - 84.5 m³/s    | 97.8 - 109.4 m³/s         | 80.6 - 86.0 m³/s  |
| **B** | 73.9 - 79.2 m³/s    | 86.2 - 97.8 m³/s          | 75.2 - 80.6 m³/s  |
| **C** | 68.6 - 73.9 m³/s    | 74.6 - 86.2 m³/s          | 70.0 - 75.2 m³/s  |
| **D** | 63.4 - 68.6 m³/s    | 63.1 - 74.6 m³/s          | 64.6 - 70.0 m³/s  |
| **E** | 58.0 - 63.4 m³/s    | 51.5 - 63.1 m³/s          | 59.1 - 64.6 m³/s  |
| **F** | < 58.0 m³/s         | < 51.5 m³/s               | < 59.1 m³/s       |

## Usage

1. Select the miner type (ORE, Modulated, or Ice) using the radio buttons
2. Copy item stats from EVE Online:
   - In EVE Online, open the item info window
   - Press Ctrl+C to copy the stats
   - The application will automatically analyze the item
3. The analysis will show:
   - Roll analysis with mutation percentages
   - Performance metrics (Base, Effective, Real-World m³/s)
   - Tier assignment with color coding
   - Recommended sell price (if roll cost is configured)
   - Tier info is automatically copied to clipboard (e.g., "S: (+5.2%) [ORE]")

**Settings Dialog:**
- **Roll Cost** - Set the cost per roll to enable sell price calculations
- **Tier Modifiers** - Configure tier modifier multipliers and optimal range modifier for price calculations

## Notes

- The application calculates real-world values assuming max skills, Rorqual boosts, and Mining Laser Upgrade II modules
- Tier assignment is based on **live effective m³/s (no residue)**
- The application runs in the background and monitors clipboard changes every 300ms
- Sell price calculations use tier modifiers and roll cost (configure via Settings dialog)

## System Requirements

- Windows 10/11, macOS, or Linux
- Node.js 18+ (for development)
- Rust (for building)
