<#
.SYNOPSIS
    Vibe Service Management System - One-click Start/Stop Script

.DESCRIPTION
    Menu-driven + command-line script to manage Vibe services:
      - Docker middleware (MySQL/Redis/MinIO/RabbitMQ/ES/XXL-JOB)
      - Spring Boot backend (vibe-server, JDK 17)
      - Vue frontends (vibe-web / vibe-mobile / vibe-portal)

    Gracefully degrades when optional dependencies are missing:
      - No Docker    -> skip middleware, still start backend/frontends
      - No JDK 17    -> skip backend, still start frontends
      - No Node.js   -> skip frontends

.NOTES
    Requires (any one is enough to do something useful):
      - Docker Desktop          (for middleware)
      - JDK 17+                 (for backend)
      - Node.js >= 18           (for frontends)

    Author: Vibe Team
#>

param(
    [string]$Action = ""
)

# ============================================================================
# Environment detection
# ============================================================================

# Project root = parent of scripts/ directory
$ProjectRoot = Split-Path $MyInvocation.MyCommand.Definition -Parent | Split-Path -Parent

# --- Detect JDK 17+ (priority: env var VIBE_JAVA_HOME > JAVA_HOME > common paths > PATH) ---
function Find-JavaHome {
    # 1. Explicit override
    if ($env:VIBE_JAVA_HOME -and (Test-Path (Join-Path $env:VIBE_JAVA_HOME "bin\java.exe"))) {
        return $env:VIBE_JAVA_HOME
    }
    # 2. Common JDK 17+ install locations
    $candidateRoots = @(
        "D:\ja-netfilter",
        "D:\dev\jdk",
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Amazon Corretto",
        "C:\Program Files\Zulu",
        "C:\Program Files\BellSoft",
        "C:\Program Files (x86)\Java"
    )
    foreach ($root in $candidateRoots) {
        if (-not (Test-Path $root)) { continue }
        # Match directories named jdk-17, jdk-17.0.x, jdk-18+, jdk-21, etc.
        $hits = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match '^(jdk-?)(17|18|19|2[0-9]|3[0-9])' } |
                Sort-Object Name -Descending
        if ($hits) {
            $javaExe = Join-Path $hits[0].FullName "bin\java.exe"
            if (Test-Path $javaExe) { return $hits[0].FullName }
        }
    }
    # 3. JAVA_HOME env var if it points to JDK 17+
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        $ver = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Out-String
        if ($ver -match 'version "(17|18|19|2[0-9]|3[0-9])') { return $env:JAVA_HOME }
    }
    # 4. java on PATH
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $ver = & java -version 2>&1 | Out-String
        if ($ver -match 'version "(17|18|19|2[0-9]|3[0-9])') {
            # java on PATH -> derive JAVA_HOME
            $binDir = Split-Path $javaCmd.Source -Parent
            if ($binDir -match '\\bin$') { return (Split-Path $binDir -Parent) }
        }
    }
    return $null
}

# --- Detect Maven (priority: env var VIBE_MAVEN_HOME > MAVEN_HOME > common paths > PATH) ---
function Find-MavenHome {
    if ($env:VIBE_MAVEN_HOME -and (Test-Path (Join-Path $env:VIBE_MAVEN_HOME "bin\mvn.cmd"))) {
        return $env:VIBE_MAVEN_HOME
    }
    if ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME "bin\mvn.cmd"))) {
        return $env:MAVEN_HOME
    }
    $candidateRoots = @(
        "E:\apache-maven-3.8.6",
        "E:\apache-maven-3.9.6",
        "C:\apache-maven",
        "C:\Program Files\Apache\Maven",
        "D:\dev\maven"
    )
    foreach ($root in $candidateRoots) {
        if (Test-Path (Join-Path $root "bin\mvn.cmd")) { return $root }
    }
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        $binDir = Split-Path $mvnCmd.Source -Parent
        if ($binDir -match '\\bin$') { return (Split-Path $binDir -Parent) }
    }
    return $null
}

# --- Detect Node.js ---
function Test-NodeAvailable {
    $nodeCmd = Get-Command node -ErrorAction SilentlyContinue
    if (-not $nodeCmd) { return $false }
    $ver = & node --version 2>&1 | Out-String
    if ($ver -match 'v(\d+)\.') {
        return [int]$matches[1] -ge 18
    }
    return $false
}

