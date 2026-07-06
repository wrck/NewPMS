# Vibe ServiceDeliver 部署指南

> 网络设备原厂实施项目管理系统（Vibe ServiceDeliver）部署文档。
> 本文档涵盖：环境要求、配置说明、各环境部署步骤、一键部署脚本使用、故障排查。

---

## 一、环境要求

### 1.1 软件依赖

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| JDK | 17+ | 21 LTS（实测 25.0.1） | Spring Boot 3.x 要求 JDK 17+ |
| Maven | 3.8+ | 3.8.6 / 3.9.6 | 后端构建 |
| Node.js | 18+ | 20 LTS | 前端构建 |
| npm | 9+ | 10+ | 随 Node.js 安装 |
| MySQL | 5.7+ | 8.0+ | 主数据库（5.7 需禁用 Flyway，见 §6.3） |
| Redis | 6+ | 7+ | 缓存 / 分布式锁 |
| MinIO | 最新 | 最新 | 对象存储（照片/文档） |
| RabbitMQ | 3.12+ | 3.13+ | 消息通知引擎（可选，MVP 阶段可不开） |
| Docker | 24.0+ | 25.0+ | 中间件容器化（推荐） |

### 1.2 硬件要求

| 环境 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| dev | 2 核 | 4 GB | 50 GB | 本地开发 |
| test | 2 核 | 4 GB | 80 GB | 联调测试 |
| staging | 4 核 | 8 GB | 100 GB SSD | 预发布 |
| prod | 8 核 | 16 GB | 500 GB SSD | 生产环境 |

操作系统：CentOS 7+ / Ubuntu 20+ / Windows 10+（开发环境）。

### 1.3 系统架构

```
                    ┌─────────────┐
                    │   Nginx SLB  │
                    │  (SSL/443)   │
                    └──────┬───────┘
           ┌───────────────┼───────────────┐
           │               │               │
    ┌──────▼──────┐ ┌─────▼─────┐ ┌──────▼──────┐
    │  vibe-web   │ │vibe-mobile│ │ vibe-portal │
    │  PC管理端    │ │  移动端H5  │ │  外部门户H5  │
    │  :80        │ │  :80      │ │  :80        │
    └──────┬──────┘ └─────┬─────┘ └──────┬──────┘
           │               │               │
           └───────────────┼───────────────┘
                           │
                    ┌──────▼───────┐
                    │  vibe-server  │
                    │  (Spring Boot)│
                    │  :8080 x2     │
                    └──────┬───────┘
           ┌───────────────┼───────────────┐
           │               │               │
    ┌──────▼──────┐ ┌─────▼─────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │   MySQL     │ │   Redis   │ │   MinIO     │ │  RabbitMQ   │
    │   主从      │ │ Sentinel  │ │  对象存储   │ │  消息队列    │
    └─────────────┘ └───────────┘ └─────────────┘ └─────────────┘
```

### 1.4 服务端口规划

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| MySQL | 3306 | 3306 | 数据库（主从） |
| Redis | 6379 | 6379 | 缓存（Sentinel） |
| MinIO | 9000/9001 | 9000/9001 | 对象存储 |
| RabbitMQ | 5672/15672 | 5672/15672 | 消息队列 |
| Elasticsearch | 9200 | 9200 | 检索（可选） |
| vibe-server | 8080 | 8080 | 后端 API（生产 2 实例负载） |
| vibe-web | 80 | 80 | PC 管理端 |
| vibe-mobile | 80 | 8081 | 移动端 H5 |
| vibe-portal | 80 | 8082 | 外部门户 H5 |

---

## 二、配置说明

### 2.1 后端 `application-{env}.y`

`vibe-server/vibe-server-bootstrap/src/main/resources/`：
- `application.yml`：公共配置（端口/Flyway/Jackson/Redisson 等）
- `application-dev.yml`：本地开发
- `application-test.yml`：联调测试
- `application-staging.yml`：预发布
- `application-prod.yml`：生产

