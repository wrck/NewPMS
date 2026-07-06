# 接口变更清单

> 文档版本：V1.0
> 更新日期：2026-07-06
> 关联变更：`enterprise-completion` Spec（A 低代码模块前端 / D 反馈系统 / C 业务流程与异常处理）
> 配套文档：[架构设计](./design-architecture.md) | [状态机转换矩阵](./state-machine.md) | [开发规范 - 异常处理规范](./development-guide.md#三异常处理规范)

---

## 目录

- [一、变更总览](#一变更总览)
- [二、低代码模块接口（已对接）](#二低代码模块接口已对接)
- [三、反馈系统接口（新增）](#三反馈系统接口新增)
- [四、乐观锁变更（修改）](#四乐观锁变更修改)
- [五、错误码变更](#五错误码变更)
- [六、影响范围与兼容性](#六影响范围与兼容性)

---

## 一、变更总览

本次 `enterprise-completion` 变更涉及三类接口调整：

| 变更类型 | 模块                 | 接口数 | 影响范围                       | 兼容性       |
| -------- | -------------------- | ------ | ------------------------------ | ------------ |
| 新增     | 反馈系统             | ~7 条  | 前端新增反馈管理页             | 完全兼容     |
| 已对接   | 低代码模块           | ~45 条 | 前端新增低代码设计器与渲染器   | 完全兼容     |
| 修改     | 关键业务表 PUT 接口  | ~30 条 | 乐观锁冲突返回 40904 错误码    | 向后兼容     |

**变更原则：**

- 所有变更向后兼容，不破坏现有接口契约
- 低代码接口此前已存在但前端未对接，本次列为「已对接」
- 反馈系统接口为本次新增，需执行数据库迁移脚本
- 乐观锁变更通过 `BaseEntity` 统一继承，子类 Entity 自动获得 `@Version` 字段

---

## 二、低代码模块接口（已对接）

> 状态：后端 Controller 此前已存在，本次 enterprise-completion 完成前端对接（spec A 项）
> Controller：`module-lowcode` 下 5 个 Controller
> 数据表：`lowcode_form_config` / `lowcode_list_config` / `lowcode_tab_config` / `lowcode_relation_config` / `lowcode_template`
> 迁移脚本：`V2__completeness_additions.sql`

### 2.1 表单配置接口（FormConfigController）

**路径前缀**：`/api/v1/lowcode/forms`
**权限**：`lowcode:config:list` / `lowcode:config:add` / `lowcode:config:remove` 或 `SUPER_ADMIN`

| # | 路径                  | 方法   | 变更类型 | 说明                              | 影响范围             |
| - | --------------------- | ------ | -------- | --------------------------------- | -------------------- |
| 1 | `/api/v1/lowcode/forms` | GET    | 已对接   | 分页查询表单配置（含 keyword 搜索）| 前端 `form-config.vue` |
| 2 | `/api/v1/lowcode/forms/{id}` | GET    | 已对接   | 表单配置详情                      | 前端设计器抽屉       |
| 3 | `/api/v1/lowcode/forms` | POST   | 已对接   | 创建表单配置（持久化 JSON Schema）| 前端设计器保存       |
| 4 | `/api/v1/lowcode/forms/{id}` | PUT    | 已对接   | 更新表单配置（含乐观锁校验）      | 前端设计器保存       |
| 5 | `/api/v1/lowcode/forms/{id}` | DELETE | 已对接   | 删除表单配置                      | 前端列表删除按钮     |
| 6 | `/api/v1/lowcode/forms/{id}/copy` | POST   | 已对接   | 复制表单配置                      | 前端列表「复制」按钮 |
| 7 | `/api/v1/lowcode/forms/{id}/export` | GET    | 已对接   | 导出表单 JSON Schema（下载文件）  | 前端「导出 JSON」按钮 |
| 8 | `/api/v1/lowcode/forms/import` | POST   | 已对接   | 导入表单 JSON Schema              | 前端「导入 JSON」按钮 |
| 9 | `/api/v1/lowcode/forms/templates/{templateId}/instantiate` | POST   | 已对接   | 基于模板实例化表单配置            | 前端模板库「实例化」 |

### 2.2 列表配置接口（ListConfigController）

**路径前缀**：`/api/v1/lowcode/lists`
**权限**：`lowcode:config:list` / `lowcode:config:add` / `lowcode:config:remove` 或 `SUPER_ADMIN`

| #  | 路径                  | 方法   | 变更类型 | 说明                              | 影响范围             |
| -- | --------------------- | ------ | -------- | --------------------------------- | -------------------- |
| 10 | `/api/v1/lowcode/lists` | GET    | 已对接   | 分页查询列表配置                  | 前端 `list-config.vue` |
| 11 | `/api/v1/lowcode/lists/{id}` | GET    | 已对接   | 列表配置详情                      | 前端设计器抽屉       |
| 12 | `/api/v1/lowcode/lists` | POST   | 已对接   | 创建列表配置                      | 前端设计器保存       |
| 13 | `/api/v1/lowcode/lists/{id}` | PUT    | 已对接   | 更新列表配置（含乐观锁校验）      | 前端设计器保存       |
| 14 | `/api/v1/lowcode/lists/{id}` | DELETE | 已对接   | 删除列表配置                      | 前端列表删除按钮     |
| 15 | `/api/v1/lowcode/lists/{id}/copy` | POST   | 已对接   | 复制列表配置                      | 前端列表「复制」按钮 |
| 16 | `/api/v1/lowcode/lists/{id}/export` | GET    | 已对接   | 导出列表 JSON Schema              | 前端「导出 JSON」按钮 |
| 17 | `/api/v1/lowcode/lists/import` | POST   | 已对接   | 导入列表 JSON Schema              | 前端「导入 JSON」按钮 |
| 18 | `/api/v1/lowcode/lists/templates/{templateId}/instantiate` | POST   | 已对接   | 基于模板实例化列表配置            | 前端模板库「实例化」 |

### 2.3 标签页配置接口（TabConfigController）

**路径前缀**：`/api/v1/lowcode/tabs`
**权限**：`lowcode:config:list` / `lowcode:config:add` / `lowcode:config:remove` 或 `SUPER_ADMIN`

| #  | 路径                  | 方法   | 变更类型 | 说明                              | 影响范围             |
| -- | --------------------- | ------ | -------- | --------------------------------- | -------------------- |
| 19 | `/api/v1/lowcode/tabs` | GET    | 已对接   | 分页查询标签页配置                | 前端 `tab-config.vue` |
| 20 | `/api/v1/lowcode/tabs/{id}` | GET    | 已对接   | 标签页配置详情                    | 前端设计器抽屉       |
| 21 | `/api/v1/lowcode/tabs` | POST   | 已对接   | 创建标签页配置                    | 前端设计器保存       |
| 22 | `/api/v1/lowcode/tabs/{id}` | PUT    | 已对接   | 更新标签页配置（含乐观锁校验）    | 前端设计器保存       |
| 23 | `/api/v1/lowcode/tabs/{id}` | DELETE | 已对接   | 删除标签页配置                    | 前端列表删除按钮     |
| 24 | `/api/v1/lowcode/tabs/{id}/copy` | POST   | 已对接   | 复制标签页配置                    | 前端列表「复制」按钮 |
| 25 | `/api/v1/lowcode/tabs/{id}/export` | GET    | 已对接   | 导出标签页 JSON Schema            | 前端「导出 JSON」按钮 |
| 26 | `/api/v1/lowcode/tabs/import` | POST   | 已对接   | 导入标签页 JSON Schema            | 前端「导入 JSON」按钮 |
| 27 | `/api/v1/lowcode/tabs/templates/{templateId}/instantiate` | POST   | 已对接   | 基于模板实例化标签页配置          | 前端模板库「实例化」 |

### 2.4 关联页配置接口（RelationConfigController）

**路径前缀**：`/api/v1/lowcode/relations`
**权限**：`lowcode:config:list` / `lowcode:config:add` / `lowcode:config:remove` 或 `SUPER_ADMIN`

| #  | 路径                  | 方法   | 变更类型 | 说明                              | 影响范围             |
| -- | --------------------- | ------ | -------- | --------------------------------- | -------------------- |
| 28 | `/api/v1/lowcode/relations` | GET    | 已对接   | 分页查询关联页配置                | 前端 `relation-config.vue` |
| 29 | `/api/v1/lowcode/relations/{id}` | GET    | 已对接   | 关联页配置详情                    | 前端设计器抽屉       |
| 30 | `/api/v1/lowcode/relations` | POST   | 已对接   | 创建关联页配置                    | 前端设计器保存       |
| 31 | `/api/v1/lowcode/relations/{id}` | PUT    | 已对接   | 更新关联页配置（含乐观锁校验）    | 前端设计器保存       |
| 32 | `/api/v1/lowcode/relations/{id}` | DELETE | 已对接   | 删除关联页配置                    | 前端列表删除按钮     |
| 33 | `/api/v1/lowcode/relations/{id}/copy` | POST   | 已对接   | 复制关联页配置                    | 前端列表「复制」按钮 |
| 34 | `/api/v1/lowcode/relations/{id}/export` | GET    | 已对接   | 导出关联页 JSON Schema            | 前端「导出 JSON」按钮 |
| 35 | `/api/v1/lowcode/relations/import` | POST   | 已对接   | 导入关联页 JSON Schema            | 前端「导入 JSON」按钮 |
| 36 | `/api/v1/lowcode/relations/templates/{templateId}/instantiate` | POST   | 已对接   | 基于模板实例化关联页配置          | 前端模板库「实例化」 |

### 2.5 模板库接口（TemplateController）

**路径前缀**：`/api/v1/lowcode/templates`
**权限**：`lowcode:template:list` / `lowcode:template:add` / `lowcode:template:remove` 或 `SUPER_ADMIN`

| #  | 路径                  | 方法   | 变更类型 | 说明                              | 影响范围             |
| -- | --------------------- | ------ | -------- | --------------------------------- | -------------------- |
| 37 | `/api/v1/lowcode/templates` | GET    | 已对接   | 分页查询模板                      | 前端 `template-library.vue` |
| 38 | `/api/v1/lowcode/templates/{id}` | GET    | 已对接   | 模板详情                          | 前端模板详情抽屉     |
| 39 | `/api/v1/lowcode/templates` | POST   | 已对接   | 创建模板                          | 前端「新建模板」     |
| 40 | `/api/v1/lowcode/templates/{id}` | PUT    | 已对接   | 更新模板（含乐观锁校验）          | 前端编辑保存         |
| 41 | `/api/v1/lowcode/templates/{id}` | DELETE | 已对接   | 删除模板                          | 前端删除按钮         |
| 42 | `/api/v1/lowcode/templates/{id}/copy` | POST   | 已对接   | 复制模板                          | 前端列表「复制」按钮 |
| 43 | `/api/v1/lowcode/templates/{id}/export` | GET    | 已对接   | 导出模板 JSON Schema              | 前端「导出 JSON」按钮 |
| 44 | `/api/v1/lowcode/templates/import` | POST   | 已对接   | 导入模板 JSON Schema              | 前端「导入 JSON」按钮 |
| 45 | `/api/v1/lowcode/templates/{templateId}/instantiate` | POST   | 已对接   | 校验模板可实例化（返回模板 ID）   | 前端「实例化」按钮   |

---

## 三、反馈系统接口（新增）

> 状态：本次 enterprise-completion 新增（spec D5 项）
> Controller：`module-system` 下新增 `FeedbackController`
> Entity：`SysFeedbackEntity`（新增，对应表 `sys_feedback`）
> Service：`FeedbackService` + `FeedbackServiceImpl`
> 迁移脚本：待新增（与 `V2__completeness_additions.sql` 合并或新建 `V6__sys_feedback.sql`）

### 3.1 反馈记录接口

**路径前缀**：`/api/v1/feedback`
**权限**：所有登录用户可提交反馈；`system:feedback:*` 管理

| #  | 路径                  | 方法   | 变更类型 | 说明                              | 影响范围                |
| -- | --------------------- | ------ | -------- | --------------------------------- | ----------------------- |
| 46 | `/api/v1/feedback` | POST   | 新增     | 提交反馈（功能建议/Bug/咨询）     | 全端用户，悬浮反馈按钮   |
| 47 | `/api/v1/feedback` | GET    | 新增     | 分页查询反馈列表（管理员）        | 前端 `system/feedback.vue` |
| 48 | `/api/v1/feedback/{id}` | GET    | 新增     | 反馈详情                          | 前端反馈详情抽屉        |
| 49 | `/api/v1/feedback/{id}` | PUT    | 新增     | 更新反馈（管理员处理：状态/处理人/处理说明） | 前端反馈处理弹窗 |
| 50 | `/api/v1/feedback/{id}` | DELETE | 新增     | 删除反馈（仅 SUPER_ADMIN）        | 前端反馈列表删除        |
| 51 | `/api/v1/feedback/mine` | GET    | 新增     | 查询当前用户提交的反馈列表        | 前端「我的反馈」        |
| 52 | `/api/v1/feedback/{id}/status` | POST   | 新增     | 修改反馈状态（PENDING/PROCESSING/RESOLVED/CLOSED）并通知提交人 | 前端状态变更按钮 |

### 3.2 数据模型（SysFeedbackEntity）

```java
@TableName("sys_feedback")
public class SysFeedbackEntity extends BaseEntity {
    private String feedbackType;    // SUGGESTION/BUG/CONSULT
    private String content;         // 反馈内容
    private String screenshot;     // 截图 URL（MinIO）
    private String contact;         // 联系方式（手机号/邮箱，可选）
    private Long submitUserId;      // 提交人 ID
    private String submitUserName;  // 提交人姓名（冗余便于查询）
    private String status;          // PENDING/PROCESSING/RESOLVED/CLOSED
    private Long handlerUserId;     // 处理人 ID
    private String handlerRemark;   // 处理说明
    private LocalDateTime handleTime; // 处理时间
}
```

### 3.3 提交反馈请求体示例

```json
POST /api/v1/feedback
{
  "feedbackType": "BUG",
  "content": "项目列表筛选时区域下拉框数据未加载",
  "screenshot": "https://minio.example.com/vibe-attachments/feedback/202607/xxx.png",
  "contact": "138****1234"
}
```

### 3.4 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": 1001,
  "timestamp": 1779900000000
}
```

---

## 四、乐观锁变更（修改）

> 状态：本次 enterprise-completion 在关键业务表上确认并启用 `@Version` 乐观锁（spec C2 项）
> 实现：通过 `BaseEntity` 统一继承，子类 Entity 自动获得 `version` 字段
> 影响：所有 PUT 接口在并发冲突时返回错误码 **40904**

### 4.1 受影响的 Entity（继承 BaseEntity）

| Entity                                | 表名                          | 模块               |
| ------------------------------------- | ----------------------------- | ------------------ |
| `ProjectEntity`                       | `project`                     | module-project     |
| `DeviceInstanceEntity`                | `device_instance`            | module-device      |
| `OutsourceTaskEntity`                 | `outsource_task`              | module-agent       |
| `WorkOrderEntity`                     | `work_order`                  | module-delivery    |
| `AcceptanceTaskEntity`                | `acceptance_task`            | module-acceptance  |
| `FinanceBudgetEntity`                 | `finance_budget`             | module-finance     |
| `FinanceWorkloadConfirmationEntity`   | `finance_workload_confirmation` | module-finance   |
| `CutoverPlanEntity`                   | `cutover_plan`               | module-delivery    |

> 实际上所有继承 `BaseEntity` 的实体（约 80 个）均自动获得 `@Version` 字段。上表仅列出 enterprise-completion 重点关注的 8 个 Entity。

### 4.2 受影响的 PUT 接口（示例）

下列 PUT 接口在并发冲突时返回 40904：

| #  | 路径                  | 方法 | 说明                              |
| -- | --------------------- | ---- | --------------------------------- |
| 53 | `/api/v1/projects/{id}` | PUT  | 更新项目（含乐观锁校验）          |
| 54 | `/api/v1/device-instances/{id}` | PUT  | 更新设备实例                      |
| 55 | `/api/v1/outsource-tasks/{id}` | PUT  | 更新转包任务                      |
| 56 | `/api/v1/work-orders/{id}` | PUT  | 更新工单                          |
| 57 | `/api/v1/acceptance-tasks/{id}` | PUT  | 更新验收任务                      |
| 58 | `/api/v1/finance/budgets/{id}` | PUT  | 更新预算                          |
| 59 | `/api/v1/finance/workloads/{id}` | PUT  | 更新工作量确认单                  |
| 60 | `/api/v1/cutover-plans/{id}` | PUT  | 更新割接方案                      |

### 4.3 并发冲突响应示例

**请求（携带过期 version）：**

```http
PUT /api/v1/projects/1001
{
  "projectName": "XX银行网络改造",
  "version": 3
}
```

**响应（40904）：**

```json
{
  "code": 40904,
  "message": "数据已被他人修改，请刷新后重试",
  "timestamp": 1779900000000
}
```

### 4.4 前端处理

前端 `axios` 拦截器统一处理 40904 错误码：

```typescript
case code === 40904:
  message.error('数据已被他人修改，请刷新后重试');
  // 自动刷新当前页数据
  location.reload();
  break;
```

详见 [开发规范 - HTTP 错误处理](./development-guide.md#62-http-错误处理)。

---

## 五、错误码变更

本次变更新增/明确以下错误码：

| 错误码 | 含义                       | 触发场景                                                | 变更类型 |
| ------ | -------------------------- | ------------------------------------------------------- | -------- |
| 40901  | 状态不允许流转             | 状态机非法流转（如 `EXECUTE` → `INIT`）                | 明确     |
| 40902  | 唯一约束冲突               | 重复创建（如重复项目编号）                              | 明确     |
| 40903  | 业务前置条件不满足         | 如未关联客户无法进入 `PLAN` 状态                         | 明确     |
| 40904  | 数据已被他人修改           | 乐观锁版本冲突（`@Version`），提示刷新重试              | 新增     |

> 错误码完整范围详见 [开发规范 - 异常处理规范 - 错误码范围表](./development-guide.md#31-错误码范围表)。

---

## 六、影响范围与兼容性

### 6.1 兼容性矩阵

| 变更                  | 前端是否需要改造 | 后端是否需要迁移 | 兼容性                 |
| --------------------- | ---------------- | ---------------- | ---------------------- |
| 低代码接口对接        | 是（新增视图）   | 否（已存在）     | 完全兼容               |
| 反馈系统接口          | 是（新增视图）   | 是（迁移 SQL）   | 完全兼容（仅新增）     |
| 乐观锁变更            | 否（40904 自动处理）| 否（version 字段已在 BaseEntity） | 向后兼容（PUT 多传 version 字段） |

### 6.2 迁移步骤

部署本次变更需按以下顺序执行：

1. **数据库迁移**：执行 `V6__sys_feedback.sql`（新增 `sys_feedback` 表）
2. **菜单与权限迁移**：执行 SQL 插入低代码菜单项与权限标识 `lowcode:config:*` / `lowcode:template:*` / `system:feedback:*`
3. **后端部署**：发布新版本 `vibe-server` 镜像
4. **前端部署**：发布新版本 `vibe-web` 静态资源
5. **健康检查**：访问 `/actuator/health` 确认服务正常

### 6.3 回滚方案

如需回滚：

1. 执行 `scripts/rollback.ps1 -Env prod -Version <上一版本号>`
2. 数据库回滚：`DROP TABLE sys_feedback;`（仅回滚反馈系统，低代码与乐观锁变更不影响旧版本）
3. 前端回滚到上一版本静态资源

### 6.4 验收清单

- [ ] 低代码设计器 5 类配置可正常 CRUD
- [ ] 低代码运行时渲染器可正确渲染 10 个通用实体（覆盖率 ≥ 60%）
- [ ] 反馈按钮可提交反馈，管理员可处理
- [ ] 关键业务表 PUT 接口并发冲突返回 40904
- [ ] 前端 40904 错误提示「数据已被他人修改，请刷新后重试」
- [ ] 状态机非法流转返回 40901
- [ ] 所有接口通过 E2E 冒烟测试（≥ 20 条）

详见 [测试策略 - 关键流程用例清单](./test-strategy.md#五关键流程用例清单)。
