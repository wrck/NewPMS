# Checklist — enterprise-completion

> 验收检查清单。每完成一项检查须实际验证（看代码 / 跑测试 / 手动操作），通过后打勾。

## A. 低代码模块

- [ ] `vibe-web/src/api/lowcode.ts` 存在，覆盖 Form/List/Tab/Relation/Template 五类接口
- [ ] `vibe-web/src/types/lowcode.ts` 类型定义完整，与后端 DTO/VO 字段一致
- [ ] `vibe-web/src/components/Lowcode/SchemaDesigner.vue` 三栏布局可拖拽字段
- [ ] `FieldPalette.vue` 至少包含 10 类字段组件
- [ ] `PropertyPanel.vue` 可编辑 label/required/defaultValue/rules/width/placeholder
- [ ] `SchemaPreview.vue` 实时预览与手工开发界面视觉一致
- [ ] `SchemaImporter.vue` 支持 JSON 导入校验与模板选择
- [ ] `RuntimeRenderer.vue` 可渲染 form/list/tab/relation 四类 schema
- [ ] `views/lowcode/form-config.vue` 实现列表 + 设计器抽屉 + CRUD + 复制 + 导入导出 + 实例化
- [ ] `views/lowcode/list-config.vue` 实现配置 + 设计器
- [ ] `views/lowcode/tab-config.vue` 实现配置 + 设计器
- [ ] `views/lowcode/relation-config.vue` 实现配置 + 设计器
- [ ] `views/lowcode/template-library.vue` 实现模板库 CRUD + 复制 + 导入导出 + 实例化
- [ ] `views/lowcode/runtime-renderer.vue` 路由入口页可访问
- [ ] `router/routes.ts` 新增 6 条低代码路由，仅 SUPER_ADMIN 可访问
- [ ] 菜单「系统管理 > 低代码配置」可见且包含 5 个子项
- [ ] `sys_menu` 迁移插入低代码菜单项与权限标识 `lowcode:config:*`
- [ ] 至少 10 个通用实体改为低代码渲染，低代码实现率 ≥ 60%
- [ ] 低代码渲染页面与手工开发页面视觉/交互一致（状态色 Tag / 分页 / 操作列 / 筛选）

## B. 引用实体管理

- [ ] `system/warehouse.vue` 仓库档案 CRUD + 库存预警阈值
- [ ] `device/spare-part.vue` 备件台账 + 领用/归还/返修 + 日志记录
- [ ] `resource/business-trip.vue` 差旅申请 CRUD + 行程 + 审批状态
- [ ] `resource/leave.vue` 请假单 CRUD + 排期联动
- [ ] `project/customer.vue` 客户档案 CRUD + 联系人子表 + 关联项目
- [ ] `project/template.vue` 内嵌阶段/任务子表树形 CRUD
- [ ] `acceptance/standard.vue` 内嵌检查项子表抽屉
- [ ] `agent/profile.vue` 内嵌代理商工程师档案
- [ ] 上述视图均使用 CrudTable + FormModal 通用组件，视觉一致

## C. 业务流程与异常处理

- [ ] 前端 `request.ts` 对 400xx 错误提取 `errors[]` 并高亮表单字段
- [ ] 403xx 提示「权限不足」；409xx 提示业务冲突原因
- [ ] 网络错误显示友好提示 + 重试按钮
- [ ] Project/DeviceInstance/OutsourceTask/WorkOrder/AcceptanceTask/FinanceBudget Entity 含 `@Version`
- [ ] 并发冲突返回 40904 + 「数据已被他人修改，请刷新后重试」
- [ ] 状态变更/审批/结算/删除操作均记录 SysLog（含前后值）
- [ ] 六类状态机 Service 校验非法流转抛 BusinessException
- [ ] `docs/state-machine.md` 包含六类状态机转换矩阵

## D. 交付与部署

- [ ] `scripts/deploy.ps1` 支持 `-Env -Version` 参数
- [ ] 部署脚本包含构建→迁移→部署→健康检查（3 次）流程
- [ ] 部署失败自动回滚
- [ ] `scripts/rollback.ps1` 支持回滚到指定版本
- [ ] `docs/deployment-guide.md` 包含环境要求/配置/步骤/故障排查
- [ ] `components/Onboarding/Tutorial.vue` 5 步新手教程
- [ ] `components/Onboarding/ContextHelp.vue` 上下文帮助气泡
- [ ] `views/help/index.vue` 文档中心支持搜索
- [ ] 路由 `/help` 可访问，首次登录自动触发教程
- [ ] 后端 `FeedbackController` + `SysFeedbackEntity` + `FeedbackService` 存在
- [ ] `sys_feedback` 迁移 SQL 已执行
- [ ] `components/Feedback/FeedbackButton.vue` 右下角悬浮按钮可用
- [ ] `views/system/feedback.vue` 反馈管理列表
- [ ] 反馈状态变更通知提交人
- [ ] 反馈菜单与权限接入

## E. 测试与质量

- [ ] 前端低代码 API 单测通过
- [ ] 前端低代码组件测试通过（SchemaDesigner/FieldPalette/PropertyPanel/SchemaPreview/SchemaImporter/RuntimeRenderer）
- [ ] 前端低代码视图测试通过
- [ ] 新增实体管理视图测试通过
- [ ] 后端 FeedbackService 单测通过
- [ ] 后端低代码 Service MockMvc 集成测试通过
- [ ] E2E 冒烟脚本扩充至 20 条且全部通过
- [ ] 新增前端代码行覆盖率 ≥ 90%（vitest 覆盖率报告）

## F. 文档体系

- [ ] `docs/requirement-overview.md` 存在且内容完整
- [ ] `docs/design-architecture.md` 存在且内容完整
- [ ] `docs/state-machine.md` 包含六类状态机
- [ ] `docs/development-guide.md` 包含命名/分层/异常/日志/缓存规范
- [ ] `docs/test-strategy.md` 包含测试策略与用例清单
- [ ] `docs/user-manual.md` 按角色分章节（管理员/PM/工程师/代理商/客户）
- [ ] `docs/api-change-log.md` 列出本次新增/变更接口

## 全局验证

- [ ] `npm run typecheck` 通过
- [ ] `npm run build` 通过
- [ ] 后端 `mvn clean install` 通过
- [ ] 后端 `mvn spring-boot:run`（dev profile）启动无报错
- [ ] 前端 `npm run dev` 启动，首页与新增低代码页可访问
- [ ] 所有 git 提交使用约定式提交格式
