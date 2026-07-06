# 企业级生产标准全模块完善 Spec

> **Change-ID**: `enterprise-completion`
> **基线文档**: `系统设计文档.md` V1.0
> **目标**: 在已成熟的代码基线（13 个后端模块 / ~60 个 Controller / ~80 个 Entity / ~60 个前端视图 / 23 个通用组件）之上，系统性补齐真实存在的功能缺口、把现有功能完善到企业级生产标准，不重写已可用的代码。

## Why

系统已具备完整骨架与多数业务模块的可用实现，但在用户提出的五个维度上仍存在**真实可验证的缺口**，阻碍其达到企业级生产标准：

1. **低代码模块**：后端 `module-lowcode` 已有 5 个 Controller（Form/List/Tab/Relation/Template）与对应 Entity/Service，但**前端完全没有实现**（无 `views/lowcode/`、无 `api/lowcode.ts`），导致用户需求第 5 项「低代码模块实现」整体缺失。
2. **实体管理覆盖**：部分被引用的实体（如 `Warehouse`、`SparePart`、`BusinessTrip`、`EngineerLeave`、`Customer`、`ProjectTemplate` 等）尚未在前端形成可视化管理界面，用户需求第 1 项「针对所有存在引用关系的实体，设计并开发对应的实体管理界面」未完全满足。
3. **交付与部署**：缺少一键部署/回滚脚本完善、用户引导系统（新手教程/上下文帮助）、技术支持体系（工单/反馈/知识库），对应用户需求第 4 项。
4. **测试与质量**：现有前端测试覆盖不足，缺少系统化回归用例，未达到用户要求「测试覆盖率不低于 90%」。
5. **业务流程与异常处理补全**：状态机转换、关键操作日志、统一异常提示等在部分模块仍待打磨。

## What Changes

### A. 低代码模块前端完整实现（最高优先级，对应需求第 5 项）
- 新增 `vibe-web/src/api/lowcode.ts`：封装 Form/List/Tab/Relation/Template 五类配置的 CRUD + 导入导出 + 模板实例化接口。
- 新增 `vibe-web/src/views/lowcode/`：
  - `form-config.vue`：表单 Schema 配置列表 + 设计器（拖拽字段、属性面板、JSON 预览、预览渲染）。
  - `list-config.vue`：列表 Schema 配置（列定义、筛选器、操作按钮、JSON 预览）。
  - `tab-config.vue`：标签页 Schema 配置。
  - `relation-config.vue`：关联页 Schema 配置。
  - `template-library.vue`：模板库管理（CRUD、复制、导入导出、版本管理、实例化）。
  - `runtime-renderer.vue`：通用运行时渲染器（根据 schema 动态渲染表单/列表/标签页/关联页，与手工开发的界面视觉一致）。
- 新增 `vibe-web/src/components/Lowcode/`：
  - `SchemaDesigner.vue`：可视化设计器壳（左侧组件库 / 中间画布 / 右侧属性面板）。
  - `FieldPalette.vue`：可拖拽字段组件库（输入框/下拉/日期/开关/级联/富文本/文件/关联选择…）。
  - `PropertyPanel.vue`：字段属性配置面板（label/required/默认值/校验规则/样式）。
  - `SchemaPreview.vue`：实时预览渲染（调用运行时渲染器）。
  - `SchemaImporter.vue`：JSON 导入校验 + 模板选择。
- 新增路由 `/lowcode/form` `/lowcode/list` `/lowcode/tab` `/lowcode/relation` `/lowcode/template` `/lowcode/runtime/:bizType/:bizId`。
- 菜单「系统管理」下新增「低代码配置」一级菜单（仅 SUPER_ADMIN 可见）。
- **低代码实现率 ≥ 60%**：将「客户档案、设备型号、备件、仓库、工程师技能、代理商工程师、验收标准项、通知模板、字典数据、岗位」等通用 CRUD 实体改为通过 `runtime-renderer.vue` 渲染，验证低代码覆盖能力。

### B. 引用实体管理界面补全（对应需求第 1 项）
- 新增/补全以下实体管理视图（基于已有 `CrudTable` + `FormModal` 通用组件实现，保证一致性）：
  - `system/warehouse.vue`（仓库档案）
  - `device/spare-part.vue`（备件管理，含领用/归还/返修记录）
  - `resource/business-trip.vue`（差旅申请与记录）
  - `resource/leave.vue`（工程师请假管理）
  - `project/customer.vue`（客户档案）
  - `project/template-phase-task.vue`（项目模板阶段/任务子管理，嵌套在 template.vue 内）
  - `acceptance/standard-item.vue`（验收检查项管理，作为 standard.vue 的子表）
  - `agent/agent-engineer.vue`（代理商工程师档案，作为 profile.vue 的子页或抽屉）
- 为上述实体补全对应 `api/*.ts` 缺失方法（如已存在则复用）。

