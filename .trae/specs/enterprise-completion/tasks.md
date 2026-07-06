# Tasks

> **执行原则**：不重写已可用的 60 个前端视图与已稳定的后端 Service；任务粒度小且可验证；无依赖任务并行执行；每完成一组任务即提交 git（遵循项目约定式提交规范）。

## A. 低代码模块前端完整实现（最高优先级）

- [ ] Task A1: 创建前端低代码 API 层 `vibe-web/src/api/lowcode.ts`
  - [ ] SubTask A1.1: 封装 FormConfig CRUD + copy + import/export + instantiate 接口（对齐后端 `/api/v1/lowcode/forms`）
  - [ ] SubTask A1.2: 封装 ListConfig / TabConfig / RelationConfig CRUD 接口
  - [ ] SubTask A1.3: 封装 Template CRUD + instantiate 接口
  - [ ] SubTask A1.4: 补充 `vibe-web/src/types/lowcode.ts` 类型定义（FormSchema / ListSchema / TabSchema / RelationSchema / TemplateVO）

- [ ] Task A2: 实现低代码通用组件 `vibe-web/src/components/Lowcode/`
  - [ ] SubTask A2.1: `SchemaDesigner.vue` 设计器壳（左字段库 / 中画布 / 右属性面板三栏布局）
  - [ ] SubTask A2.2: `FieldPalette.vue` 可拖拽字段组件库（input/select/date/switch/cascader/richText/file/relSelect/number/textarea 共 10 类）
  - [ ] SubTask A2.3: `PropertyPanel.vue` 字段属性配置面板（label/required/defaultValue/rules/width/placeholder）
  - [ ] SubTask A2.4: `SchemaPreview.vue` 实时预览渲染（调用运行时渲染器，只读模式）
  - [ ] SubTask A2.5: `SchemaImporter.vue` JSON 导入校验（Ajv）+ 模板选择下拉

- [ ] Task A3: 实现运行时渲染器 `vibe-web/src/components/Lowcode/RuntimeRenderer.vue`
  - [ ] SubTask A3.1: 根据 schema.type 动态渲染表单（基于 a-form）
  - [ ] SubTask A3.2: 根据 schema.type 动态渲染列表（基于 CrudTable）
  - [ ] SubTask A3.3: 根据 schema.type 动态渲染标签页（a-tabs）
  - [ ] SubTask A3.4: 根据 schema.type 动态渲染关联页（master-detail）
  - [ ] SubTask A3.5: 数据源绑定（apiUrl + 字段映射）+ 分页 + 操作按钮回调

- [ ] Task A4: 实现低代码配置管理视图 `vibe-web/src/views/lowcode/`
  - [ ] SubTask A4.1: `form-config.vue` 表单配置列表 + 设计器抽屉
  - [ ] SubTask A4.2: `list-config.vue` 列表配置 + 设计器
  - [ ] SubTask A4.3: `tab-config.vue` 标签页配置 + 设计器
  - [ ] SubTask A4.4: `relation-config.vue` 关联页配置 + 设计器
  - [ ] SubTask A4.5: `template-library.vue` 模板库（CRUD + 复制 + 导入导出 + 实例化按钮）
  - [ ] SubTask A4.6: `runtime-renderer.vue` 路由入口页（`/lowcode/runtime/:bizType/:bizId`）

- [ ] Task A5: 路由与菜单接入
  - [ ] SubTask A5.1: `router/routes.ts` 新增 6 条低代码路由（仅 SUPER_ADMIN）
  - [ ] SubTask A5.2: 菜单「系统管理」下追加「低代码配置」一级菜单（含 5 个子项）
  - [ ] SubTask A5.3: 后端 `sys_menu` 数据迁移：插入低代码菜单项与权限标识 `lowcode:config:*`

- [ ] Task A6: 低代码实现率达标（≥ 60%）
  - [ ] SubTask A6.1: 评估 10 个通用 CRUD 实体改为低代码渲染：客户档案、设备型号、备件、仓库、工程师技能、代理商工程师、验收标准项、通知模板、字典数据、岗位
  - [ ] SubTask A6.2: 为上述 10 个实体创建对应 schema 记录（插入 `lowcode_*_config` 种子数据）
  - [ ] SubTask A6.3: 改造对应菜单项指向 `/lowcode/runtime/:bizType`，验证渲染效果

## B. 引用实体管理界面补全

- [x] Task B1: 仓库档案管理 `system/warehouse.vue`
  - [x] B1.1: 基于 CrudTable + FormModal 实现 CRUD + 库存预警阈值配置
  - [x] B1.2: 路由与菜单接入「设备资产 > 仓库管理」
- [x] Task B2: 备件管理 `device/spare-part.vue`（领用/归还/返修）
  - [x] B2.1: 备件台账 CRUD + 库存展示
  - [x] B2.2: 领用/归还/返修操作弹窗 + `spare_part_log` 记录
- [x] Task B3: 差旅管理 `resource/business-trip.vue`
  - [x] B3.1: 差旅申请 CRUD + 行程信息 + 审批状态
- [x] Task B4: 工程师请假 `resource/leave.vue`
  - [x] B4.1: 请假单 CRUD + 排期自动标记不可分配
- [x] Task B5: 客户档案 `project/customer.vue`
  - [x] B5.1: 客户 CRUD + 联系人子表 + 关联项目列表
- [x] Task B6: 项目模板阶段/任务子管理 `project/template.vue` 扩展
  - [x] B6.1: 在 template.vue 内嵌阶段与任务子表的树形 CRUD
