@echo off
chcp 65001 >nul
title Vibe - Stop All Services
setlocal

:: ============================================================================
:: Vibe Service Management System - Stop All Services
:: Equivalent to: start.bat stop
:: All stop logic (PID tree-kill + port cleanup + Docker) is in start.ps1
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PS1_SCRIPT=%SCRIPT_DIR%scripts\start.ps1"

if not exist "%PS1_SCRIPT%" (
    echo [ERROR] PowerShell script not found: %PS1_SCRIPT%
    echo.
    pause
    exit /b 1
)

echo ============================================================
echo      Vibe - Stop All Services
echo ============================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1_SCRIPT%" "stop"
set "EXIT_CODE=%errorLevel%"

echo.
pause
endlocal
exit /b %EXIT_CODE%