### C. 业务流程与异常处理完善（对应需求第 1 项）
- 前端：统一 `axios` 拦截器对 400xx/403xx/409xx 错误做友好提示（已在 `request.ts` 部分实现，补全关键字段错误高亮）。
- 后端：补全关键业务表（Project/DeviceInstance/OutsourceTask/WorkOrder/AcceptanceTask）的 `@Version` 乐观锁与状态机校验注解。
- 关键操作（状态变更/删除/审批/结算）补全操作日志（`SysLog`）记录。
- 编写状态转换矩阵文档 `docs/state-machine.md`：项目/设备/转包任务/验收任务/割接方案/工作量确认六类状态机。

### D. 交付与部署标准完善（对应需求第 4 项）
- 完善 `scripts/`：
  - `deploy.ps1` / `deploy.sh`：一键部署（拉镜像/迁移/启动/健康检查），支持 `--rollback`。
  - `rollback.ps1`：版本回滚。
  - 现有 `start.ps1` / `init-db.js` 保留并修复已知问题。
- 新增 `docs/deployment-guide.md`：环境要求、配置说明、部署步骤、故障排查。
- 新增用户引导系统：
  - `vibe-web/src/components/Onboarding/Tutorial.vue`：交互式新手教程（基于 `driver.js` 或自研，5 步引导）。
  - `vibe-web/src/components/Onboarding/ContextHelp.vue`：上下文帮助提示气泡（基于 `?` 图标 hover）。
  - `vibe-web/src/views/help/index.vue`：功能说明文档中心（按模块组织，支持搜索）。
- 新增反馈与工单系统：
  - `vibe-web/src/components/Feedback/FeedbackButton.vue`：右下角悬浮反馈按钮（功能建议/Bug 报告）。
  - 后端 `module-system` 新增 `FeedbackController` + `sys_feedback` 表（类型/内容/截图/联系方式/状态/处理人）。
  - `vibe-web/src/views/system/feedback.vue`：反馈管理列表（管理员查看与处理）。

### E. 测试与质量保障（对应需求第 1 项，覆盖率 ≥ 90%）
- 前端：为低代码模块、新增实体管理页、关键流程（登录/项目创建/任务派发/交付物提交/验收签核）补全 Vitest 单元测试 + `@vue/test-utils` 组件测试。
- 后端：为低代码 Service、Feedback Service 补全 JUnit5 单测 + MockMvc 集成测试。
- 新增 E2E 冒烟脚本 `scripts/e2e-smoke.spec.ts`（Playwright，覆盖核心 11 条 API 已有，扩充至 20 条）。

### F. 文档体系（对应需求「建立完善文档体系」）
- `docs/requirement-overview.md`：需求总览（基于系统设计文档提炼）。
- `docs/design-architecture.md`：架构设计（从系统设计文档抽取并补充实现细节）。
- `docs/state-machine.md`：六类状态机转换矩阵。
- `docs/development-guide.md`：开发规范（命名/分层/异常/日志/缓存）。
- `docs/test-strategy.md`：测试策略与用例清单。
- `docs/user-manual.md`：用户手册（按角色：管理员/PM/工程师/代理商/客户）。
- `docs/api-change-log.md`：本次新增/变更接口清单。

## Impact

- **Affected specs**: 本次为首次 spec，无前置 spec 受影响。
- **Affected code（关键文件，非全部）**:
  - 前端新增：`vibe-web/src/api/lowcode.ts`、`vibe-web/src/views/lowcode/*`、`vibe-web/src/views/system/warehouse.vue|feedback.vue`、`vibe-web/src/views/device/spare-part.vue`、`vibe-web/src/views/resource/business-trip.vue|leave.vue`、`vibe-web/src/views/project/customer.vue`、`vibe-web/src/views/acceptance/standard-item.vue`、`vibe-web/src/views/agent/agent-engineer.vue`、`vibe-web/src/views/help/index.vue`、`vibe-web/src/components/Lowcode/*`、`vibe-web/src/components/Onboarding/*`、`vibe-web/src/components/Feedback/*`。
  - 前端修改：`vibe-web/src/router/routes.ts`（注册新路由）、`vibe-web/src/layouts/` 菜单（追加菜单项）、`vibe-web/src/utils/request.ts`（错误提示补全）、`vibe-web/src/views/device/spare.vue|inout.vue|ledger.vue`（部分改造为低代码渲染以达 60% 覆盖率）。
  - 后端新增：`vibe-server/module-system/.../controller/FeedbackController.java` + `entity/SysFeedbackEntity.java` + `service/FeedbackService.java`、迁移脚本 `V_lowcode_feedback.sql`。
  - 后端修改：关键 Entity 追加 `@Version`；状态机 Service 补全校验。
  - 脚本：`scripts/deploy.ps1`、`scripts/rollback.ps1`、`scripts/e2e-smoke.spec.ts`。
  - 文档：`docs/*` 七份新增文档。
- **BREAKING**：无破坏性变更；不回滚用户已有改动。
- **不在本次范围**：重写已可用的 60 个前端视图与已稳定的后端业务 Service；引入新中间件；NMS 网管对接（Phase 3）；数据大屏（可选）。

