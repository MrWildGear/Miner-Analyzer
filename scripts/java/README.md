# Java Build Scripts - EVE Miner Analyzer

## Quick Start

### Easiest Way:
**Double-click `scripts\EVE Miner Analyzer.bat`** from the project root.

The launcher will:
- Automatically build the JAR if needed (first time only)
- Launch the application
- Close the console window automatically

### Manual Build Options

#### Build JAR Manually
```bash
cd scripts\java
build.bat
```
The JAR will be created in `target\EveMinerAnalyzer-*.jar`

#### Run the JAR
```bash
java -jar ..\..\target\EveMinerAnalyzer-*.jar
```

#### Create Native Executable (Optional)
```bash
cd scripts\java
create_executable.bat
```
This creates a standalone `.exe` file in `target\dist\` that doesn't require Java.

## Files

- `build.bat` - Build script to create JAR
- `create_executable.bat` - Creates native Windows executable (optional)
- `MANIFEST.MF` - JAR manifest file

## Source Code Location

The Java source code is located at: `src\main\java\EveMinerAnalyzer.java`

## Build Outputs

All build outputs (JAR files, compiled classes) are placed in the `target\` directory at the project root.

## Requirements

- Java 8 or higher (JDK recommended for compilation)

See the main [README.md](../../README.md) for full documentation.

