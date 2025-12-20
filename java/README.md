# Java Version - EVE Miner Analyzer

## Quick Start - Double-Click to Run! 🚀

### Easiest Way:
**Double-click `EVE Miner Analyzer.bat`** from the root folder.

The launcher will:
- Automatically build the JAR if needed (first time only)
- Launch the application
- Close the console window automatically

### Other Options:

#### Build JAR Manually
```bash
cd java
build.bat
java -jar EveMinerAnalyzer.jar
```

#### Create Native Executable (Optional)
```bash
cd java
create_executable.bat
```
This creates a standalone `.exe` file that doesn't require Java.

## Files

- `EveMinerAnalyzer.java` - Main Java source file
- `build.bat` - Build script to create JAR
- `create_executable.bat` - Creates native Windows executable (optional)
- `MANIFEST.MF` - JAR manifest file
- `EveMinerAnalyzer.jar` - Compiled JAR file (created by build.bat)

## Requirements

- Java 8 or higher (JDK recommended for compilation)

See the main [README.md](../README.md) for full documentation.