# --- Detect Docker ---
function Test-DockerAvailable {
    $dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $dockerCmd) { return $false }
    docker info *> $null
    return ($LASTEXITCODE -eq 0)
}

# ============================================================================
# Configuration
# ============================================================================

$DetectedJAVA_HOME = Find-JavaHome
$DetectedMAVEN_HOME = Find-MavenHome
$HasNode = Test-NodeAvailable
$HasDocker = Test-DockerAvailable

# --- 加载集中端口配置 (.env.ports) ---
# 解析 KEY=VALUE 格式，跳过空行与 # 注释；不覆盖已存在的环境变量
# 这样允许用户用 `set VIBE_WEB_PORT=6001` 临时覆盖默认值
function Import-PortConfig {
    param([string]$Path)
    $portsFile = Join-Path $ProjectRoot ".env.ports"
    if (-not (Test-Path $portsFile)) { return }
    Get-Content $portsFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -le 0) { return }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        # 仅当环境变量未设置时才注入（已有环境变量优先）
        if (-not (Get-Item -Path "Env:$key" -ErrorAction SilentlyContinue)) {
            Set-Item -Path "Env:$key" -Value $val
        }
    }
}
Import-PortConfig

# 端口解析：环境变量 > .env.ports > 内置默认值
# VIBE_SERVER_PORT: 后端 Spring Boot server.port
# VIBE_WEB_PORT/VIBE_MOBILE_PORT/VIBE_PORTAL_PORT: 前端 vite dev server
$Config = @{
    ProjectRoot  = $ProjectRoot
    DockerFile   = Join-Path $ProjectRoot "docker-compose.yml"
    PidFile      = Join-Path $ProjectRoot ".vibe-pids.json"
    JAVA_HOME    = $DetectedJAVA_HOME
    MAVEN_HOME   = $DetectedMAVEN_HOME
    MavenCmd     = if ($DetectedMAVEN_HOME) { Join-Path $DetectedMAVEN_HOME "bin\mvn.cmd" } else { "mvn" }
    BackendDir   = Join-Path $ProjectRoot "vibe-server"
    WebDir       = Join-Path $ProjectRoot "vibe-web"
    MobileDir    = Join-Path $ProjectRoot "vibe-mobile"
    PortalDir    = Join-Path $ProjectRoot "vibe-portal"
    BackendPort  = if ($env:VIBE_SERVER_PORT) { [int]$env:VIBE_SERVER_PORT } else { 8080 }
    WebPort      = if ($env:VIBE_WEB_PORT)    { [int]$env:VIBE_WEB_PORT }    else { 5173 }
    MobilePort   = if ($env:VIBE_MOBILE_PORT) { [int]$env:VIBE_MOBILE_PORT } else { 5174 }
    PortalPort   = if ($env:VIBE_PORTAL_PORT) { [int]$env:VIBE_PORTAL_PORT } else { 5175 }
    HasNode      = $HasNode
    HasDocker    = $HasDocker
}

# ============================================================================
# Helpers
# ============================================================================

function Write-Status {
    param([string]$Message, [string]$Status = "Info", [switch]$NoNewline)
    $colors = @{ Success = "Green"; Error = "Red"; Warning = "Yellow"; Info = "Cyan"; Header = "Magenta"; Sub = "DarkGray" }
    if ($NoNewline) {
        Write-Host $Message -ForegroundColor $colors[$Status] -NoNewline
    } else {
        Write-Host $Message -ForegroundColor $colors[$Status]
    }
}

function Test-Port {
    param([int]$Port)
    try {
        $tcp = New-Object System.Net.Sockets.TCPClient
        # 200ms timeout for fast probing
        $iar = $tcp.BeginConnect("localhost", $Port, $null, $null)
        $success = $iar.AsyncWaitHandle.WaitOne(200)
        if ($success) { $tcp.EndConnect($iar); $tcp.Close(); return $true }
        $tcp.Close()
        return $false
    } catch { return $false }
}

function Clear-Port {
    param([int]$Port)
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { return $true }
    $ownerIds = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $ownerIds) {
        $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Status "  端口 $Port 被 $($proc.Name) (PID: $pid) 占用，将终止..." -Status Warning
            # Tree-kill to handle mvn -> java, npm -> node child processes
            & taskkill /F /T /PID $pid *> $null
            Start-Sleep -Seconds 2
        }
    }
    if (Test-Port $Port) {
        Write-Status "  端口 $Port 仍被占用，请手动释放" -Status Error
        return $false
    }
    return $true
}

