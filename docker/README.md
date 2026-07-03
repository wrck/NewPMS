# 开发环境部署说明

网络设备原厂实施项目管理系统（Vibe ServiceDeliver）开发环境基于 Docker Compose 编排，
包含 MySQL、Redis、MinIO、RabbitMQ 四个中间件，以及可选的 vibe-server / vibe-web 应用容器。

---

## 一、目录结构

```
ServiceDeliver/
├── docker-compose.yml            # 主编排文件（中间件 + 应用，应用用 profile 控制）
├── docker-compose.dev.yml        # 纯中间件版（应用本地运行，便于调试）
├── docker/
│   ├── README.md                 # 本部署说明
│   ├── mysql/
│   │   ├── my.cnf                # MySQL 配置（utf8mb4 字符集）
│   │   ├── init/
│   │   │   └── init.sql          # 启动时自动执行：建库 + 加载 schema/data
│   │   └── data/                 # MySQL 数据持久化（自动生成）
│   ├── redis/
│   │   └── data/                 # Redis AOF 持久化（自动生成）
│   ├── minio/
│   │   └── data/                 # MinIO 对象存储（自动生成）
│   ├── rabbitmq/
│   │   └── data/                 # RabbitMQ 数据（自动生成）
│   └── nginx/
│       └── nginx.conf            # Nginx 配置（静态资源 + /api 反代）
├── vibe-server/
│   ├── Dockerfile                # 后端镜像构建（Maven → JRE17 多阶段）
│   └── .dockerignore
└── vibe-web/
    ├── Dockerfile                # 前端镜像构建（Node20 → Nginx 多阶段）
    └── .dockerignore
```

---

## 二、前置要求

- Docker Engine ≥ 24.0
- Docker Compose v2（`docker compose` 命令）
- 可用内存 ≥ 4GB（推荐 8GB）
- 本地端口未被占用：3306 / 6379 / 9000 / 9001 / 5672 / 15672 / 8080 / 80

---

## 三、启动方式

### 方式 A：仅启动中间件（推荐，便于本地调试应用）

```bash
# 使用主编排文件（应用服务带 profile，默认不启动）
docker compose up -d

# 或使用纯中间件版编排文件（效果相同，语义更清晰）
docker compose -f docker-compose.dev.yml up -d
```

启动后，在 IDE 中本地运行 vibe-server（`SPRING_PROFILES_ACTIVE=dev`）与 vibe-web（`npm run dev`）。
中间件连接地址统一使用 `localhost`，详见下方「服务连接信息」。

### 方式 B：启动全部服务（含应用容器）

```bash
docker compose --profile app up -d --build
```

`--profile app` 会触发 vibe-server 与 vibe-web 镜像构建并启动。
`--build` 首次运行或代码变更时使用，强制重新构建镜像。

---

## 四、常用运维命令

```bash
# 查看运行状态
docker compose ps

# 查看某服务日志（实时跟随）
docker compose logs -f mysql
docker compose logs -f vibe-server

# 重启某服务
docker compose restart redis

# 停止全部服务（保留数据）
docker compose down

# 停止并删除数据卷（⚠️ 清空所有持久化数据，慎用）
docker compose down -v

# 进入容器执行命令
docker compose exec mysql mysql -uroot -pvibe123 vibe_db
docker compose exec redis redis-cli -a vibe123
```

---

## 五、服务连接信息

### 中间件默认账号密码

| 服务     | 宿主机端口 | 容器内地址                     | 账号          | 密码         |
|----------|------------|--------------------------------|---------------|--------------|
| MySQL    | 3306       | `mysql:3306`                   | root          | `vibe123`    |
| Redis    | 6379       | `redis:6379`                   | -             | `vibe123`    |
| MinIO API| 9000       | `minio:9000`                   | vibe          | `vibe123456` |
| MinIO 控制台 | 9001    | http://localhost:9001          | vibe          | `vibe123456` |
| RabbitMQ | 5672       | `rabbitmq:5672`                | vibe          | `vibe123`    |
| RabbitMQ 管理界面 | 15672 | http://localhost:15672       | vibe          | `vibe123`    |

### 应用服务

| 服务        | 宿主机端口 | 访问地址                       | 说明                     |
|-------------|------------|--------------------------------|--------------------------|
| vibe-server | 8080       | http://localhost:8080          | Spring Boot 后端 API     |
| vibe-web    | 80         | http://localhost               | 前端 PC 管理后台         |

### 本地开发连接配置

