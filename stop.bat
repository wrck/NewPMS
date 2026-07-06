@echo off
chcp 65001 >nul
title Vibe Service Management System - Stop All Services
setlocal EnableDelayedExpansion

:: ============================================================================
:: Vibe 服务管理系统 - 一键停止全部服务
::   1) 读取 .vibe-pids.json，按 PID 树形终止进程
::   2) 端口兜底清理 (8080/5173/5174/5175)
::   3) 关闭 Docker 容器
:: ============================================================================

set "PROJECT_ROOT=%~dp0"
set "PID_FILE=%PROJECT_ROOT%.vibe-pids.json"
set "DC_FILE=%PROJECT_ROOT%docker-compose.yml"

echo ============================================================
echo      Vibe Service Management System - Stop All Services
echo ============================================================
echo.

:: --- Self-elevate to Administrator (taskkill /T needs it) ---
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] 当前未以管理员身份运行，正在尝试提权...
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo [1/4] 停止前端应用...
if exist "%PID_FILE%" (
    powershell -NoProfile -Command ^
        "$p = Get-Content '%PID_FILE%' -Raw | ConvertFrom-Json;" ^
        "foreach ($k in @('web','mobile','portal')) {" ^
        "  if ($p.$k) {" ^
        "    $proc = Get-Process -Id $p.$k -ErrorAction SilentlyContinue;" ^
        "    if ($proc) {" ^
        "      Write-Host ('  Stopping ' + $k + ' (PID: ' + $p.$k + ')');" ^
        "      taskkill /F /T /PID $p.$k | Out-Null;" ^
        "    } else { Write-Host ('  ' + $k + ' already stopped'); }" ^
        "  }" ^
        "}"
) else (
    echo   PID 文件不存在，跳过按 PID 停止
)
:: Port-based fallback for frontends
for %%P in (5173 5174 5175) do (
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr ":%%P " ^| findstr LISTENING 2^>nul') do (
        echo   端口 %%P 仍被占用，终止 PID %%A
        taskkill /F /T /PID %%A >nul 2>&1
    )
)
echo.

echo [2/4] 停止后端服务 (vibe-server)...
if exist "%PID_FILE%" (
    powershell -NoProfile -Command ^
        "$p = Get-Content '%PID_FILE%' -Raw | ConvertFrom-Json;" ^
        "if ($p.backend) {" ^
        "  $proc = Get-Process -Id $p.backend -ErrorAction SilentlyContinue;" ^
        "  if ($proc) {" ^
        "    Write-Host ('  Stopping backend (PID: ' + $p.backend + ')');" ^
        "    taskkill /F /T /PID $p.backend | Out-Null;" ^
        "  } else { Write-Host '  backend already stopped'; }" ^
        "}"
)
:: Port-based fallback for backend
for /f "tokens=5" %%A in ('netstat -ano ^| findstr ":8080 " ^| findstr LISTENING 2^>nul') do (
    echo   端口 8080 仍被占用，终止 PID %%A
    taskkill /F /T /PID %%A >nul 2>&1
)
echo.

echo [3/4] 停止 Docker 容器...
where docker >nul 2>&1
if %errorLevel% neq 0 (
    echo   Docker 未安装，跳过
) else if not exist "%DC_FILE%" (
    echo   docker-compose.yml 不存在，跳过
) else (
    docker compose -f "%DC_FILE%" down
    echo   Docker 容器已停止
)
echo.

echo [4/4] 清理 PID 文件...
if exist "%PID_FILE%" (
    del "%PID_FILE%"
    echo   已删除 %PID_FILE%
) else (
    echo   无 PID 文件需要清理
)
echo.

echo ============================================================
echo  全部服务已停止
echo ============================================================
echo.
pause
endlocal
exit /b 0
