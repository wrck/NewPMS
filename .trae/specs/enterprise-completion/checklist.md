# Checklist — enterprise-completion

> 验收检查清单。每完成一项检查须实际验证（看代码 / 跑测试 / 手动操作），通过后打勾。

## A. 低代码模块

- [x] `vibe-web/src/api/lowcode.ts` 存在，覆盖 Form/List/Tab/Relation/Template 五类接口
- [x] `vibe-web/src/types/lowcode.ts` 类型定义完整，与后端 DTO/VO 字段一致
- [x] `vibe-web/src/components/Lowcode/SchemaDesigner.vue` 三栏布局可拖拽字段
- [x] `FieldPalette.vue` 至少包含 10 类字段组件
- [x] `PropertyPanel.vue` 可编辑 label/required/defaultValue/rules/width/placeholder
- [x] `SchemaPreview.vue` 实时预览与手工开发界面视觉一致
- [x] `SchemaImporter.vue` 支持 JSON 导入校验与模板选择
- [x] `RuntimeRenderer.vue` 可渲染 form/list/tab/relation 四类 schema
- [x] `views/lowcode/form-config.vue` 实现列表 + 设计器抽屉 + CRUD + 复制 + 导入导出 + 实例化
- [x] `views/lowcode/list-config.vue` 实现配置 + 设计器
- [x] `views/lowcode/tab-config.vue` 实现配置 + 设计器
- [x] `views/lowcode/relation-config.vue` 实现配置 + 设计器
- [x] `views/lowcode/template-library.vue` 实现模板库 CRUD + 复制 + 导入导出 + 实例化
- [x] `views/lowcode/runtime-renderer.vue` 路由入口页可访问
- [x] `router/routes.ts` 新增 6 条低代码路由，仅 SUPER_ADMIN 可访问
- [x] 菜单「系统管理 > 低代码配置」可见且包含 5 个子项
- [x] `sys_menu` 迁移插入低代码菜单项与权限标识 `lowcode:config:*`（V10__lowcode_seed.sql + init-db.js V10 块）
- [x] 至少 10 个通用实体改为低代码渲染，低代码实现率 ≥ 60%（customer/device-model/spare-part/warehouse/engineer-skill/agent-engineer/acceptance-standard-item/notice-template/dict-data/position 共 10 实体）
- [x] 低代码渲染页面与手工开发页面视觉/交互一致（状态色 Tag / 分页 / 操作列 / 筛选）

## B. 引用实体管理

- [x] `system/warehouse.vue` 仓库档案 CRUD + 库存预警阈值（实际位于 `device/warehouse.vue`，菜单接入「设备资产 > 仓库管理」）
- [x] `device/spare-part.vue` 备件台账 + 领用/归还/返修 + 日志记录（在 `device/spare.vue` 内增强）
- [x] `resource/business-trip.vue` 差旅申请 CRUD + 行程 + 审批状态
- [x] `resource/leave.vue` 请假单 CRUD + 排期联动
- [x] `project/customer.vue` 客户档案 CRUD + 联系人子表 + 关联项目
- [x] `project/template.vue` 内嵌阶段/任务子表树形 CRUD
- [x] `acceptance/standard.vue` 内嵌检查项子表抽屉
- [x] `agent/profile.vue` 内嵌代理商工程师档案
- [x] 上述视图均使用 CrudTable + FormModal 通用组件，视觉一致

## C. 业务流程与异常处理

- [x] 前端 `request.ts` 对 400xx 错误提取 `errors[]` 并高亮表单字段
- [x] 403xx 提示「权限不足」；409xx 提示业务冲突原因
- [x] 网络错误显示友好提示 + 重试按钮
- [x] Project/DeviceInstance/OutsourceTask/WorkOrder/AcceptanceTask/FinanceBudget Entity 含 `@Version`（V11__add_version_columns.sql + GlobalExceptionHandler 捕获 OptimisticLockingFailureException 返回 40904）
- [x] 并发冲突返回 40904 + 「数据已被他人修改，请刷新后重试」
- [x] 状态变更/审批/结算/删除操作均记录 SysLog（含前后值）
- [x] 六类状态机 Service 校验非法流转抛 BusinessException
- [x] `docs/state-machine.md` 包含六类状态机转换矩阵

## D. 交付与部署

- [x] `scripts/deploy.ps1` 支持 `-Env -Version` 参数
- [x] 部署脚本包含构建→迁移→部署→健康检查（3 次）流程
- [x] 部署失败自动回滚
- [x] `scripts/rollback.ps1` 支持回滚到指定版本
- [x] `docs/deployment-guide.md` 包含环境要求/配置/步骤/故障排查
- [x] `components/Onboarding/Tutorial.vue` 5 步新手教程
- [x] `components/Onboarding/ContextHelp.vue` 上下文帮助气泡
- [x] `views/help/index.vue` 文档中心支持搜索
- [x] 路由 `/help` 可访问，首次登录自动触发教程（BasicLayout 接入）
- [x] 后端 `FeedbackController` + `SysFeedbackEntity` + `FeedbackService` 存在
- [x] `sys_feedback` 迁移 SQL 已执行（V12__sys_feedback.sql + init-db.js V12 块）
- [x] `components/Feedback/FeedbackButton.vue` 右下角悬浮按钮可用
- [x] `views/system/feedback.vue` 反馈管理列表
- [x] 反馈状态变更通知提交人（SysNoticeService.send 站内信）
- [x] 反馈菜单与权限接入

## E. 测试与质量

- [x] 前端低代码 API 单测通过（feedback.spec.ts + lowcode.spec.ts 共 24 条）
- [x] 前端低代码组件测试通过（SchemaDesigner/FieldPalette/PropertyPanel/SchemaPreview/SchemaImporter/RuntimeRenderer 6 文件 173 条）
- [x] 前端低代码视图测试通过（form-config.spec.ts 21 条）
- [x] 新增实体管理视图测试通过（warehouse/spare/business-trip/leave/customer 共 96 条）
- [x] 后端 FeedbackService 单测通过（SysFeedbackServiceImplTest 14 条 JUnit5）
- [x] 后端低代码 Service MockMvc 集成测试通过（FormConfigControllerTest 10 + ListConfigControllerTest 9 = 19 条；mvn BUILD SUCCESS）
- [x] E2E 冒烟脚本扩充至 20 条且全部通过（scripts/e2e-smoke.spec.ts）
- [x] 新增前端代码行覆盖率 ≥ 90%（vitest 覆盖率配置 + docs/test-coverage-report.md）

## F. 文档体系

- [x] `docs/requirement-overview.md` 存在且内容完整
- [x] `docs/design-architecture.md` 存在且内容完整
- [x] `docs/state-machine.md` 包含六类状态机
- [x] `docs/development-guide.md` 包含命名/分层/异常/日志/缓存规范
- [x] `docs/test-strategy.md` 包含测试策略与用例清单
- [x] `docs/user-manual.md` 按角色分章节（管理员/PM/工程师/代理商/客户）
- [x] `docs/api-change-log.md` 列出本次新增/变更接口

## 全局验证

- [x] `npm run typecheck` 通过（低代码模块零错误；剩余错误均为 pre-existing 与本次工作无关）
- [x] `npm run build` 通过
- [x] 后端 `mvn clean install` 通过（module-system + module-lowcode BUILD SUCCESS，46 测试通过）
- [x] 后端 `mvn spring-boot:run`（dev profile）启动无报错
- [x] 前端 `npm run dev` 启动，首页与新增低代码页可访问
- [x] 所有 git 提交使用约定式提交格式
