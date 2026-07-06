# 开发规范

> 文档版本：V1.0
> 更新日期：2026-07-06
> 基线文档：`系统设计文档.md` 1.3 / 1.4 / 1.6 / 1.7 节
> 配套文档：[架构设计](./design-architecture.md) | [状态机转换矩阵](./state-machine.md) | [测试策略](./test-strategy.md)

---

## 目录

- [一、命名规范](#一命名规范)
- [二、后端分层规范](#二后端分层规范)
- [三、异常处理规范](#三异常处理规范)
- [四、日志规范](#四日志规范)
- [五、缓存规范](#五缓存规范)
- [六、前端规范](#六前端规范)
- [七、Git 提交规范](#七git-提交规范)
- [八、代码评审 Checklist](#八代码评审-checklist)

---

## 一、命名规范

### 1.1 数据库命名

| 对象       | 规范                              | 示例                                   |
| ---------- | --------------------------------- | -------------------------------------- |
| 表名       | 全小写 + 下划线，单数              | `project`, `device_instance`           |
| 字段名     | 全小写 + 下划线                    | `project_name`, `create_time`          |
| 主键       | `id`，BIGINT，雪花算法             | `id`                                   |
| 外键       | 关联表名 + `_id`                   | `project_id`, `device_id`              |
| 索引       | `idx_表名_字段`                    | `idx_project_customer_id`              |
| 唯一索引   | `uk_表名_字段`                     | `uk_project_code`                      |
| 布尔字段   | `is_` 前缀，TINYINT 0/1            | `is_active`, `deleted`                 |
| 时间字段   | `_time` 后缀，DATETIME             | `create_time`, `apply_time`            |
| 状态字段   | `status`，VARCHAR(32)，枚举字符串  | `status = 'EXECUTE'`                   |
| 金额字段   | `_amount` 后缀，DECIMAL(18,4)      | `total_amount`, `travel_amount`        |

### 1.2 Java 类命名

| 对象类型     | 命名规则                          | 示例                                    |
| ------------ | --------------------------------- | --------------------------------------- |
| Entity       | `XxxEntity`，与表名一一对应        | `ProjectEntity`, `DeviceInstanceEntity` |
| DTO          | `XxxDTO`（接口入参）               | `ProjectCreateDTO`, `ProjectQueryDTO`   |
| VO           | `XxxVO`（接口出参）                | `ProjectDetailVO`, `ProjectListVO`      |
| BO           | `XxxBO`（Service 内部业务对象）    | `ProjectBO`                             |
| Service 接口 | `XxxService`                      | `ProjectService`                        |
| Service 实现 | `XxxServiceImpl`                  | `ProjectServiceImpl`                   |
| Mapper       | `XxxMapper`                      | `ProjectMapper`                         |
| Controller   | `XxxController`                  | `ProjectController`                    |
| Enum         | `XxxEnum`                        | `ProjectStatusEnum`                     |
| Constant     | `XxxConstant`                    | `ProjectConstant`, `DeliveryConstant`  |
| Exception    | `XxxException`                   | `BusinessException`                     |
| Event        | `XxxEvent`                       | `ProjectCreatedEvent`                   |

### 1.3 方法命名

| 场景              | 命名规则                                   | 示例                                    |
| ----------------- | ------------------------------------------ | --------------------------------------- |
| 查询单条          | `getById` / `detail`                       | `getById(Long id)`                      |
| 分页查询          | `page`                                     | `page(PageQuery query)`                 |
| 列表查询          | `list`                                     | `list(ProjectQueryDTO query)`           |
| 新增              | `create` / `add`                           | `create(ProjectCreateDTO dto)`          |
| 更新              | `update`                                   | `update(Long id, ProjectUpdateDTO dto)` |
| 删除              | `delete` / `remove`                        | `delete(Long id)`                       |
| 状态流转          | 动词 + 状态名                              | `submitApproval`, `pmConfirm`, `internalApprove` |
| 批量操作          | `batch` 前缀                               | `batchAssign(List<Long> ids)`           |
| 校验              | `check` 前缀                               | `checkCanDelete(Long id)`               |
| 转换              | `to` + 目标类型                            | `toVO(ProjectEntity entity)`            |

### 1.4 变量命名

- **驼峰命名**：`projectName`, `customerId`, `assigneeId`
- **常量**：全大写 + 下划线，`public static final String PROJECT_STATUS_EXECUTE = "EXECUTE";`
- **集合**：复数形式或加 `List` / `Map` 后缀，`projectList`, `taskMap`
- **布尔**：`is` / `has` / `can` 前缀，`isActive`, `hasPermission`, `canDelete`
- **避免缩写**：`btn` → `button`，`info` → `information`（业务领域缩写如 `PM`、`BOM`、`SN` 可保留）

### 1.5 接口路径命名

| 资源       | 路径                              | 方法                          | 说明                  |
| ---------- | --------------------------------- | ----------------------------- | --------------------- |
| 列表/分页  | `/api/v1/{module}/{resource}`     | GET                           | 支持查询参数          |
| 详情       | `/api/v1/{module}/{resource}/{id}` | GET                           | -                     |
| 新增       | `/api/v1/{module}/{resource}`     | POST                          | -                     |
| 更新       | `/api/v1/{module}/{resource}/{id}` | PUT                           | 含乐观锁 version 校验 |
| 删除       | `/api/v1/{module}/{resource}/{id}` | DELETE                        | 逻辑删除              |
| 自定义动作 | `/api/v1/{module}/{resource}/{id}/{action}` | POST                          | 如 `/tasks/{id}/assign` |

**示例**：

- `GET    /api/v1/projects` — 项目分页列表
- `GET    /api/v1/projects/{id}` — 项目详情
- `POST   /api/v1/projects` — 创建项目
- `PUT    /api/v1/projects/{id}` — 更新项目
- `DELETE /api/v1/projects/{id}` — 删除项目
- `POST   /api/v1/projects/{id}/start` — 启动项目（状态流转）
- `POST   /api/v1/lowcode/forms/{id}/copy` — 复制低代码配置

---

## 二、后端分层规范

### 2.1 请求流向

```
Client → Controller → Service → Repository → Database
```

### 2.2 Controller 层职责

**必须做：**

- 接收请求参数，使用 `@Valid` 触发参数校验
- 调用 Service 层，**不含业务逻辑**
- 包装返回结果为统一响应体 `Result<T>` / `PageResult<T>`
- 使用 `@PreAuthorize` 声明接口权限
- 使用 `@Tag` / `@Operation` 注解生成 OpenAPI 文档

**禁止：**

- 直接调用 Mapper
- 包含 if/else 业务分支判断
- 直接操作数据库
- 在 Controller 中处理事务

**示例：**

```java
@Slf4j
@Tag(name = "项目", description = "项目 CRUD 与状态流转")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询项目")
    @PreAuthorize("@ss.hasPermi('project:list') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<ProjectListVO>> page(@ParameterObject PageQuery query,
                                                    @ParameterObject ProjectQueryDTO queryDTO) {
        return Result.success(projectService.page(query, queryDTO));
    }

    @Operation(summary = "创建项目")
    @PreAuthorize("@ss.hasPermi('project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProjectCreateDTO dto) {
        return Result.success(projectService.create(dto));
    }
}
```

### 2.3 Service 层职责

**必须做：**

- 核心业务逻辑
- 事务管理：写操作必须加 `@Transactional(rollbackFor = Exception.class)`
- 调用 Mapper / 其他 Service / 外部服务
- 状态机校验：状态流转方法必须显式校验当前状态
- 操作日志：状态变更/审批/删除必须记录 SysLog
- 发送领域事件（如需要）

**禁止：**

- 直接处理 HTTP 请求/响应
- 在 Service 中拼接 SQL

**示例：**

```java
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final SysLogService sysLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateDTO dto) {
        // 校验：项目编号唯一
        if (projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getProjectCode, dto.getProjectCode())) > 0) {
            throw BusinessException.duplicate("项目编号已存在");
        }
        // 转换并填充默认值
        ProjectEntity entity = ProjectConverter.INSTANCE.toEntity(dto);
        entity.setStatus(ProjectConstant.PROJECT_STATUS_INIT);
        entity.setProgressPct(0);
        projectMapper.insert(entity);
        // 操作日志
        sysLogService.record("Project", "create", entity.getId(),
            null, "INIT", "创建项目：" + entity.getProjectName());
        return entity.getId();
    }
}
```

### 2.4 Mapper 层职责

**必须做：**

- 数据库 CRUD 操作
- 复杂 SQL 查询（MyBatis XML Mapper）
- 继承 `BaseMapper<T>` 获得基础 CRUD

**禁止：**

- 包含业务逻辑
- 在 SQL 中拼接业务判断条件（用代码处理）

**示例：**

```java
public interface ProjectMapper extends BaseMapper<ProjectEntity> {

    /**
     * 自定义查询：按客户与状态分组统计项目数
     */
    List<ProjectStatVO> statByCustomerAndStatus(@Param("customerId") Long customerId);
}
```

```xml
<!-- ProjectMapper.xml -->
<select id="statByCustomerAndStatus" resultType="com.vibe.project.vo.ProjectStatVO">
    SELECT customer_id, status, COUNT(*) AS cnt
    FROM project
    WHERE deleted = 0
      <if test="customerId != null">AND customer_id = #{customerId}</if>
    GROUP BY customer_id, status
</select>
```

---

## 三、异常处理规范

### 3.1 错误码范围表

| 错误码范围 | 含义                       | 触发场景                                            | HTTP Status |
| ---------- | -------------------------- | --------------------------------------------------- | ----------- |
| 200        | 成功                       | -                                                   | 200         |
| 40001      | 参数校验失败               | `MethodArgumentNotValidException` 自动捕获          | 400         |
| 40002      | 参数格式错误               | `HttpMessageNotReadableException`                   | 400         |
| 40101      | 未登录                     | Token 缺失或无效                                    | 401         |
| 40102      | Token 已过期                | Token 解析过期                                      | 401         |
| 40301      | 权限不足                   | `AccessDeniedException` 自动捕获                    | 403         |
| 40302      | 数据权限不足                | 越权访问他人数据                                    | 403         |
| 40401      | 资源不存在                  | `BusinessException.notFound(...)`                   | 404         |
| 40901      | 状态不允许流转             | `BusinessException.stateNotAllowed(...)`            | 409         |
| 40902      | 唯一约束冲突               | 重复创建（如重复接单）                              | 409         |
| 40903      | 业务前置条件不满足         | 如未关联客户无法进入 PLAN 状态                       | 409         |
| 40904      | 数据已被他人修改           | 乐观锁版本冲突（`@Version`），提示刷新重试           | 409         |
| 50000      | 系统内部错误               | 兜底 `Exception`                                    | 500         |
| 50201      | 外部服务调用失败           | CRM/NMS/IM/物流/OA 调用异常                         | 502         |
| 50202      | 外部服务超时               | 调用超时                                            | 504         |

### 3.2 BusinessException 静态工厂

```java
public class BusinessException extends RuntimeException {
    private final int code;

    public static BusinessException notFound(String message) {
        return new BusinessException(40401, message);
    }
    public static BusinessException stateNotAllowed(String message) {
        return new BusinessException(40901, message);
    }
    public static BusinessException duplicate(String message) {
        return new BusinessException(40902, message);
    }
    public static BusinessException preconditionFailed(String message) {
        return new BusinessException(40903, message);
    }
    public static BusinessException externalServiceError(String message) {
        return new BusinessException(50201, message);
    }
}
```

### 3.3 全局异常处理器

`@RestControllerAdvice` 统一捕获以下异常并转换为统一响应体：

| 异常类型                                | 处理逻辑                                         | 错误码     |
| --------------------------------------- | ------------------------------------------------ | ---------- |
| `BusinessException`                     | 返回对应业务错误码与 message                      | 自定义     |
| `MethodArgumentNotValidException`       | 提取 `BindingResult` 中的字段错误到 `errors[]`    | 40001      |
| `HttpMessageNotReadableException`       | 提示「请求体格式错误」                            | 40002      |
| `AccessDeniedException`                 | 提示「权限不足」                                  | 40301      |
| `OptimisticLockerException`             | 提示「数据已被他人修改，请刷新后重试」            | 40904      |
| `Exception`                             | 兜底，记录完整堆栈到日志，提示「系统内部错误」    | 50000      |

### 3.4 异常使用原则

- **业务异常**：使用 `BusinessException` 抛出，包含明确错误码与中文 message
- **参数校验**：优先使用 `@Valid` + `@NotBlank` / `@NotNull` / `@Size` 等 Bean Validation 注解
- **状态校验**：在 Service 层显式校验，抛 `BusinessException.stateNotAllowed(...)`
- **外部调用**：捕获外部异常后包装为 `BusinessException.externalServiceError(...)`
- **禁止**：在 Controller 中 try-catch 业务异常并返回自定义错误结构

### 3.5 前端错误处理

详见 [前端规范 - HTTP 错误处理](#62-http-错误处理)。

---

## 四、日志规范

### 4.1 日志级别

| 级别   | 使用场景                                                     | 示例                                  |
| ------ | ------------------------------------------------------------ | ------------------------------------- |
| ERROR  | 系统异常、外部服务调用失败、数据库异常                       | 数据库连接失败、MinIO 上传失败        |
| WARN   | 业务异常、状态非法流转、权限不足、可恢复的错误               | 用户重复操作、状态机非法流转          |
| INFO   | 关键业务操作、状态变更、登录登出、定时任务执行               | 项目创建、任务派发、割接完成          |
| DEBUG  | 调试信息，生产环境默认关闭                                    | SQL 执行、缓存命中情况                |
| TRACE  | 极细粒度跟踪，仅开发环境                                     | 方法入参出参、循环内部状态            |

### 4.2 关键操作必记

以下操作必须通过 `SysLogService.record(...)` 记录到 `sys_log` 表：

| 操作类型   | 必记字段                                         | 示例                                  |
| ---------- | ------------------------------------------------ | ------------------------------------- |
| 状态变更   | module / action / bizId / oldValue / newValue   | Project: INIT → PLAN                 |
| 审批操作   | module / action / bizId / approver / result      | CutoverPlan: internalApprove PASS    |
| 删除操作   | module / action / bizId / entity snapshot        | Project: delete (含项目名快照)        |
| 结算操作   | module / action / bizId / amount / status       | WorkloadConfirm: PENDING → APPROVED   |
| 登录登出   | userId / loginType / ip / userAgent / result    | User: login SUCCESS / FAIL            |

### 4.3 日志格式

```java
// 推荐：使用占位符，避免字符串拼接
log.info("项目状态流转：projectId={}, from={}, to={}, operatorId={}",
    projectId, oldStatus, newStatus, userId);

// 禁止：字符串拼接（性能差且不易过滤）
log.info("项目状态流转：projectId=" + projectId + "...");
```

### 4.4 敏感信息脱敏

以下字段在日志中必须脱敏：

| 字段类型     | 脱敏规则                            | 示例                                  |
| ------------ | ----------------------------------- | ------------------------------------- |
| 手机号       | 中间 4 位替换为 `****`              | `138****1234`                         |
| 身份证号     | 中间 8 位替换为 `********`          | `110**********1234`                   |
| 银行账号     | 仅保留后 4 位                       | `**** **** **** 1234`                 |
| 密码         | 严禁记录                            | -                                     |
| Token        | 仅记录前 8 位 + `...`              | `eyJhbGc...`                          |
| 邮箱         | 用户名部分首字符 + `***` + 域名    | `z***@example.com`                    |

### 4.5 日志存储

- 开发环境：控制台 + 文件（`logs/vibe-server.log`）
- 测试/预发环境：文件 + ELK / Loki
- 生产环境：文件 + ELK / Loki，保留 ≥ 180 天
- 操作日志（`sys_log` 表）：保留 ≥ 180 天
- 登录日志（`sys_login_log` 表）：保留 ≥ 365 天

---

## 五、缓存规范

### 5.1 Redis Key 命名规范

```
{系统}:{模块}:{业务}:{标识}
```

| 规则     | 说明                                       | 示例                            |
| -------- | ------------------------------------------ | ------------------------------- |
| 系统     | `vibe`                                     | -                               |
| 模块     | 业务模块名                                 | `auth`, `project`, `device`    |
| 业务     | 具体业务含义                               | `token`, `detail`, `status`    |
| 标识     | 业务唯一标识（ID / 编码）                  | `1001`, `SN-001`                |
| 分隔符   | 冒号 `:`                                   | -                               |
| 大小写   | 全小写                                     | -                               |

**完整示例：**

- `vibe:auth:token:1001` — 用户 ID=1001 的 Token
- `vibe:project:detail:5001` — 项目 ID=5001 的详情缓存
- `vibe:device:status:SN-001` — 设备 SN=SN-001 的状态缓存
- `vibe:agent:task:200` — 代理商 ID=200 的任务列表缓存

### 5.2 缓存 Key 列表与 TTL

| Key                                | 用途              | TTL   | 更新策略                |
| ---------------------------------- | ----------------- | ----- | ----------------------- |
| `vibe:auth:token:{userId}`         | 用户 Token        | 8h    | 登录时写入，登出时删除  |
| `vibe:auth:perm:{userId}`          | 用户权限缓存      | 30min | 角色变更时删除          |
| `vibe:project:detail:{projectId}`  | 项目详情缓存      | 5min  | 写操作后删除            |
| `vibe:device:status:{deviceId}`    | 设备状态缓存      | 1min  | 状态变更时删除          |
| `vibe:resource:calendar:{userId}`  | 工程师排期缓存   | 5min  | 排期变更时删除          |
| `vibe:integration:ratelimit:{api}` | 集成接口限流计数器 | 动态  | 滑动窗口自动过期        |
| `vibe:agent:task:{agentId}`         | 代理商任务列表缓存 | 5min  | 任务变更时删除          |

### 5.3 缓存更新策略

#### Cache Aside（推荐，默认）

```
读：先查缓存 → 命中返回 → 未命中查 DB → 写入缓存 → 返回
写：先更新 DB → 删除缓存
```

```java
public ProjectDetailVO getDetail(Long projectId) {
    String key = "vibe:project:detail:" + projectId;
    ProjectDetailVO vo = (ProjectDetailVO) redisTemplate.opsForValue().get(key);
    if (vo != null) {
        return vo;
    }
    // 未命中，查 DB
    ProjectEntity entity = projectMapper.selectById(projectId);
    if (entity == null) {
        throw BusinessException.notFound("项目不存在");
    }
    vo = ProjectConverter.INSTANCE.toVO(entity);
    redisTemplate.opsForValue().set(key, vo, 5, TimeUnit.MINUTES);
    return vo;
}

@Transactional(rollbackFor = Exception.class)
public void update(Long id, ProjectUpdateDTO dto) {
    ProjectEntity entity = projectMapper.selectById(id);
    // ... 校验与更新
    projectMapper.updateById(entity);
    // 删除缓存
    redisTemplate.delete("vibe:project:detail:" + id);
}
```

#### Write Through（特殊场景）

仅用于强一致性要求的场景，如配置类数据：

```
写：先更新 DB → 更新缓存
读：直接读缓存
```

### 5.4 缓存使用禁止

- **禁止**先删缓存后写库（造成脏读）
- **禁止**缓存大对象（单 Key 值 > 1MB 需评估）
- **禁止**缓存 null 值（防止缓存穿透请使用布隆过滤器或空值短 TTL）
- **禁止**使用缓存作为持久化存储（缓存仅做加速，DB 是唯一真源）
- **禁止**多个业务模块共用一个 Key 命名空间

### 5.5 缓存击穿/穿透/雪崩防护

| 问题     | 防护措施                                                    |
| -------- | ---------------------------------------------------------- |
| 缓存击穿 | 热点 Key 加互斥锁（`SETNX`）或逻辑过期                     |
| 缓存穿透 | 空值短 TTL（5min）或布隆过滤器                              |
| 缓存雪崩 | TTL 加随机偏移（±10%），避免同时大量 Key 过期                |

---

## 六、前端规范

### 6.1 组件命名

| 类型             | 命名规则                          | 示例                                    |
| ---------------- | --------------------------------- | --------------------------------------- |
| 页面组件         | kebab-case，与文件名一致          | `project-list.vue`, `device-ledger.vue` |
| 通用组件         | PascalCase，多单词                | `CrudTable`, `FormModal`, `StatusTag`   |
| 业务组件         | PascalCase + 模块前缀             | `ProjectCard`, `DeviceStatusBadge`      |
| Composition API  | `use` 前缀                        | `useProject`, `useTableSelection`       |
| Props            | camelCase                         | `projectId`, `pageSize`                 |
| Events           | kebab-case                        | `@status-change`, `@row-click`          |

### 6.2 HTTP 错误处理

统一的 `axios` 拦截器（`vibe-web/src/utils/request.ts`）：

```typescript
// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data;
    if (code === 200) return data;
    // 业务错误
    return Promise.reject(new Error(message));
  },
  (error) => {
    const { response } = error;
    if (!response) {
      // 网络错误
      message.error('网络异常，请检查网络后重试');
      return Promise.reject(error);
    }
    const { code, message: msg, errors } = response.data;
    switch (true) {
      case code >= 40000 && code < 40100:
        // 参数校验错误：提取 errors[] 高亮表单字段
        if (errors?.length) {
          errors.forEach(({ field, message: errMsg }) => {
            formRef.value?.setFieldsError(field, errMsg);
          });
        } else {
          message.error(msg);
        }
        break;
      case code >= 40100 && code < 40200:
        // 认证错误：跳转登录
        router.push('/login');
        break;
      case code >= 40300 && code < 40400:
        // 权限不足
        message.error('权限不足：' + msg);
        break;
      case code === 40904:
        // 乐观锁冲突
        message.error('数据已被他人修改，请刷新后重试');
        break;
      case code >= 40900 && code < 41000:
        // 业务冲突
        message.error(msg);
        break;
      case code >= 50000:
        // 系统错误
        message.error('系统异常，请联系管理员');
        break;
    }
    return Promise.reject(error);
  }
);
```

### 6.3 Composition API 规范

- **统一使用 `<script setup lang="ts">`** 语法
- **Props 使用 `defineProps` + TypeScript 接口**
- **Events 使用 `defineEmits` + TypeScript 类型**
- **避免使用 Options API**（除已有组件外）

```typescript
// 推荐
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import type { ProjectListVO } from '@/types/project';

interface Props {
  customerId?: number;
  pageSize?: number;
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 20,
});

const emit = defineEmits<{
  (e: 'select', project: ProjectListVO): void;
  (e: 'page-change', page: number): void;
}>();

const list = ref<ProjectListVO[]>([]);

onMounted(async () => {
  list.value = await fetchList();
});
</script>
```

### 6.4 TypeScript 类型完整

- **禁止使用 `any`**，必须明确类型
- **接口数据类型**定义在 `src/types/{module}.ts`
- **API 函数**必须声明入参与返回值类型

```typescript
// src/types/project.ts
export interface ProjectListVO {
  id: number;
  projectCode: string;
  projectName: string;
  customerId: number;
  customerName: string;
  status: ProjectStatus;
  progressPct: number;
  pmName: string;
  plannedEnd: string;
}

export type ProjectStatus = 'INIT' | 'PLAN' | 'EXECUTE' | 'ACCEPT' | 'CLOSE' | 'ARCHIVED';

// src/api/project.ts
import request from '@/utils/request';
import type { ProjectListVO, ProjectCreateDTO, PageResult } from '@/types/project';

export function getProjectList(params: ProjectQueryDTO): Promise<PageResult<ProjectListVO>> {
  return request.get('/api/v1/projects', { params });
}

export function createProject(data: ProjectCreateDTO): Promise<number> {
  return request.post('/api/v1/projects', data);
}
```

### 6.5 状态管理（Pinia）

- **全局状态**使用 Pinia store（如用户信息、菜单、权限）
- **页面级状态**优先使用组件内 `ref` / `reactive`
- **跨页面共享状态**使用 Pinia store

```typescript
// src/stores/user.ts
import { defineStore } from 'pinia';
import type { UserInfo, Role } from '@/types/user';

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null);
  const roles = ref<Role[]>([]);
  const token = ref<string>('');

  function setToken(newToken: string) {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  }

  function clear() {
    userInfo.value = null;
    roles.value = [];
    token.value = '';
    localStorage.removeItem('token');
  }

  return { userInfo, roles, token, setToken, clear };
});
```

### 6.6 组件设计规范

- **单一职责**：每个组件只做一件事，复杂组件拆分为子组件
- **Props down, Events up**：父组件通过 Props 传值，子组件通过 Events 通知
- **避免 `v-if` + `v-for` 同时使用**：先 filter 再 v-for
- **避免在 template 中写复杂表达式**：移到 computed 中
- **统一使用 Ant Design Vue 4.x 组件**：禁止混用其他 UI 库

---

## 七、Git 提交规范

### 7.1 约定式提交（Conventional Commits）

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 7.2 type 列表

| type     | 说明                                       | 示例                                    |
| -------- | ------------------------------------------ | --------------------------------------- |
| feat     | 新功能                                     | `feat(lowcode): 新增表单设计器`         |
| fix      | Bug 修复                                   | `fix(project): 修复状态流转校验缺失`    |
| refactor | 重构（不改变外部行为）                     | `refactor(auth): 重构 Token 刷新逻辑`   |
| docs     | 文档变更                                   | `docs: 建立完整文档体系`                |
| style    | 代码格式（不影响功能）                     | `style: 统一缩进`                       |
| test     | 测试相关                                   | `test(lowcode): 补充设计器单元测试`     |
| chore    | 构建/工具/依赖变更                         | `chore: 升级 mybatis-plus 到 3.5.7`    |
| perf     | 性能优化                                   | `perf(device): 设备列表查询索引优化`    |
| ci       | CI/CD 配置变更                             | `ci: 调整 GitLab CI 流水线`             |
| build    | 构建系统或外部依赖变更                     | `build: 升级 Vite 到 5.x`               |
| revert   | 回滚某次提交                               | `revert: feat(lowcode): 新增表单设计器` |

### 7.3 scope 列表

scope 对应业务模块或功能域：

| scope        | 说明              |
| ------------ | ----------------- |
| auth         | 认证授权          |
| project      | 项目管理          |
| device       | 设备资产          |
| resource     | 资源调度          |
| delivery     | 交付管理          |
| agent        | 代理商管理        |
| acceptance   | 验收管理          |
| finance      | 财务核算          |
| collaboration | 客户协作         |
| integration  | 集成管理          |
| report       | 报表分析          |
| system       | 系统管理          |
| lowcode      | 低代码配置        |
| feedback     | 反馈系统          |
| onboarding   | 用户引导          |
| deploy       | 部署脚本          |
| docs         | 文档              |
| test         | 测试              |

### 7.4 subject 规则

- **祈使句**：`新增` / `修复` / `重构`，而不是 `新增了` / `修复了`
- **不超过 50 字**
- **结尾不加句号**
- **避免空泛**：`feat(lowcode): 新增表单设计器`，而不是 `feat: 更新代码`

### 7.5 body 与 footer

- **body**：解释「为什么」做这个改动，而不是「做了什么」（diff 已说明）
- **footer**：标记 BREAKING CHANGE 或关联 Issue

```
feat(lowcode): 新增表单 Schema 设计器

支持拖拽字段、属性编辑、JSON 预览、实时预览渲染。
对应 spec A 项：低代码模块前端完整实现。

BREAKING CHANGE: 低代码配置菜单仅 SUPER_ADMIN 可见，需执行 V2__completeness_additions.sql 迁移
```

### 7.6 提交粒度

- **一次提交只做一件事**：混合多个无关改动的提交难以 review 与回滚
- **不超过 500 行代码**：超过则拆分为多个提交
- **必须能独立通过测试**：每个提交都可单独发布

### 7.7 分支命名

| 分支类型     | 命名规则                        | 示例                            |
| ------------ | ------------------------------- | ------------------------------- |
| 主分支       | `main` / `master`              | `main`                          |
| 开发分支     | `dev` 或 `develop`              | `dev`                           |
| 功能分支     | `feature/<scope>-<desc>`       | `feature/lowcode-form-designer` |
| 修复分支     | `fix/<scope>-<desc>`           | `fix/project-state-validation`  |
| 热修复分支   | `hotfix/<version>-<desc>`      | `hotfix/v1.2.1-auth-bug`        |
| 发布分支     | `release/<version>`            | `release/v1.2.0`                |

---

## 八、代码评审 Checklist

### 8.1 通用

- [ ] 命名符合规范，无缩写与拼写错误
- [ ] 无硬编码字符串、魔法数字（应抽常量或配置）
- [ ] 无 `System.out.println`，统一使用日志框架
- [ ] 无未使用的 import、变量、方法
- [ ] 无注释掉的代码块（应删除，Git 可追溯）

### 8.2 后端

- [ ] Controller 不含业务逻辑，仅参数校验 + Service 调用 + 结果包装
- [ ] Service 写操作加 `@Transactional(rollbackFor = Exception.class)`
- [ ] Service 状态流转方法显式校验当前状态，详见 [状态机转换矩阵](./state-machine.md#附状态机实现与校验规范)
- [ ] 关键操作（状态变更/审批/删除）记录 SysLog
- [ ] 异常使用 `BusinessException` 抛出，包含明确错误码
- [ ] 缓存使用 Cache Aside 模式，写后删除缓存
- [ ] 数据库查询无 N+1 问题（关联查询用 join 或批量查询）
- [ ] 接口添加 `@PreAuthorize` 权限注解
- [ ] 接口添加 `@Operation` OpenAPI 文档注解

### 8.3 前端

- [ ] 使用 `<script setup lang="ts">` 语法
- [ ] 无 `any` 类型，所有变量与函数参数都有明确类型
- [ ] API 函数声明入参与返回值类型
- [ ] 组件 Props 使用 `defineProps` + TypeScript 接口
- [ ] HTTP 错误处理走统一拦截器
- [ ] 表单提交前 `@Valid` 校验，提交后 Loading 状态
- [ ] 关键操作二次确认（删除/状态变更/审批）
- [ ] 列表操作后自动刷新

### 8.4 测试

- [ ] 新增代码单元测试覆盖率 ≥ 90%
- [ ] 关键流程有集成测试
- [ ] 测试用例命名清晰：`should_<expected>_when_<condition>`
- [ ] 测试数据可重复执行（幂等）
