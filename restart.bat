@echo off
chcp 65001 >nul
title Vibe Service Management System - Restart
setlocal

:: ============================================================================
:: Vibe 服务管理系统 - 重启全部服务快捷方式
:: 等价于: start.bat restart
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PS1_SCRIPT=%SCRIPT_DIR%scripts\start.ps1"

if not exist "%PS1_SCRIPT%" (
    echo [ERROR] 找不到 PowerShell 脚本: %PS1_SCRIPT%
    pause
    exit /b 1
)

echo ============================================================
echo      Vibe Service Management System - Restart All
echo ============================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1_SCRIPT%" "restart"

echo.
pause
endlocal
exit /b 0
