# EVE Online Strip Miner Roll Analyzer (Tauri Version)

This is a recode of the EVE Online Strip Miner Roll Analyzer using Tauri + React + TypeScript.

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ and npm/pnpm
- Rust (latest stable version)
- Tauri CLI: `npm install -g @tauri-apps/cli` or `cargo install tauri-cli`

### Development

1. Install dependencies:
   ```bash
   npm install
   # or
   pnpm install
   ```

2. Run in development mode:
   ```bash
   npm run tauri:dev
   # or
   pnpm tauri:dev
   ```

### Build

1. Build the application:
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

## Differences from Java Version

- Modern UI built with React and shadcn/ui components
- Native performance with Tauri (smaller bundle size)
- Better cross-platform support
- More maintainable codebase with TypeScript
- Configuration stored in app data directory (more appropriate for desktop apps)

## Notes

- The application calculates real-world values assuming max skills, Rorqual boosts, and Mining Laser Upgrade II modules
- Tier assignment is based on **base m³/s** (not effective m³/s)
- The application runs in the background and monitors clipboard changes every 300ms
- Sell price calculations use tier modifiers and roll cost (configure via Settings dialog)