function Save-Pids {
    param([hashtable]$Pids)
    $Pids | ConvertTo-Json | Out-File $Config.PidFile -Encoding utf8 -Force
}

function Load-Pids {
    if (Test-Path $Config.PidFile) {
        try { return Get-Content $Config.PidFile -Raw | ConvertFrom-Json }
        catch { return $null }
    }
    return $null
}

function Remove-Pids {
    if (Test-Path $Config.PidFile) { Remove-Item $Config.PidFile -Force }
}

function Update-Pid {
    param([string]$Key, [int]$PidValue, [switch]$Remove)
    $pids = @{}
    $loaded = Load-Pids
    if ($loaded) {
        foreach ($prop in $loaded.PSObject.Properties) { $pids[$prop.Name] = $prop.Value }
    }
    if ($Remove) {
        $pids.Remove($Key) | Out-Null
    } else {
        $pids[$Key] = $PidValue
    }
    if ($pids.Count -gt 0) { Save-Pids -Pids $pids }
    else { Remove-Pids }
}

# Tree-kill a process and all its children (more reliable than Stop-Process for mvn/npm)
function Stop-ProcessTree {
    param([int]$ProcessId, [string]$Label)
    if (-not $ProcessId) { return }
    $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if (-not $proc) {
        Write-Status "  $Label (PID: $ProcessId) 已不在运行" -Status Sub
        return
    }
    Write-Status "  正在停止 $Label (PID: $ProcessId)..." -Status Info
    & taskkill /F /T /PID $ProcessId *> $null
    Start-Sleep -Seconds 1
    Write-Status "  $Label 已停止" -Status Success
}

# ============================================================================
# Service control functions
# ============================================================================

function Start-Middleware {
    Write-Status ""
    Write-Status "========== 启动 Docker 中间件 ==========" -Status Header

    if (-not $Config.HasDocker) {
        Write-Status "[跳过] Docker Desktop 未安装或未运行" -Status Warning
        Write-Status "  请安装 Docker Desktop: https://www.docker.com/products/docker-desktop/" -Status Sub
        Write-Status "  安装后启动 Docker Desktop，再次运行本脚本" -Status Sub
        return $false
    }

    if (-not (Test-Path $Config.DockerFile)) {
        Write-Status "[错误] docker-compose.yml 不存在: $($Config.DockerFile)" -Status Error
        return $false
    }

    Write-Status "  拉取镜像并启动容器（首次启动需要 60-120 秒）..." -Status Info
    docker compose -f $Config.DockerFile up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Status "[错误] Docker 容器启动失败" -Status Error
        return $false
    }

    Write-Status ""
    Write-Status "  等待健康检查..." -Status Info
    $services = @("mysql", "redis", "minio", "rabbitmq", "elasticsearch", "xxl-job-admin")
    $timeout = 180
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        $healthy = 0
        $states = @{}
        foreach ($svc in $services) {
            $cid = docker compose -f $Config.DockerFile ps -q $svc 2>$null
            if ($cid) {
                $status = docker inspect -f "{{.State.Health.Status}}" $cid 2>$null
                $states[$svc] = $status
                if ($status -eq "healthy") { $healthy++ }
            } else {
                $states[$svc] = "missing"
            }
        }
        if ($healthy -eq $services.Count) {
            Write-Status "  全部中间件就绪 ($healthy/$($services.Count))" -Status Success
            return $true
        }
        $detail = $services | ForEach-Object { "$_=$($states[$_])" } | Join-String -sep ", "
        Write-Status "  等待中... ($healthy/$($services.Count)) [$detail]" -Status Sub
        Start-Sleep -Seconds 5
        $elapsed += 5
    }
    Write-Status "[警告] 部分中间件健康检查超时（$elapsed 秒），但服务可能仍可用" -Status Warning
    return $true
}

function Stop-Middleware {
    Write-Status ""
    Write-Status "========== 停止 Docker 中间件 ==========" -Status Header
    if (-not $Config.HasDocker) {
        Write-Status "[跳过] Docker 未安装" -Status Sub
        return
    }
    if (-not (Test-Path $Config.DockerFile)) {
        Write-Status "[跳过] docker-compose.yml 不存在" -Status Sub
        return
    }
    docker compose -f $Config.DockerFile down
    Write-Status "  中间件已停止" -Status Success
}

