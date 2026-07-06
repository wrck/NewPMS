@echo off
chcp 65001 >nul
title Vibe Service Management System - Launcher
setlocal EnableDelayedExpansion

:: ============================================================================
:: Vibe Service Management System - Launcher
::
:: Usage:
::   start.bat                 (no arg) -> interactive menu
::   start.bat all             -> start all services (middleware + backend + frontends)
::   start.bat middleware      -> start Docker middleware only
::   start.bat backend         -> start vibe-server only
::   start.bat frontends       -> start all frontends (web + mobile + portal)
::   start.bat web             -> start vibe-web only
::   start.bat mobile          -> start vibe-mobile only
::   start.bat portal          -> start vibe-portal only
::   start.bat stop            -> stop all services
::   start.bat status          -> show service status
::   start.bat restart         -> restart all services
::
:: All Chinese output is provided by start.ps1 (UTF-8 with BOM).
:: This .bat file uses English only to avoid cmd.exe GBK encoding issues.
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "PS1_SCRIPT=%PROJECT_ROOT%scripts\start.ps1"
set "ACTION=%~1"

echo ============================================================
echo      Vibe Service Management System - Launcher
echo ============================================================
echo.

:: --- Check PowerShell script exists ---
if not exist "%PS1_SCRIPT%" (
    echo [ERROR] PowerShell script not found: %PS1_SCRIPT%
    echo.
    pause
    exit /b 1
)

:: --- Build PowerShell arguments ---
set "PS_ARGS=-ExecutionPolicy Bypass -NoProfile -File "%PS1_SCRIPT%""
if /i not "%ACTION%"=="" (
    set "PS_ARGS=!PS_ARGS! "%ACTION%""
)

:: --- Launch PowerShell ---
if /i "%ACTION%"=="" (
    echo [INFO] Launching interactive menu...
) else (
    echo [INFO] Action: %ACTION%
)
echo.

powershell !PS_ARGS!
set "EXIT_CODE=%errorLevel%"

echo.
if not "%ACTION%"=="" (
    echo ============================================================
    if %EXIT_CODE% equ 0 (
        echo  Done.
    ) else (
        echo  Failed (exit code: %EXIT_CODE%)
    )
    echo ============================================================
    echo.
    pause
)

endlocal
exit /b %EXIT_CODE%
