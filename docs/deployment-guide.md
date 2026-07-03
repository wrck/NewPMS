# 生产环境部署指南

网络设备原厂实施项目管理系统（Vibe ServiceDeliver）生产环境部署文档。

---

## 一、系统架构

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

---

## 二、环境要求

| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 4 核 | 8 核 |
| 内存 | 8 GB | 16 GB |
| 磁盘 | 100 GB SSD | 500 GB SSD |
| 操作系统 | CentOS 7+ / Ubuntu 20+ | Ubuntu 22.04 LTS |
| Docker | 24.0+ | 25.0+ |
| Docker Compose | v2 | v2.24+ |

---

## 三、服务清单与端口规划

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| MySQL | 3306 | 3306 | 数据库（主从） |
| Redis | 6379 | 6379 | 缓存（Sentinel） |
| MinIO | 9000/9001 | 9000/9001 | 对象存储 |
| RabbitMQ | 5672/15672 | 5672/15672 | 消息队列 |
| vibe-server | 8080 | 8080 | 后端 API（2 实例负载） |
| vibe-web | 80 | 80 | PC 管理端 |
| vibe-mobile | 80 | 8081 | 移动端 H5 |
| vibe-portal | 80 | 8082 | 外部门户 H5 |

---

## 四、部署步骤

### 4.1 准备部署目录

```bash
mkdir -p /opt/vibe/{data/mysql,data/redis,data/minio,data/rabbitmq,logs,config}
cd /opt/vibe
```

### 4.2 配置环境变量

创建 `.env` 文件：

```env
# MySQL
MYSQL_ROOT_PASSWORD=<强密码>
MYSQL_DATABASE=vibe_db

# Redis
REDIS_PASSWORD=<强密码>

# MinIO
MINIO_ROOT_USER=vibe
MINIO_ROOT_PASSWORD=<强密码>

# RabbitMQ
RABBITMQ_USER=vibe
RABBITMQ_PASSWORD=<强密码>

# JWT
JWT_SECRET=<64位随机字符串>
JWT_ACCESS_EXPIRATION=7200
JWT_REFRESH_EXPIRATION=604800

# 应用
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms1g -Xmx2g -XX:MaxRAMPercentage=75.0
```

### 4.3 生产环境 Nginx 配置（SSL + 负载均衡）

创建 `/opt/vibe/config/nginx.conf`：

```nginx
# 上游服务：vibe-server 负载均衡
upstream vibe_backend {
    server vibe-server-1:8080 weight=1;
    server vibe-server-2:8080 weight=1;
}

# PC 管理端
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

# 移动端 H5
server {
    listen 443 ssl http2;
    server_name m.vibe.example.com;

    ssl_certificate     /etc/nginx/ssl/vibe.crt;
    ssl_certificate_key /etc/nginx/ssl/vibe.key;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://vibe_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size 100m;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}

# 外部门户 H5
server {
    listen 443 ssl http2;
    server_name portal.vibe.example.com;

    ssl_certificate     /etc/nginx/ssl/vibe.crt;
    ssl_certificate_key /etc/nginx/ssl/vibe.key;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://vibe_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size 100m;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name admin.vibe.example.com m.vibe.example.com portal.vibe.example.com;
    return 301 https://$host$request_uri;
}
```

### 4.4 构建与启动

```bash
# 构建全部镜像
docker compose --profile app build

# 启动中间件（等待健康检查通过）
docker compose up -d mysql redis minio rabbitmq

# 等待中间件就绪
sleep 30

# 启动应用
docker compose --profile app up -d

# 查看状态
docker compose ps
```

### 4.5 初始化 MinIO Bucket

```bash
# 方式1：通过初始化脚本
docker compose exec minio mc mb data/vibe-photos data/vibe-documents data/vibe-attachments data/vibe-avatars data/vibe-exports

# 方式2：通过管理控制台
# 访问 http://<host>:9001 → Buckets → Create Bucket
```

---

## 五、数据库维护

### 5.1 日常备份

```bash
# 全量备份
docker compose exec -T mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} \
  --single-transaction --routines --triggers vibe_db | \
  gzip > /opt/vibe/data/backup/vibe_db_$(date +%Y%m%d_%H%M%S).sql.gz

# 保留最近 30 天备份
find /opt/vibe/data/backup -name "vibe_db_*.sql.gz" -mtime +30 -delete
```

### 5.2 定时备份（crontab）

```bash
# 每日凌晨 2 点自动备份
0 2 * * * /opt/vibe/scripts/backup-db.sh >> /opt/vibe/logs/backup.log 2>&1
```

### 5.3 数据恢复

```bash
gunzip < /opt/vibe/data/backup/vibe_db_20260101_020000.sql.gz | \
docker compose exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} vibe_db
```

---

## 六、日志管理

### 6.1 日志收集

```bash
# 查看后端日志
docker compose logs -f --tail=200 vibe-server

# 查看特定模块日志（需在后端 logback 配置中启用文件输出）
tail -f /opt/vibe/logs/vibe-server.log
```

### 6.2 日志轮转

在 `application-prod.yml` 中配置：

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

## 七、监控告警

### 7.1 健康检查端点

```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# 前端健康检查
curl http://localhost/healthz
curl http://localhost:8081/healthz
curl http://localhost:8082/healthz
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

### 7.3 Prometheus + Grafana（可选）

在 `docker-compose.yml` 中添加 Prometheus 抓取配置：

```yaml
vibe-server:
  environment:
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,metrics,prometheus
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
query_cache_size = 64M
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

## 十、CI/CD 流水线

### GitLab CI 示例（`.gitlab-ci.yml`）

```yaml
stages:
  - build
  - test
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

# 后端构建
backend-build:
  stage: build
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd vibe-server && mvn -B clean package -DskipTests
  artifacts:
    paths:
      - vibe-server/vibe-server-bootstrap/target/vibe-server.jar

# 前端构建
frontend-build:
  stage: build
  image: node:20-alpine
  script:
    - cd vibe-web && npm ci && npm run build
    - cd vibe-mobile && npm ci && npm run build
    - cd vibe-portal && npm ci && npm run build
  artifacts:
    paths:
      - vibe-web/dist
      - vibe-mobile/dist
      - vibe-portal/dist

# 单元测试
backend-test:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd vibe-server && mvn -B test

# 部署到测试环境
deploy-staging:
  stage: deploy
  only:
    - develop
  script:
    - docker compose --profile app build
    - docker compose --profile app up -d

# 部署到生产环境
deploy-production:
  stage: deploy
  only:
    - main
  when: manual
  script:
    - docker compose --profile app build
    - docker compose --profile app up -d
```

---

## 十一、故障排查

### 常见问题

| 问题 | 排查方法 |
|------|---------|
| 后端启动失败 | `docker logs vibe-server`，检查数据库/Redis 连接 |
| 前端 502 | 检查 `vibe-server` 是否健康，Nginx 代理配置 |
| 数据库连接超时 | 检查 MySQL 容器健康状态、网络、防火墙 |
| 照片上传失败 | 检查 MinIO 容器状态、Bucket 是否存在、磁盘空间 |
| 消息通知不发送 | 检查 RabbitMQ 状态、队列是否有积压 |
| 登录返回 401 | 检查 JWT Secret 是否一致、Token 是否过期 |

### 完全重置

```bash
docker compose down -v
rm -rf data/mysql data/redis data/minio data/rabbitmq
docker compose up -d
docker compose --profile app up -d --build
```
