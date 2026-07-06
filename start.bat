@echo off
chcp 65001 >nul
title Vibe Service Management System - Launcher
setlocal EnableDelayedExpansion

:: ============================================================================
:: Vibe 服务管理系统 - 一键启动入口
:: 用法:
::   start.bat                 (无参数) -> 进入交互菜单
::   start.bat all             -> 一键启动全部
::   start.bat middleware      -> 仅启动 Docker 中间件
::   start.bat backend         -> 仅启动后端
::   start.bat frontends       -> 启动全部前端
::   start.bat web             -> 仅启动 vibe-web
::   start.bat mobile          -> 仅启动 vibe-mobile
::   start.bat portal          -> 仅启动 vibe-portal
::   start.bat stop            -> 停止全部服务
::   start.bat status          -> 查看服务状态
::   start.bat restart         -> 重启全部服务
:: ============================================================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "PS1_SCRIPT=%PROJECT_ROOT%scripts\start.ps1"
set "ACTION=%~1"

echo ============================================================
echo      Vibe Service Management System - Launcher
echo ============================================================
echo.

:: --- Self-elevate to Administrator (port management needs it) ---
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] 当前未以管理员身份运行，正在尝试提权...
    echo.
    powershell -Command "Start-Process -FilePath '%~f0' -ArgumentList '%ACTION%' -Verb RunAs"
    exit /b
)

:: --- Check PowerShell script exists ---
if not exist "%PS1_SCRIPT%" (
    echo [ERROR] 找不到 PowerShell 脚本: %PS1_SCRIPT%
    echo.
    pause
    exit /b 1
)

:: --- Build PowerShell arguments ---
set "PS_ARGS=-ExecutionPolicy Bypass -NoProfile -File \"%PS1_SCRIPT%\""
if /i not "%ACTION%"=="" (
    set "PS_ARGS=!PS_ARGS! \"%ACTION%\""
)

:: --- Launch PowerShell ---
if /i "%ACTION%"=="" (
    echo [INFO] 启动交互菜单...
) else (
    echo [INFO] 执行操作: %ACTION%
)
echo.

powershell !PS_ARGS!
set "EXIT_CODE=%errorLevel%"

echo.
if not "%ACTION%"=="" (
    echo ============================================================
    if %EXIT_CODE% equ 0 (
        echo  操作完成
    ) else (
        echo  操作失败 (退出码: %EXIT_CODE%)
    )
    echo ============================================================
    echo.
    pause
)

endlocal
exit /b %EXIT_CODE%