function Start-Backend {
    Write-Status ""
    Write-Status "========== 启动后端服务 (vibe-server) ==========" -Status Header

    if (-not $Config.JAVA_HOME) {
        Write-Status "[跳过] 未检测到 JDK 17+" -Status Warning
        Write-Status "  Spring Boot 3.x 需要 JDK 17 或更高版本" -Status Sub
        Write-Status "  请安装 JDK 17，或通过环境变量 VIBE_JAVA_HOME 指定路径" -Status Sub
        Write-Status "  推荐下载: https://adoptium.net/temurin/releases/?version=17" -Status Sub
        return $false
    }

    $javaExe = Join-Path $Config.JAVA_HOME "bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        Write-Status "[错误] Java 可执行文件不存在: $javaExe" -Status Error
        return $false
    }

    if (-not $Config.MAVEN_HOME) {
        Write-Status "[跳过] 未检测到 Maven" -Status Warning
        Write-Status "  请安装 Maven，或通过环境变量 VIBE_MAVEN_HOME 指定路径" -Status Sub
        return $false
    }

    Write-Status "  JDK:     $($Config.JAVA_HOME)" -Status Sub
    Write-Status "  Maven:   $($Config.MAVEN_HOME)" -Status Sub

    # Check if backend already running
    $pids = Load-Pids
    if ($pids -and $pids.backend) {
        $existing = Get-Process -Id $pids.backend -ErrorAction SilentlyContinue
        if ($existing -and (Test-Port $Config.BackendPort)) {
            Write-Status "[跳过] 后端已在运行 (PID: $($pids.backend), 端口: $($Config.BackendPort))" -Status Warning
            return $true
        }
    }

    if (-not (Clear-Port -Port $Config.BackendPort)) { return $false }

    $env:JAVA_HOME = $Config.JAVA_HOME
    # Set PATH so mvn.cmd can find java
    $env:PATH = "$($Config.JAVA_HOME)\bin;$($Config.MAVEN_HOME)\bin;$env:PATH"

    # Step 1: install all modules to local repo so bootstrap can resolve them.
    $BootstrapDir = Join-Path $Config.BackendDir "vibe-server-bootstrap"
    $installMarker = Join-Path $ProjectRoot ".vibe-mvn-installed"
    if (-not (Test-Path $installMarker)) {
        Write-Status "  首次启动：编译并安装所有模块到本地仓库（含 clean，约 2-5 分钟）..." -Status Info
        Push-Location $Config.BackendDir
        try {
            & $Config.MavenCmd -q -DskipTests -pl vibe-server-bootstrap -am clean install 2>&1 | Out-Null
            $installExit = $LASTEXITCODE
            if ($installExit -ne 0) {
                Write-Status "  静默安装失败，使用详细模式重试..." -Status Warning
                & $Config.MavenCmd -DskipTests -pl vibe-server-bootstrap -am clean install -U 2>&1 |
                    Select-String -Pattern "BUILD|ERROR" | ForEach-Object { Write-Status "  $_" -Status Sub }
                $installExit = $LASTEXITCODE
            }
        } finally {
            Pop-Location
        }
        if ($installExit -ne 0) {
            Write-Status "[错误] Maven install 失败，请手动执行: cd vibe-server; mvn clean install -DskipTests" -Status Error
            return $false
        }
        New-Item -ItemType File -Path $installMarker -Force | Out-Null
        Write-Status "  模块安装完成" -Status Success
    }

    # Step 2: run spring-boot:run from the bootstrap directory
    $mvnArgs = @("-q", "-DskipTests", "spring-boot:run")
    Write-Status "  启动 Spring Boot (端口 $($Config.BackendPort))..." -Status Info

    $proc = Start-Process -FilePath $Config.MavenCmd `
                          -ArgumentList $mvnArgs `
                          -WorkingDirectory $BootstrapDir `
                          -PassThru -NoNewWindow `
                          -RedirectStandardOutput (Join-Path $ProjectRoot "logs\backend.out.log") `
                          -RedirectStandardError (Join-Path $ProjectRoot "logs\backend.err.log")

    Update-Pid -Key "backend" -PidValue $proc.Id
    Write-Status "  后端进程已启动 (PID: $($proc.Id))，日志: logs\backend.out.log" -Status Success

    Write-Status "  等待端口监听..." -Status Info
    for ($i = 0; $i -lt 40; $i++) {
        if (Test-Port $Config.BackendPort) {
            Write-Status "  后端就绪! API: http://localhost:$($Config.BackendPort)" -Status Success
            Write-Status "  API 文档: http://localhost:$($Config.BackendPort)/doc.html" -Status Sub
            return $true
        }
        # Bail out early if process died
        $alive = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
        if (-not $alive) {
            Write-Status "[错误] 后端进程已退出，请查看日志: logs\backend.err.log" -Status Error
            Update-Pid -Key "backend" -Remove
            return $false
        }
        Start-Sleep -Seconds 3
    }
    Write-Status "[警告] 后端启动超时（120 秒），可能仍在初始化中" -Status Warning
    Write-Status "  查看日志: logs\backend.out.log" -Status Sub
    return $true
}

function Stop-Backend {
    $pids = Load-Pids
    if ($pids -and $pids.backend) {
        Stop-ProcessTree -ProcessId $pids.backend -Label "后端服务"
        Update-Pid -Key "backend" -Remove
    } else {
        # No PID file, try port-based cleanup
        $conns = Get-NetTCPConnection -LocalPort $Config.BackendPort -State Listen -ErrorAction SilentlyContinue
        if ($conns) {
            foreach ($pid in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
                Stop-ProcessTree -ProcessId $pid -Label "后端服务 (按端口 $($Config.BackendPort) 发现)"
            }
        } else {
            Write-Status "  后端未在运行" -Status Sub
        }
    }
}

function Start-Frontend {
    param([string]$Name, [string]$Dir, [int]$Port)
    Write-Status ""
    Write-Status "========== 启动 $Name (端口 $Port) ==========" -Status Header

    if (-not $Config.HasNode) {
        Write-Status "[跳过] Node.js 未安装或版本低于 18" -Status Warning
        Write-Status "  请安装 Node.js >= 18: https://nodejs.org/" -Status Sub
        return $false
    }

    if (-not (Test-Path (Join-Path $Dir "package.json"))) {
        Write-Status "[错误] 目录不存在或缺少 package.json: $Dir" -Status Error
        return $false
    }

    # Check if already running
    $pids = Load-Pids
    $key = $Name.ToLower()
    if ($pids -and $pids.$key) {
        $existing = Get-Process -Id $pids.$key -ErrorAction SilentlyContinue
        if ($existing -and (Test-Port $Port)) {
            Write-Status "[跳过] $Name 已在运行 (PID: $($pids.$key))" -Status Warning
            return $true
        }
    }

    # Install dependencies if node_modules missing
    if (-not (Test-Path (Join-Path $Dir "node_modules"))) {
        Write-Status "  node_modules 不存在，执行 npm install..." -Status Info
        # On Windows npm is a .cmd batch script; must invoke npm.cmd explicitly
        $npmInstallCmd = if ($env:OS -eq "Windows_NT") { "npm.cmd" } else { "npm" }
        & $npmInstallCmd install --prefix $Dir --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            Write-Status "[错误] npm install 失败: $Dir" -Status Error
            return $false
        }
    }

    if (-not (Clear-Port -Port $Port)) { return $false }

    # 把端口注入为 VITE_PORT，vite.config.ts 据此设置 dev server 监听端口
    # 优先用脚本计算的 Port（已合并环境变量与 .env.ports），不覆盖用户已显式设置的 VITE_PORT
    if (-not $env:VITE_PORT) {
        $env:VITE_PORT = "$Port"
    }

    Write-Status "  启动 $Name..." -Status Info
    # On Windows npm is a .cmd batch script; must invoke npm.cmd explicitly
    $npmCmd = if ($IsWindows -or $env:OS -eq "Windows_NT") { "npm.cmd" } else { "npm" }
    $proc = Start-Process -FilePath $npmCmd `
                          -ArgumentList @("run", "dev") `
                          -WorkingDirectory $Dir `
                          -PassThru -NoNewWindow `
                          -RedirectStandardOutput (Join-Path $ProjectRoot "logs\$key.out.log") `
                          -RedirectStandardError (Join-Path $ProjectRoot "logs\$key.err.log")

    # 启动后清掉本进程设置的 VITE_PORT，避免污染下一个前端启动
    # （每个前端启动前都会根据自身 Port 重新注入）
    Remove-Item -Path "Env:VITE_PORT" -ErrorAction SilentlyContinue

    Update-Pid -Key $key -PidValue $proc.Id
    Write-Status "  $Name 进程已启动 (PID: $($proc.Id))" -Status Success

    Write-Status "  等待端口监听..." -Status Info
    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Port $Port) {
            Write-Status "  $Name 就绪! http://localhost:$Port" -Status Success
            return $true
        }
        $alive = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
        if (-not $alive) {
            Write-Status "[错误] $Name 进程已退出，请查看日志: logs\$key.err.log" -Status Error
            Update-Pid -Key $key -Remove
            return $false
        }
        Start-Sleep -Seconds 2
    }
    Write-Status "[警告] $Name 启动超时（60 秒），可能仍在编译中" -Status Warning
    return $true
}