- **后端**：本地运行 vibe-server 时，连接中间件使用 `localhost`（中间件端口已映射到宿主机）
  - MySQL：`jdbc:mysql://localhost:3306/vibe_db`
  - Redis：`localhost:6379`（密码 `vibe123`）
  - MinIO：`http://localhost:9000`
  - RabbitMQ：`localhost:5672`
- **前端**：本地运行 `npm run dev`，Vite 开发服务器（5173）通过 `.env.development` 配置代理到 `http://localhost:8080`

---

## 六、数据初始化说明

### 自动初始化（首次启动）

MySQL 容器首次启动时，会自动执行 `/docker-entrypoint-initdb.d/` 下的脚本：

1. **`init.sql`**（位于 `docker/mysql/init/`）
   - 创建数据库 `vibe_db`（utf8mb4 字符集）
   - 通过 `SOURCE` 命令加载表结构脚本 `schema.sql` 与初始化数据 `data.sql`

2. **`schema.sql` / `data.sql`**
   - 来源：`vibe-server/vibe-server-bootstrap/src/main/resources/db/`
   - 通过 docker-compose 卷以只读方式挂载到容器内 `/docker-entrypoint-initdb.d/db-scripts/`

> **重要**：MySQL 仅在数据目录为空（首次初始化）时执行上述脚本。
> 若需重新初始化，必须先删除 `docker/mysql/data/` 目录后重启容器：
>
> ```bash
> docker compose down
> rm -rf docker/mysql/data    # Linux/macOS
> # Windows PowerShell: Remove-Item -Recurse -Force docker\mysql\data
> docker compose up -d
> ```

### 手动初始化（如需）

```bash
# 进入 MySQL 容器执行脚本
docker compose exec -T mysql mysql -uroot -pvibe123 vibe_db \
  < vibe-server/vibe-server-bootstrap/src/main/resources/db/schema.sql
docker compose exec -T mysql mysql -uroot -pvibe123 vibe_db \
  < vibe-server/vibe-server-bootstrap/src/main/resources/db/data.sql
```

### MinIO Bucket 初始化

设计文档规划的 Bucket（`vibe-photos` / `vibe-documents` / `vibe-attachments` / `vibe-avatars` / `vibe-exports`）
默认不自动创建，由后端应用首次启动时通过 MinIO SDK 创建，或通过控制台手动创建：

1. 访问 http://localhost:9001（账号 `vibe` / `vibe123456`）
2. 进入 `Buckets` → `Create Bucket`
3. 依次创建上述 5 个 Bucket

---

## 七、健康检查

所有服务均配置了健康检查，可通过 `docker compose ps` 查看 `STATUS` 列的 `(healthy)` 标识：

| 服务     | 检查命令                                            |
|----------|-----------------------------------------------------|
| MySQL    | `mysqladmin ping -h 127.0.0.1 -uroot -pvibe123`     |
| Redis    | `redis-cli -a vibe123 ping`                         |
| MinIO    | `curl -f http://localhost:9000/minio/health/ready`  |
| RabbitMQ | `rabbitmq-diagnostics -q ping`                      |

`vibe-server` 与 `vibe-web` 通过 `depends_on.condition: service_healthy` 确保中间件健康后再启动。

---

## 八、常见问题

### 1. 端口冲突

启动前确认本机 3306/6379/9000/9001/5672/15672/8080/80 未被占用：

```bash
# Linux/macOS
lsof -i :3306

# Windows
netstat -ano | findstr :3306
```

如需修改端口，编辑 `docker-compose.yml` 中对应服务的 `ports` 映射（左侧宿主机端口）。

### 2. MySQL 字符集验证

```bash
docker compose exec mysql mysql -uroot -pvibe123 -e "SHOW VARIABLES LIKE 'character%';"
```

预期 `character_set_server`、`character_set_database`、`character_set_client` 均为 `utf8mb4`。

### 3. 数据卷权限问题（Linux）

若 `docker/mysql/data` 等目录出现权限错误，可手动创建并授权：

```bash
mkdir -p docker/{mysql,redis,minio,rabbitmq}/data
chmod -R 755 docker/
```

### 4. 镜像构建缓慢

- 后端：Dockerfile 已利用层缓存（先复制 pom 再复制源码），重复构建仅编译变更代码
- 前端：可在 `vibe-web/Dockerfile` 中取消注释 npm 镜像源配置行加速依赖下载

### 5. 完全重置环境

```bash
docker compose down -v
# 删除所有持久化数据目录
rm -rf docker/mysql/data docker/redis/data docker/minio/data docker/rabbitmq/data
docker compose up -d
```
