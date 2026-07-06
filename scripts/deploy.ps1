﻿<#
.SYNOPSIS
    Vibe ServiceDeliver 一键部署脚本

.DESCRIPTION
    执行完整的部署流程：
      1. 备份当前部署产物（用于失败回滚）
      2. 构建后端 vibe-server.jar（mvn clean install -pl vibe-server-bootstrap -am）
      3. 构建前端 vibe-web（npm run build）
      4. 执行数据库迁移（node scripts/init-db.js）
      5. 部署产物到 dist/ 目录
      6. 健康检查（连续 3 次 GET /actuator/health 返回 200）
    任意步骤失败自动回滚到上一版本（从 backups/<timestamp>/ 恢复）。

.PARAMETER Env
    目标环境：dev / test / staging / prod（默认 dev）

.PARAMETER Version
    部署版本号（建议格式如 v1.2.0；不传则使用时间戳）

.EXAMPLE
    .\scripts\deploy.ps1 -Env dev -Version v1.2.0
    .\scripts\deploy.ps1 -Env prod -Version v1.2.0

.NOTES
    - PowerShell 5.x 中文编码：脚本已保存为 UTF-8 with BOM
    - 不重写 scripts/start.ps1；本脚本只做"构建+部署+健康检查"
    - 失败回滚策略：备份上一版本 jar/web 资源到 backups/<timestamp>/
    作者: Vibe Team
#>

param(
    [ValidateSet('dev', 'test', 'staging', 'prod')]
    [string]$Env = 'dev',

    [string]$Version = ''
)

# ============================================================================
# 全局变量与项目根目录
# ============================================================================

# 解决 PowerShell 5.x 中文输出乱码问题
$OutputEncoding = [System.Text.Encoding]::UTF8
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
try { $PSDefaultParameterValues['Out-File:Encoding'] = 'utf8' } catch { }

$ErrorActionPreference = 'Stop'

$ScriptPath = $MyInvocation.MyCommand.Definition
$ProjectRoot = Split-Path $ScriptPath -Parent | Split-Path -Parent

# 时间戳（用于备份目录命名）
$Timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
if (-not $Version) { $Version = "build-$Timestamp" }

# 关键路径
$BackendDir = Join-Path $ProjectRoot 'vibe-server'
$BootstrapDir = Join-Path $BackendDir 'vibe-server-bootstrap'
$WebDir = Join-Path $ProjectRoot 'vibe-web'
$BackupsRoot = Join-Path $ProjectRoot 'backups'
$DeployDir = Join-Path $ProjectRoot 'dist'
$LogsDir = Join-Path $ProjectRoot 'logs'
$InitDbScript = Join-Path $ProjectRoot 'scripts\init-db.js'

# 当前部署的备份目录（部署前创建）
$CurrentBackupDir = Join-Path $BackupsRoot "$Timestamp`_$Version"

# 部署成功后的版本标记文件
$VersionMarker = Join-Path $DeployDir '.version'

# 健康检查目标
$HealthUrl = 'http://localhost:8080/actuator/health'

# ============================================================================
# 工具函数
# ============================================================================

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet('Info', 'Success', 'Warning', 'Error', 'Step')]
        [string]$Level = 'Info'
    )
    $ts = Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'
    $colors = @{
        Info    = 'Cyan'
        Success = 'Green'
        Warning = 'Yellow'
        Error   = 'Red'
        Step    = 'Magenta'
    }
    $prefix = switch ($Level) {
        'Info'    { '[INFO]    ' }
        'Success' { '[SUCCESS] ' }
        'Warning' { '[WARN]    ' }
        'Error'   { '[ERROR]   ' }
        'Step'    { '[STEP]    ' }
    }
    Write-Host "[$ts] $prefix$Message" -ForegroundColor $colors[$Level]
}

function Test-Command {
    param([string]$Name, [scriptblock]$Checker)
    if (-not (& $Checker)) {
        Write-Log "缺少依赖：$Name" -Level 'Error'
        exit 1
    }
}

