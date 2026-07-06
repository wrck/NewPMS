﻿<#
.SYNOPSIS
    Vibe ServiceDeliver 回滚脚本

.DESCRIPTION
    从 backups/ 目录恢复指定版本的部署产物（jar + 前端静态资源）。
    支持：
      - 按版本号回滚：-Version v1.2.0（匹配 backups/<timestamp>_v1.2.0）
      - 回滚到最近一次备份：-Latest（默认）
    回滚完成后输出恢复路径与版本信息；如需重启服务，请另行执行 scripts/start.ps1。

.PARAMETER Version
    目标版本号（与 deploy.ps1 的 -Version 一致）

.PARAMETER Latest
    回滚到最近一次备份目录（默认行为）

.EXAMPLE
    .\scripts\rollback.ps1 -Latest
    .\scripts\rollback.ps1 -Version v1.1.0

.NOTES
    PowerShell 5.x 中文编码：脚本已保存为 UTF-8 with BOM
    作者: Vibe Team
#>

param(
    [string]$Version = '',

    [switch]$Latest
)

# 解决 PowerShell 5.x 中文输出乱码问题
$OutputEncoding = [System.Text.Encoding]::UTF8
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
try { $PSDefaultParameterValues['Out-File:Encoding'] = 'utf8' } catch { }

$ErrorActionPreference = 'Stop'

$ScriptPath = $MyInvocation.MyCommand.Definition
$ProjectRoot = Split-Path $ScriptPath -Parent | Split-Path -Parent
$BackupsRoot = Join-Path $ProjectRoot 'backups'
$DeployDir = Join-Path $ProjectRoot 'dist'
$VersionMarker = Join-Path $DeployDir '.version'

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet('Info', 'Success', 'Warning', 'Error', 'Step')]
        [string]$Level = 'Info'
    )
    $ts = Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'
    $colors = @{ Info = 'Cyan'; Success = 'Green'; Warning = 'Yellow'; Error = 'Red'; Step = 'Magenta' }
    $prefix = switch ($Level) {
        'Info'    { '[INFO]    ' }
        'Success' { '[SUCCESS] ' }
        'Warning' { '[WARN]    ' }
        'Error'   { '[ERROR]   ' }
        'Step'    { '[STEP]    ' }
    }
    Write-Host "[$ts] $prefix$Message" -ForegroundColor $colors[$Level]
}

function Find-Backup {
    param([string]$Version)
    if (-not (Test-Path $BackupsRoot)) {
        return $null
    }
    $dirs = Get-ChildItem -Path $BackupsRoot -Directory -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending
    if (-not $dirs) { return $null }
    if ($Version) {
        # 匹配 backups/<timestamp>_<version>
        $match = $dirs | Where-Object { $_.Name -like "*_$Version" -or $_.Name -eq $Version }
        if (-not $match) {
            # 兼容：直接全匹配
            $match = $dirs | Where-Object { $_.Name -like "*$Version*" }
        }
        if (-not $match) { return $null }
        return $match[0].FullName
    }
    # 默认：取最近一次（且包含 jar 或 web 资源）
    foreach ($d in $dirs) {
        $hasJar = Get-ChildItem -Path $d.FullName -Filter 'vibe-server*.jar' -ErrorAction SilentlyContinue
        $hasWeb = Test-Path (Join-Path $d.FullName 'web')
        if ($hasJar -or $hasWeb) { return $d.FullName }
    }
    return $null
}

function Restore-Backup {
    param([string]$BackupDir)
    Write-Log "回滚源：$BackupDir" -Level 'Info'

    if (-not (Test-Path $DeployDir)) {
        New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
    }

    # 1. 恢复 jar
    $jarFiles = Get-ChildItem -Path $BackupDir -Filter 'vibe-server*.jar' -ErrorAction SilentlyContinue
    if ($jarFiles) {
        # 清理现有 jar
        Get-ChildItem -Path $DeployDir -Filter 'vibe-server*.jar' -ErrorAction SilentlyContinue |
            ForEach-Object { Remove-Item $_.FullName -Force }
        foreach ($jar in $jarFiles) {
            Copy-Item $jar.FullName -Destination $DeployDir -Force
            Write-Log "  已恢复 jar: $($jar.Name)" -Level 'Info'
        }
    } else {
        Write-Log "  备份中无 jar 文件" -Level 'Warning'
    }

    # 2. 恢复前端静态资源
    $webBackup = Join-Path $BackupDir 'web'
    if (Test-Path $webBackup) {
        $targetWeb = Join-Path $DeployDir 'web'
        if (Test-Path $targetWeb) {
            Remove-Item -Path $targetWeb -Recurse -Force
        }
        Copy-Item $webBackup -Destination $targetWeb -Recurse -Force
        Write-Log "  已恢复前端资源: dist\web\" -Level 'Info'
    } else {
        Write-Log "  备份中无前端资源" -Level 'Warning'
    }

    # 3. 更新版本标记
    $dirName = Split-Path $BackupDir -Leaf
    $restoredVersion = if ($dirName -match '_(.+)$') { $matches[1] } else { $dirName }
    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    "$restoredVersion`n$timestamp (rollback)`nsource=$BackupDir" |
        Out-File -FilePath $VersionMarker -Encoding utf8 -Force
    Write-Log "  版本标记更新: $restoredVersion" -Level 'Info'
}

# ============================================================================
# 主流程
# ============================================================================

Write-Log "Vibe 回滚脚本启动" -Level 'Step'

# 决策模式：如果 -Latest 未传，但 -Version 也未传，则默认 -Latest
if (-not $Version -and -not $Latest) {
    $Latest = $true
    Write-Log "未指定 -Version，默认回滚到最近一次备份" -Level 'Info'
}

$target = Find-Backup -Version $Version
if (-not $target) {
    if ($Version) {
        Write-Log "未找到版本 $Version 的备份目录" -Level 'Error'
    } else {
        Write-Log "backups/ 目录下无可回滚的备份" -Level 'Error'
    }
    Write-Log "请通过 scripts\deploy.ps1 重新部署" -Level 'Info'
    exit 1
}

try {
    Restore-Backup -BackupDir $target
    Write-Log ""
    Write-Log "============================================" -Level 'Step'
    Write-Log "  回滚成功！" -Level 'Success'
    Write-Log "  恢复源: $target" -Level 'Info'
    Write-Log "  产物:   $DeployDir" -Level 'Info'
    Write-Log "============================================" -Level 'Step'
    Write-Log ""
    Write-Log "提示：如需重启服务，请执行 scripts\start.ps1" -Level 'Info'
} catch {
    Write-Log "回滚失败：$_" -Level 'Error'
    exit 1
}