**关键配置项（application-dev.yml）**：

```yaml
spring:
  # Flyway 禁用：本地 MySQL 5.7 不被 Flyway 8.x+ Community Edition 支持
  # V2/V5/V11/V12 的幂等 DDL 由 scripts/init-db.js 在首次启动前手动执行
  flyway:
    enabled: false
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${VIBE_DB_HOST:localhost}:${VIBE_DB_PORT:3306}/${VIBE_DB_NAME:vibe_db}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
    username: ${VIBE_DB_USER:root}
    password: ${VIBE_DB_PASSWORD:!Q@W3e4r}
  data:
    redis:
      host: ${VIBE_REDIS_HOST:localhost}
      port: ${VIBE_REDIS_PORT:6379}
      database: ${VIBE_REDIS_DB:0}
  rabbitmq:
    host: ${VIBE_RABBITMQ_HOST:localhost}
    port: ${VIBE_RABBITMQ_PORT:5672}
    username: ${VIBE_RABBITMQ_USER:guest}
    password: ${VIBE_RABBITMQ_PASSWORD:guest}
    virtual-host: /vibe

vibe:
  notification:
    feishu:
      webhook: ${VIBE_FEISHU_WEBHOOK:}
    dingtalk:
      webhook: ${VIBE_DINGTALK_WEBHOOK:}
```

环境变量优先级高于 yml 默认值，便于容器化部署。

### 2.2 前端 `vibe-web/vite.config.ts` 代理

```ts
server: {
  host: '0.0.0.0',
  port,                    // 默认 5173，可由 VITE_PORT 覆盖
  open: true,
  proxy: {
    '/api': {
      target: env.VITE_API_TARGET || 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '/api')
    }
  }
}
```

前端构建产物默认输出到 `vibe-web/dist/`，部署时由 Nginx 静态服务或拷贝到 `dist/web/`。

### 2.3 docker-compose 拓扑

项目根目录提供：
- `docker-compose.yml`：中间件 + 应用（推荐生产使用 `--profile app` 启动应用）
- `docker-compose.dev.yml`：仅中间件（开发用，应用本地启动）
- `.env.docker`：容器化环境变量模板

---

## 三、部署步骤

### 3.1 dev 环境（本地开发）

**A. 中间件启动（Docker Compose）**：

```bash
docker compose -f docker-compose.dev.yml up -d
# 等待中间件就绪（约 60-120 秒）
docker compose -f docker-compose.dev.yml ps
```

**B. 数据库迁移（MySQL 5.7 必做，8.0+ 可跳过）**：

```bash
node scripts/init-db.js
```

脚本幂等，可重复执行；首次执行创建 V2/V5/V11/V12 涉及的全部表与字段。

**C. 启动后端 + 前端**：

```powershell
# Windows
.\scripts\start.ps1 all        # 一键启动全部
.\scripts\start.ps1 backend    # 仅后端
.\scripts\start.ps1 web        # 仅 vibe-web

# 菜单模式（交互式）
.\scripts\start.ps1
```

**D. 访问入口**：

| 服务 | URL |
|------|-----|
| PC 管理后台 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| API 文档 | http://localhost:8080/doc.html |
| MinIO 控制台 | http://localhost:9001 （vibe / vibe123456） |
| RabbitMQ | http://localhost:15672 （vibe / vibe123） |

### 3.2 test 环境

```bash
# 1. 准备环境变量
export VIBE_DB_HOST=mysql.test.vibe.local
export VIBE_DB_USER=vibe_app
export VIBE_DB_PASSWORD=<safe-password>
export VIBE_REDIS_HOST=redis.test.vibe.local

# 2. 一键部署
pwsh ./scripts/deploy.ps1 -Env test -Version v1.2.0
```

或分步执行：