function Stop-Frontend {
    param([string]$Name, [int]$Port)
    $pids = Load-Pids
    $key = $Name.ToLower()
    if ($pids -and $pids.$key) {
        Stop-ProcessTree -ProcessId $pids.$key -Label $Name
        Update-Pid -Key $key -Remove
    } else {
        $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($conns) {
            foreach ($pid in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
                Stop-ProcessTree -ProcessId $pid -Label "$Name (按端口 $Port 发现)"
            }
        } else {
            Write-Status "  $Name 未在运行" -Status Sub
        }
    }
}

function Start-AllFrontends {
    $result = $true
    if (Test-Path $Config.WebDir)    { if (-not (Start-Frontend -Name "web"    -Dir $Config.WebDir    -Port $Config.WebPort))    { $result = $false } }
    if (Test-Path $Config.MobileDir) { if (-not (Start-Frontend -Name "mobile" -Dir $Config.MobileDir -Port $Config.MobilePort)) { $result = $false } }
    if (Test-Path $Config.PortalDir) { if (-not (Start-Frontend -Name "portal" -Dir $Config.PortalDir -Port $Config.PortalPort)) { $result = $false } }
    return $result
}

function Stop-AllFrontends {
    Stop-Frontend -Name "web"    -Port $Config.WebPort
    Stop-Frontend -Name "mobile" -Port $Config.MobilePort
    Stop-Frontend -Name "portal" -Port $Config.PortalPort
}