function Find-JavaHome {
    if ($env:VIBE_JAVA_HOME -and (Test-Path (Join-Path $env:VIBE_JAVA_HOME 'bin\java.exe'))) {
        return $env:VIBE_JAVA_HOME
    }
    $candidates = @(
        'D:\ja-netfilter\jdk-21.0.9+10',
        'D:\ja-netfilter\jdk-17.0.9',
        'C:\Program Files\Java'
    )
    foreach ($root in $candidates) {
        if (-not (Test-Path $root)) { continue }
        if (Test-Path (Join-Path $root 'bin\java.exe')) { return $root }
        $hits = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match '^(jdk-?)(17|18|19|2[0-9]|3[0-9])' } |
                Sort-Object Name -Descending
        if ($hits) {
            $javaExe = Join-Path $hits[0].FullName 'bin\java.exe'
            if (Test-Path $javaExe) { return $hits[0].FullName }
        }
    }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }
    return $null
}

function Find-MavenHome {
    if ($env:VIBE_MAVEN_HOME -and (Test-Path (Join-Path $env:VIBE_MAVEN_HOME 'bin\mvn.cmd'))) {
        return $env:VIBE_MAVEN_HOME
    }
    $candidates = @('E:\apache-maven-3.8.6', 'E:\apache-maven-3.9.6', 'C:\apache-maven', 'D:\dev\maven')
    foreach ($root in $candidates) {
        if (Test-Path (Join-Path $root 'bin\mvn.cmd')) { return $root }
    }
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        $binDir = Split-Path $mvnCmd.Source -Parent
        if ($binDir -match '\\bin$') { return (Split-Path $binDir -Parent) }
    }
    return $null
}

function Test-NodeAvailable {
    $nodeCmd = Get-Command node -ErrorAction SilentlyContinue
    if (-not $nodeCmd) { return $false }
    $ver = & node --version 2>&1 | Out-String
    if ($ver -match 'v(\d+)\.') { return [int]$matches[1] -ge 18 }
    return $false
}

function Invoke-Step {
    param([string]$StepName, [scriptblock]$Action)
    Write-Log ""
    Write-Log "==== $StepName ====" -Level 'Step'
    & $Action
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        throw "步骤失败：$StepName (exit=$LASTEXITCODE)"
    }
}

# ============================================================================
# 备份与回滚
# ============================================================================

function Backup-Current {
    Write-Log "备份当前部署产物到 $CurrentBackupDir ..." -Level 'Info'
    if (-not (Test-Path $BackupsRoot)) {
        New-Item -ItemType Directory -Path $BackupsRoot -Force | Out-Null
    }
    New-Item -ItemType Directory -Path $CurrentBackupDir -Force | Out-Null

    $hadArtifact = $false

    # 备份后端 jar
    if (Test-Path $DeployDir) {
        $existingJar = Get-ChildItem -Path $DeployDir -Filter 'vibe-server*.jar' -ErrorAction SilentlyContinue
        if ($existingJar) {
            foreach ($jar in $existingJar) {
                Copy-Item $jar.FullName -Destination (Join-Path $CurrentBackupDir $jar.Name) -Force
                $hadArtifact = $true
                Write-Log "  已备份 jar: $($jar.Name)" -Level 'Info'
            }
        }
        # 备份前端静态资源
        $existingWeb = Join-Path $DeployDir 'web'
        if (Test-Path $existingWeb) {
            Copy-Item $existingWeb -Destination (Join-Path $CurrentBackupDir 'web') -Recurse -Force
            $hadArtifact = $true
            Write-Log "  已备份前端资源: dist/web/" -Level 'Info'
        }
    }

    if (-not $hadArtifact) {
        Write-Log "  当前无可备份产物（首次部署）" -Level 'Warning'
        # 删除空备份目录避免回滚误用
        if (-not (Get-ChildItem -Path $CurrentBackupDir -Force)) {
            Remove-Item -Path $CurrentBackupDir -Force -Recurse
            return $null
        }
    }
    return $CurrentBackupDir
}

function Invoke-Rollback {
    param([string]$BackupDir)
    if (-not $BackupDir -or -not (Test-Path $BackupDir)) {
        Write-Log "无可用备份，无法回滚" -Level 'Error'
        return
    }
    Write-Log "回滚到备份：$BackupDir" -Level 'Warning'

    if (-not (Test-Path $DeployDir)) {
        New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
    }

    # 恢复 jar
    $jarFiles = Get-ChildItem -Path $BackupDir -Filter 'vibe-server*.jar' -ErrorAction SilentlyContinue
    foreach ($jar in $jarFiles) {
        Copy-Item $jar.FullName -Destination $DeployDir -Force
        Write-Log "  已恢复 jar: $($jar.Name)" -Level 'Info'
    }
    # 恢复前端资源
    $webBackup = Join-Path $BackupDir 'web'
    if (Test-Path $webBackup) {
        $targetWeb = Join-Path $DeployDir 'web'
        if (Test-Path $targetWeb) { Remove-Item -Path $targetWeb -Recurse -Force }
        Copy-Item $webBackup -Destination $targetWeb -Recurse -Force
        Write-Log "  已恢复前端资源" -Level 'Info'
    }
}

