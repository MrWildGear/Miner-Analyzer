@echo off
REM ============================================================================
REM EVE Online Strip Miner Roll Analyzer - Build Script
REM ============================================================================
REM Builds the Java application from source to target directory
REM ============================================================================

REM Add JDK bin folder to PATH if not already there
set "JDK_PATH=C:\Program Files\Java\jdk-22\bin"
if exist "%JDK_PATH%\jar.exe" (
    set "PATH=%JDK_PATH%;%PATH%"
)

REM Get the project root directory (2 levels up from scripts/java/)
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%..\..\"
REM Normalize the path by changing to it
cd /d "%PROJECT_ROOT%"
set "PROJECT_ROOT=%CD%"

echo ========================================
echo Building EVE Miner Analyzer
echo ========================================
echo.

REM Extract version from Java file
REM Find the VERSION line and extract the quoted value using simple batch parsing
setlocal enabledelayedexpansion
set "VERSION=unknown"
REM Look for the pattern: VERSION = "x.x.x"
for /f "tokens=7 delims= " %%a in ('findstr /C:"VERSION = " "src\main\java\EveMinerAnalyzer.java"') do (
    set "VER_TEMP=%%a"
    REM Remove quotes and semicolon
    set "VER_TEMP=!VER_TEMP:"=!"
    set "VER_TEMP=!VER_TEMP:;=!"
    set "VERSION=!VER_TEMP!"
)
endlocal & set "VERSION=%VERSION%"
if "%VERSION%"=="" (
    echo Warning: Could not extract version, using default
    set "VERSION=unknown"
) else (
    echo Extracted version: %VERSION%
)

set "JAR_NAME=EveMinerAnalyzer-%VERSION%.jar"
set "TARGET_DIR=%PROJECT_ROOT%\target"
set "BUILD_DIR=%TARGET_DIR%\build"
set "SOURCE_FILE=%PROJECT_ROOT%\src\main\java\EveMinerAnalyzer.java"
set "MANIFEST_FILE=%SCRIPT_DIR%MANIFEST.MF"

echo Cleaning previous build...
REM Clean up old versioned JARs in target
for %%f in ("%TARGET_DIR%\EveMinerAnalyzer-*.jar") do (
    if exist "%%f" del /q "%%f" 2>nul
)
REM Also clean old non-versioned JAR if it exists
if exist "%TARGET_DIR%\EveMinerAnalyzer.jar" del "%TARGET_DIR%\EveMinerAnalyzer.jar"
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%" 2>nul
mkdir "%BUILD_DIR%" 2>nul

echo.
echo Compiling...
echo Source: %PROJECT_ROOT%\src\main\java\*.java
echo Output: %BUILD_DIR%
REM Collect all Java files recursively
setlocal enabledelayedexpansion
set "JAVA_FILES="
for /r "%PROJECT_ROOT%\src\main\java" %%f in (*.java) do (
    set "JAVA_FILES=!JAVA_FILES! "%%f""
)
REM Compile all Java files
javac -d "%BUILD_DIR%" -sourcepath "%PROJECT_ROOT%\src\main\java" !JAVA_FILES!
if !errorlevel! neq 0 (
    endlocal
    echo.
    echo Compilation failed!
    echo Please check that Java is installed and the source files exist.
    if "%1" neq "silent" pause
    exit /b 1
)
endlocal

echo.
echo Creating JAR file (version %VERSION%)...
if not exist "%MANIFEST_FILE%" (
    echo ERROR: Manifest file not found at %MANIFEST_FILE%
    if "%1" neq "silent" pause
    exit /b 1
)
cd "%BUILD_DIR%"
if not exist "%BUILD_DIR%\EveMinerAnalyzer.class" (
    echo ERROR: Compiled classes not found in %BUILD_DIR%
    cd "%PROJECT_ROOT%"
    if "%1" neq "silent" pause
    exit /b 1
)
jar cfm "%TARGET_DIR%\%JAR_NAME%" "%MANIFEST_FILE%" *
cd "%PROJECT_ROOT%"

if exist "%TARGET_DIR%\%JAR_NAME%" (
    echo.
    echo ========================================
    echo Build successful!
    echo ========================================
    echo.
    echo JAR file created: %TARGET_DIR%\%JAR_NAME%
    echo.
    echo To run: java -jar "%TARGET_DIR%\%JAR_NAME%"
    echo.
) else (
    echo.
    echo JAR creation failed!
    if "%1" neq "silent" pause
    exit /b 1
)

if "%1" neq "silent" pause

