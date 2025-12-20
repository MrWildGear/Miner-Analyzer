# EVE Online Strip Miner Roll Analyzer

A GUI application (available in both Python and Java) that analyzes EVE Online Strip Miner rolls by monitoring your clipboard. Supports both ORE Strip Miner and Modulated Strip Miner II.

## 🚀 Quick Start - Single File Distribution

**Want to share with others?** See [SHARING_INSTRUCTIONS.md](SHARING_INSTRUCTIONS.md) for how to create a single executable file that doesn't require Java!

**Quick option:** Navigate to `java` folder and run `create_executable.bat` to create a standalone `.exe` file.

## Features

- **Real-time clipboard monitoring** - Automatically analyzes items when you copy stats from EVE Online
- **Dual miner support** - Switch between ORE and Modulated Strip Miner analysis
- **System theme support** - Automatically detects and uses your system's light/dark theme
- **Tier assignment** - Automatically assigns tiers (S, A, B, C, D, E, F) based on base m³/s
- **Comprehensive metrics** - Shows base, effective, and real-world m³/s values
- **Clipboard output** - Automatically copies tier and percentage to clipboard for easy container naming

## Installation

## Project Structure

```
Rolled Mods/
├── python/              # Python version
│   ├── eve_miner_analyzer.py
│   └── requirements.txt
├── powershell/          # PowerShell scripts
│   ├── rolled Ore Strip miner.ps1
│   ├── roll_analyer_modulated.ps1
│   ├── roll_analyzer_modulated_realworld.ps1
│   └── mutaplasmid_odds_calculator.ps1
├── java/                # Java version
│   ├── EveMinerAnalyzer.java
│   ├── build.bat / build.sh
│   ├── run.bat / run.sh
│   └── compile_and_run.bat / compile_and_run.sh
└── README.md
```

## Installation

### Python Version

1. Navigate to the `python` folder
2. Install Python 3.7 or higher
3. Install dependencies:
   ```bash
   cd python
   pip install -r requirements.txt
   ```

### Java Version

1. Navigate to the `java` folder
2. Install Java 8 or higher (JDK recommended)
3. No additional dependencies required - uses built-in Java libraries

## Usage

### Python Version

1. Navigate to the `python` folder:
   ```bash
   cd python
   ```

2. Run the application:
   ```bash
   python eve_miner_analyzer.py
   ```

### Java Version

**Easiest Way:** **Double-click `EVE Miner Analyzer.bat`** in the root folder!

It will automatically:
- Build the JAR if needed (first time only)
- Launch the application

Or navigate to the `java` folder for manual control:
```bash
cd java
```

Then:

1. **Build the JAR:**
   ```bash
   build.bat
   ```

2. **Run the JAR:**
   ```bash
   java -jar EveMinerAnalyzer.jar
   ```

3. **Create native executable (optional):**
   ```bash
   create_executable.bat
   ```
   This creates a standalone `.exe` that doesn't require Java.

### PowerShell Version

1. Navigate to the `powershell` folder:
   ```powershell
   cd powershell
   ```

2. Run the desired script:
   ```powershell
   .\rolled Ore Strip miner.ps1
   # or
   .\roll_analyer_modulated.ps1
   ```

2. Select the miner type (ORE or Modulated) using the radio buttons

3. Copy item stats from EVE Online:
   - In EVE Online, open the item info window
   - Press Ctrl+C to copy the stats
   - The application will automatically analyze the item

4. The analysis will show:
   - Roll analysis with mutation percentages
   - Performance metrics (Base, Effective, Real-World m³/s)
   - Tier assignment with color coding
   - Tier info is automatically copied to clipboard (e.g., "S: (+5.2%) [ORE]")

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

## System Requirements

### Python Version
- Windows 10/11, macOS, or Linux
- Python 3.7+
- tkinter (usually included with Python)
- pyperclip library

### Java Version
- Windows 10/11, macOS, or Linux
- Java 8 or higher (JDK recommended)
- No additional libraries required

## Notes

- The application calculates real-world values assuming max skills, Rorqual boosts, and Mining Laser Upgrade II modules
- Tier assignment is based on **base m³/s** (not effective m³/s)
- The application runs in the background and monitors clipboard changes every 300ms