# ============================================================================
# 健康检查
# ============================================================================

function Wait-Health {
    param([int]$Attempts = 3, [int]$IntervalSec = 5, [int]$TimeoutSec = 120)
    Write-Log "健康检查：连续 $Attempts 次 $HealthUrl 返回 200" -Level 'Info'

    $elapsed = 0
    $successCount = 0
    while ($elapsed -lt $TimeoutSec) {
        try {
            $resp = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($resp.StatusCode -eq 200) {
                $successCount++
                Write-Log "  健康 ($successCount/$Attempts)" -Level 'Success'
                if ($successCount -ge $Attempts) {
                    Write-Log "健康检查通过" -Level 'Success'
                    return $true
                }
                Start-Sleep -Seconds $IntervalSec
                $elapsed += $IntervalSec
                continue
            }
        } catch {
            # 连接失败/超时/非 200 都视为失败
        }
        Write-Log "  健康检查未通过，等待 ${IntervalSec}s 后重试..." -Level 'Warning'
        Start-Sleep -Seconds $IntervalSec
        $elapsed += $IntervalSec
        $successCount = 0
    }
    return $false
}

# ============================================================================
# 主流程
# ============================================================================

# 强制步骤日志
function Main {
    Write-Log "Vibe 一键部署启动" -Level 'Step'
    Write-Log "  环境:   $Env" -Level 'Info'
    Write-Log "  版本:   $Version" -Level 'Info'
    Write-Log "  项目:   $ProjectRoot" -Level 'Info'

    # --- 环境探测 ---
    $javaHome = Find-JavaHome
    $mavenHome = Find-MavenHome
    $hasNode = Test-NodeAvailable

    if (-not $javaHome) {
        Write-Log "未检测到 JDK 17+，请通过 VIBE_JAVA_HOME 环境变量指定" -Level 'Error'
        exit 1
    }
    if (-not $mavenHome) {
        Write-Log "未检测到 Maven，请通过 VIBE_MAVEN_HOME 环境变量指定" -Level 'Error'
        exit 1
    }
    if (-not $hasNode) {
        Write-Log "未检测到 Node.js >= 18" -Level 'Error'
        exit 1
    }

    $env:JAVA_HOME = $javaHome
    $env:PATH = "$javaHome\bin;$mavenHome\bin;$env:PATH"
    $mvnCmd = Join-Path $mavenHome 'bin\mvn.cmd'

    Write-Log "  JDK:    $javaHome" -Level 'Info'
    Write-Log "  Maven:  $mavenHome" -Level 'Info'
    Write-Log "  Node:   $((node --version 2>&1 | Out-String).Trim())" -Level 'Info'

    # 确保日志目录存在
    if (-not (Test-Path $LogsDir)) {
        New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
    }

    # --- 步骤 0：备份当前产物 ---
    $backupDir = $null
    try {
        $backupDir = Backup-Current
    } catch {
        Write-Log "备份失败（继续部署）：$_" -Level 'Warning'
    }

    # --- 步骤 1：构建后端 ---
    Invoke-Step '构建后端 (mvn clean install)' {
        Write-Log "执行：mvn clean install -pl vibe-server-bootstrap -am -DskipTests" -Level 'Info'
        Push-Location $BackendDir
        try {
            & $mvnCmd clean install -pl vibe-server-bootstrap -am -DskipTests 2>&1 |
                Select-String -Pattern 'BUILD|ERROR|Compiling.*source|\[INFO\] Building jar' |
                ForEach-Object { Write-Log "  $_" -Level 'Info' }
            if ($LASTEXITCODE -ne 0) {
                throw "Maven 构建失败 exit=$LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
        Write-Log "后端构建完成" -Level 'Success'
    }

    # --- 步骤 2：构建前端 ---
    Invoke-Step '构建前端 (npm run build)' {
        if (-not (Test-Path (Join-Path $WebDir 'package.json'))) {
            throw "前端目录不存在或缺少 package.json: $WebDir"
        }
        if (-not (Test-Path (Join-Path $WebDir 'node_modules'))) {
            Write-Log "node_modules 缺失，执行 npm install ..." -Level 'Info'
            & npm.cmd install --prefix $WebDir --no-audit --no-fund 2>&1 |
                Select-Object -Pattern 'added|npm warn|npm error' |
                ForEach-Object { Write-Log "  $_" -Level 'Info' }
            if ($LASTEXITCODE -ne 0) { throw "npm install 失败 exit=$LASTEXITCODE" }
        }
        Write-Log "执行：npm run build" -Level 'Info'
        & npm.cmd run build --prefix $WebDir 2>&1 |
            Select-String -Pattern 'built|error|warn|vite|creating' |
            ForEach-Object { Write-Log "  $_" -Level 'Info' }
        if ($LASTEXITCODE -ne 0) { throw "前端构建失败 exit=$LASTEXITCODE" }
        Write-Log "前端构建完成" -Level 'Success'
    }

    # --- 步骤 3：数据库迁移 ---
    Invoke-Step '执行 DB 迁移 (init-db.js)' {
        if (-not (Test-Path $InitDbScript)) {
            Write-Log "scripts/init-db.js 不存在，跳过 DB 迁移" -Level 'Warning'
            return
        }
        & node $InitDbScript 2>&1 |
            ForEach-Object { Write-Log "  $_" -Level 'Info' }
        if ($LASTEXITCODE -ne 0) {
            throw "DB 迁移失败 exit=$LASTEXITCODE"
        }
        Write-Log "DB 迁移完成（幂等）" -Level 'Success'
    }

    # --- 步骤 4：部署产物 ---
    Invoke-Step '部署产物到 dist/' {
        if (-not (Test-Path $DeployDir)) {
            New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
        }

        # 4.1 部署后端 jar
        $jarPath = Join-Path $BootstrapDir "target\vibe-server.jar"
        if (-not (Test-Path $jarPath)) {
            throw "后端 jar 不存在：$jarPath"
        }
        Copy-Item $jarPath -Destination $DeployDir -Force
        Write-Log "  已部署 jar: dist\vibe-server.jar" -Level 'Info'

        # 4.2 部署前端静态资源
        $webDist = Join-Path $WebDir 'dist'
        if (-not (Test-Path $webDist)) {
            throw "前端构建产物不存在：$webDist"
        }
        $targetWeb = Join-Path $DeployDir 'web'
        if (Test-Path $targetWeb) { Remove-Item -Path $targetWeb -Recurse -Force }
        Copy-Item $webDist -Destination $targetWeb -Recurse -Force
        Write-Log "  已部署前端资源: dist\web\" -Level 'Info'

        # 4.3 写入版本标记
        "$Version`n$Timestamp`nenv=$Env" | Out-File -FilePath $VersionMarker -Encoding utf8 -Force
        Write-Log "  版本标记写入 .version" -Level 'Info'

        Write-Log "部署完成" -Level 'Success'
    }

    # --- 步骤 5：健康检查 ---
    Invoke-Step '健康检查' {
        $ok = Wait-Health -Attempts 3 -IntervalSec 5 -TimeoutSec 120
        if (-not $ok) {
            throw "健康检查未通过"
        }
    }

    Write-Log ""
    Write-Log "============================================" -Level 'Step'
    Write-Log "  部署成功！" -Level 'Success'
    Write-Log "  版本: $Version" -Level 'Info'
    Write-Log "  环境: $Env" -Level 'Info'
    Write-Log "  产物: $DeployDir" -Level 'Info'
    Write-Log "  备份: $CurrentBackupDir" -Level 'Info'
    Write-Log "============================================" -Level 'Step'
}

# 失败时自动回滚
function Run-WithRollback {
    try {
        Main
    } catch {
        Write-Log ""
        Write-Log "部署失败：$_" -Level 'Error'
        Write-Log "正在回滚到上一版本..." -Level 'Warning'
        if ($CurrentBackupDir -and (Test-Path $CurrentBackupDir)) {
            Invoke-Rollback -BackupDir $CurrentBackupDir
            Write-Log "回滚完成，已恢复上一版本产物" -Level 'Warning'
        } else {
            Write-Log "无可用备份，无法回滚（首次部署或备份失败）" -Level 'Error'
        }
        Write-Log "请查看 logs\deploy.log 获取详细日志" -Level 'Info'
        exit 1
    }
}

Run-WithRollback