```bash
cd vibe-server && mvn clean install -pl vibe-server-bootstrap -am -DskipTests
cd ../vibe-web && npm ci && npm run build
node ../scripts/init-db.js
# 启动后端
java -jar vibe-server/vibe-server-bootstrap/target/vibe-server.jar \
  --spring.profiles.active=test
```

### 3.3 staging 环境

与 test 一致，但启用 Nginx SSL 与生产级配置：

```bash
pwsh ./scripts/deploy.ps1 -Env staging -Version v1.2.0
```

`application-staging.yml` 应启用：
- Flyway（如使用 MySQL 8.0+）
- Prometheus metrics 暴露
- 日志文件输出与轮转

### 3.4 prod 环境

```bash
# 生产部署（推荐在 CI/CD 中触发）
pwsh ./scripts/deploy.ps1 -Env prod -Version v1.2.0
```

**生产 Nginx 配置（SSL + 负载均衡）**：

```nginx
upstream vibe_backend {
    server vibe-server-1:8080 weight=1;
    server vibe-server-2:8080 weight=1;
}

server {
    listen 443 ssl http2;
    server_name admin.vibe.example.com;

    ssl_certificate     /etc/nginx/ssl/vibe.crt;
    ssl_certificate_key /etc/nginx/ssl/vibe.key;
    ssl_protocols       TLSv1.2 TLSv1.3;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://vibe_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 200m;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name admin.vibe.example.com;
    return 301 https://$host$request_uri;
}
```

### 3.5 MinIO Bucket 初始化

```bash
docker compose exec minio mc mb data/vibe-photos data/vibe-documents data/vibe-attachments data/vibe-avatars data/vibe-exports
```

---

## 四、一键部署脚本使用说明

### 4.1 `scripts/deploy.ps1`

执行完整部署流程：

```powershell
# 部署到 dev，使用自动版本号（时间戳）
.\scripts\deploy.ps1

# 部署到 prod，指定版本号
.\scripts\deploy.ps1 -Env prod -Version v1.2.0
```

**流程**：

1. **备份当前产物** → `backups/<timestamp>_<version>/`（用于回滚）
2. **构建后端** → `mvn clean install -pl vibe-server-bootstrap -am -DskipTests`
3. **构建前端** → `npm run build`（vibe-web）
4. **数据库迁移** → `node scripts/init-db.js`（幂等）
5. **部署产物** → 拷贝 jar 与 `dist/web/` 到项目根 `dist/`
6. **健康检查** → 连续 3 次 `GET http://localhost:8080/actuator/health` 返回 200

**失败回滚**：任意步骤失败自动从最新备份恢复 jar 与 web 资源。

**输出**：每步骤均带时间戳的彩色日志，便于追踪。

### 4.2 `scripts/rollback.ps1`

```powershell
# 回滚到最近一次备份（默认）
.\scripts\rollback.ps1 -Latest

# 回滚到指定版本
.\scripts\rollback.ps1 -Version v1.1.0
```

回滚完成后，请通过 `scripts\start.ps1` 重启服务。

### 4.3 `scripts/init-db.js`

幂等数据库迁移脚本（替代 Flyway，用于 MySQL 5.7）：

```bash
node scripts/init-db.js
# 通过环境变量覆盖默认连接
VIBE_DB_HOST=mysql.local VIBE_DB_PASSWORD=secret node scripts/init-db.js
```

支持的版本：
- V2：客户协作 + 低代码配置 8 表
- V5：integration_config 4 个字段扩展
- V11：6 个关键业务表追加 `version` 列（乐观锁）
- V12：sys_feedback 反馈与工单表

### 4.4 `scripts/start.ps1`（保留）

日常开发启停脚本，**不**执行构建或迁移。菜单模式：

```
1. 一键启动全部
2. 仅启动中间件
3. 仅启动后端
4. 仅启动前端
8. 停止全部服务
9. 查看服务状态
r. 重启全部
```

---

## 五、数据库维护

### 5.1 日常备份

