@echo off
REM ============================================================================
REM EVE Online Strip Miner Roll Analyzer - Launcher
REM ============================================================================
REM This launcher automatically builds the application if needed and launches it.
REM ============================================================================

REM Add JDK bin folder to PATH if not already there
set "JDK_PATH=C:\Program Files\Java\jdk-22\bin"
if exist "%JDK_PATH%\javac.exe" (
    set "PATH=%JDK_PATH%;%PATH%"
)

REM Get the project root directory (1 level up from scripts/)
set "LAUNCHER_DIR=%~dp0"
set "PROJECT_ROOT=%LAUNCHER_DIR%..\"
REM Normalize the path by changing to it
cd /d "%PROJECT_ROOT%"
set "PROJECT_ROOT=%CD%"

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo ERROR: Java is not installed or not in PATH
    echo ========================================
    echo.
    echo Please install Java 8 or higher from:
    echo https://www.java.com/download/
    echo.
    echo After installing Java, run this file again.
    echo.
    pause
    exit /b 1
)

REM Check if source file exists
set "SOURCE_FILE=%PROJECT_ROOT%\src\main\java\app\EveMinerAnalyzer.java"
if not exist "%SOURCE_FILE%" (
    echo.
    echo ========================================
    echo ERROR: Cannot find app\EveMinerAnalyzer.java
    echo ========================================
    echo.
    echo Expected location: %SOURCE_FILE%
    echo.
    pause
    exit /b 1
)

REM Extract version from Java file
setlocal enabledelayedexpansion
set "VERSION=unknown"
for /f "tokens=7" %%a in ('findstr /C:"VERSION = " "%SOURCE_FILE%"') do (
    set "VER_TEMP=%%a"
    REM Remove quotes and semicolon
    set "VERSION=!VER_TEMP:"=!"
    set "VERSION=!VERSION:;=!"
)
endlocal & set "VERSION=%VERSION%"
if "%VERSION%"=="" (
    set "VERSION=unknown"
)

REM Look for versioned JAR first, then fall back to non-versioned
set "TARGET_DIR=%PROJECT_ROOT%\target"
set "JAR_FILE=%TARGET_DIR%\EveMinerAnalyzer-%VERSION%.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=%TARGET_DIR%\EveMinerAnalyzer.jar"
)

REM Check if JAR exists, if not build it
if not exist "%JAR_FILE%" (
    echo.
    echo ========================================
    echo Building application (first time only)...
    echo ========================================
    echo This may take a few seconds...
    echo.
    
    call "%LAUNCHER_DIR%java\build.bat" silent
    if errorlevel 1 (
        echo.
        echo Build failed!
        pause
        exit /b 1
    )
    
    REM Update JAR_FILE after build
    set "JAR_FILE=%TARGET_DIR%\EveMinerAnalyzer-%VERSION%.jar"
    if not exist "%JAR_FILE%" (
        set "JAR_FILE=%TARGET_DIR%\EveMinerAnalyzer.jar"
    )
    
    if not exist "%JAR_FILE%" (
        echo.
        echo JAR file not found after build!
        pause
        exit /b 1
    )
    
    echo.
    echo Build successful!
    echo.
)

REM Launch the application
echo Starting EVE Miner Analyzer...
start "" javaw -jar "%JAR_FILE%" 2>nul
if %errorlevel% neq 0 (
    REM If javaw fails, try java (will show console)
    start "" java -jar "%JAR_FILE%"
)

REM Exit immediately
exit