- [x] Task B7: 验收标准检查项管理 `acceptance/standard-item.vue`
  - [x] B7.1: 作为 standard.vue 的子表抽屉，支持检查项 CRUD
- [x] Task B8: 代理商工程师档案 `agent/agent-engineer.vue`
  - [x] B8.1: 作为 profile.vue 的子页或抽屉，支持代理商工程师 CRUD + 启用停用

## C. 业务流程与异常处理完善

- [x] Task C1: 前端 `utils/request.ts` 错误处理增强
  - [x] C1.1: 400xx 错误提取 errors[] 并高亮表单字段
  - [x] C1.2: 网络错误友好提示 + 重试按钮
- [x] Task C2: 后端关键 Entity 追加 `@Version` 乐观锁
  - [x] C2.1: Project / DeviceInstance / OutsourceTask / WorkOrder / AcceptanceTask / FinanceBudget Entity
  - [x] C2.2: Service 并发冲突返回 40904 错误码
- [x] Task C3: 关键操作日志补全
  - [x] C3.1: 状态变更/审批/结算/删除操作通过 SysLogService.record 记录前后值
- [x] Task C4: 状态机校验补全
  - [x] C4.1: Project / Device / OutsourceTask / AcceptanceTask / CutoverPlan / Workload 六类状态机 Service 校验非法流转抛 BusinessException
  - [x] C4.2: 编写 `docs/state-machine.md` 六类状态机转换矩阵

## D. 交付与部署标准完善

- [x] Task D1: 一键部署脚本 `scripts/deploy.ps1`
  - [x] D1.1: 参数 `-Env dev|test|staging|prod -Version <ver>`
  - [x] D1.2: 流程：构建→迁移→部署→健康检查（3 次 /actuator/health）
  - [x] D1.3: 失败自动回滚到上一版本
- [x] Task D2: 回滚脚本 `scripts/rollback.ps1`
  - [x] D2.1: 回滚到指定版本号
- [x] Task D3: 部署文档 `docs/deployment-guide.md`
  - [x] D3.1: 环境要求 / 配置说明 / 部署步骤 / 故障排查
- [x] Task D4: 用户引导系统
  - [x] D4.1: `components/Onboarding/Tutorial.vue` 5 步新手教程
  - [x] D4.2: `components/Onboarding/ContextHelp.vue` 上下文帮助气泡
  - [x] D4.3: `views/help/index.vue` 功能说明文档中心（支持搜索）
  - [x] D4.4: 路由 `/help` 接入，首次登录自动触发教程
- [x] Task D5: 反馈与工单系统
  - [x] D5.1: 后端 `module-system` 新增 `FeedbackController` + `SysFeedbackEntity` + `FeedbackService` + 迁移 SQL
  - [x] D5.2: 前端 `components/Feedback/FeedbackButton.vue` 右下角悬浮按钮
  - [x] D5.3: 前端 `views/system/feedback.vue` 反馈管理列表
  - [x] D5.4: 反馈状态变更通知提交人（站内信）
  - [x] D5.5: 菜单与权限接入

## E. 测试与质量保障

- [ ] Task E1: 前端低代码模块单元测试
  - [ ] E1.1: `api/lowcode.ts` 接口测试
  - [ ] E1.2: `components/Lowcode/*` 组件测试（拖拽/属性编辑/预览渲染）
  - [ ] E1.3: `views/lowcode/*` 视图测试
- [ ] Task E2: 新增实体管理视图测试
  - [ ] E2.1: warehouse / spare-part / business-trip / leave / customer 等视图测试
- [ ] Task E3: 后端 Feedback / 低代码 Service 单测与集成测试
  - [ ] E3.1: FeedbackService JUnit5 单测
  - [ ] E3.2: 低代码 Service 已有，补全 MockMvc 集成测试
- [ ] Task E4: E2E 冒烟脚本扩充
  - [ ] E4.1: 现有 11 条扩充到 20 条（覆盖低代码、反馈、新实体管理）
- [ ] Task E5: 测试覆盖率报告
  - [ ] E5.1: 配置 vitest 覆盖率收集，新增代码覆盖率 ≥ 90%

## F. 文档体系

- [x] Task F1: `docs/requirement-overview.md` 需求总览
- [x] Task F2: `docs/design-architecture.md` 架构设计
- [x] Task F3: `docs/state-machine.md` 状态机转换矩阵（与 C4.2 合并）
- [x] Task F4: `docs/development-guide.md` 开发规范
- [x] Task F5: `docs/test-strategy.md` 测试策略与用例清单
- [x] Task F6: `docs/user-manual.md` 用户手册（按角色分章节）
- [x] Task F7: `docs/api-change-log.md` 本次接口变更清单

# Task Dependencies

- Task A2 / A3 可并行（A2 设计器，A3 渲染器，互不依赖）；A4 依赖 A1+A2+A3。
- Task A5 依赖 A4。
- Task A6 依赖 A3+A5（渲染器就绪 + 路由就绪才能改造实体页）。
- Task B 全部可并行（彼此独立），依赖对应 api 模块已存在。
- Task C1 与 A/B/E 前端任务可并行；C2/C3/C4 后端任务可并行。
- Task D1-D3 部署相关可并行；D4 引导系统与 D5 反馈系统可并行。
- Task E1 依赖 A1-A4 完成；E2 依赖 B 完成；E3 依赖 D5.1 完成。
- Task F 文档可在对应任务完成后撰写，F3 依赖 C4.2。
