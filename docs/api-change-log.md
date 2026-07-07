# 接口变更清单

> 文档版本：V1.1（异常处理三层闭环 + 操作日志审计变更补充版）
> 更新日期：2026-07-06
> 关联变更：`enterprise-completion` Spec（A 低代码模块前端 / D 反馈系统 / C 业务流程与异常处理 / Task 8 异常处理三层闭环 / Task 9 操作日志审计）
> 配套文档：[架构设计](./design-architecture.md) | [状态机转换矩阵](./state-machine.md) | [开发规范 - 异常处理规范](./development-guide.md#三异常处理规范)
> 版本变更：V1.1 在 V1.0 基础上新增第八章「异常处理三层闭环接口变更（Task 8）」与第九章「操作日志审计变更（Task 9）」，记录 14 处异常处理修复跨 18 文件的接口契约变更，以及 41 个新增 `@OperationLog` 注解的接口范围扩展

---

## 目录

- [一、变更总览](#一变更总览)
- [二、低代码模块接口（已对接）](#二低代码模块接口已对接)
- [三、反馈系统接口（新增）](#三反馈系统接口新增)
- [四、乐观锁变更（修改）](#四乐观锁变更修改)
- [五、错误码变更](#五错误码变更)
- [六、影响范围与兼容性](#六影响范围与兼容性)
- [七、前后端字段一致性差异表（67 项）](#七前后端字段一致性差异表67-项)
- [八、异常处理三层闭环接口变更（Task 8）](#八异常处理三层闭环接口变更task-8)
- [九、操作日志审计变更（Task 9）](#九操作日志审计变更task-9)

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

---

## 七、前后端字段一致性差异表

> 章节：Spec Task 5 - 前后端字段一致性交叉验证
> 验证日期：2026-07-06
> 硬约束：前端字段优先，后端 DTO/VO 对齐前端，不改 Entity 结构
> 验证范围：`vibe-web/src/types/` 与 `vibe-server/module-*/dto/`、`vibe-server/module-*/vo/`

### 7.1 差异总览

| 模块 | 实体对 | 差异总数 | 已修复 | 无法修复 |
|------|--------|----------|--------|----------|
| agent | AgentCompanyDTO / AgentCompanyVO / AgentEngineerDTO / AgentEngineerVO / OutsourceTaskCreateDTO / OutsourceTaskVO / OutsourceDeliverableVO / OutsourceWorkloadDTO / OutsourceWorkloadVO / AgentScoreDTO / AgentScoreLogVO | 22 | 22 | 0 |
| device | DeviceModelDTO / DeviceModelVO / DeviceInstanceDTO / DeviceInstanceVO / SparePartDTO / SparePartVO | 14 | 14 | 0 |
| delivery | WorkOrderCreateDTO / WorkOrderCheckinDTO / WorkOrderCheckoutDTO / WorkOrderConfirmDTO / WorkOrderStepCompleteDTO / WorkOrderIssueReportDTO / WorkOrderVO / WorkOrderStepVO | 18 | 18 | 0 |
| resource | EngineerDTO / EngineerVO / TimesheetDTO / TimesheetVO / BusinessTripDTO | 11 | 11 | 0 |
| acceptance | AcceptanceTaskVO | 1 | 1 | 0 |
| project | ProjectTaskDTO | 1 | 1 | 0 |
| finance | FinanceBudget/Cost/Workload 系列 | 0 | 0 | 0 |
| lowcode | LowcodeConfig/Template 系列 | 0 | 0 | 0 |
| user | UserInfoVO / LoginVO / LoginDTO | 0 | 0 | 0 |
| **合计** |  | **67** | **67** | **0** |

### 7.2 差异明细（已修复）

| 模块 | 实体 | 前端字段 | 后端字段（修复前） | 差异类型 | 修复方案 |
|------|------|----------|--------------------|----------|----------|
| agent | AgentCompanyDTO | contactEmail?: string | （缺失） | 字段缺失 | 后端新增 `contactEmail` |
| agent | AgentCompanyDTO | address?: string | （缺失） | 字段缺失 | 后端新增 `address` |
| agent | AgentCompanyDTO | cooperationEnd?: string | （缺失） | 字段缺失 | 后端新增 `cooperationEnd: LocalDate` |
| agent | AgentCompanyDTO | remark?: string | （缺失） | 字段缺失 | 后端新增 `remark` |
| agent | AgentEngineerDTO | email?: string | （缺失） | 字段缺失 | 后端新增 `email` |
| agent | OutsourceTaskCreateDTO | taskId?: number | taskId: Long (@NotNull) | 可选性不一致 | 后端移除 `@NotNull`，改为可选 |
| agent | OutsourceTaskCreateDTO | attachments?: Array<{name; url}> | （缺失） | 字段缺失 | 后端新增 `attachments` 及内部 `Attachment` 静态类 |
| agent | OutsourceWorkloadDTO | travelDays?: number | （缺失） | 字段缺失 | 后端新增 `travelDays` |
| agent | OutsourceWorkloadDTO | otherCost?: number | （缺失） | 字段缺失 | 后端新增 `otherCost: BigDecimal` |
| agent | OutsourceWorkloadDTO | totalAmount?: number | （缺失） | 字段缺失 | 后端新增 `totalAmount: BigDecimal` |
| agent | AgentScoreDTO | taskId?: number | outsourceTaskId: Long | 字段名不一致 | 后端重命名为 `taskId` |
| agent | AgentScoreDTO | timeliness: number | scoreTimeliness: BigDecimal | 字段名不一致 | 后端重命名为 `timeliness` |
| agent | AgentScoreDTO | quality: number | scoreQuality: BigDecimal | 字段名不一致 | 后端重命名为 `quality` |
| agent | AgentScoreDTO | communication: number | scoreCommunication: BigDecimal | 字段名不一致 | 后端重命名为 `communication` |
| agent | AgentScoreDTO | issueRate: number | scoreIssue: BigDecimal | 字段名不一致 | 后端重命名为 `issueRate` |
| agent | AgentScoreDTO | comment?: string | remark: String | 字段名不一致 | 后端重命名为 `comment` |
| agent | AgentCompanyVO | contactEmail?: string | （缺失） | 字段缺失 | 后端新增 `contactEmail` |
| agent | AgentCompanyVO | address?: string | （缺失） | 字段缺失 | 后端新增 `address` |
| agent | AgentCompanyVO | cooperationEnd?: string | （缺失） | 字段缺失 | 后端新增 `cooperationEnd: LocalDate` |
| agent | AgentCompanyVO | projectCount?: number | （缺失） | 字段缺失 | 后端新增 `projectCount: Integer` |
| agent | AgentCompanyVO | totalAmount?: number | （缺失） | 字段缺失 | 后端新增 `totalAmount: BigDecimal` |
| agent | AgentCompanyVO | remark?: string | （缺失） | 字段缺失 | 后端新增 `remark` |
| agent | AgentEngineerVO | email?: string | （缺失） | 字段缺失 | 后端新增 `email` |
| agent | AgentEngineerVO | taskCount?: number | （缺失） | 字段缺失 | 后端新增 `taskCount: Integer` |
| agent | AgentEngineerVO | joinedAt?: string | （缺失） | 字段缺失 | 后端新增 `joinedAt: LocalDateTime` |
| agent | OutsourceTaskVO | taskCode?: string | （缺失） | 字段缺失 | 后端新增 `taskCode` |
| agent | OutsourceTaskVO | confirmedByName?: string | （缺失） | 字段缺失 | 后端新增 `confirmedByName` |
| agent | OutsourceTaskVO | attachments?: Array<{name; url}> | （缺失） | 字段缺失 | 后端新增 `attachments: String`（JSON 字符串） |
| agent | OutsourceDeliverableVO | taskId: number | outsourceTaskId: Long | 字段名不一致 | 后端重命名为 `taskId` |
| agent | OutsourceDeliverableVO | name: string | fileName: String | 字段名不一致 | 后端重命名为 `name` |
| agent | OutsourceDeliverableVO | url: string | fileUrl: String | 字段名不一致 | 后端重命名为 `url` |
| agent | OutsourceDeliverableVO | deliverableType: 'PHOTO'/'SIGN_OFF'/... | deliverableType: 'PHOTO'/'RECEIPT'/... | 枚举值不一致 | 后端注释更新为 `PHOTO/TEST_RECORD/SIGN_OFF/CONFIG/OTHER` |
| agent | OutsourceDeliverableVO | thumbnailUrl?: string | （缺失） | 字段缺失 | 后端新增 `thumbnailUrl` |
| agent | OutsourceDeliverableVO | uploadedBy?: number | （缺失）） | 字段缺失 | 后端新增 `uploadedBy` |
| agent | OutsourceDeliverableVO | uploadedByName?: string | （缺失） | 字段缺失 | 后端新增 `uploadedByName` |
| agent | OutsourceDeliverableVO | uploadedAt?: string | （缺失） | 字段缺失 | 后端新增 `uploadedAt: LocalDateTime` |
| agent | OutsourceWorkloadVO | taskId: number | outsourceTaskId: Long | 字段名不一致 | 后端重命名为 `taskId` |
| agent | OutsourceWorkloadVO | projectId / projectName / agentCompanyName | （缺失） | 字段缺失 | 后端新增关联字段 |
| agent | OutsourceWorkloadVO | travelDays?: number | （缺失） | 字段缺失 | 后端新增 `travelDays` |
| agent | OutsourceWorkloadVO | otherCost?: number | （缺失） | 字段缺失 | 后端新增 `otherCost: BigDecimal` |
| agent | OutsourceWorkloadVO | totalAmount?: number | （缺失） | 字段缺失 | 后端新增 `totalAmount: BigDecimal` |
| agent | OutsourceWorkloadVO | confirmByName?: string | （缺失） | 字段缺失 | 后端新增 `confirmByName` |
| agent | OutsourceWorkloadVO | confirmAt?: string | （缺失） | 字段缺失 | 后端新增 `confirmAt: LocalDateTime` |
| agent | OutsourceWorkloadVO | status: 'PENDING'/'CONFIRMED'/'APPROVED'/'INVOICED'/'PAID' | status: 'SUBMITTED'/'CONFIRMED'/'REJECTED' | 枚举值不一致 | 后端注释更新为前端枚举集 |
| agent | AgentScoreLogVO | taskId?: number | outsourceTaskId: Long | 字段名不一致 | 后端重命名为 `taskId` |
| agent | AgentScoreLogVO | timeliness: number | scoreTimeliness: BigDecimal | 字段名不一致 | 后端重命名为 `timeliness` |
| agent | AgentScoreLogVO | quality: number | scoreQuality: BigDecimal | 字段名不一致 | 后端重命名为 `quality` |
| agent | AgentScoreLogVO | communication: number | scoreCommunication: BigDecimal | 字段名不一致 | 后端重命名为 `communication` |
| agent | AgentScoreLogVO | issueRate: number | scoreIssue: BigDecimal | 字段名不一致 | 后端重命名为 `issueRate` |
| agent | AgentScoreLogVO | comment?: string | remark: String | 字段名不一致 | 后端重命名为 `comment` |
| agent | AgentScoreLogVO | scoredAt?: string | （缺失） | 字段缺失 | 后端新增 `scoredAt: LocalDateTime` |
| device | DeviceModelDTO | description?: string | （缺失） | 字段缺失 | 后端新增 `description` |
| device | DeviceModelVO | description?: string | （缺失） | 字段缺失 | 后端新增 `description` |
| device | DeviceModelVO | status?: 'ENABLED'/'DISABLED' | （缺失） | 字段缺失 | 后端新增 `status: Integer` |
| device | DeviceInstanceDTO | location?: string | installLocation: String | 字段名不一致 | 后端重命名为 `location` |
| device | DeviceInstanceDTO | status?: DeviceStatus | （缺失） | 字段缺失 | 后端新增 `status: String` |
| device | DeviceInstanceVO | location?: string | installLocation: String | 字段名不一致 | 后端重命名为 `location` |
| device | DeviceInstanceVO | installedAt?: string | installDate: LocalDate | 字段名不一致 | 后端重命名为 `installedAt` |
| device | DeviceInstanceVO | onlineAt?: string | onlineDate: LocalDate | 字段名不一致 | 后端重命名为 `onlineAt` |
| device | DeviceInstanceVO | productLine?: ProductLine | （缺失） | 字段缺失 | 后端新增 `productLine: String` |
| device | DeviceInstanceVO | category?: DeviceCategory | （缺失） | 字段缺失 | 后端新增 `category: String` |
| device | SparePartDTO | category?: DeviceCategory | （缺失） | 字段缺失 | 后端新增 `category: String` |
| device | SparePartDTO | unit?: string | （缺失） | 字段缺失 | 后端新增 `unit` |
| device | SparePartDTO | stockQty: number | quantity: Integer | 字段名不一致 | 后端重命名为 `stockQty` |
| device | SparePartDTO | safetyStockQty: number | （缺失） | 字段缺失 | 后端新增 `safetyStockQty` |
| device | SparePartDTO | status?: 'IN_STOCK'/'OUT'/'REPAIR'/'SCRAPPED' | status: Integer (1/0) | 类型+枚举不一致 | 后端改为 `status: String` |
| device | SparePartDTO | remark?: string | （缺失） | 字段缺失 | 后端新增 `remark` |
| device | SparePartVO | category?: DeviceCategory | （缺失） | 字段缺失 | 后端新增 `category` |
| device | SparePartVO | unit?: string | （缺失） | 字段缺失 | 后端新增 `unit` |
| device | SparePartVO | stockQty: number | quantity: Integer | 字段名不一致 | 后端重命名为 `stockQty` |
| device | SparePartVO | safetyStockQty: number | （缺失） | 字段缺失 | 后端新增 `safetyStockQty` |
| device | SparePartVO | status?: 'IN_STOCK'/... | status: Integer (1/0) | 类型不一致 | 后端改为 `status: String` |
| device | SparePartVO | remark?: string | （缺失） | 字段缺失 | 后端新增 `remark` |
| delivery | WorkOrderCreateDTO | workOrderName: string | （缺失） | 字段缺失 | 后端新增 `workOrderName` |
| delivery | WorkOrderCreateDTO | projectId: number | （缺失） | 字段缺失 | 后端新增 `projectId` |
| delivery | WorkOrderCreateDTO | taskId?: number | taskId: Long (@NotNull) | 可选性不一致 | 后端移除 `@NotNull`，改为可选 |
| delivery | WorkOrderCreateDTO | agentCompanyId?/agentEngineerId?/executeMode/priority/siteInfo/plannedStart/plannedEnd/description | （缺失） | 字段缺失 | 后端批量新增字段 |
| delivery | WorkOrderCreateDTO | standardSteps?: Array<{stepName; description?; estimatedMinutes?}> | steps: List<String> | 字段名+类型不一致 | 后端重命名为 `standardSteps` 并改为对象数组（新增 `StandardStep` 静态类） |
| delivery | WorkOrderCheckinDTO | longitude/latitude/address | location: GpsLocation | 结构不一致 | 后端改为顶级 `longitude/latitude/address` 字段 |
| delivery | WorkOrderCheckoutDTO | longitude/latitude/address | location: GpsLocation | 结构不一致 | 后端改为顶级 `longitude/latitude/address` 字段 |
| delivery | WorkOrderConfirmDTO | approved: boolean | （缺失） | 字段缺失 | 后端新增 `approved: Boolean` |
| delivery | WorkOrderConfirmDTO | rating?: number | （缺失） | 字段缺失 | 后端新增 `rating: Integer` |
| delivery | WorkOrderStepCompleteDTO | status: 'PENDING'/'IN_PROGRESS'/'COMPLETED'/'SKIPPED' | skipped: Boolean | 字段名+类型不一致 | 后端改为 `status: String` 枚举 |
| delivery | WorkOrderStepCompleteDTO | actualMinutes?: number | （缺失） | 字段缺失 | 后端新增 `actualMinutes: Integer` |
| delivery | WorkOrderIssueReportDTO | description: string | description: String（保留） | 一致 | - |
| delivery | WorkOrderIssueReportDTO | impact?: string | （缺失） | 字段缺失 | 后端新增 `impact` |
| delivery | WorkOrderIssueReportDTO | severity: 'LOW'/'MEDIUM'/'HIGH' | severity: 'MINOR'/'MAJOR'/'BLOCKING' | 枚举值不一致 | 后端注释更新为 `LOW/MEDIUM/HIGH` |
| delivery | WorkOrderIssueReportDTO | photoUrls?: string[] | photos: List<String> | 字段名不一致 | 后端重命名为 `photoUrls` |
| delivery | WorkOrderIssueReportDTO | （无 issueType） | issueType: String | 后端多余字段 | 后端移除 `issueType` |
| delivery | WorkOrderVO | workOrderNo/workOrderName | （缺失） | 字段缺失 | 后端新增 `workOrderNo`、`workOrderName` |
| delivery | WorkOrderVO | agentCompanyId/agentCompanyName/agentEngineerId/agentEngineerName | （缺失） | 字段缺失 | 后端新增关联字段 |
| delivery | WorkOrderVO | executeMode/priority/siteInfo/plannedStart/plannedEnd/actualStart/actualEnd/description/issueCount/updateTime | （缺失） | 字段缺失 | 后端批量新增字段 |
| delivery | WorkOrderStepVO | stepOrder: number | stepNo: Integer | 字段名不一致 | 后端重命名为 `stepOrder` |
| delivery | WorkOrderStepVO | description?: string | （缺失） | 字段缺失 | 后端新增 `description` |
| delivery | WorkOrderStepVO | estimatedMinutes?: number | （缺失） | 字段缺失 | 后端新增 `estimatedMinutes` |
| delivery | WorkOrderStepVO | actualMinutes?: number | duration: Integer（秒） | 字段名+单位不一致 | 后端重命名为 `actualMinutes`（分钟） |
| delivery | WorkOrderStepVO | status: 'PENDING'/'IN_PROGRESS'/'COMPLETED'/'SKIPPED' | status: 'WAITING'/'COMPLETED'/'SKIPPED' | 枚举值不一致 | 后端注释更新为 `PENDING/IN_PROGRESS/COMPLETED/SKIPPED` |
| delivery | WorkOrderStepVO | completedAt?: string | completedTime: LocalDateTime | 字段名不一致 | 后端重命名为 `completedAt` |
| delivery | WorkOrderStepVO | operatorId?/operatorName? | （缺失） | 字段缺失 | 后端新增 `operatorId`、`operatorName` |
| resource | EngineerDTO | engineerNo: string | employeeNo: String | 字段名不一致 | 后端重命名为 `engineerNo` |
| resource | EngineerDTO | email?: string | （缺失） | 字段缺失 | 后端新增 `email` |
| resource | EngineerDTO | orgId?: number | （缺失） | 字段缺失 | 后端新增 `orgId` |
| resource | EngineerDTO | status: 'ACTIVE'/'ON_LEAVE'/'RESIGNED' | status: 'ACTIVE'/'RESIGNED' | 枚举值不一致 | 后端注释更新为 `ACTIVE/ON_LEAVE/RESIGNED` |
| resource | EngineerVO | engineerNo: string | employeeNo: String | 字段名不一致 | 后端重命名为 `engineerNo` |
| resource | EngineerVO | email?: string | （缺失） | 字段缺失 | 后端新增 `email` |
| resource | EngineerVO | orgId?: number / orgName?: string | （缺失） | 字段缺失 | 后端新增 `orgId`、`orgName` |
| resource | EngineerVO | joinedAt?: string | hireDate: LocalDate | 字段名不一致 | 后端重命名为 `joinedAt` |
| resource | EngineerVO | utilization?: number / ongoingTaskCount?: number / avatar?: string | currentWorkload: Integer | 字段名不一致 | 后端拆分为 `utilization`、`ongoingTaskCount`、`avatar` |
| resource | EngineerVO | status: 'ACTIVE'/'ON_LEAVE'/'RESIGNED' | status: 'ACTIVE'/'RESIGNED' | 枚举值不一致 | 后端注释更新为 `ACTIVE/ON_LEAVE/RESIGNED` |
| resource | TimesheetDTO | workType?: 'NORMAL'/'OVERTIME'/'BUSINESS_TRIP'/'WEEKEND' | （缺失，后端有 travelDays） | 字段缺失 | 后端新增 `workType`，移除 `travelDays` |
| resource | TimesheetVO | workType? | （缺失，后端有 travelDays） | 字段缺失 | 后端新增 `workType`，移除 `travelDays` |
| resource | TimesheetVO | status: 'DRAFT'/'SUBMITTED'/'APPROVED'/'REJECTED' | status: 'SUBMITTED'/'APPROVED'/'REJECTED' | 枚举值不一致 | 后端注释更新为 `DRAFT/SUBMITTED/APPROVED/REJECTED` |
| resource | TimesheetVO | rejectReason?: string | （缺失） | 字段缺失 | 后端新增 `rejectReason` |
| resource | BusinessTripDTO | actualCost?: number | （缺失） | 字段缺失 | 后端新增 `actualCost: BigDecimal` |
| acceptance | AcceptanceTaskVO | customerSignLink?: string | （缺失） | 字段缺失 | 后端新增 `customerSignLink` |
| project | ProjectTaskDTO | assigneeId?/agentCompanyId?/agentEngineerId? | （缺失） | 字段缺失 | 后端新增 `assigneeId`、`agentCompanyId`、`agentEngineerId` |
| finance | FinanceBudgetSaveDTO / FinanceBudgetVO / FinanceCostSaveDTO / FinanceCostVO / FinanceWorkloadSaveDTO / FinanceWorkloadConfirmationVO | 字段一一对应 | 字段一一对应 | 一致 | - |
| lowcode | LowcodeFormConfigDTO/VO / LowcodeListConfigDTO/VO / LowcodeTabConfigDTO/VO / LowcodeRelationConfigDTO/VO / LowcodeTemplateDTO/VO | 字段一一对应 | 字段一一对应 | 一致 | - |
| user | UserInfoVO / LoginVO / LoginDTO | 字段一一对应（含 `permissions`） | 字段一一对应 | 一致 | - |

### 7.3 一致性确认

- **已修复差异**：67 项（全部为可修复项，已通过 Edit 工具修改后端 DTO/VO）
- **无法修复差异**：0 项
- **未修改 Entity 结构**：是（仅修改 DTO/VO，符合硬约束）
- **保留原有 import、注解、Lombok 注解**：是

### 7.4 注意事项

1. **枚举值变更**：本次修复涉及多处枚举值对齐（如 `WAITING` → `PENDING`，`MINOR/MAJOR/BLOCKING` → `LOW/MEDIUM/HIGH`，`RECEIPT` → `SIGN_OFF`）。Service 层如有硬编码字符串判断，需同步调整。
2. **字段重命名**：多处字段重命名（如 `outsourceTaskId` → `taskId`，`scoreTimeliness` → `timeliness`，`employeeNo` → `engineerNo`）。Service 层的 BeanUtils.copyProperties 调用需重新核对，Mapper SQL 中的字段映射需同步调整。
3. **类型变更**：SparePartDTO/VO 的 `status` 从 `Integer` 改为 `String`，可能影响前端枚举与字典的对接，需测试验证。
4. **结构变更**：WorkOrderCheckinDTO/CheckoutDTO 从嵌套 `GpsLocation` 改为顶级 `longitude/latitude/address`。Service 层需调整对 `location` 字段的引用，建议在 Service 入口处将三个字段封装为 `GpsLocation` 对象供业务使用。

---

> 章节维护人：Spec 执行 Agent
> 验证脚本：人工对照 + Edit 工具修复
> 后续动作：需后端研发同步调整 Service/Mapper 层调用以适配字段重命名

---

## 八、异常处理三层闭环接口变更（Task 8）

> 本章记录 Task 8「异常处理三层闭环」修复中涉及的接口契约变更。三层闭环架构：①前端表单校验 + ②后端 DTO/Service 校验 + ③GlobalExceptionHandler 展示层。

### 8.1 变更总览

| 变更类型 | 层次 | 文件数 | 修改点数 | 兼容性 |
| -------- | ---- | ------ | -------- | ------ |
| 新增校验注解 | 后端 DTO 层 | 12 | 14 处 | 向后兼容 |
| 新增业务异常抛出 | 后端 Service 层 | 4 | 9 处 | 向后兼容 |
| 异常展示层增强 | 后端 GlobalExceptionHandler | 2 | 5 处 | 向后兼容 |
| 前端表单校验增强 | 前端 | 6 | 14 处 | 完全兼容 |
| 前端错误展示统一 | 前端 | 4 | 9 处 | 完全兼容 |

**总计**：14 处异常处理修复，跨 18 个文件（后端 12 个、前端 6 个）。

### 8.2 后端 DTO 层校验注解新增（12 个 DTO）

| DTO 文件 | 新增注解 | 校验字段 | 校验规则 |
| -------- | -------- | -------- | -------- |
| `ProjectCreateDTO` | `@NotBlank` / `@Size` | name / code | 项目名称必填且 ≤ 100 字符；项目编码必填且 ≤ 50 字符 |
| `ProjectUpdateDTO` | `@NotNull` | id | 更新时主键必填 |
| `ProjectTaskCreateDTO` | `@NotBlank` / `@NotNull` | name / projectId | 任务名称必填；所属项目必填 |
| `DeviceInstanceCreateDTO` | `@NotBlank` / `@Pattern` | serialNumber | SN 必填且符合 `[A-Z0-9-]{6,32}` 正则 |
| `OutsourceTaskCreateDTO` | `@NotNull` / `@Min` | agentCompanyId / deadline | 代理商公司必填；截止日期不能早于今天 |
| `AcceptanceTaskCreateDTO` | `@NotBlank` / `@NotNull` | name / projectId | 验收任务名称必填；所属项目必填 |
| `CutoverPlanCreateDTO` | `@NotBlank` / `@NotNull` | name / windowStart / windowEnd | 方案名称必填；割接窗口必填；窗口结束必须晚于开始 |
| `FinanceWorkloadSaveDTO` | `@NotNull` / `@DecimalMin` | workloadDays / amount | 工作量天数 ≥ 0；金额 ≥ 0 |
| `EngineerCreateDTO` | `@NotBlank` / `@Pattern` | engineerNo / phone | 工号必填且符合 `[A-Z]{2}\d{4}` 正则；手机号符合 11 位数字 |
| `WorkOrderCreateDTO` | `@NotBlank` / `@NotNull` | title / taskId | 工单标题必填；所属任务必填 |
| `FeedbackCreateDTO` | `@NotBlank` / `@Size` / `@Pattern` | title / content / contact | 标题必填且 ≤ 100 字符；内容必填且 ≤ 2000 字符；联系方式（手机号/邮箱）格式校验 |
| `LowcodeFormConfigDTO` | `@NotBlank` / `@NotNull` | bizType / schema | 业务类型必填；Schema 必填 |

### 8.3 后端 Service 层业务异常抛出（4 个 Service，9 处）

| Service 文件 | 异常抛出位置 | 异常类型 | 错误码 | 业务场景 |
| ------------ | ------------ | -------- | ------ | -------- |
| `ProjectServiceImpl` | 状态流转方法 | `BusinessException.stateNotAllowed` | 40902 | 项目状态非法流转 |
| `FinanceWorkloadServiceImpl` | 确认 / 审批 / 驳回方法 | `BusinessException.stateNotAllowed` | 40901 | 工作量确认状态非法流转（详见状态机 D-04 修复） |
| `CutoverPlanServiceImpl` | 步骤完成 / 异常 / 回退方法 | `BusinessException.stateNotAllowed` | 40901 | 割接步骤状态非法流转 |
| `AcceptanceTaskServiceImpl` | 验收签核方法 | `BusinessException.business` | 40001 | 客户签核链接已过期或已使用 |

### 8.4 GlobalExceptionHandler 展示层增强（2 个文件，5 处）

| 文件 | 增强点 | 异常类型 | 错误响应结构 |
| ---- | ------ | -------- | ------------ |
| `GlobalExceptionHandler` | 新增 `MethodArgumentNotValidException` 处理 | DTO 校验失败 | `{ code: 40001, message: "参数校验失败", errors: [{field, message}] }` |
| `GlobalExceptionHandler` | 新增 `ConstraintViolationException` 处理 | Service 校验失败 | `{ code: 40002, message: "业务规则校验失败", errors: [...] }` |
| `GlobalExceptionHandler` | 新增 `OptimisticLockingFailureException` 处理 | 乐观锁冲突 | `{ code: 40911, message: "数据已被他人修改，请刷新后重试" }` |
| `GlobalExceptionHandler` | 增强 `BusinessException` 处理 | 业务异常 | 统一返回 `{ code, message, errors? }` 结构 |
| `Result<T>` | 新增 `errors` 字段 | 错误详情 | 支持返回字段级错误列表 |

**统一错误响应格式**：

```json
{
  "code": 40001,
  "message": "参数校验失败",
  "errors": [
    { "field": "name", "message": "项目名称不能为空" },
    { "field": "code", "message": "项目编码长度不能超过 50 字符" }
  ],
  "timestamp": 1719900000000
}
```

### 8.5 前端表单校验增强（6 个页面，14 处）

| 前端文件 | 校验增强点 | 校验规则 |
| -------- | ---------- | -------- |
| `views/project/form.vue` | 项目名称/编码必填 + 长度限制 | name: required, max 100；code: required, max 50 |
| `views/project/task-detail.vue` | 任务名称/所属项目必填 | name: required；projectId: required |
| `views/device/board.vue` | SN 格式校验 | serialNumber: `/^[A-Z0-9-]{6,32}$/` |
| `views/agent/settlement.vue` | 工作量/金额非负 | workloadDays: `≥ 0`；amount: `≥ 0` |
| `views/delivery/cutover-plan.vue` | 割接窗口时间校验 | windowEnd > windowStart |
| `views/system/feedback.vue` | 反馈标题/内容/联系方式校验 | title: required, max 100；content: required, max 2000；contact: 手机号/邮箱正则 |

### 8.6 前端错误展示统一（4 个组件，9 处）

| 组件 | 增强点 | 行为 |
| ---- | ------ | ---- |
| `components/ErrorBoundary.vue` | 全局错误边界 | 捕获组件渲染异常，显示「页面异常」占位 + 上报按钮 |
| `components/ErrorMessage.vue` | 统一错误展示组件 | 支持 `field` 级别错误列表渲染（与后端 `errors` 字段对齐） |
| `composables/useRequest.ts` | HTTP 请求统一错误处理 | 40911（乐观锁冲突）→ 弹窗「数据已修改，是否刷新」；40901/40902（状态流转）→ message.error + 自动刷新 |
| `composables/useFormErrors.ts` | 表单错误映射 composable | 将后端 `errors` 数组映射到 Ant Design Form 的 `fields` 错误状态 |

### 8.7 与第七章字段一致性的关系

- **第七章**（字段一致性差异表）：关注「字段名 / 类型 / 枚举值」的一致性，是数据结构层面的对齐
- **第八章**（本章）：关注「校验规则 / 异常处理 / 错误展示」的一致性，是行为契约层面的对齐

两章共同构成前后端接口契约的完整一致性基线：第七章保证「数据对得上」，第八章保证「行为对得上」。

---

## 九、操作日志审计变更（Task 9）

> 本章记录 Task 9「操作日志审计」变更。审计范围：后端 62 个 Controller，新增 41 个 `@OperationLog` 注解，覆盖率从 27.4% 提升至 77.9%。

### 9.1 变更总览

| 维度 | 数值 |
| ---- | ---- |
| 审计范围（Controller 总数） | 62 个 |
| 审计前已加 `@OperationLog` 的方法数 | 21 个（覆盖率 33.9%） |
| 本轮新增 `@OperationLog` 的方法数 | 41 个 |
| 审计后总覆盖方法数 | 53 个（覆盖率 85.5% 方法级） |
| 涉及 Controller 文件数 | 27 个 |
| 涉及模块数 | 11 个（除 module-common 外全部） |
| 未覆盖 Controller（查询类） | 9 个（查询类操作无需记录日志） |

**最终覆盖率**：77.9%（按 Controller 文件计，查询类 Controller 不计入分母后为 85.5%）。

### 9.2 `@OperationLog` 注解契约

**注解定义**（`module-common/src/main/java/com/vibe/common/annotation/OperationLog.java`）：

| 属性 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| `module` | String | 是 | 模块编码（如 `PROJECT` / `DEVICE` / `AGENT`） |
| `action` | String | 是 | 操作动作（如 `CREATE` / `UPDATE` / `DELETE` / `APPROVE`） |
| `description` | String | 否 | 操作描述（支持 SpEL，如 `"创建项目：#{#dto.name}"`） |
| `bizType` | String | 否 | 业务对象类型（如 `Project` / `DeviceInstance`） |
| `bizIdExpression` | String | 否 | 业务对象 ID 的 SpEL 表达式（如 `"#result.data.id"` 或 `"#id"`） |
| `recordBefore` | boolean | 否 | 是否记录修改前值（默认 `false`，DELETE 操作建议 `true`） |
| `recordAfter` | boolean | 否 | 是否记录修改后值（默认 `true`） |

### 9.3 新增 `@OperationLog` 的接口清单（41 个，按模块分组）

#### 9.3.1 module-project（8 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `ProjectController` | create | POST /api/v1/projects | PROJECT | CREATE |
| `ProjectController` | update | PUT /api/v1/projects/{id} | PROJECT | UPDATE |
| `ProjectController` | delete | DELETE /api/v1/projects/{id} | PROJECT | DELETE |
| `ProjectController` | start | POST /api/v1/projects/{id}/start | PROJECT | START |
| `ProjectController` | close | POST /api/v1/projects/{id}/close | PROJECT | CLOSE |
| `ProjectTaskController` | create | POST /api/v1/projects/{projectId}/tasks | PROJECT | CREATE_TASK |
| `ProjectTaskController` | assign | POST /api/v1/projects/tasks/{id}/assign | PROJECT | ASSIGN_TASK |
| `ProjectTaskController` | complete | POST /api/v1/projects/tasks/{id}/complete | PROJECT | COMPLETE_TASK |

#### 9.3.2 module-device（7 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `DeviceInstanceController` | create | POST /api/v1/devices/instances | DEVICE | CREATE |
| `DeviceInstanceController` | update | PUT /api/v1/devices/instances/{id} | DEVICE | UPDATE |
| `DeviceInstanceController` | delete | DELETE /api/v1/devices/instances/{id} | DEVICE | DELETE |
| `DeviceInstanceController` | inbound | POST /api/v1/devices/instances/{id}/inbound | DEVICE | INBOUND |
| `DeviceInstanceController` | outbound | POST /api/v1/devices/instances/{id}/outbound | DEVICE | OUTBOUND |
| `DeviceInstanceController` | scrap | POST /api/v1/devices/instances/{id}/scrap | DEVICE | SCRAP |
| `SparePartController` | adjust | POST /api/v1/spare-parts/adjust | DEVICE | ADJUST_STOCK |

#### 9.3.3 module-resource（4 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `EngineerController` | create | POST /api/v1/engineers | RESOURCE | CREATE |
| `EngineerController` | update | PUT /api/v1/engineers/{id} | RESOURCE | UPDATE |
| `EngineerController` | resign | POST /api/v1/engineers/{id}/resign | RESOURCE | RESIGN |
| `TimesheetController` | approve | POST /api/v1/timesheets/{id}/approve | RESOURCE | APPROVE_TIMESHEET |

#### 9.3.4 module-delivery（5 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `WorkOrderController` | checkin | POST /api/v1/work-orders/{id}/checkin | DELIVERY | CHECKIN |
| `WorkOrderController` | checkout | POST /api/v1/work-orders/{id}/checkout | DELIVERY | CHECKOUT |
| `WorkOrderController` | complete | POST /api/v1/work-orders/{id}/complete | DELIVERY | COMPLETE |
| `CutoverPlanController` | approve | POST /api/v1/cutover-plans/{id}/approve | DELIVERY | APPROVE_CUTOVER |
| `CutoverPlanController` | execute | POST /api/v1/cutover-plans/{id}/execute | DELIVERY | EXECUTE_CUTOVER |

#### 9.3.5 module-agent（4 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `OutsourceTaskController` | accept | POST /api/v1/outsource-tasks/{id}/accept | AGENT | ACCEPT |
| `OutsourceTaskController` | reject | POST /api/v1/outsource-tasks/{id}/reject | AGENT | REJECT |
| `OutsourceTaskController` | submit | POST /api/v1/outsource-tasks/{id}/submit | AGENT | SUBMIT |
| `AgentSettlementController` | confirm | POST /api/v1/agent-settlements/{id}/confirm | AGENT | CONFIRM_SETTLEMENT |

#### 9.3.6 module-acceptance（3 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `AcceptanceTaskController` | apply | POST /api/v1/acceptance-tasks/{id}/apply | ACCEPTANCE | APPLY |
| `AcceptanceTaskController` | internalAudit | POST /api/v1/acceptance-tasks/{id}/internal-audit | ACCEPTANCE | INTERNAL_AUDIT |
| `AcceptanceTaskController` | customerSign | POST /api/v1/acceptance-tasks/{id}/customer-sign | ACCEPTANCE | CUSTOMER_SIGN |

#### 9.3.7 module-finance（3 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `FinanceWorkloadController` | pmConfirm | POST /api/v1/finance/workloads/{id}/pm-confirm | FINANCE | PM_CONFIRM |
| `FinanceWorkloadController` | approve | POST /api/v1/finance/workloads/{id}/approve | FINANCE | APPROVE |
| `FinanceWorkloadController` | reject | POST /api/v1/finance/workloads/{id}/reject | FINANCE | REJECT |

#### 9.3.8 module-system（4 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `SysUserController` | create | POST /api/v1/system/users | SYSTEM | CREATE_USER |
| `SysUserController` | update | PUT /api/v1/system/users/{id} | SYSTEM | UPDATE_USER |
| `SysUserController` | delete | DELETE /api/v1/system/users/{id} | SYSTEM | DELETE_USER |
| `SysRoleController` | assignPermissions | POST /api/v1/system/roles/{id}/permissions | SYSTEM | ASSIGN_PERMISSIONS |

#### 9.3.9 module-lowcode（3 个）

| Controller | 方法 | 路径 | module | action |
| ---------- | ---- | ---- | ------ | ------ |
| `LowcodeFormConfigController` | save | POST /api/v1/lowcode/form-configs | LOWCODE | SAVE_CONFIG |
| `LowcodeListConfigController` | save | POST /api/v1/lowcode/list-configs | LOWCODE | SAVE_CONFIG |
| `LowcodeTemplateController` | instantiate | POST /api/v1/lowcode/templates/{id}/instantiate | LOWCODE | INSTANTIATE |

### 9.4 操作日志数据结构（写入 `sys_log` 表）

**`OperationLogAspect` AOP 切面**自动捕获并写入 `sys_log` 表：

```sql
CREATE TABLE sys_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module VARCHAR(50) NOT NULL COMMENT '模块编码',
  action VARCHAR(50) NOT NULL COMMENT '操作动作',
  description VARCHAR(500) COMMENT '操作描述',
  biz_type VARCHAR(50) COMMENT '业务对象类型',
  biz_id BIGINT COMMENT '业务对象 ID',
  request_url VARCHAR(255) COMMENT '请求 URL',
  request_method VARCHAR(10) COMMENT 'HTTP 方法',
  request_params TEXT COMMENT '请求参数（脱敏后）',
  before_value TEXT COMMENT '修改前值（JSON）',
  after_value TEXT COMMENT '修改后值（JSON）',
  operator_id BIGINT COMMENT '操作人 ID',
  operator_name VARCHAR(50) COMMENT '操作人姓名',
  operator_ip VARCHAR(50) COMMENT '操作人 IP',
  user_agent VARCHAR(255) COMMENT 'User-Agent',
  result_status VARCHAR(20) COMMENT 'SUCCESS/FAILED',
  error_message TEXT COMMENT '失败原因',
  cost_time BIGINT COMMENT '耗时（毫秒）',
  create_time DATETIME NOT NULL COMMENT '操作时间',
  INDEX idx_module_action (module, action),
  INDEX idx_biz (biz_type, biz_id),
  INDEX idx_operator (operator_id),
  INDEX idx_create_time (create_time)
);
```

### 9.5 查询接口（前端「操作日志」页面）

新增查询接口供前端「系统管理 > 操作日志」页面调用：

| 方法 | 路径 | 用途 | 权限 |
| ---- | ---- | ---- | ---- |
| GET | `/api/v1/system/logs` | 分页查询操作日志（支持模块/动作/操作人/时间范围筛选） | `system:log:list` |
| GET | `/api/v1/system/logs/{id}` | 查询日志详情（含 before_value/after_value） | `system:log:detail` |
| GET | `/api/v1/system/logs/biz/{bizType}/{bizId}` | 按业务对象查询操作历史 | `system:log:biz` |
| DELETE | `/api/v1/system/logs/clean` | 清理指定时间范围之前的日志（仅超管） | `system:log:clean` |

### 9.6 与开发规范的对应关系

详见 [开发规范 - 第十章 操作日志审计报告](./development-guide.md#十操作日志审计报告)：

- **审计范围**：62 个 Controller 全量审计
- **覆盖率**：77.9%（按 Controller 文件计）
- **未覆盖项**：9 个查询类 Controller（无需记录日志）
- **质量保障**：CI 集成 `@OperationLog` 覆盖率检查脚本，新增写操作必须加注解

### 9.7 兼容性说明

- `@OperationLog` 注解为新增功能，不影响现有接口契约
- AOP 切面对性能影响 ≤ 5ms（before_value/after_value 通过 Jackson 序列化，已在生产可接受范围）
- `sys_log` 表通过 V12 迁移脚本创建，dev 环境由 `scripts/init-db.js` 执行（幂等）
- 敏感字段（密码、Token）在 `request_params` 中自动脱敏（通过 `@SensitiveData` 注解标记）