```bash
docker compose exec -T mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} \
  --single-transaction --routines --triggers vibe_db | \
  gzip > /opt/vibe/data/backup/vibe_db_$(date +%Y%m%d_%H%M%S).sql.gz

# 保留最近 30 天
find /opt/vibe/data/backup -name "vibe_db_*.sql.gz" -mtime +30 -delete
```

### 5.2 定时备份（crontab）

```bash
0 2 * * * /opt/vibe/scripts/backup-db.sh >> /opt/vibe/logs/backup.log 2>&1
```

### 5.3 数据恢复

```bash
gunzip < /opt/vibe/data/backup/vibe_db_20260101_020000.sql.gz | \
  docker compose exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} vibe_db
```

---

## 六、故障排查

### 6.1 端口占用

**症状**：`Address already in use` / `BindException: Address already in use`

**排查**：

```bash
# 查看占用端口的进程
netstat -ano | findstr :8080            # Windows
lsof -i :8080                            # Linux/Mac

# 强制终止
taskkill /F /T /PID <PID>                # Windows
kill -9 <PID>                            # Linux
```

或使用 `scripts\start.ps1` 的内置端口清理（自动 `taskkill` 占用进程）。

### 6.2 数据库连接失败

**症状**：`Communications link failure` / `Unable to connect to MySQL`

**排查步骤**：

1. 检查 MySQL 容器是否健康：`docker compose ps mysql`
2. 检查容器网络：`docker network inspect vibe_default`
3. 检查账号密码：`mysql -h <host> -u <user> -p` 手动登录
4. 检查 `application-{env}.yml` 中 `spring.datasource.url` 的 host/port
5. 检查防火墙是否放行 3306

**常见原因**：
- 密码包含特殊字符（`!@#$%`）未在 yml 中正确转义（应使用单引号或环境变量）
- MySQL 8.0+ 默认 caching_sha2_password，需在 url 追加 `allowPublicKeyRetrieval=true`

### 6.3 Flyway 不兼容 MySQL 5.7

**症状**：启动报错 `Flyway migrations are not supported on MySQL 5.7` 或 `Unknown column 'installed_on'`。

**根因**：Flyway 8.x+ Community Edition 不再支持 MySQL 5.7。

**解决方案（dev 环境）**：

```yaml
# application-dev.yml
spring:
  flyway:
    enabled: false     # 关闭 Flyway，使用 scripts/init-db.js 手动迁移
```

执行：`node scripts/init-db.js`（幂等，可重复执行）。

**解决方案（prod 环境）**：升级 MySQL 到 8.0+，启用 Flyway：

```yaml
# application-prod.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
    validate-on-migrate: true
```

### 6.4 PowerShell 脚本中文乱码

**症状**：执行 `deploy.ps1` / `rollback.ps1` 时中文输出乱码。

**根因**：PowerShell 5.x 默认编码为 GBK，而脚本文件为 UTF-8。

**解决方案**：

1. 脚本已保存为 **UTF-8 with BOM**（项目内 `deploy.ps1` / `rollback.ps1` / `start.ps1` 已处理）
2. 执行前临时切换控制台编码：

```powershell
chcp 65001                                    # 切到 UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
.\scripts\deploy.ps1 -Env dev -Version v1.2.0
```

3. 永久修复（PowerShell 配置文件）：

```powershell
# 编辑 $PROFILE
notepad $PROFILE
# 加入：
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

### 6.5 健康检查失败

**症状**：`deploy.ps1` 卡在「健康检查」步骤，连续 3 次失败回滚。

**排查**：

```bash
# 1. 确认后端进程是否启动
docker compose ps vibe-server
# 2. 查看后端日志
docker compose logs --tail=200 vibe-server
# 或本地：tail -f logs/backend.err.log
# 3. 手动 curl
curl -v http://localhost:8080/actuator/health
```

**常见原因**：数据库连接失败、Redis 不可达、端口被占用、jar 损坏。

### 6.6 前端构建失败

**症状**：`npm run build` 报 TS 类型错误或 vite 构建失败。

**排查**：

```bash
cd vibe-web
npm ci                              # 干净重装依赖
npm run type-check                  # 单独跑类型检查
npm run build                       # 重新构建
```

### 6.7 完全重置（清库重装）

```bash
docker compose down -v
rm -rf data/mysql data/redis data/minio data/rabbitmq
docker compose up -d
docker compose --profile app up -d --build
```

---

## 七、监控与日志

### 7.1 健康检查端点

```bash
curl http://localhost:8080/actuator/health
```

### 7.2 Spring Boot Actuator

`application-prod.yml` 配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

### 7.3 日志轮转

```yaml
logging:
  file:
    name: /app/logs/vibe-server.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 5GB
