# EVE Online Strip Miner Roll Analyzer

A Java GUI application that analyzes EVE Online Strip Miner rolls by monitoring your clipboard. Supports ORE Strip Miner, Modulated Strip Miner II, and ORE Ice Harvester.

## 🚀 Quick Start

**Easiest Way:** Double-click `scripts\EVE Miner Analyzer.bat` in the root folder!

It will automatically:
- Build the JAR if needed (first time only)
- Launch the application

**Want to create a standalone executable?** Navigate to `scripts\java` folder and run `create_executable.bat` to create a standalone `.exe` file that doesn't require Java.

## Features

- **Real-time clipboard monitoring** - Automatically analyzes items when you copy stats from EVE Online
- **Triple miner support** - Switch between ORE, Modulated, and Ice Harvester analysis
- **Theme customization** - Manual theme control (Light/Dark/Auto) with system theme detection
- **Tier assignment** - Automatically assigns tiers (S, A, B, C, D, E, F) based on base m³/s
- **Comprehensive metrics** - Shows base, effective, and real-world m³/s values
- **Sell price calculation** - Calculates and displays recommended sell price based on roll cost and tier modifiers
- **Customizable settings** - Configure roll cost and tier modifiers via Settings menu
- **Clipboard output** - Automatically copies tier and percentage to clipboard for easy container naming

## Project Structure

```
Rolled Mods/
├── src/
│   └── main/
│       └── java/
│           ├── app/
│           │   └── EveMinerAnalyzer.java    # Main application class
│           ├── analyzer/
│           │   └── RollAnalyzer.java         # Roll analysis logic
│           ├── calculator/
│           │   └── MiningCalculator.java     # Mining calculations
│           ├── config/
│           │   ├── ConfigManager.java         # Configuration file management
│           │   ├── MinerConfig.java           # Configuration and tier ranges
│           │   ├── OptimalRangeModifierManager.java  # Optimal range modifier management
│           │   └── TierModifierManager.java   # Tier modifier management
│           ├── model/
│           │   └── AnalysisResult.java       # Data model
│           ├── parser/
│           │   └── ItemStatsParser.java      # Clipboard parsing
│           ├── service/
│           │   └── ClipboardMonitor.java    # Clipboard monitoring service
│           └── ui/
│               ├── AnalysisDisplay.java      # UI display logic
│               └── ThemeManager.java         # Theme management
├── scripts/
│   ├── EVE Miner Analyzer.bat                # Main launcher (double-click to run)
│   └── java/
│       ├── build.bat                         # Build script
│       ├── create_executable.bat             # Create native .exe
│       └── MANIFEST.MF                       # JAR manifest
├── target/                                   # Build outputs (generated)
│   ├── build/                                # Compiled classes
│   ├── config/                               # Configuration files
│   │   ├── roll_cost.txt                     # Roll cost configuration
│   │   ├── tier_modifiers.txt                # Tier modifier multipliers
│   │   ├── optimal_range_modifier.txt        # Optimal range modifier
│   │   └── miner_type_modifiers.txt          # Miner type modifiers
│   └── *.jar                                 # JAR files
├── LICENSE
└── README.md
```

## Installation

1. Install Java 8 or higher (JDK recommended)
2. No additional dependencies required - uses built-in Java libraries
3. The project follows standard Maven/Gradle directory structure (`src/main/java/`)

## Usage

**Easiest Way:** **Double-click `scripts\EVE Miner Analyzer.bat`** in the root folder!

It will automatically:
- Build the JAR if needed (first time only)
- Launch the application

**Manual Build and Run:**

1. **Build the JAR:**
   ```bash
   cd scripts\java
   build.bat
   ```
   The JAR will be created in the `target\` directory.

2. **Run the JAR:**
   ```bash
   java -jar ..\..\target\EveMinerAnalyzer-*.jar
   ```

3. **Create native executable (optional):**
   ```bash
   cd scripts\java
   create_executable.bat
   ```
   This creates a standalone `.exe` in `target\dist\` that doesn't require Java.

**Using the Application:**

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

**Settings Menu:**
- **Roll Cost** - Set the cost per roll to enable sell price calculations
- **Tier Modifiers** - Configure tier modifier multipliers and optimal range modifier for price calculations

**Theme Menu:**
- **Toggle Theme** - Switch between light and dark themes
- **Auto (Follow System)** - Automatically follow system theme (default)
- **Light** - Force light theme
- **Dark** - Force dark theme

## Tier Ranges

### ORE Strip Miner
- **S**: 6.27 - 6.61+ m³/s
- **A**: 5.92 - 6.27 m³/s
- **B**: 5.57 - 5.92 m³/s
- **C**: 5.23 - 5.57 m³/s
- **D**: 4.88 - 5.23 m³/s
- **E**: 4.44 - 4.88 m³/s
- **F**: < 4.44 m³/s

### Modulated Strip Miner II
- **S**: 3.76188 - 3.97+ m³/s
- **A**: 3.55376 - 3.76188 m³/s
- **B**: 3.34564 - 3.55376 m³/s
- **C**: 3.13752 - 3.34564 m³/s
- **D**: 2.92940 - 3.13752 m³/s
- **E**: 2.67 - 2.92940 m³/s
- **F**: < 2.67 m³/s

### ORE Ice Harvester
- **S**: 7.033 - 7.44+ m³/s
- **A**: 6.627 - 7.033 m³/s
- **B**: 6.220 - 6.627 m³/s
- **C**: 5.813 - 6.220 m³/s
- **D**: 5.407 - 5.813 m³/s
- **E**: 5.000 - 5.407 m³/s
- **F**: < 5.000 m³/s

## System Requirements

- Windows 10/11, macOS, or Linux
- Java 8 or higher (JDK recommended)
- No additional libraries required

## Configuration

Configuration files are stored in the `target/config/` directory (created automatically):
- `roll_cost.txt` - Cost per roll (used for sell price calculations)
- `tier_modifiers.txt` - Tier modifier multipliers (S, A, B, C, D, E, F)
- `optimal_range_modifier.txt` - Optimal range modifier (applies when tier has '+')
- `miner_type_modifiers.txt` - Miner type-specific modifiers

You can edit these files directly or use the Settings menu in the application.

## Notes

- The application calculates real-world values assuming max skills, Rorqual boosts, and Mining Laser Upgrade II modules
- Tier assignment is based on **base m³/s** (not effective m³/s)
- The application runs in the background and monitors clipboard changes every 300ms
- Sell price calculations use tier modifiers and roll cost (configure via Settings menu)
- Configuration files are stored relative to the JAR file location

