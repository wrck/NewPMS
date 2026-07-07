# 架构设计

> 文档版本：V1.1（架构对齐与低代码 / 异常处理 / 用户引导补充版）
> 更新日期：2026-07-06
> 基线文档：`系统设计文档.md` V1.0（第一部分系统架构设计）
> 配套文档：[需求总览](./requirement-overview.md) | [状态机转换矩阵](./state-machine.md) | [开发规范](./development-guide.md) | [部署指南](./deployment-guide.md)
> 版本变更：V1.1 在 V1.0 基础上新增第十五章「本轮迭代架构补充（Task 1-21）」，补充低代码架构、异常处理三层闭环架构、新增前端组件清单、用户引导架构

---

## 目录

- [一、系统架构分层](#一系统架构分层)
- [二、技术选型](#二技术选型)
- [三、后端分层架构](#三后端分层架构)
- [四、对象分层](#四对象分层)
- [五、后端模块清单与依赖](#五后端模块清单与依赖)
- [六、统一响应与异常处理](#六统一响应与异常处理)
- [七、认证与权限](#七认证与权限)
- [八、数据库设计规范](#八数据库设计规范)
- [九、缓存设计](#九缓存设计)
- [十、集成架构](#十集成架构)
- [十一、消息通知架构](#十一消息通知架构)
- [十二、文件存储](#十二文件存储)
- [十三、部署架构](#十三部署架构)
- [十四、CI/CD 流程](#十四cicd-流程)
- [十五、本轮迭代架构补充（Task 1-21）](#十五本轮迭代架构补充task-1-21)

---

## 一、系统架构分层

```
┌─────────────────────────────────────────────────────────────────────┐
│ 展现层                                                              │
│ Vue3 + TypeScript + Vite + Ant Design Vue + Pinia + Vue Router      │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│ │ PC管理后台  │ │ 移动端H5   │ │ 外部入口H5  │ │ 数据大屏   │       │
│ │ (全功能)   │ │ (工程师用) │ │(代理商/客户)│ │ (ECharts)  │       │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘       │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ HTTPS / WSS
┌───────────────────────────────▼─────────────────────────────────────┐
│ 接入层                                                              │
│ Nginx 1.25+ · SSL 终止 · 静态资源 · API 反向代理 · Gzip · 限流     │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│ 网关层                                                              │
│ Spring Cloud Gateway · JWT 校验 · 角色权限拦截 · 限流熔断 · 日志    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│ 业务服务层（Spring Boot 3.x + Java 17，单体模块化部署）             │
│                                                                     │
│ module-auth           认证授权模块                                  │
│ module-project        项目管理模块                                  │
│ module-device         设备资产管理模块                              │
│ module-resource       资源调度模块                                  │
│ module-delivery       交付管理模块                                  │
│ module-agent          代理商管理模块                                │
│ module-acceptance     验收管理模块                                  │
│ module-finance        财务核算模块                                  │
│ module-collaboration  客户协作模块                                  │
│ module-integration    集成管理模块                                  │
│ module-report         报表分析模块                                  │
│ module-system         系统管理模块                                  │
│ module-lowcode        低代码配置模块                                │
│ module-common         公共模块(工具类/常量/异常/基类)               │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│ 平台能力层                                                          │
│ 流程引擎 · 文件服务 · 消息服务 · 地图服务 · 导出服务 · 定时任务     │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│ 数据层                                                              │
│ MySQL 8.0 主从 · Redis 7.x Sentinel · ES 8.x · MinIO · RabbitMQ   │
└─────────────────────────────────────────────────────────────────────┘
```

> **部署形态说明**：当前生产形态为「单体模块化」部署，即所有 module-* 编译打包为单一 `vibe-server` 可执行 JAR，由 `vibe-server-bootstrap` 聚合启动。架构上保留了拆分为微服务的网关层接入能力（Spring Cloud Gateway），可在不改动业务代码的前提下平滑演进为微服务架构。

---

## 二、技术选型

| 层次     | 技术                          | 版本/说明                   |
| ------ | --------------------------- | ----------------------- |
| 语言     | Java                        | 17 (LTS)                |
| 框架     | Spring Boot                 | 3.2.x                   |
| ORM    | MyBatis-Plus                | 3.5.x                   |
| 构建     | Maven                       | 3.9.x                   |
| 接口文档   | Knife4j                     | 4.x (OpenAPI 3.0)       |
| 认证     | Spring Security + JWT       | 6.x                     |
| 缓存     | Redis                       | 7.x (Spring Data Redis) |
| 数据库    | MySQL                       | 8.0.x                   |
| 搜索     | Elasticsearch               | 8.x                     |
| 文件     | MinIO                       | 最新稳定版                   |
| 消息队列   | RabbitMQ                    | 3.12+ (Spring AMQP)     |
| 定时任务   | XXL-JOB                     | 2.4.x                   |
| 流程引擎   | Flowable                    | 7.x（轻量审批流）              |
| 地图     | 高德地图 JS API                 | 2.0                     |
| Excel  | EasyExcel                   | 3.x                     |
| PDF    | Flying Saucer / iText       | -                       |
| 工具库    | Hutool / MapStruct / Lombok | -                       |
| **前端** |                             |                         |
| 框架     | Vue                         | 3.4+ (Composition API)  |
| 语言     | TypeScript                  | 5.x                     |
| 构建     | Vite                        | 5.x                     |
| UI 组件库 | Ant Design Vue              | 4.x                     |
| 状态管理   | Pinia                       | 2.x                     |
| 路由     | Vue Router                  | 4.x                     |
| HTTP   | Axios                       | 1.x                     |
| 图表     | ECharts                     | 5.x                     |
| 甘特图    | dhtmlx-gantt                | 8.x                     |
| 测试     | Vitest / Playwright          | -                       |
| **部署** |                             |                         |
| 容器     | Docker                      | 24.x                    |
| 编排     | Docker Compose / K8s        | -                       |
| 反向代理   | Nginx                       | 1.25+                   |
| CI/CD  | GitLab CI / Jenkins         | -                       |
| 监控     | Prometheus + Grafana        | -                       |
| 日志     | ELK / Loki + Grafana        | -                       |

---

## 三、后端分层架构

### 3.1 请求流向

```
Client → Controller → Service → Repository → Database
```

### 3.2 各层职责

#### Controller 层（接口层）

- 接收请求参数，调用参数校验（`@Valid`）
- 调用 Service 层
- 包装返回结果（统一响应体 `Result<T>` / `PageResult<T>`）
- 不包含任何业务逻辑
- 命名规范：`XxxController`
- 路径规范：`/api/v1/{module}/{resource}`

#### Service 层（业务逻辑层）

- 核心业务逻辑
- 事务管理（`@Transactional`）
- 调用 Repository / 外部服务
- 发送领域事件
- 命名规范：`XxxService`（接口）/ `XxxServiceImpl`（实现）

#### Repository / Mapper 层（数据访问层）

- 数据库 CRUD 操作
- 复杂 SQL 查询（MyBatis XML Mapper）
- 不包含业务逻辑
- 工具：MyBatis-Plus BaseMapper + 自定义 Mapper

### 3.3 模块内部结构

每个业务模块遵循统一的包结构约定：

```
module-project/
├── controller/       ProjectController.java
├── service/          ProjectService.java
│   └── impl/         ProjectServiceImpl.java
├── mapper/           ProjectMapper.java + ProjectMapper.xml
├── entity/           ProjectEntity.java
├── dto/              ProjectCreateDTO.java
├── vo/               ProjectDetailVO.java
├── bo/               ProjectBO.java
├── enums/            ProjectStatusEnum.java
├── constant/         ProjectConstant.java
└── event/            ProjectCreatedEvent.java
```

> 实际代码中以 `module-project` 为例，主要包路径为 `com.vibe.project.controller / .service / .service.impl / .mapper / .entity / .dto / .vo`。`bo/enums/constant/event` 为可选包，按需创建。

---

## 四、对象分层

| 对象类型   | 用途              | 示例                 |
| ------ | --------------- | ------------------ |
| Entity | 数据库实体，与表一一对应    | `ProjectEntity`    |
| DTO    | 接口入参（前端 → 后端）   | `ProjectCreateDTO` |
| VO     | 接口出参（后端 → 前端）   | `ProjectDetailVO`  |
| BO     | Service 层内部业务对象 | `ProjectBO`        |

**转换工具**：MapStruct（`Entity ↔ BO ↔ DTO/VO`）

> 实际代码中：实体均继承 `com.vibe.common.base.BaseEntity`（含 id/createBy/createTime/updateBy/updateTime/deleted/version 公共字段）；DTO 命名为 `XxxDTO`（如 `LowcodeFormConfigDTO`），VO 命名为 `XxxVO`（如 `LowcodeFormConfigVO`）。

---

## 五、后端模块清单与依赖

### 5.1 实际模块清单（vibe-server 下）

实际工程在 `vibe-server/` 下聚合了 14 个业务模块 + 1 个 bootstrap 启动模块：

| 模块                       | Maven artifactId       | 主要职责                                                  | Controller 数 | Entity 数 |
| -------------------------- | ---------------------- | --------------------------------------------------------- | ------------ | --------- |
| module-auth                | vibe-module-auth       | 认证授权：登录 / Token / 当前用户                          | 1            | -         |
| module-project             | vibe-module-project    | 项目、阶段、任务、里程碑、变更、风险、问题、模板、客户     | 9            | 11        |
| module-device              | vibe-module-device     | 设备型号、实例、BOM、出入库、备件、仓库、看板             | 11           | 11        |
| module-resource            | vibe-module-resource   | 工程师、技能、排期、任务派发、工时、差旅、请假             | 5            | 7         |
| module-delivery            | vibe-module-delivery   | 工单、施工步骤、施工照片、异常、割接方案与执行            | 6            | 6         |
| module-agent               | vibe-module-agent      | 代理商公司 / 工程师、转包任务、交付物、工作量、评分、外部门户 | 7            | 6         |
| module-acceptance          | vibe-module-acceptance | 验收标准 / 检查项、测试记录、验收任务、遗留问题、竣工文档  | 4            | 6         |
| module-finance             | vibe-module-finance    | 预算、成本、利润、工作量确认                               | 4            | 3         |
| module-collaboration       | vibe-module-collaboration | 客户协作门户（H5）                                         | 1            | 3         |
| module-integration         | vibe-module-integration | 外部系统集成配置、调用日志、同步                           | 3            | 2         |
| module-report              | vibe-module-report     | 仪表盘、管理驾驶舱、业务报表                               | 3            | -         |
| module-system              | vibe-module-system     | 用户、角色、菜单、组织、岗位、字典、配置、通知、日志、反馈 | 12           | 13        |
| module-lowcode             | vibe-module-lowcode    | 低代码配置：表单 / 列表 / 标签页 / 关联页 / 模板          | 5            | 5         |
| module-common              | vibe-module-common     | 公共基类、Result、异常、工具、常量                         | -            | 1（Base）  |
| vibe-server-bootstrap      | vibe-server-bootstrap  | 启动类、application.yml、DB 迁移脚本（V1~V5）             | -            | -         |

**统计**：业务 Controller 共约 62 个；业务实体（不含 BaseEntity）共约 80 个。

### 5.2 模块依赖关系

```
                       ┌────────────┐
                       │ bootstrap  │ (启动)
                       └─────┬──────┘
                             │ scan
       ┌─────────────────────┼────────────────────────────┐
       ▼                     ▼                            ▼
┌─────────────┐      ┌──────────────┐             ┌──────────────┐
│ module-auth  │      │ module-system │             │ module-common │ (基础)
└──────┬──────┘      └──────┬───────┘             └──────┬───────┘
       │ depends            │ depends                    │
       └────────────────────┴────────────────────────────┘
                                ▲
                                │ depends on common
       ┌────────────────────────┼────────────────────────────┐
       │                        │                            │
┌──────────────┐        ┌──────────────┐             ┌──────────────┐
│ module-project│        │ module-device │             │ module-agent  │
└──────────────┘        └──────────────┘             └──────────────┘
       ▲                        ▲                            │
       │ depends (任务/转包)      │ depends (项目设备)         │
       │                        │                            │
┌──────────────┐        ┌──────────────┐             ┌──────────────┐
│module-resource│        │module-delivery│             │module-finance │
└──────────────┘        └──────────────┘             └──────────────┘
       │                        │                            │
       │                        │ depends (工单/割接)         │
       │                        ▼                            │
       │                ┌──────────────┐                    │
       │                │module-accept. │                    │
       │                └──────────────┘                    │
       │                                                        │
       │                ┌──────────────┐   ┌──────────────┐    │
       │                │module-report │   │module-lowcode │    │
       │                └──────────────┘   └──────────────┘    │
       │                                                        │
       │                ┌────────────────┐                      │
       └───────────────▶│module-collabor.│◀─────────────────────┘
                        │module-integrat.│
                        └────────────────┘
```

**依赖原则：**

- 所有业务模块依赖 `module-common`（基类、Result、异常、工具）
- `module-auth` 与 `module-system` 是基础模块，被其他业务模块依赖（鉴权 / 字典 / 日志）
- `module-project` 是核心域，被 `module-delivery` / `module-acceptance` / `module-finance` 引用（项目任务 / 设备 / 阶段）
- `module-agent` 依赖 `module-project`（转包任务关联项目任务）和 `module-finance`（工作量结算）
- `module-lowcode` 与 `module-report` 是横向能力模块，不依赖具体业务模块
- `module-collaboration` 与 `module-integration` 是外部接入模块，依赖业务模块提供查询能力

---

## 六、统一响应与异常处理

### 6.1 统一响应体

**成功响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1719900000000
}
```

**分页响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ ... ],
    "total": 156,
    "page": 1,
    "size": 20,
    "pages": 8
  },
  "timestamp": 1719900000000
}
```

**错误响应：**

```json
{
  "code": 40001,
  "message": "项目名称不能为空",
  "errors": [
    { "field": "name", "message": "项目名称不能为空" }
  ],
  "timestamp": 1719900000000
}
```

### 6.2 错误码规范

| 错误码范围 | 含义                       | 备注                                       |
| --------- | -------------------------- | ------------------------------------------ |
| 200       | 成功                       | -                                          |
| 400xx     | 参数校验错误                | `MethodArgumentNotValidException` 自动捕获 |
| 401xx     | 认证错误（未登录/Token过期） | -                                          |
| 403xx     | 权限不足                   | `AccessDeniedException` 自动捕获           |
| 404xx     | 资源不存在                  | -                                          |
| 409xx     | 业务冲突（状态不允许/重复操作） | 状态机非法流转 / 唯一约束冲突              |
| 40904     | 数据已被他人修改            | 乐观锁版本冲突，提示「数据已被他人修改，请刷新后重试」 |
| 500xx     | 系统内部错误                | 兜底 `Exception`                           |
| 502xx     | 外部服务调用失败            | CRM/NMS/IM/物流/OA 调用异常                |

> 详见 [开发规范 - 异常处理规范](./development-guide.md#三异常处理规范)。

### 6.3 全局异常处理器

`@RestControllerAdvice` 统一捕获以下异常并转换为统一响应体：

- `BusinessException` → 返回对应业务错误码
- `MethodArgumentNotValidException` → 参数校验错误 400xx
- `AccessDeniedException` → 权限不足 403xx
- `Exception` → 系统错误 500xx

---

## 七、认证与权限

### 7.1 Token 机制

- 登录成功 → 签发 JWT Token（含 userId, roles, tenantType）
- Token 有效期：PC 8h / 移动端 7 天
- 刷新机制：每次请求自动续期（距过期 < 2h 时续签）
- 强制下线：Redis 存储 Token 黑名单

**Token 载荷结构：**

```json
{
  "userId": 1001,
  "userName": "zhangsan",
  "realName": "张三",
  "roles": ["PM", "ENGINEER"],
  "tenantType": "INTERNAL",
  "tenantId": null,
  "orgId": 101
}
```

| 字段        | 取值                                          |
| ----------- | --------------------------------------------- |
| tenantType  | `INTERNAL`（内部）/ `AGENT`（代理商）/ `CUSTOMER`（客户） |
| tenantId    | 代理商登录时填充代理商公司 ID；其他为 null     |

### 7.2 多端认证

| 终端      | 认证方式                              |
| ------- | --------------------------------- |
| PC 管理后台 | 账号密码 → JWT                        |
| 移动端     | 账号密码 / 飞书钉钉扫码 → JWT               |
| 代理商入口   | 账号密码 → JWT（tenantType=AGENT，权限受限） |
| 客户入口    | 手机号 + 短信验证码 → 临时 Token（有效期 2h）    |

### 7.3 权限模型

采用 **RBAC（角色-权限）+ 数据权限** 模型。

#### 内部角色

| 角色    | 编码           | 说明       |
| ----- | ------------ | -------- |
| 超级管理员 | SUPER_ADMIN  | 全系统权限    |
| 项目总监  | DIRECTOR     | 全部项目可见   |
| 项目经理  | PM           | 自己负责的项目  |
| 资源调度员 | DISPATCHER   | 人员排期与派单  |
| 实施工程师 | ENGINEER     | 分配给自己的任务 |
| 设备管理员 | DEVICE_ADMIN | 设备台账与出入库 |
| 财务人员  | FINANCE      | 预算/成本/结算 |

#### 外部角色

| 角色     | 编码             | 说明           |
| ------ | -------------- | ------------ |
| 代理商管理员 | AGENT_ADMIN    | 本公司数据，管理与查看  |
| 代理商工程师 | AGENT_ENGINEER | 仅自己的任务       |
| 客户联系人  | CUSTOMER       | 极有限只读 + 审批签核 |

### 7.4 数据权限规则

| 角色                      | 数据范围              |
| ----------------------- | ----------------- |
| SUPER_ADMIN / DIRECTOR | 全部数据              |
| PM                      | 自己负责的项目及项目下所有数据   |
| ENGINEER                | 分配给自己的任务及相关数据     |
| AGENT_ADMIN             | 本公司所有代理商数据        |
| AGENT_ENGINEER          | 分配给自己的任务          |
| CUSTOMER                | 自己关联的项目（只读，极有限字段） |

**实现方式：**

- 注解式接口权限：`@PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")`
- MyBatis 拦截器自动拼接 WHERE 条件实现行级数据隔离
- 代理商数据自动追加 `AND agent_company_id = 当前代理商ID`
- 内部敏感字段（合同金额 / 成本 / 客户联系方式）按角色脱敏

### 7.5 接口权限标识约定

权限标识格式：`{module}:{resource}:{action}`

| 权限标识                | 含义                 | 默认角色             |
| --------------------- | ------------------ | ---------------- |
| `project:list`        | 查看项目列表           | PM/DIRECTOR      |
| `project:add`         | 创建项目             | PM               |
| `project:remove`      | 删除项目             | SUPER_ADMIN      |
| `device:instance:*`  | 设备实例全权限         | DEVICE_ADMIN     |
| `agent:outsource:*`   | 转包任务全权限         | PM/DISPATCHER    |
| `lowcode:config:list` | 低代码配置查看         | SUPER_ADMIN      |
| `lowcode:config:add`  | 低代码配置新增/编辑    | SUPER_ADMIN      |
| `lowcode:config:remove` | 低代码配置删除       | SUPER_ADMIN      |
| `system:feedback:*`   | 反馈管理（待新增）       | SUPER_ADMIN      |

---

## 八、数据库设计规范

### 8.1 命名规范

| 对象   | 规范                | 示例                            |
| ---- | ----------------- | ----------------------------- |
| 表名   | 小写+下划线            | `project`, `device_instance`  |
| 字段   | 小写+下划线            | `project_name`, `create_time` |
| 主键   | id (BIGINT, 雪花算法) | `id`                          |
| 外键   | 关联表名_id           | `project_id`, `device_id`     |
| 索引   | idx_表名_字段        | `idx_project_customer_id`     |
| 唯一索引 | uk_表名_字段         | `uk_project_code`             |

### 8.2 公共字段（每张表必有）

由 `BaseEntity` 统一提供：

| 字段名          | 类型       | 说明           | MyBatis-Plus 处理                  |
| ------------ | -------- | ------------ | -------------------------------- |
| id           | BIGINT   | 主键，雪花算法      | `@TableId(type = IdType.ASSIGN_ID)` |
| create_by    | BIGINT   | 创建人ID        | `@TableField(fill = FieldFill.INSERT)` |
| create_time  | DATETIME | 创建时间         | `@TableField(fill = FieldFill.INSERT)` |
| update_by    | BIGINT   | 最后修改人ID      | `@TableField(fill = FieldFill.INSERT_UPDATE)` |
| update_time  | DATETIME | 最后修改时间       | `@TableField(fill = FieldFill.INSERT_UPDATE)` |
| deleted      | TINYINT  | 逻辑删除 0-否 1-是 | `@TableLogic` + `@TableField(fill = INSERT)` |
| version      | INT      | 乐观锁版本号       | `@Version`（MyBatis-Plus 自动处理）   |

### 8.3 数据库迁移脚本

迁移脚本位于 `vibe-server/vibe-server-bootstrap/src/main/resources/db/migration/`，遵循 Flyway 命名规范 `V{序号}__{描述}.sql`：

| 脚本                          | 内容                                                              |
| ----------------------------- | ----------------------------------------------------------------- |
| `V1__baseline.sql`            | 基线表结构：用户/角色/菜单/字典/项目/设备/代理商等核心表            |
| `V2__completeness_additions.sql` | 低代码 5 表（form/list/tab/relation/template）、其他补全           |
| `V3__flowable_schema.sql`     | Flowable 流程引擎表（割接/变更/工作量审批）                       |
| `V4__xxl_job_schema.sql`      | XXL-JOB 定时任务调度表                                            |
| `V5__integration_adapter.sql` | 集成适配器配置表与外部系统映射                                    |

### 8.4 索引规范

- 高频查询字段必须建立索引（如 `project_id`, `customer_id`, `status`, `agent_company_id`）
- 多字段组合查询建立联合索引，遵循「最左前缀」原则
- 唯一业务字段（如 `project_code`, `serial_number`, `phone`）建立唯一索引
- 避免在更新频繁的字段上建立过多索引

### 8.5 字段类型约定

| 业务含义     | 推荐类型          | 备注                                  |
| ------------ | ----------------- | ------------------------------------- |
| 主键         | BIGINT            | 雪花算法生成                           |
| 金额         | DECIMAL(18, 4)    | 避免浮点误差                          |
| 百分比       | INT               | 0-100 整数                            |
| 状态码       | VARCHAR(32)       | 枚举字符串，可读性优先                |
| JSON 数据    | JSON              | MySQL 8.0 原生支持                    |
| 时间戳       | DATETIME          | 不使用 TIMESTAMP（避免 2038 问题）   |
| 备注         | TEXT / VARCHAR(500) | 视长度而定                          |
| 是否删除     | TINYINT           | 0-否 1-是                            |

---

## 九、缓存设计

### 9.1 Redis Key 命名规范

```
{系统}:{模块}:{业务}:{标识}
```

示例：`vibe:auth:token:1001`

### 9.2 缓存 Key 列表

| Key                                | 用途              | TTL   |
| ---------------------------------- | ----------------- | ----- |
| `vibe:auth:token:{userId}`         | 用户 Token        | 8h    |
| `vibe:auth:perm:{userId}`          | 用户权限缓存      | 30min |
| `vibe:project:detail:{projectId}`  | 项目详情缓存      | 5min  |
| `vibe:device:status:{deviceId}`    | 设备状态缓存      | 1min  |
| `vibe:resource:calendar:{userId}`  | 工程师排期缓存    | 5min  |
| `vibe:integration:ratelimit:{api}` | 集成接口限流计数器 | 动态  |
| `vibe:agent:task:{agentId}`        | 代理商任务列表缓存 | 5min  |

### 9.3 缓存更新策略

- 写操作后主动删除缓存（**Cache Aside** 模式）
- 高频读场景设置较短 TTL + 后台异步刷新
- 全局配置类数据变更后广播清除（Redis Pub/Sub）
- 严禁「先删缓存后写库」造成脏读

> 详见 [开发规范 - 缓存规范](./development-guide.md#五缓存规范)。

---

## 十、集成架构

### 10.1 集成设计原则

采用 **适配器模式 + 事件驱动** 的统一集成方案。

### 10.2 统一适配器接口

```java
public interface ExternalSystemAdapter {
    String getSystemCode();                // 系统编码
    boolean healthCheck();                 // 健康检查
    void syncData(SyncRequest req);        // 数据同步
    void pushData(PushRequest req);        // 数据推送
    void handleCallback(CallbackData data); // 回调处理
}
```

> 实际代码位于 `module-integration`：`IntegrationConfigController` / `IntegrationSyncController` / `IntegrationCallLogController` 三个 Controller + `IntegrationConfigEntity` / `IntegrationCallLogEntity` 两个核心实体，落地于 `V5__integration_adapter.sql` 迁移脚本。

### 10.3 各集成系统详情

#### ① CRM/ERP（SAP/Oracle/用友）

| 方向 | 事件      | 系统动作              |
| -- | ------- | ----------------- |
| 入站 | 销售订单签订  | 自动创建实施项目 + 关联 BOM |
| 入站 | 设备发货通知  | 更新设备运输状态          |
| 入站 | 客主数据变更  | 同步客户信息            |
| 出站 | 项目结项    | 触发开票/收款流程         |
| 出站 | 工时/成本数据 | 回写人力成本            |

#### ② NMS 网管（eSight/iMaster NCE）

| 方向 | 事件     | 系统动作     |
| -- | ------ | -------- |
| 入站 | 设备上线事件 | 自动标记安装完成 |
| 入站 | 设备告警   | 关联项目创建问题 |
| 出站 | 新设备入网  | 注册设备     |
| 出站 | 配置下发   | 批量配置     |

> NMS 网管对接为 Phase 3 范围，不在 enterprise-completion 范围内。

#### ③ IM 平台（飞书/钉钉/企微）

- 任务派发通知（自有工程师 + 代理商工程师）
- 割接审批通知 → 客户/管理层
- 验收签核通知 → 客户
- 风险预警 → PM/管理层
- 进度日报 → 项目群

#### ④ 物流平台

| 方向 | 事件     | 系统动作     |
| -- | ------ | -------- |
| 入站 | 物流轨迹更新 | 更新设备运输状态 |
| 入站 | 签收通知   | 触发到货确认流程 |
| 出站 | 创建发货单  | 生成物流运单   |

#### ⑤ OA/财务系统

- 审批流对接（割接审批、变更审批、费用审批）
- 组织架构同步
- 费用报销数据同步

### 10.4 集成安全

- 所有外部调用统一走集成网关，**禁止业务层直接调外部系统**
- 密钥/API Key 统一存储在配置中心（加密），禁止硬编码
- 所有调用记录日志（请求/响应/耗时/状态），落到 `integration_call_log` 表
- 熔断降级：Sentinel / Resilience4j，外部系统不可用不影响核心流程
- 重试机制：指数退避，最多重试 3 次

---

## 十一、消息通知架构

### 11.1 通知引擎架构

```
业务事件 ── 消息队列 ──> Notification Service ──> 触达渠道
                                                    ├── 飞书消息
                                                    ├── 钉钉消息
                                                    ├── 企微消息
                                                    ├── 短信
                                                    ├── 邮件
                                                    └── 站内信
```

通知引擎能力：

- 模板渲染（变量替换）
- 渠道路由（根据事件类型和接收人选择渠道）
- 频率控制（避免短时间内重复通知）

### 11.2 通知模板示例

```
模板编码：TASK_ASSIGNED
标题：您有新的实施任务
内容：
  【任务派发】{assignerName} 将任务「{taskName}」分配给您。
  项目：{projectName}
  客户：{customerName}
  截止日期：{deadline}
  请登录系统查看详情。
渠道：飞书 + 站内信
```

### 11.3 触达规则

| 接收人类型        | 渠道选择                      |
| ------------ | ------------------------- |
| 内部人员（PM/工程师） | 飞书/钉钉 + 站内信               |
| 代理商          | 飞书/钉钉 + 站内信（模板不同，不暴露内部信息） |
| 客户           | 短信/邮件 + H5 链接             |
| 紧急事项         | 多渠道同时触达                   |

### 11.4 通知触达矩阵

| 事件      | 内部人员     | 代理商    | 客户     | 渠道    |
| ------- | -------- | ------ | ------ | ----- |
| 任务派发    | ✅        | ✅      | -      | 飞书/钉钉 |
| 任务即将到期  | ✅        | ✅      | -      | 飞书/钉钉 |
| 任务超期    | ✅        | ✅      | -      | 飞书+短信 |
| 交付物待审核  | ✅        | -      | -      | 飞书/钉钉 |
| 交付物被退回  | -        | ✅      | -      | 飞书/钉钉 |
| 交付物审核通过 | -        | ✅      | -      | 飞书/钉钉 |
| 割接方案待审批 | ✅        | -      | ✅      | 飞书+短信 |
| 割接审批结果  | ✅        | -      | -      | 飞书/钉钉 |
| 验收申请    | -        | -      | ✅      | 短信+邮件 |
| 验收签核结果  | ✅        | -      | -      | 飞书/钉钉 |
| 设备到货    | ✅        | -      | -      | 飞书/钉钉 |
| 设备异常    | ✅        | -      | -      | 飞书+短信 |
| 项目风险预警  | ✅(PM+上级) | -      | -      | 飞书/钉钉 |
| 工作量确认   | ✅        | ✅      | -      | 飞书/钉钉 |

---

## 十二、文件存储

### 12.1 存储引擎

MinIO（兼容 S3 协议）

### 12.2 Bucket 规划

| Bucket             | 用途                        |
| ------------------ | ------------------------- |
| `vibe-photos`      | 施工照片、现场照片、勘察照片            |
| `vibe-documents`   | 合同、方案、验收报告、竣工文档、配置备份      |
| `vibe-attachments` | 任务附件、评论附件、其他文件            |
| `vibe-avatars`     | 用户头像                      |
| `vibe-exports`     | 导出文件（Excel/PDF 临时存储，7天过期） |

### 12.3 目录结构

```
/{bucket}/{类型}/{年月}/{业务ID}/{文件名}
```

示例：`/vibe-photos/construct/202507/project_1001/task_5001/photo1.jpg`

### 12.4 图片处理

- 上传时自动压缩（质量 85%，长边不超过 2048px）
- 生成缩略图（用于列表展示）
- 施工照片自动添加水印（时间 + GPS 坐标 + 上传人）

### 12.5 上传方式

- PC 端：前端直传 MinIO（预签名 URL）
- 移动端：前端直传 + 进度条 + 断点续传
- 大文件（>100MB）：分片上传

---

## 十三、部署架构

### 13.1 开发环境（Docker Compose）

```yaml
# docker-compose.yml
services:
  vibe-server:     # Spring Boot 应用 (8080)
  vibe-web:        # Nginx + Vue 前端 (80/443)
  mysql:           # MySQL 8.0 (3306)
  redis:           # Redis 7.x (6379)
  elasticsearch:   # ES 8.x (9200)
  minio:           # MinIO (9000/9001)
  rabbitmq:        # RabbitMQ (5672/15672)
```

### 13.2 生产环境

```
用户 → CDN → Nginx集群(2+) → Spring Boot集群(2+)
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
               MySQL主从     Redis Sentinel   ES集群
               (1主2从)     (1主2从3哨兵)    (3节点)

               MinIO集群(4节点)  RabbitMQ镜像集群(3节点)
```

### 13.3 环境划分

| 环境          | 说明                   |
| ----------- | -------------------- |
| dev（开发）     | Docker Compose 本地启动  |
| test（测试）    | 单台服务器 Docker Compose |
| staging（预发） | 与生产同架构，独立数据库         |
| prod（生产）    | 高可用集群部署              |

### 13.4 资源要求

详见 [部署指南](./deployment-guide.md#二环境要求)。

| 组件            | 最低配置   | 推荐配置           |
| --------------- | ---------- | ------------------ |
| CPU             | 4 核       | 8 核               |
| 内存            | 8 GB       | 16 GB             |
| 磁盘            | 100 GB SSD | 500 GB SSD        |
| 操作系统         | CentOS 7+ / Ubuntu 20+ | Ubuntu 22.04 LTS |
| Docker          | 24.0+      | 25.0+             |
| Docker Compose  | v2         | v2.24+            |

---

## 十四、CI/CD 流程

```
代码提交 → GitLab CI 自动构建 → 单元测试 → 镜像推送 →
测试环境自动部署 → staging 手动确认 → 生产灰度发布
```

### 14.1 流程节点

| 节点           | 工具                       | 触发条件              | 产物                          |
| -------------- | -------------------------- | --------------------- | ----------------------------- |
| 代码提交       | Git                        | Push / Merge Request  | -                             |
| 自动构建       | GitLab CI / Maven          | Push 到 dev/test 分支 | 编译产物 + 单元测试报告       |
| 镜像构建       | Docker / Kaniko            | 测试通过              | Docker 镜像（推送至镜像仓库）  |
| 测试环境部署    | Docker Compose             | 镜像推送成功          | test 环境服务更新              |
| 健康检查       | curl `/actuator/health`    | 部署后                | 3 次连续 200 视为成功         |
| staging 部署   | 手动触发                    | 测试环境验收通过      | staging 环境服务更新          |
| 生产灰度发布    | 手动触发                    | staging 验收通过      | prod 环境滚动更新             |
| 失败回滚       | `scripts/rollback.ps1`     | 健康检查失败 / 手动触发 | 回滚到上一版本镜像            |

### 14.2 一键部署脚本

详见 [部署指南](./deployment-guide.md) 与 `scripts/deploy.ps1`：

```powershell
# 一键部署到生产
scripts/deploy.ps1 -Env prod -Version v1.2.0

# 失败自动回滚
scripts/rollback.ps1 -Env prod -Version v1.1.9
```

### 14.3 配置文件分层

```
vibe-server-bootstrap/src/main/resources/
├── application.yml              # 通用配置
├── application-dev.yml          # 开发环境（连本地中间件）
├── application-test.yml         # 测试环境
├── application-staging.yml      # 预发环境
├── application-prod.yml         # 生产环境（环境变量注入敏感信息）
└── db/migration/                # Flyway 迁移脚本
```

### 14.4 监控与告警

- **指标采集**：Spring Boot Actuator + Micrometer → Prometheus
- **可视化**：Grafana 仪表盘（JVM / HTTP / 数据库连接池 / 业务指标）
- **日志聚合**：ELK 或 Loki + Grafana
- **告警规则**：
  - 服务健康检查失败 ≥ 3 次 → 飞书/钉钉告警
  - 接口 P95 响应时间 > 3s 持续 5 分钟 → 告警
  - JVM 堆内存使用率 > 85% → 告警
  - 数据库连接池活跃连接 > 80% → 告警

---

## 十五、本轮迭代架构补充（Task 1-21）

> 章节来源：enterprise-completion Spec 执行 Agent 于 2026-07-06 补充
> 章节目的：在 V1.0 已有架构说明基础上，补充本轮 Task 1-21 完成后新增的架构元素与对齐情况
> 范围：低代码架构完整说明、异常处理三层闭环架构、新增前端组件清单、用户引导架构、模块清单与技术栈对齐结果

### 15.1 模块清单与最终实现对齐

V1.0 第五章「后端模块清单与依赖」已列出 14 个业务模块 + bootstrap，本轮迭代未新增模块，但完成以下对齐：

| 对齐项 | V1.0 描述 | 实际状态 | 对齐结果 |
| ------ | --------- | -------- | -------- |
| `module-lowcode` | 已在模块清单（5 Controller / 5 Entity） | 后端完整 + 前端 6 组件 6 视图 + 3 业务示例 + 100% 测试覆盖 | 前后端完整落地 |
| `module-common` | 公共基类 + Result + 异常 + 工具 | 新增 `@OperationLog` 注解 + `OperationLogAspect`（位于 module-system）+ `MaxUploadSizeExceededException` / `HttpMediaTypeNotSupportedException` 异常处理 | 异常处理能力增强 |
| `module-system` | 12 Controller / 13 Entity | 新增 `FeedbackController` + `SysFeedbackEntity` + 反馈管理（已在 V1.0 §5.1 列出 12 Controller，实际为 16 个含 Feedback） | 反馈系统新增 |
| Controller 总数 | ~62 个 | 实际 68 个（V1.0 估算偏少） | 已在 [开发规范 - 第九章 操作日志审计报告](./development-guide.md#九操作日志审计报告) 校正 |
| 业务实体总数 | ~80 个 | 实际 ~80 个（含 `SysFeedbackEntity`） | 一致 |

### 15.2 低代码架构详细说明

> V1.0 第十章「集成架构」未涵盖低代码架构，本节补充。

#### 15.2.1 低代码整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│ 展现层                                                              │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 低代码设计器（仅 SUPER_ADMIN）                                  │ │
│ │ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │ │
│ │ │ Schema       │ │ FieldPalette │ │ PropertyPanel│            │ │
│ │ │ Designer     │ │ 字段库       │ │ 属性面板     │            │ │
│ │ │ 设计画布     │ │              │ │              │            │ │
│ │ └──────────────┘ └──────────────┘ └──────────────┘            │ │
│ │ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │ │
│ │ │ Schema       │ │ Schema       │ │ Schema       │            │ │
│ │ │ Preview      │ │ Importer     │ │ Exporter     │            │ │
│ │ │ 预览渲染     │ │ JSON 导入    │ │ JSON 导出    │            │ │
│ │ └──────────────┘ └──────────────┘ └──────────────┘            │ │
│ └─────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 运行时渲染器（业务用户）                                        │ │
│ │ RuntimeRenderer：根据 Schema 动态渲染表单/列表/标签页/关联页    │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ /api/v1/lowcode/*
┌──────────────────────────────────▼──────────────────────────────────┐
│ 业务服务层（module-lowcode）                                        │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐│
│ │ FormConfig   │ │ ListConfig   │ │ TabConfig    │ │ RelationConf ││
│ │ Controller   │ │ Controller   │ │ Controller    │ │ Controller   ││
│ │ 表单配置      │ │ 列表配置     │ │ 标签页配置    │ │ 关联页配置   ││
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘│
│ ┌──────────────┐                                                    │
│ │ Template     │                                                    │
│ │ Controller   │                                                    │
│ │ 模板库        │                                                    │
│ └──────────────┘                                                    │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ Service / Mapper / Entity 五套                                   │ │
│ │ lowcode_form_config / list_config / tab_config / relation_config │ │
│ │ / template                                                      │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │
                                   ▼
                          数据层（MySQL lowcode_* 五表）
```

#### 15.2.2 五类配置器职责

| 配置器 | 后端 Controller | 前端视图 | 数据表 | 用途 |
| ------ | --------------- | -------- | ------ | ---- |
| 表单配置 | FormConfigController | form-config.vue | lowcode_form_config | 定义表单字段、布局、校验规则 |
| 列表配置 | ListConfigController | list-config.vue | lowcode_list_config | 定义列表列、筛选器、操作按钮、数据源 API |
| 标签页配置 | TabConfigController | tab-config.vue | lowcode_tab_config | 定义详情页标签页布局与内容 |
| 关联页配置 | RelationConfigController | relation-config.vue | lowcode_relation_config | 定义 master-detail 关联关系页 |
| 模板库 | TemplateController | template-library.vue | lowcode_template | 模板 CRUD + 复制 + 导入导出 + 实例化 |

#### 15.2.3 Schema 数据结构

低代码配置以 JSON Schema 形式持久化，结构示例：

```json
{
  "type": "FORM",
  "bizType": "customer",
  "version": 1,
  "fields": [
    {
      "name": "customerName",
      "label": "客户名称",
      "type": "INPUT",
      "required": true,
      "rules": { "max": 128 },
      "width": "100%"
    },
    {
      "name": "phone",
      "label": "联系电话",
      "type": "INPUT",
      "required": true,
      "rules": { "pattern": "^1[3-9]\\d{9}$" }
    }
  ]
}
```

#### 15.2.4 业务示例

本轮迭代实现 3 个业务示例验证低代码能力：

| 示例 | 类型 | bizType | 说明 |
| ---- | ---- | ------- | ---- |
| 客户档案表单 | FORM | customer | 客户 CRUD 表单的 Schema 化定义 |
| 项目列表 | LIST | project | 项目列表的列定义与筛选器 Schema 化 |
| 设备台账标签页 | TAB | device | 设备详情页的标签页布局 Schema 化 |

### 15.3 异常处理三层闭环架构

> V1.0 第六章「统一响应与异常处理」描述了 GlobalExceptionHandler，但未涵盖前端表单校验与后端 DTO 校验的闭环关系。本节补充完整的三层闭环架构。

#### 15.3.1 三层闭环架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│ 第一层：前端表单校验拦截                                             │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ Ant Design Vue a-form :rules + ref.validate()                  │ │
│ │ 在用户提交前拦截无效输入，减少无效请求                           │ │
│ │ 本轮修复：7 个关键表单页面的 :rules 配置                        │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ 仅校验通过才提交
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 第二层：后端业务校验                                                 │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 第二层 A：Bean Validation（@Valid + @NotBlank / @NotNull / ...） │ │
│ │ 本轮修复：6 个 DTO 新增校验注解；2 个 Controller 新增 @Valid     │ │
│ │ 触发：MethodArgumentNotValidException → 40001 + errors[]        │ │
│ └─────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 第二层 B：Service 业务规则校验                                  │ │
│ │ 状态机校验：BusinessException.stateNotAllowed() → 40901         │ │
│ │ 引用关系校验：BusinessException.conflict() → 40902              │ │
│ │ 唯一性校验：BusinessException.duplicate() → 40902                │ │
│ │ 本轮修复：2 个 ServiceImpl.delete 新增引用校验                  │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ 校验失败抛 BusinessException
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 第三层：展示层错误提示友好化                                         │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 后端 GlobalExceptionHandler（17 类异常处理）                     │ │
│ │ 本轮新增：MaxUploadSizeExceededException（413）                  │ │
│ │          HttpMediaTypeNotSupportedException（415）               │ │
│ │ 统一封装为 Result<T> 响应体                                     │ │
│ └─────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ 前端 request.ts 响应拦截器（9 个错误码区间处理）                 │ │
│ │ 400xx：提取 errors[] 高亮表单字段                               │ │
│ │ 401xx：跳转登录                                                  │ │
│ │ 403xx：提示权限不足                                              │ │
│ │ 404xx：提示资源不存在                                            │ │
│ │ 409xx：直接展示后端 message                                      │ │
│ │ 413/415：HTTP 错误分支处理                                       │ │
│ │ 500xx：提示系统异常                                              │ │
│ │ 502xx：提示操作失败                                              │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

#### 15.3.2 三层闭环覆盖的异常类型

| 异常类型 | 第一层（前端） | 第二层 A（DTO） | 第二层 B（Service） | 第三层（展示） |
| -------- | -------------- | --------------- | ------------------- | -------------- |
| 必填字段为空 | `:rules` required | `@NotBlank` / `@NotNull` | - | 40001 + errors[] |
| 格式错误（手机/邮箱） | `:rules` pattern | `@Pattern` / `@Email` | - | 40001 + errors[] |
| 长度超限 | `:rules` max | `@Size(max=)` | - | 40001 + errors[] |
| 数值范围 | `:rules` min/max | `@Min` / `@Max` | - | 40001 + errors[] |
| 唯一性冲突 | - | - | `BusinessException.duplicate()` | 40902 |
| 状态机非法流转 | 按钮置灰 | - | `BusinessException.stateNotAllowed()` | 40901 |
| 引用关系冲突 | - | - | `BusinessException.conflict()` | 40902 |
| 乐观锁冲突 | - | - | `OptimisticLockingFailureException` | 40904 |
| 文件超限 | - | - | - | 413（MaxUploadSize） |
| Content-Type 错误 | - | - | - | 415（HttpMediaType） |
| 权限不足 | - | - | `AccessDeniedException` | 40301 |
| 资源不存在 | - | - | `BusinessException.notFound()` | 40401 |
| 外部服务失败 | - | - | `ExternalException` | 50201 |
| 系统异常 | - | - | `Exception` 兜底 | 50000 |

### 15.4 新增前端组件清单

> V1.0 第二章「技术选型」与第十章未涵盖本轮新增的前端组件，本节补充。

| 组件 | 路径 | 类型 | 用途 | 实现来源 |
| ---- | ---- | ---- | ---- | -------- |
| SchemaDesigner | `vibe-web/src/components/Lowcode/SchemaDesigner.vue` | 业务组件 | 低代码 Schema 设计画布（三栏布局：字段库 + 画布 + 属性面板） | Task 13-19 |
| FieldPalette | `vibe-web/src/components/Lowcode/FieldPalette.vue` | 业务组件 | 字段库（3 分组 / 10 字段类型 / dragstart） | Task 13-19 |
| PropertyPanel | `vibe-web/src/components/Lowcode/PropertyPanel.vue` | 业务组件 | 字段属性编辑面板（label/required/rules/width） | Task 13-19 |
| SchemaPreview | `vibe-web/src/components/Lowcode/SchemaPreview.vue` | 业务组件 | Schema 实时预览渲染（4 种 schema 类型） | Task 13-19 |
| SchemaImporter | `vibe-web/src/components/Lowcode/SchemaImporter.vue` | 业务组件 | JSON Schema 导入对话框（4 类校验） | Task 13-19 |
| RuntimeRenderer | `vibe-web/src/components/Lowcode/RuntimeRenderer.vue` | 业务组件 | 运行时渲染器（根据 Schema 动态渲染表单/列表/标签页/关联页） | Task 13-19 |
| OnboardingTour | `vibe-web/src/components/Onboarding/OnboardingTour.vue` | 业务组件 | 5 步交互式新手教程（工作台/创建项目/派发任务/查看设备/提交验收） | Task 10 |
| HelpHint | `vibe-web/src/components/Onboarding/HelpHint.vue` | 业务组件 | 上下文帮助气泡（悬浮 `?` 图标显示） | Task 10 |
| FeedbackButton | `vibe-web/src/components/Feedback/FeedbackButton.vue` | 业务组件 | 右下角悬浮反馈按钮（功能建议/Bug/咨询） | enterprise-completion |

### 15.5 用户引导架构

> V1.0 未涵盖用户引导架构，本节补充。

#### 15.5.1 引导能力分层

```
┌─────────────────────────────────────────────────────────────────────┐
│ 系统级引导                                                          │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ OnboardingTour：首次登录自动触发的 5 步交互式教程               │ │
│ │ 存储：localStorage onboarding_tour_done / onboarding_done       │ │
│ │ 触发：first_login=1 或 localStorage 标记不存在                   │ │
│ │ 跳过：教程气泡右下角「跳过教程」按钮                              │ │
│ │ 重启：右上角 ? 图标 → 「重新查看教程」                            │ │
│ └─────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────┤
│ 页面级帮助                                                          │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ HelpHint：页面标题右侧 ? 图标，悬浮显示该页面操作要点            │ │
│ │ 集成页面：dashboard / project/list / device/ledger /            │ │
│ │           delivery/board / agent/profile / system/user（6 个）   │ │
│ └─────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────┤
│ 反馈通道                                                            │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ FeedbackButton：右下角悬浮按钮，全端用户可提交反馈               │ │
│ │ 类型：SUGGESTION / BUG / CONSULT                                │ │
│ │ 处理：SUPER_ADMIN 在「系统管理 > 反馈管理」处理                  │ │
│ └─────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

#### 15.5.2 引导状态管理

| 状态 | 存储位置 | 说明 |
| ---- | -------- | ---- |
| 教程完成标记 | `localStorage.onboarding_tour_done` | 完成或跳过后写入，不再自动弹出 |
| 教程跳过标记 | `localStorage.onboarding_done` | 同上，双重标记 |
| 重启触发 | 右上角 ? 图标点击 | 清除标记后重新触发，或调用 `appStore.triggerTutorial()` |

### 15.6 技术栈对齐结果

| 层次 | V1.0 描述 | 实际使用 | 对齐结果 |
| ---- | --------- | -------- | -------- |
| 后端 ORM | MyBatis-Plus 3.5.x | 实际使用 MyBatis-Plus 3.5.x | ✅ 一致 |
| 对象转换 | MapStruct | 实际使用 BeanUtils.copyProperties | ⚠️ 偏差（见 [需求总览 D3](./requirement-overview.md#103-实现偏差)） |
| 集成安全 | Sentinel / Resilience4j 熔断降级 | 未显式实现 | 🟡 待补 |
| 前端甘特图 | dhtmlx-gantt 8.x | 组件已开发未在业务页面使用 | 🟡 待接入（见 [需求总览 L1](./requirement-overview.md#102-逻辑缺陷)） |
| 前端地图 | 高德地图 JS API 2.0 | AMap 组件已开发未在项目列表使用 | 🟡 待接入 |
| 测试框架（后端） | JUnit 5 + Mockito | 实际使用，累计 17 个测试类 | ✅ 一致 |
| 测试框架（前端） | Vitest + @vue/test-utils | 实际使用，49 文件 1052 用例 | ✅ 一致 |
| E2E 框架 | Playwright | 实际使用 Vitest 运行 e2e-smoke.spec.ts | ✅ 一致（用 Vitest 替代 Playwright 运行 e2e） |
| 操作日志 | -（V1.0 未描述） | `@OperationLog` 注解 + `OperationLogAspect` AOP 切面 | ✅ 本轮新增 |
| 异常处理 | GlobalExceptionHandler | 实际 17 类异常处理（含本轮新增 2 类） | ✅ 增强 |

### 15.7 架构与最终实现对齐总结

| 架构维度 | V1.0 描述 | 最终实现 | 对齐状态 |
| -------- | --------- | -------- | -------- |
| 系统架构分层 | 6 层（展现/接入/网关/业务/平台/数据） | 与 V1.0 一致（网关层为预留） | ✅ |
| 后端模块清单 | 14 业务模块 + bootstrap | 与 V1.0 一致 | ✅ |
| 模块依赖关系 | module-common 基础 + module-project 核心 | 与 V1.0 一致 | ✅ |
| 统一响应体 | Result<T> + PageResult<T> | 与 V1.0 一致 | ✅ |
| 错误码规范 | 200/400xx/401xx/403xx/404xx/409xx/500xx/502xx | 与 V1.0 一致 + 413/415 新增 | ✅ 增强 |
| 认证与权限 | JWT + RBAC + 数据权限 | 与 V1.0 一致 | ✅ |
| 数据库规范 | BaseEntity + 逻辑删除 + 乐观锁 + Flyway | 与 V1.0 一致（dev 用 init-db.js 替代 Flyway） | ✅ |
| 缓存设计 | Caffeine L1 + Redis L2 | 与 V1.0 一致 | ✅ |
| 集成架构 | ExternalSystemAdapter 统一接口 | 偏差：各系统独立 Service | ⚠️ 偏差 |
| 消息通知 | 14 类领域事件 + RabbitMQ | 与 V1.0 一致 | ✅ |
| 文件存储 | MinIO + 5 Bucket | 偏差：单一 bucket | ⚠️ 偏差 |
| 部署架构 | Docker Compose + 4 环境配置 | 与 V1.0 一致 | ✅ |
| CI/CD | GitLab CI | 与 V1.0 一致 | ✅ |
| 低代码架构 | V1.0 未描述 | 本轮补充（§15.2） | ✅ 新增 |
| 异常处理三层闭环 | V1.0 仅描述第三层 | 本轮补充完整三层（§15.3） | ✅ 新增 |
| 用户引导架构 | V1.0 未描述 | 本轮补充（§15.5） | ✅ 新增 |