function Start-All {
    Write-Status ""
    Write-Status "########## 一键启动全部服务 ##########" -Status Header
    Write-Status "  启动顺序: 中间件 -> 后端 -> 前端" -Status Sub
    $mw = Start-Middleware
    $be = Start-Backend
    Start-AllFrontends | Out-Null
    Write-Status ""
    Write-Status "########## 启动流程结束 ##########" -Status Header
    Show-Status
    if (-not $mw) { Write-Status "  提示: 中间件未就绪，后端可能因连不上数据库而失败" -Status Warning }
    if (-not $be) { Write-Status "  提示: 后端未就绪，前端 API 调用会失败 (ECONNREFUSED)" -Status Warning }
}

function Stop-All {
    Write-Status ""
    Write-Status "########## 停止全部服务 ##########" -Status Header
    Stop-AllFrontends
    Stop-Backend
    Stop-Middleware
    Remove-Pids
    Write-Status ""
    Write-Status "  全部服务已停止" -Status Success
}

function Restart-All {
    Stop-All
    Write-Status ""
    Write-Status "  等待 3 秒确保端口完全释放..." -Status Sub
    Start-Sleep -Seconds 3
    Start-All
}

function Show-Status {
    Write-Status ""
    Write-Status "########## 服务状态 ##########" -Status Header

    Write-Status ""
    Write-Status "[环境]" -Status Info
    Write-Status "  JDK 17+:   $(if ($Config.JAVA_HOME) { $Config.JAVA_HOME } else { '未检测到 (后端不可启动)' })" -Status $(if ($Config.JAVA_HOME) { 'Success' } else { 'Error' })
    Write-Status "  Maven:     $(if ($Config.MAVEN_HOME) { $Config.MAVEN_HOME } else { '未检测到' })" -Status $(if ($Config.MAVEN_HOME) { 'Success' } else { 'Error' })
    Write-Status "  Node.js:   $(if ($Config.HasNode) { (node --version 2>&1 | Out-String).Trim() } else { '未检测到 (前端不可启动)' })" -Status $(if ($Config.HasNode) { 'Success' } else { 'Error' })
    Write-Status "  Docker:    $(if ($Config.HasDocker) { '可用' } else { '未安装/未运行 (中间件不可启动)' })" -Status $(if ($Config.HasDocker) { 'Success' } else { 'Error' })
    Write-Status "  项目根目录: $($Config.ProjectRoot)" -Status Sub

    Write-Status ""
    Write-Status "[Docker 中间件]" -Status Info
    if (-not $Config.HasDocker) {
        Write-Status "  Docker 未安装" -Status Error
    } elseif (-not (Test-Path $Config.DockerFile)) {
        Write-Status "  docker-compose.yml 不存在" -Status Error
    } else {
        $services = @("mysql", "redis", "minio", "rabbitmq", "elasticsearch", "xxl-job-admin")
        foreach ($svc in $services) {
            $cid = docker compose -f $Config.DockerFile ps -q $svc 2>$null
            if ($cid) {
                $status = docker inspect -f "{{.State.Health.Status}}" $cid 2>$null
                if ($status -eq "healthy") {
                    Write-Status "  [OK]    $svc" -Status Success
                } else {
                    Write-Status "  [WARN]  $svc (健康状态: $status)" -Status Warning
                }
            } else {
                Write-Status "  [STOP]  $svc" -Status Sub
            }
        }
    }

    $pids = Load-Pids
    Write-Status ""
    Write-Status "[后端服务]" -Status Info
    if ($pids -and $pids.backend) {
        $proc = Get-Process -Id $pids.backend -ErrorAction SilentlyContinue
        if ($proc) {
            if (Test-Port $Config.BackendPort) {
                Write-Status "  [OK]    vibe-server (PID: $($proc.Id), http://localhost:$($Config.BackendPort))" -Status Success
            } else {
                Write-Status "  [WARN]  vibe-server (PID: $($proc.Id) 运行中但端口未就绪)" -Status Warning
            }
        } else {
            Write-Status "  [STOP]  vibe-server (PID 失效)" -Status Sub
        }
    } else {
        Write-Status "  [STOP]  vibe-server" -Status Sub
    }

    Write-Status ""
    Write-Status "[前端应用]" -Status Info
    $apps = @(
        @{ Name = "vibe-web";    Key = "web";    Port = $Config.WebPort;    Dir = $Config.WebDir },
        @{ Name = "vibe-mobile"; Key = "mobile"; Port = $Config.MobilePort; Dir = $Config.MobileDir },
        @{ Name = "vibe-portal"; Key = "portal"; Port = $Config.PortalPort; Dir = $Config.PortalDir }
    )
    foreach ($app in $apps) {
        if (-not (Test-Path $app.Dir)) {
            Write-Status "  [SKIP]  $($app.Name) (目录不存在)" -Status Sub
            continue
        }
        if ($pids -and $pids.($app.Key)) {
            $proc = Get-Process -Id $pids.($app.Key) -ErrorAction SilentlyContinue
            if ($proc) {
                if (Test-Port $app.Port) {
                    Write-Status "  [OK]    $($app.Name) (PID: $($proc.Id), http://localhost:$($app.Port))" -Status Success
                } else {
                    Write-Status "  [WARN]  $($app.Name) (PID: $($proc.Id) 运行中但端口未就绪)" -Status Warning
                }
            } else {
                Write-Status "  [STOP]  $($app.Name) (PID 失效)" -Status Sub
            }
        } else {
            Write-Status "  [STOP]  $($app.Name)" -Status Sub
        }
    }

    Write-Status ""
    Write-Status "[快速访问]" -Status Info
    Write-Status "  PC 管理后台:  http://localhost:$($Config.WebPort)" -Status Sub
    Write-Status "  工程师 H5:    http://localhost:$($Config.MobilePort)" -Status Sub
    Write-Status "  客户门户 H5:  http://localhost:$($Config.PortalPort)" -Status Sub
    Write-Status "  后端 API:     http://localhost:$($Config.BackendPort)" -Status Sub
    Write-Status "  API 文档:     http://localhost:$($Config.BackendPort)/doc.html" -Status Sub
    Write-Status "  MinIO 控制台: http://localhost:9001 (账号: vibe / vibe123456)" -Status Sub
    Write-Status "  RabbitMQ:     http://localhost:15672 (账号: vibe / vibe123)" -Status Sub
    Write-Status "  XXL-JOB:      http://localhost:8081/xxl-job-admin (账号: admin / 123456)" -Status Sub
}

