@echo off
chcp 65001 >nul
title Vibe - Restart All Services
setlocal

:: ============================================================================
:: Vibe Service Management System - Restart All Services
:: Equivalent to: start.bat restart
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PS1_SCRIPT=%SCRIPT_DIR%scripts\start.ps1"

if not exist "%PS1_SCRIPT%" (
    echo [ERROR] PowerShell script not found: %PS1_SCRIPT%
    pause
    exit /b 1
)

echo ============================================================
echo      Vibe - Restart All Services
echo ============================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1_SCRIPT%" "restart"
set "EXIT_CODE=%errorLevel%"

echo.
pause
endlocal
exit /b %EXIT_CODE%
