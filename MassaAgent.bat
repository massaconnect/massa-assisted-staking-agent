@echo off
title Massa Assisted Staking Agent
echo Starting Massa Agent...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher from: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Run the JAR
echo Running MassaAgent...
echo.
java -jar "%SCRIPT_DIR%MassaAgent-windows-x64-1.0.0.jar"

pause
