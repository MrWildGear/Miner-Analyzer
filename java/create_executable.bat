@echo off
REM Create Native Windows Executable using jpackage

echo ========================================
echo Creating Native Windows Executable
echo ========================================
echo.
echo.

REM Check if jpackage is available
jpackage --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: jpackage not found!
    echo.
    echo jpackage requires Java 14 or higher (JDK).
    echo Please install Java 14+ JDK from:
    echo https://adoptium.net/
    echo.
    echo Make sure to add Java to your PATH.
    pause
    exit /b 1
)

REM Check if JAR exists, build if needed
if not exist EveMinerAnalyzer.jar (
    echo JAR not found. Building...
    call build.bat silent
    if errorlevel 1 (
        echo Build failed!
        pause
        exit /b 1
    )
)

REM Create output directory
if not exist dist mkdir dist

echo.
echo Creating executable...
echo This may take a few minutes...
echo.

REM Create executable (includes Java runtime)
jpackage --input . --name "EVE Miner Analyzer" --main-jar EveMinerAnalyzer.jar --main-class EveMinerAnalyzer --type exe --dest dist --app-version 1.0.0 --description "EVE Online Strip Miner Roll Analyzer" --vendor "EVE Miner Analyzer" --copyright "2024"

if errorlevel 1 (
    echo.
    echo Failed to create executable.
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo Executable created in: dist\EVE Miner Analyzer.exe
echo.
echo This is a standalone executable that includes Java.
echo Users don't need Java installed to run it.
echo.
echo You can now share this single .exe file!
echo.
pause