function Show-Menu {
    Write-Status ""
    Write-Status "==========================================" -Status Header
    Write-Status "     Vibe 服务管理系统 - 控制面板" -Status Header
    Write-Status "==========================================" -Status Header
    Write-Status "已检测环境:" -Status Info
    Write-Status "  JDK 17+:  $(if ($Config.JAVA_HOME) { '[OK] ' + $Config.JAVA_HOME } else { '[缺失]' })" -Status $(if ($Config.JAVA_HOME) { 'Success' } else { 'Error' })
    Write-Status "  Maven:    $(if ($Config.MAVEN_HOME) { '[OK] ' + $Config.MAVEN_HOME } else { '[缺失]' })" -Status $(if ($Config.MAVEN_HOME) { 'Success' } else { 'Error' })
    Write-Status "  Node.js:  $(if ($Config.HasNode) { '[OK] ' + ((node --version 2>&1 | Out-String).Trim()) } else { '[缺失]' })" -Status $(if ($Config.HasNode) { 'Success' } else { 'Error' })
    Write-Status "  Docker:   $(if ($Config.HasDocker) { '[OK] 可用' } else { '[缺失]' })" -Status $(if ($Config.HasDocker) { 'Success' } else { 'Error' })
    Write-Status "==========================================" -Status Header
    Write-Status "  1. 一键启动全部" -Status Info
    Write-Status "  2. 仅启动中间件 (Docker)" -Status Info
    Write-Status "  3. 仅启动后端 (vibe-server)" -Status Info
    Write-Status "  4. 仅启动前端 (web + mobile + portal)" -Status Info
    Write-Status "  5. 单独启动 vibe-web" -Status Info
    Write-Status "  6. 单独启动 vibe-mobile" -Status Info
    Write-Status "  7. 单独启动 vibe-portal" -Status Info
    Write-Status "  8. 停止全部服务" -Status Info
    Write-Status "  9. 查看服务状态" -Status Info
    Write-Status "  r. 重启全部服务" -Status Info
    Write-Status "  0. 退出" -Status Info
    Write-Status "==========================================" -Status Header
    Write-Status "请输入选项 [0-9/r]: " -Status Info -NoNewline
}