```

---

## 八、性能调优

### 8.1 JVM 参数

```env
JAVA_OPTS=-Xms2g -Xmx4g -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof
```

### 8.2 MySQL 调优

`/opt/vibe/config/mysql-custom.cnf`：

```ini
[mysqld]
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
max_connections = 500
slow_query_log = 1
long_query_time = 2
```

### 8.3 Redis 调优

```bash
redis-server --maxmemory 1gb --maxmemory-policy allkeys-lru --save 900 1 --save 300 10
```

---

## 九、安全加固

1. **修改默认密码**：所有中间件使用强密码（已在 `.env` 中配置）
2. **SSL/HTTPS**：配置 TLS 1.2+ 证书
3. **防火墙**：仅暴露 443（HTTPS）端口，内部服务不对外
4. **数据库**：MySQL 仅允许内网访问，禁止 root 远程登录
5. **JWT Secret**：使用 64+ 位随机字符串
6. **CORS**：生产环境在 Nginx 层限制 `Access-Control-Allow-Origin` 为具体域名
7. **Rate Limiting**：Nginx 配置请求限流（`limit_req_zone`）

---

## 十、CI/CD 流水线（GitLab CI 示例）

```yaml
stages:
  - build
  - test
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

backend-build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd vibe-server && mvn -B clean package -DskipTests
  artifacts:
    paths:
      - vibe-server/vibe-server-bootstrap/target/vibe-server.jar

frontend-build:
  stage: build
  image: node:20-alpine
  script:
    - cd vibe-web && npm ci && npm run build
  artifacts:
    paths:
      - vibe-web/dist

backend-test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd vibe-server && mvn -B test

deploy-staging:
  stage: deploy
  only:
    - develop
  script:
    - pwsh ./scripts/deploy.ps1 -Env staging -Version $CI_COMMIT_TAG

deploy-production:
  stage: deploy
  only:
    - main
  when: manual
  script:
    - pwsh ./scripts/deploy.ps1 -Env prod -Version $CI_COMMIT_TAG
```

---

## 十一、回滚流程

### 11. 自动回滚

`deploy.ps1` 在任一步骤失败时自动回滚到 `backups/<最新>/` 中的产物。

### 11.2 手动回滚

```powershell
# 回滚到最近一次备份
.\scripts\rollback.ps1 -Latest

# 回滚到指定版本
.\scripts\rollback.ps1 -Version v1.1.0
```

### 11.3 数据库回滚

数据库回滚需通过备份恢复（§5.3），`rollback.ps1` 仅回滚应用产物，不回退 DDL。

---

## 附录：服务清单与快速访问

| 服务 | URL | 凭据 |
|------|-----|------|
| PC 管理后台 | http://localhost:5173 | 默认 admin / vibe@123 |
| 工程师 H5 | http://localhost:5174 | - |
| 客户门户 H5 | http://localhost:5175 | - |
| 后端 API | http://localhost:8080 | - |
| API 文档 | http://localhost:8080/doc.html | - |
| MinIO 控制台 | http://localhost:9001 | vibe / vibe123456 |
| RabbitMQ | http://localhost:15672 | vibe / vibe123 |
| XXL-JOB | http://localhost:8081/xxl-job-admin | admin / 123456 |
