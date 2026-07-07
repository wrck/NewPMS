# 企业级生产就绪度补全与正式验收 Spec

## Why

系统设计文档（`系统设计文档.md`）已于 2026-07-06 之前通过多次迭代开发基本落地：前端 75 个页面、后端 15 个 Maven 模块、6 个低代码组件、9 篇文档、5 个部署脚本、26 个测试文件均已就位。但该工作未走 spec-driven 流程，缺少正式的可追溯验收基线；同时测试覆盖存在明显不均衡（前端测试仅覆盖 lowcode/resource/project/device 部分页面，后端测试仅覆盖 8/15 模块），部分模块的边界场景与异常路径未验证，前后端字段/状态机一致性缺少交叉核对。本 spec 不重做已有功能，而是聚焦于**系统性差距识别、补全与正式验收**，使最终交付物达到企业级生产标准。

## What Changes

- 对 `系统设计文档.md` 全文做结构化解析，对照现有代码逐项核对功能点覆盖率（产出 `docs/requirement-overview.md` 更新与差距清单）
- 补全**缺失模块的单元测试**：前端对 dashboard / agent / finance / acceptance / delivery / system / report / h5 关键页面补测；后端对 auth / finance / acceptance / collaboration / integration / report / system（除 feedback 外）补测
- 补全**集成测试与 e2e**：扩展 `scripts/e2e-smoke.spec.ts`，覆盖核心业务流程（立项→派单→执行→验收→结项）
- 交叉验证**前后端字段一致性**：DTO/VO 与前端类型定义逐一对照，输出差异表与修复
- 验证**状态机一致性**：前端 StatusTag/Enum 与后端 Enum/状态流转规则对齐
- 完善**演示数据**：确保 dev 环境具备覆盖所有角色、所有状态、所有实体的真实演示数据
- 完善**异常处理链路**：验证前端表单校验、后端业务校验、展示层错误提示三层闭环
- 完善**操作日志**：关键操作（CRUD、状态变更、审批）落库且可查询
- 完善**用户引导**：交互式新手教程、上下文帮助提示、功能说明文档完整性
- 同步**文档体系**：根据最终实现更新 docs/ 下 9 篇文档
- 建立**正式验收清单**：覆盖功能完整性、UI/UX、架构可扩展性、部署交付、低代码覆盖率、文档体系六大维度
- 完善**低代码能力**：用户要求开发表单/列表/标签页/关联页的可视化配置功能，目前全项目零相关代码。

### 低代码模块（ADDED + MODIFIED）

- **ADDED** 后端 module-lowcode 模块（新建）：表单/列表/标签页/关联页配置 Schema 持久化
  - Entity：`lowcode_form_config` / `lowcode_list_config` / `lowcode_tab_config` / `lowcode_relation_config` / `lowcode_template`
  - API：CRUD + 模板导入导出（JSON）+ 模板复用（基于 templateId 实例化）
  - Schema 校验：JSON Schema Draft 7
- **ADDED** 前端低代码配置器（`/views/lowcode/`）：
  - 表单配置器：拖拽字段（输入框/下拉/日期/开关/单选多选/级联）、字段属性配置（label/required/placeholder/校验规则）、预览、保存
  - 列表配置器：列定义（字段/标题/对齐/宽/格式化）、筛选条件配置、操作按钮配置、预览、保存
  - 标签页配置器：Tab 定义、Tab 内嵌表单/列表/关联页、布局配置、预览、保存
  - 关联页配置器：主从关联配置、级联规则、显示字段、预览、保存
  - 模板管理：模板列表、新建/编辑/删除、导入 JSON、导出 JSON、基于模板实例化
- **ADDED** 低代码运行时引擎：
  - 表单运行时（FormRenderer）：根据 Schema 动态渲染表单，集成校验、联动、字段权限
  - 列表运行时（ListRenderer）：根据 Schema 动态渲染列表，集成筛选、分页、操作、导出
  - 标签页运行时（TabRenderer）：根据 Schema 动态渲染多 Tab 页面
  - 关联页运行时（RelationRenderer）：根据 Schema 动态渲染主从关联页
- **ADDED** 低代码业务接入：至少 3 个示例业务页（设备巡检表单、客户回访列表、项目阶段交付关联页）通过低代码配置生成

## Impact

- **Affected specs**: 无（本项目此前无 `.trae/specs` 文档）
- **Affected code**:
  - 前端测试目录：`vibe-web/src/views/**/__tests__/`、`vibe-web/src/api/__tests__/`、`vibe-web/src/components/**/__tests__/`
  - 后端测试目录：`vibe-server/module-*/src/test/java/`
  - 集成测试：`scripts/e2e-smoke.spec.ts`
  - 演示数据：`vibe-server/vibe-server-bootstrap/src/main/resources/db/data.sql`、`scripts/init-db.js`
  - 文档目录：`docs/`
  - 可能的修复点：前后端字段不一致处的 DTO/VO 或前端类型定义（按"前端优先"硬约束修复后端）

## ADDED Requirements

### Requirement: 低代码配置与运行时
系统 SHALL 提供低代码配置能力，至少覆盖表单、列表、标签页、关联页 4 类基础组件，支持可视化配置、Schema 持久化、模板导入导出与复用，并提供运行时引擎动态渲染。