# Ensure logs directory exists
$logDir = Join-Path $ProjectRoot "logs"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

# ============================================================================
# Entry point: support both menu mode and command-line mode
# ============================================================================

# Command-line mode: powershell -File start.ps1 <action>
#   actions: all | middleware | backend | frontends | web | mobile | portal | stop | status | restart
if ($Action) {
    switch ($Action.ToLower()) {
        "all"         { Start-All }
        "middleware"  { Start-Middleware | Out-Null }
        "backend"     { Start-Backend | Out-Null }
        "frontends"   { Start-AllFrontends | Out-Null }
        "web"         { Start-Frontend -Name "web" -Dir $Config.WebDir -Port $Config.WebPort | Out-Null }
        "mobile"      { Start-Frontend -Name "mobile" -Dir $Config.MobileDir -Port $Config.MobilePort | Out-Null }
        "portal"      { Start-Frontend -Name "portal" -Dir $Config.PortalDir -Port $Config.PortalPort | Out-Null }
        "stop"        { Stop-All }
        "status"      { Show-Status }
        "restart"     { Restart-All }
        default {
            Write-Status "未知操作: $Action" -Status Error
            Write-Status "可用操作: all | middleware | backend | frontends | web | mobile | portal | stop | status | restart" -Status Sub
            exit 1
        }
    }
    exit 0
}

# Menu mode
Write-Status "==========================================" -Status Header
Write-Status "     Vibe 服务管理系统" -Status Header
Write-Status "==========================================" -Status Header
Write-Status "项目根目录: $($Config.ProjectRoot)" -Status Info

while ($true) {
    Show-Menu
    $choice = Read-Host
    switch ($choice) {
        "1" { Start-All }
        "2" { Start-Middleware | Out-Null }
        "3" { Start-Backend | Out-Null }
        "4" { Start-AllFrontends | Out-Null }
        "5" { Start-Frontend -Name "web" -Dir $Config.WebDir -Port $Config.WebPort | Out-Null }
        "6" { Start-Frontend -Name "mobile" -Dir $Config.MobileDir -Port $Config.MobilePort | Out-Null }
        "7" { Start-Frontend -Name "portal" -Dir $Config.PortalDir -Port $Config.PortalPort | Out-Null }
        "8" { Stop-All }
        "9" { Show-Status }
        "r" { Restart-All }
        "0" { Write-Status ""; Write-Status "再见!"; exit 0 }
        default { Write-Status "无效选项，请输入 0-9 或 r" -Status Error }
    }
}
