@echo off
REM ============================================================================
REM EVE Online Strip Miner Roll Analyzer - Single File Launcher
REM ============================================================================
REM This is a standalone launcher that can be shared as a single file.
REM It will automatically build the application if needed.
REM 
REM TO SHARE: Send this file along with the 'java' folder, or use
REM           create_executable.bat to create a single .exe file.
REM ============================================================================

REM Add JDK bin folder to PATH if not already there
set "JDK_PATH=C:\Program Files\Java\jdk-22\bin"
if exist "%JDK_PATH%\javac.exe" (
    set "PATH=%JDK_PATH%;%PATH%"
)

REM Get the directory where this batch file is located
set "LAUNCHER_DIR=%~dp0"
cd /d "%LAUNCHER_DIR%"

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

REM Check if we're in the java folder or root folder
if exist "java\EveMinerAnalyzer.java" (
    set "JAVA_DIR=%LAUNCHER_DIR%java"
    cd /d "%JAVA_DIR%"
) else if exist "EveMinerAnalyzer.java" (
    set "JAVA_DIR=%LAUNCHER_DIR%"
    cd /d "%JAVA_DIR%"
) else (
    echo.
    echo ========================================
    echo ERROR: Cannot find EveMinerAnalyzer.java
    echo ========================================
    echo.
    echo Please ensure this launcher is in the same folder as the Java files.
    echo.
    pause
    exit /b 1
)

REM Extract version from Java file
setlocal enabledelayedexpansion
set "VERSION=unknown"
for /f "tokens=7" %%a in ('findstr /C:"VERSION = " "%JAVA_DIR%\EveMinerAnalyzer.java"') do (
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
set "JAR_FILE=%JAVA_DIR%\EveMinerAnalyzer-%VERSION%.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=%JAVA_DIR%\EveMinerAnalyzer.jar"
)

REM Check if JAR exists, if not build it
if not exist "%JAR_FILE%" (
    echo.
    echo ========================================
    echo Building application (first time only)...
    echo ========================================
    echo This may take a few seconds...
    echo.
    
    REM Clean previous build
    if exist build rmdir /s /q build
    mkdir build 2>nul
    
    REM Compile
    echo Compiling...
    javac -d build "%JAVA_DIR%\EveMinerAnalyzer.java"
    if %errorlevel% neq 0 (
        echo.
        echo Compilation failed!
        echo Please check that all files are present.
        pause
        exit /b 1
    )
    
    REM Create JAR with versioned name
    set "BUILD_JAR_FILE=%JAVA_DIR%\EveMinerAnalyzer-%VERSION%.jar"
    echo Creating JAR file (version %VERSION%)...
    cd build
    if exist "%JAVA_DIR%\MANIFEST.MF" (
        jar cfm "%BUILD_JAR_FILE%" "%JAVA_DIR%\MANIFEST.MF" *
    ) else (
        REM Create minimal manifest if it doesn't exist
        echo Manifest-Version: 1.0 > "%JAVA_DIR%\temp_manifest.mf"
        echo Main-Class: EveMinerAnalyzer >> "%JAVA_DIR%\temp_manifest.mf"
        jar cfm "%BUILD_JAR_FILE%" "%JAVA_DIR%\temp_manifest.mf" *
        del "%JAVA_DIR%\temp_manifest.mf"
    )
    cd ..
    
    set "JAR_FILE=%BUILD_JAR_FILE%"
    if not exist "%JAR_FILE%" (
        echo.
        echo JAR creation failed!
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

