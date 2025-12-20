@echo off
REM Add JDK bin folder to PATH if not already there
set "JDK_PATH=C:\Program Files\Java\jdk-22\bin"
if exist "%JDK_PATH%\jar.exe" (
    set "PATH=%JDK_PATH%;%PATH%"
)

echo ========================================
echo Building EVE Miner Analyzer
echo ========================================
echo.

REM Extract version from Java file
REM Find the VERSION line and extract the quoted value using simple batch parsing
setlocal enabledelayedexpansion
set "VERSION=unknown"
for /f "tokens=7" %%a in ('findstr /C:"VERSION = " EveMinerAnalyzer.java') do (
    set "VER_TEMP=%%a"
    REM Remove quotes and semicolon
    set "VERSION=!VER_TEMP:"=!"
    set "VERSION=!VERSION:;=!"
)
endlocal & set "VERSION=%VERSION%"
if "%VERSION%"=="" (
    echo Warning: Could not extract version, using default
    set "VERSION=unknown"
)

set "JAR_NAME=EveMinerAnalyzer-%VERSION%.jar"

echo Cleaning previous build...
REM Clean up old versioned JARs
for %%f in (EveMinerAnalyzer-*.jar) do del "%%f" 2>nul
REM Also clean old non-versioned JAR if it exists
if exist EveMinerAnalyzer.jar del EveMinerAnalyzer.jar
if exist build del /s /q build
mkdir build 2>nul

echo.
echo Compiling...
javac -d build EveMinerAnalyzer.java
if %errorlevel% neq 0 (
    echo.
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Creating JAR file (version %VERSION%)...
cd build
jar cfm ..\%JAR_NAME% ..\MANIFEST.MF *
cd ..

if exist %JAR_NAME% (
    echo.
    echo ========================================
    echo Build successful!
    echo ========================================
    echo.
    echo JAR file created: %JAR_NAME%
    echo.
    echo To run: java -jar %JAR_NAME%
    echo.
) else (
    echo.
    echo JAR creation failed!
    if "%1" neq "silent" pause
    exit /b 1
)

if "%1" neq "silent" pause

