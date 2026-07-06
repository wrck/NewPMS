@echo off
chcp 65001 >nul
title Vibe - Service Status
setlocal

:: ============================================================================
:: Vibe Service Management System - Show Service Status
:: Equivalent to: start.bat status
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PS1_SCRIPT=%SCRIPT_DIR%scripts\start.ps1"

if not exist "%PS1_SCRIPT%" (
    echo [ERROR] PowerShell script not found: %PS1_SCRIPT%
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1_SCRIPT%" "status"

echo.
pause
endlocal
exit /b 0