#### Scenario: 表单可视化配置
- **WHEN** 管理员在低代码表单配置器拖拽 5 个字段（输入框/下拉/日期/开关/单选）并保存
- **THEN** 系统持久化 Schema 到 `lowcode_form_config` 表，预览渲染出对应表单，字段属性（label/required/placeholder/校验规则）全部生效

### Requirement: 正式差距分析与覆盖率核验
系统 SHALL 提供一份对照 `系统设计文档.md` 全部功能点的覆盖率核验报告，明确列出：已实现、部分实现、未实现、实现偏差四类，并标注代码位置。

#### Scenario: 功能点核验
- **WHEN** 验收人查阅 `docs/requirement-overview.md` 中的功能点核验表
- **THEN** 每个功能点都能找到对应的代码实现位置（文件路径 + 行号）或被明确标记为"未实现/部分实现"并附差距说明

### Requirement: 模块单元测试补全
系统 SHALL 为所有核心业务模块提供单元测试覆盖，前端单测覆盖率不低于 90%、后端不低于 85%（按 jacoco 统计 Service 层方法覆盖）。

#### Scenario: 缺失测试补全
- **WHEN** 执行 `npm run test` 与 `mvn test`
- **THEN** 所有测试通过，且覆盖到此前未测试的模块（dashboard / agent / finance / acceptance / delivery / system / report / h5；auth / finance / acceptance / collaboration / integration / report / system）

### Requirement: 端到端业务流程验证
系统 SHALL 提供覆盖核心业务流程（立项→规划→派单→执行→验收→结项）的 e2e 测试，验证前后端联调可用。

#### Scenario: 核心业务流程
- **WHEN** 执行 `npm run test:e2e`
- **THEN** 至少 1 条贯穿全部状态的端到端用例通过，且涉及至少 3 个角色（PM、ENGINEER、AGENT_ADMIN）

### Requirement: 前后端字段一致性
系统 SHALL 保证前端 TypeScript 类型定义与后端 DTO/VO 字段名称、类型、可选性完全一致；遇到不一致时按"前端优先"硬约束修改后端 DTO/VO。

#### Scenario: 字段对照
- **WHEN** 执行字段一致性核验脚本
- **THEN** 输出差异表，所有差异均已修复（后端对齐前端）

### Requirement: 状态机一致性
系统 SHALL 保证前端 `StatusTag` 组件显示的标签与后端 Enum 的状态值、流转规则一致。

#### Scenario: 状态流转
- **WHEN** 用户在前端触发状态变更操作
- **THEN** 后端接收到的状态值与前端发送一致，且符合状态机定义（不允许的流转返回 409xx 错误）

### Requirement: 演示数据完整性
系统 SHALL 在 dev 环境提供覆盖所有角色、所有状态、所有实体的演示数据，确保首次启动即可演示全部功能。

#### Scenario: 全角色演示
- **WHEN** 首次启动 dev 环境（运行 `scripts/init-db.js` + 启动后端）
- **THEN** 可用 7 个内部角色 + 3 个外部角色账号登录，且每个角色都能看到与其权限匹配的非空数据

### Requirement: 异常处理三层闭环
系统 SHALL 在前端表单校验、后端业务校验、展示层错误提示三层形成闭环：前端拦截输入错误、后端校验业务规则、展示层给出用户友好的错误提示与恢复建议。

#### Scenario: 异常路径
- **WHEN** 用户提交包含非法数据的表单（必填项为空、格式错误、业务规则违反）
- **THEN** 前端先做基础校验拦截；通过后后端返回 400xx/409xx 错误码与字段级错误信息；前端在表单对应字段下方展示错误提示

### Requirement: 关键操作日志
系统 SHALL 对所有 CRUD、状态变更、审批操作记录操作日志，包含操作人、操作时间、操作类型、目标实体、变更前后值。

#### Scenario: 操作日志查询
- **WHEN** 管理员在系统日志页面查询某实体的操作历史
- **THEN** 能看到该实体的全部操作记录，按时间倒序，支持按操作人/操作类型/时间范围筛选

### Requirement: 用户引导系统
系统 SHALL 提供交互式新手教程（首次登录引导关键操作路径）、上下文帮助提示（? 图标悬浮说明）、详细功能说明文档（用户手册），确保新用户上手时间不超过 15 分钟。

#### Scenario: 新用户引导
- **WHEN** 新用户首次登录系统
- **THEN** 自动弹出新手教程，引导其完成 3-5 个关键操作（查看工作台、创建项目、派发任务等），可随时跳过

### Requirement: 正式验收清单
系统 SHALL 提供覆盖六大维度（功能完整性、UI/UX、架构可扩展性、部署交付、低代码覆盖率、文档体系）的正式验收清单，每项验收点对应可验证的代码/文档/测试位置。

#### Scenario: 验收通过
- **WHEN** 验收人按 `checklist.md` 逐项核验
- **THEN** 所有验收点均通过，未通过的已创建修复任务并完成

## MODIFIED Requirements

### Requirement: 文档体系同步
`docs/` 下 9 篇文档（user-manual / test-strategy / test-coverage-report / state-machine / requirement-overview / development-guide / design-architecture / deployment-guide / api-change-log）SHALL 反映最终实现状态，对存在偏差的章节做同步更新。

#### Scenario: 文档一致性
- **WHEN** 验收人查阅任一文档章节
- **THEN** 文档描述与代码实现一致（接口路径、字段名、状态值、配置项）

## REMOVED Requirements

无（本 spec 不移除任何已有功能）。