## ADDED Requirements

### Requirement: 低代码配置设计器
系统 SHALL 提供可视化拖拽式低代码配置设计器，支持表单/列表/标签页/关联页四类 Schema 的设计、属性调整、样式自定义、JSON 预览、实时预览渲染、导入导出（JSON）、模板复用与版本管理。

#### Scenario: 设计表单 Schema
- **WHEN** 管理员进入「低代码 > 表单配置」并点击「新建」
- **THEN** 左侧字段组件库可拖拽至中间画布，右侧属性面板可编辑字段 label/required/默认值/校验规则/宽度
- **AND** 顶部「预览」按钮可实时渲染表单，效果与手工开发一致
- **AND** 「保存」生成 JSON Schema 并持久化到 `lowcode_form_config` 表

#### Scenario: 导入导出与模板复用
- **WHEN** 管理员点击「导出 JSON」
- **THEN** 下载符合 schema 的 JSON 文件，可被另一环境「导入 JSON」还原
- **AND** 可将配置「保存为模板」到 `lowcode_template`，后续通过「实例化模板」一键创建配置

### Requirement: 低代码运行时渲染器
系统 SHALL 提供通用运行时渲染器，根据 Schema 动态渲染对应业务实体的管理界面，与手工开发界面视觉与交互一致。

#### Scenario: 通过低代码渲染客户档案
- **WHEN** 用户访问 `/lowcode/runtime/customer/list`
- **THEN** 渲染器读取 `customer` 对应 list schema，调用 `/api/v1/customers` 获取数据
- **AND** 展示与 `system/user.vue` 同等视觉标准的列表（筛选/分页/操作列/状态 Tag）
- **AND** 低代码实现率（低代码渲染的实体管理页 / 全部实体管理页）≥ 60%

### Requirement: 引用实体管理界面
系统 SHALL 为所有被引用的实体提供可视化管理界面，确保实体间关系的可视化操作。

#### Scenario: 备件领用与归还
- **WHEN** 用户在「备件管理」页选择备件并点击「领用」
- **THEN** 弹窗填写领用项目/领用人/数量/备注，提交后生成 `spare_part_log` 记录并扣减库存
- **AND** 「归还」操作逆向恢复库存并记录日志

### Requirement: 用户引导与反馈系统
系统 SHALL 提供交互式新手教程、上下文帮助提示、功能说明文档中心，以及反馈与工单收集能力。

#### Scenario: 新用户首次登录引导
- **WHEN** 用户首次登录（`sys_user.first_login = 1`）进入工作台
- **THEN** 自动启动 5 步新手教程（工作台→项目→设备→任务→个人中心），每步高亮目标元素
- **AND** 用户可随时通过右上角 `?` 重新打开教程或进入文档中心

#### Scenario: 提交反馈
- **WHEN** 用户点击右下角悬浮反馈按钮，选择类型（建议/Bug/咨询），填写内容并提交
- **THEN** 后端创建 `sys_feedback` 记录，管理员在「系统管理 > 反馈管理」可见并处理
- **AND** 反馈状态变更（待处理/处理中/已解决/已关闭）通知提交人

### Requirement: 一键部署与回滚
系统 SHALL 提供自动化部署脚本，支持一键部署与版本回滚，并配套部署文档与故障排查指南。

#### Scenario: 一键部署
- **WHEN** 运维执行 `scripts/deploy.ps1 -Env prod -Version v1.2.0`
- **THEN** 脚本依次执行：拉取镜像/构建产物、执行 DB 迁移、滚动重启服务、健康检查（连续 3 次 `/actuator/health` 200）
- **AND** 全程输出步骤日志，失败时自动回滚到上一版本并告警

### Requirement: 测试覆盖率
系统 SHALL 为新增功能提供完整测试用例，新增代码测试覆盖率不低于 90%。

#### Scenario: 低代码设计器测试
- **WHEN** 运行 `npm run test:unit -- lowcode`
- **THEN** Schema 设计器的字段拖拽、属性编辑、JSON 导出/导入、预览渲染均有测试覆盖
- **AND** 整体新增前端代码行覆盖率 ≥ 90%

## MODIFIED Requirements

### Requirement: 异常处理与操作日志
**现有实现**：`request.ts` 已有基础错误拦截；`SysLog` 已有记录能力但部分关键操作未接入。
**修改后**：
- 前端 `axios` 拦截器对 400xx 错误自动提取 `errors[]` 字段错误并高亮对应表单项；403xx 提示「权限不足」；409xx 提示业务冲突原因；网络错误友好提示并提供「重试」按钮。
- 后端状态变更/审批/结算/删除等关键操作 Service 方法统一通过 `@LogAction` 注解（或显式调用 `SysLogService.record`）记录操作人/前后值/耗时。
- 关键业务表追加 `@Version` 乐观锁，并发冲突返回 40904 并提示「数据已被他人修改，请刷新后重试」。

## REMOVED Requirements
无移除项。
