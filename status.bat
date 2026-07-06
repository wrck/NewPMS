@echo off
chcp 65001 >nul
title Vibe Service Management System - Status
setlocal

:: ============================================================================
:: Vibe 服务管理系统 - 状态查询快捷方式
:: 等价于: start.bat status
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PS1_SCRIPT=%SCRIPT_DIR%scripts\start.ps1"

if not exist "%PS1_SCRIPT%" (
    echo [ERROR] 找不到 PowerShell 脚本: %PS1_SCRIPT%
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1_SCRIPT%" "status"

echo.
pause
endlocal
exit /b 0
