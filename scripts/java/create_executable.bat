@echo off
REM ============================================================================
REM EVE Online Strip Miner Roll Analyzer - Create Native Executable
REM ============================================================================
REM Creates a native Windows executable using jpackage
REM ============================================================================

REM Get the project root directory (2 levels up from scripts/java/)
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%..\..\"
REM Normalize the path by changing to it
cd /d "%PROJECT_ROOT%"
set "PROJECT_ROOT=%CD%"

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
set "TARGET_DIR=%PROJECT_ROOT%\target"
set "JAR_FILE="
REM First, try to find versioned JAR
for %%f in ("%TARGET_DIR%\EveMinerAnalyzer-*.jar") do (
    if exist "%%f" set "JAR_FILE=%%f"
)
REM If no versioned JAR found, try non-versioned
if not defined JAR_FILE (
    if exist "%TARGET_DIR%\EveMinerAnalyzer.jar" (
        set "JAR_FILE=%TARGET_DIR%\EveMinerAnalyzer.jar"
    )
)

REM If still no JAR, build it
if not defined JAR_FILE (
    echo JAR not found. Building...
    call "%SCRIPT_DIR%build.bat" silent
    if errorlevel 1 (
        echo Build failed!
        pause
        exit /b 1
    )
    REM Try to find JAR again after build
    for %%f in ("%TARGET_DIR%\EveMinerAnalyzer-*.jar") do (
        if exist "%%f" set "JAR_FILE=%%f"
    )
    if not defined JAR_FILE (
        if exist "%TARGET_DIR%\EveMinerAnalyzer.jar" (
            set "JAR_FILE=%TARGET_DIR%\EveMinerAnalyzer.jar"
        )
    )
)

REM Final check
if not defined JAR_FILE (
    echo ERROR: JAR file not found in target directory!
    echo Expected location: %TARGET_DIR%\EveMinerAnalyzer-*.jar
    pause
    exit /b 1
)

REM Create output directory
set "DIST_DIR=%PROJECT_ROOT%\target\dist"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

echo.
echo Creating executable...
echo This may take a few minutes...
echo.

REM Create executable (includes Java runtime)
REM Extract just the filename from the full path
for %%F in ("%JAR_FILE%") do set "JAR_FILENAME=%%~nxF"
echo Using JAR: %JAR_FILE%
echo JAR Filename: %JAR_FILENAME%
jpackage --input "%TARGET_DIR%" --name "EVE Miner Analyzer" --main-jar "%JAR_FILENAME%" --main-class EveMinerAnalyzer --type exe --dest "%DIST_DIR%" --app-version 1.0.0 --description "EVE Online Strip Miner Roll Analyzer" --vendor "EVE Miner Analyzer" --copyright "2024"

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
echo Executable created in: %DIST_DIR%\EVE Miner Analyzer.exe
echo.
echo This is a standalone executable that includes Java.
echo Users don't need Java installed to run it.
echo.
echo You can now share this single .exe file!
echo.
pause
