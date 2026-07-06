# 系统模块全面补全 — Spec

> change-id：`complete-system-modules`
> 来源：用户对 `系统设计文档.md` 的全面深入分析与系统性补全要求
> 前序 spec：`implement-mvp-phase1`（MVP Phase 1，已完成主链路骨架）

## Why

`implement-mvp-phase1` 完成 MVP 主链路（410 REST 端点 / 63 页面 / 316 API 函数），但对照《系统设计文档.md》与行业领先水平，仍存在显著短板：

1. **可视化能力零落地**：`echarts`、`dhtmlx-gantt` 已声明依赖但全项目零 import；高德地图未安装；驾驶舱与报表页用 `a-table` + 文本列表呈现"图表"。
2. **设计文档明确要求未达成**：项目列表"列表/看板/甘特图/地图视图切换"只完成 1/4（仅列表，看板占位，甘特图与地图完全缺失）。
3. **后端关键能力短板**：无工作流引擎（Flowable）、无定时任务框架（XXL-JOB）、无搜索引擎（ES），导致复杂审批/预警/超时/检索场景无法支撑。
4. **低代码能力完全缺失**：用户要求开发表单/列表/标签页/关联页的可视化配置功能，目前全项目零相关代码。
5. **交付与部署体系薄弱**：仅有 docker-compose 占位，缺少 CI/CD 流水线、生产部署文档、用户引导系统、技术支持机制。
6. **响应式覆盖不足**：仅 6/63 页面有媒体查询；H5 页面存在硬编码颜色未走 less 变量。
7. **已知 Bug 与死代码**：`dashboard/my-tasks.vue` 取 `res.myTasks` 字段不存在；`project/detail.vue` 新增任务按钮无 `@click` 绑定；`_placeholder/index.vue` 未被路由引用。

本 spec 聚焦 **系统性补全**，覆盖用户提出的 5 大维度（功能完善度 / 界面与体验 / 系统架构与可扩展性 / 交付部署 / 低代码模块），将系统从"MVP 可用"提升至"行业领先水平"。

## What Changes

### 一、后端能力补全（MODIFIED + ADDED）

- **ADDED** 集成 Flowable 7 工作流引擎，支撑验收、割接、外包、项目变更等复杂审批流（含节点回退、会签、催办、撤回）
- **ADDED** 集成 XXL-JOB 2.4 定时任务框架，落地库存预警、备件补货、工单超时升级、验收逾期提醒、对账类业务（替代原 spec 中的"定时任务占位"）
- **ADDED** 集成 ElasticSearch 8.x，覆盖项目/设备/工单全文检索与多维聚合查询，加速 module-report 跨模块聚合
- **ADDED** 推广 EasyExcel 至财务预算/成本、项目任务清单、资源工时、代理商工作量等高频导出场景
- **ADDED** module-integration 实际对接 Adapter（ERP 客户主数据同步、IM 系统通知转发、物流状态拉取、OA 审批联动）
- **MODIFIED** module-auth 重构：拆分独立 SysUserMapper 复用为 AuthUser聚合根，支持多类型用户（内部/代理商/客户）独立认证体系
- **MODIFIED** module-collaboration 增加独立持久层（客户偏好、订阅关系、客户会话表）
- **MODIFIED** module-report 拆分 ReportMapper 为按业务域的多个 Mapper（ProjectReportMapper / DeviceReportMapper / ResourceReportMapper / FinanceReportMapper）
- **ADDED** 领域事件总线（Spring ApplicationEvents + RabbitMQ 投递），统一模块间通信规范
- **ADDED** 全局异常处理增强：业务异常分类（业务/权限/数据/外部/系统）、错误恢复机制（重试/降级/熔断）、链路追踪（TraceId）

### 二、前端可视化与体验补全（MODIFIED + ADDED）

- **ADDED** ECharts 5 通用图表包装组件（`@/components/charts/`），覆盖饼图/折线/柱状/堆叠/漏斗/地图/雷达/仪表盘 8 类
- **ADDED** dhtmlx-gantt 甘特图组件（`@/components/Gantt/`），落地项目甘特图与排期甘特图
- **ADDED** 高德地图组件（`@/components/AMap/`），落地项目列表地图视图与设备分布地图
- **ADDED** 数据大屏页面（`/views/bigscreen/`），运营指挥中心
- **ADDED** 项目列表 4 视图切换（列表/看板/甘特图/地图），完成设计文档 3.3.2 明确要求
- **MODIFIED** `report/cockpit.vue` 重写为 ECharts 图表化呈现（饼图 + 折线 + 风险列表 + KPI 卡片）
- **MODIFIED** `report/project.vue` / `device.vue` / `resource.vue` / `finance.vue` 4 个报表页接入 ECharts + 实现导出
- **MODIFIED** `dashboard/my-tasks.vue` 修复字段不匹配 Bug（改为按角色取 `dashboard.pm.myProjects` 或 `dashboard.engineer.todayTasks`）
- **MODIFIED** `project/detail.vue` 任务 Tab"新增任务"按钮绑定弹窗
- **MODIFIED** 全项目响应式断点覆盖（PC≥1280px / 平板 768-1279px / 手机<768px），新增约 57 个页面的媒体查询
- **MODIFIED** H5 页面硬编码颜色替换为 less 变量，统一主题切换
- **ADDED** 通用可复用组件抽取：CRUD 表格（CrudTable）、表单弹窗（FormModal）、文件上传（FileUpload）、富文本（RichEditor）、组织树（OrgTree）、导入导出按钮（ImportExport）
- **MODIFIED** 品牌设计系统统一：扩展 less 变量（间距 8px 网格、字号、圆角、阴影、动画曲线），落地到全部页面

### 三、低代码模块实现（ADDED）

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

### 四、交付与部署标准（ADDED + MODIFIED）

- **ADDED** 部署流程文档：
  - `docs/deployment/dev-deploy.md`：开发环境一键启动（docker-compose）
  - `docs/deployment/test-deploy.md`：测试环境 Docker Compose 部署
  - `docs/deployment/prod-deploy.md`：生产环境部署（Nginx + Spring Boot 集群 + MySQL 主从 + Redis Sentinel + ES + MinIO + RabbitMQ + XXL-JOB + Flowable）
  - `docs/deployment/rollback.md`：回滚流程
  - `docs/deployment/monitoring.md`：监控告警配置（Prometheus + Grafana）
- **ADDED** 自动化部署脚本：
  - `scripts/deploy/dev-up.sh` / `dev-down.sh`：开发环境启停
  - `scripts/deploy/test-deploy.sh`：测试环境部署
  - `scripts/deploy/prod-deploy.sh`：生产环境蓝绿部署
  - `scripts/deploy/db-migrate.sh`：数据库迁移（Flyway）
  - `scripts/deploy/backup-restore.sh`：备份恢复
- **ADDED** CI/CD 流水线：
  - `.gitlab-ci.yml` 或 `Jenkinsfile`：构建 → 单测 → 镜像推送 → 部署 → 烟雾测试
  - GitHub Actions workflow（如使用 GitHub）
- **ADDED** 用户引导系统：
  - 首次登录引导浮层（功能概览 + 角色化教程卡片）
  - 角色化新手教程（总监/PM/工程师/代理商/客户各 1 套）
  - 页面级功能说明（HelpDrawer 抽屉，含操作步骤 + 截图）
  - 内置视频教程占位（后续补充）
- **ADDED** 技术支持体系：
  - `docs/support/faq.md`：常见问题
  - `docs/support/troubleshooting.md`：故障排查指南
  - 系统内问题反馈入口（FeedbackDrawer，提交到 GitLab Issue / 飞书工单）
  - `docs/api/knife4j-export.yaml`：OpenAPI 3 规范导出

### 五、测试与验证（ADDED）

- **ADDED** 后端单元测试覆盖率提升至 ≥80%（关键业务状态机、权限拦截器、低代码 Schema 校验）
- **ADDED** 后端集成测试：Flowable 流程引擎、XXL-JOB 调度、ES 检索、数据权限隔离、低代码 CRUD
- **ADDED** 前端组件测试（Vitest）：通用组件（CrudTable/FormModal/Charts/Gantt/AMap）、低代码渲染器
- **ADDED** 前端 E2E 测试（Playwright）：登录 → 项目立项 → 4 视图切换 → 派单 → 移动端签到 → 代理商交付 → 验收签核 → 低代码配置 → 数据大屏
- **ADDED** 性能测试脚本（k6/JMeter）：核心接口压测，给出 P95 延迟基线
- **ADDED** 异常处理专项测试：网络超时、并发冲突、数据校验失败、权限拒绝、外部依赖不可用

## Impact

### 受影响规格
- 前序 spec `implement-mvp-phase1`：本 spec 在其之上扩展，不修改已交付的核心业务逻辑
- 新增独立 spec delta：`complete-system-modules`（本 spec）

### 受影响代码

**后端（vibe-server）**
- 新建模块：`module-lowcode`
- 修改模块：`module-auth`、`module-collaboration`、`module-report`、`module-integration`、`module-common`、`module-system`、`module-project`、`module-device`、`module-resource`、`module-delivery`、`module-agent`、`module-acceptance`、`module-finance`
- 新增基础设施依赖：Flowable 7、XXL-JOB 2.4、ElasticSearch 8.x、Spring Cloud OpenFeign（对接外部系统）
- 配置文件：`application.yml`（新增 flowable/xxl-job/es/openfeign 配置块）

**前端（vibe-web）**
- 新建目录：`src/views/lowcode/`、`src/views/bigscreen/`、`src/components/charts/`、`src/components/Gantt/`、`src/components/AMap/`、`src/components/CrudTable/`、`src/components/FormModal/`、`src/components/FileUpload/`、`src/components/RichEditor/`、`src/components/OrgTree/`
- 修改页面：`report/*`（5 个）、`project/list.vue`、`project/detail.vue`、`dashboard/my-tasks.vue`、约 57 个页面响应式适配、H5 页面 less 变量替换
- 新增依赖：`@amap/amap-jsapi-loader`、`vue-echarts`（可选封装）
- 路由配置：`router/routes.ts` 新增低代码、数据大屏、菜单管理等路由

**部署**
- 新增：`docs/deployment/`、`scripts/deploy/`、`.gitlab-ci.yml` 或 `Jenkinsfile`、`docs/support/`
- 修改：`docker-compose.yml`（新增 es/xxl-job-admin/flowable-rest 服务）

**数据库**
- 新增表：`lowcode_form_config` / `lowcode_list_config` / `lowcode_tab_config` / `lowcode_relation_config` / `lowcode_template`、`flowable_*`（Flowable 自带 38 张表）、`xxl_job_*`（XXL-JOB 自带 16 张表）、`customer_preference`、`customer_subscription`、`customer_session`
- 索引：ES 索引 `vibe_project` / `vibe_device` / `vibe_work_order`

## ADDED Requirements

### Requirement: 工作流引擎集成
系统 SHALL 集成 Flowable 7 工作流引擎，支撑验收、割接、外包、项目变更等复杂审批流，支持流程定义、节点回退、会签、催办、撤回。

#### Scenario: 验收审批流会签
- **WHEN** 验收任务到达"会签节点"（多部门并行审批）
- **THEN** 系统并行触发 3 个审批人（PM/总监/客户代表），全部通过后流转到下一节点；任一拒绝则回退到发起人

#### Scenario: 流程节点回退
- **WHEN** 总监审批节点选择"回退到上一节点"
- **THEN** 流程回到 PM 审批节点，PM 可重新审批或修改后再次提交，记录回退历史

### Requirement: 定时任务调度
系统 SHALL 集成 XXL-JOB 2.4 调度框架，落地库存预警、备件补货提醒、工单超时升级、验收逾期提醒、对账类定时业务。

#### Scenario: 库存预警定时扫描
- **WHEN** XXL-JOB 每日 09:00 触发库存预警任务
- **THEN** 系统扫描所有仓库所有型号库存，低于安全库存的生成预警记录并通知设备管理员，避免漏告警

#### Scenario: 工单超时升级
- **WHEN** 工单计划完成时间已过且状态未完成
- **THEN** 系统每小时扫描，超期 24h 内通知工程师，超期 48h 升级通知 PM，超期 72h 升级通知总监

### Requirement: 全文检索与多维聚合
系统 SHALL 集成 ElasticSearch 8.x，覆盖项目、设备、工单的全文检索与多维聚合查询，加速报表跨模块聚合。

#### Scenario: 项目全文检索
- **WHEN** 用户在项目列表搜索框输入"北京 银行 路由器"
- **THEN** 系统通过 ES 检索项目名称/客户名称/产品线/区域/备注字段，返回匹配项目列表，P95 延迟 < 200ms

#### Scenario: 多维聚合查询
- **WHEN** 报表页请求近 12 月项目趋势（按月分组、按状态分桶）
- **THEN** 系统通过 ES 聚合查询返回结果，避免 MySQL 全表扫描

### Requirement: ECharts 图表化
系统 SHALL 在驾驶舱、报表页、数据大屏全面使用 ECharts 5 呈现图表，至少覆盖饼图、折线、柱状、堆叠、漏斗、地图、雷达、仪表盘 8 类。

#### Scenario: 驾驶舱图表呈现
- **WHEN** 总监登录访问管理驾驶舱
- **THEN** 页面展示：4 张 KPI 卡片（含环比）、项目阶段分布饼图、近 12 月趋势堆叠折线图、风险项目列表、设备状态仪表盘、区域分布地图，全部基于 ECharts 渲染

### Requirement: 甘特图与地图视图
系统 SHALL 在项目列表与排期日历使用 dhtmlx-gantt 渲染甘特图，在项目列表提供高德地图视图，完成设计文档 3.3.2 要求的"列表/看板/甘特图/地图"4 视图切换。

#### Scenario: 项目甘特图拖拽排期
- **WHEN** PM 在项目甘特图视图拖拽任务条调整时间
- **THEN** 系统更新任务计划开始/结束日期，校验依赖关系，冲突时阻止保存并提示

#### Scenario: 项目地图视图
- **WHEN** 总监切换到项目地图视图
- **THEN** 系统基于高德地图渲染项目地理分布（按客户所在地），气泡大小代表项目规模，颜色代表状态，点击气泡弹出项目卡片

### Requirement: 低代码配置与运行时
系统 SHALL 提供低代码配置能力，至少覆盖表单、列表、标签页、关联页 4 类基础组件，支持可视化配置、Schema 持久化、模板导入导出与复用，并提供运行时引擎动态渲染。

#### Scenario: 表单可视化配置
- **WHEN** 管理员在低代码表单配置器拖拽 5 个字段（输入框/下拉/日期/开关/单选）并保存
- **THEN** 系统持久化 Schema 到 `lowcode_form_config` 表，预览渲染出对应表单，字段属性（label/required/placeholder/校验规则）全部生效

#### Scenario: 列表配置导入导出
- **WHEN** 管理员在列表配置器点击"导出"
- **THEN** 系统下载 JSON Schema 文件；导入时校验 JSON Schema Draft 7 规范，校验失败给出明确错误提示

#### Scenario: 模板复用
- **WHEN** 管理员基于模板"设备巡检表单"实例化新业务表单
- **THEN** 系统复制模板 Schema 到新配置，可独立修改不影响原模板

### Requirement: 数据大屏
系统 SHALL 提供运营指挥中心数据大屏页面，全屏展示核心 KPI、实时项目分布地图、设备状态仪表盘、近期待办滚动、风险预警滚动。

#### Scenario: 大屏全屏轮播
- **WHEN** 总监进入数据大屏页面
- **THEN** 系统全屏展示 6 个图表区块，每 30s 自动轮播高亮区块，支持手动切换，数据每 5 分钟自动刷新

### Requirement: 通用组件抽取
系统 SHALL 抽取通用可复用组件：CrudTable、FormModal、FileUpload、RichEditor、OrgTree、ImportExport、Charts（ECharts 包装）、Gantt（dhtmlx 包装）、AMap（高德包装），并在业务页面推广使用。

#### Scenario: CrudTable 替换
- **WHEN** 开发者在用户管理页使用 CrudTable 组件替换原有 a-table + a-modal 拼装代码
- **THEN** CRUD 表格自动支持搜索表单、分页、新增/编辑/删除弹窗、表单验证、状态反馈、异常处理，代码行数减少 60% 以上

### Requirement: 响应式断点全面覆盖
系统 SHALL 对全部页面应用响应式断点（PC≥1280px / 平板 768-1279px / 手机<768px），PC 为主、移动端针对核心场景适配。

#### Scenario: 平板访问项目列表
- **WHEN** 用户使用 iPad（1024px 宽）访问项目列表
- **THEN** 页面调整为单列布局，搜索表单折叠为抽屉，表格列优先级排序（隐藏低优先级列），操作按钮折叠到下拉菜单

### Requirement: 用户引导系统
系统 SHALL 提供首次登录引导浮层、角色化新手教程、页面级功能说明抽屉，最小化用户使用门槛。

#### Scenario: 首次登录引导
- **WHEN** 新用户首次登录系统
- **THEN** 系统弹出引导浮层，展示功能概览 + 角色化教程卡片（5 张：总监/PM/工程师/代理商/客户），用户点击对应卡片进入分步教程

### Requirement: CI/CD 流水线
系统 SHALL 提供 GitLab CI 或 Jenkins 流水线配置，覆盖构建 → 单测 → 镜像推送 → 部署 → 烟雾测试全流程。

#### Scenario: 流水线自动触发
- **WHEN** 开发者 push 代码到 main 分支
- **THEN** 流水线自动触发：Maven 构建 → 单测覆盖率检查（≥80%）→ Docker 镜像构建并推送到 Harbor → 部署到测试环境 → 烟雾测试（5 个核心接口健康检查）→ 失败则回滚并通知

### Requirement: 部署文档与脚本
系统 SHALL 提供开发/测试/生产三套部署文档与配套自动化脚本，覆盖一键启动、数据库迁移、备份恢复、监控告警。

#### Scenario: 生产环境一键部署
- **WHEN** 运维人员执行 `scripts/deploy/prod-deploy.sh`
- **THEN** 系统依次执行：环境变量加载 → 数据库 Flyway 迁移 → ES 索引初始化 → Flowable 表初始化 → 镜像拉取 → 滚动重启服务 → 健康检查 → 流量切换

### Requirement: 异常处理增强
系统 SHALL 建立分级异常处理策略：业务异常（4xx）、权限异常（403）、数据异常（422）、外部依赖异常（502）、系统异常（500），并支持错误恢复（重试/降级/熔断）与链路追踪（TraceId 贯穿日志）。

#### Scenario: 外部依赖熔断降级
- **WHEN** 调用 ERP 同步客户主数据连续失败 5 次
- **THEN** 系统触发熔断，10 分钟内直接返回降级数据（缓存），10 分钟后半开试探，成功则恢复

#### Scenario: 链路追踪
- **WHEN** 接口请求处理过程中产生 3 条日志（Controller/Service/Mapper）
- **THEN** 三条日志均含相同 TraceId，可通过 TraceId 在 ELK 中检索完整调用链

## MODIFIED Requirements

### Requirement: module-auth 多类型用户认证
**原**：仅支持内部用户账号密码登录。
**改**：支持内部用户、代理商、客户三类独立认证体系，代理商门户与客户门户可独立配置 Token 有效期与权限边界。

### Requirement: module-report 报表性能
**原**：单一 ReportMapper 承担全部跨模块聚合查询。
**改**：按业务域拆分为 4 个 Mapper（ProjectReportMapper / DeviceReportMapper / ResourceReportMapper / FinanceReportMapper），高并发聚合查询走 ES，明细查询走 MySQL。

### Requirement: module-integration 真实对接
**原**：仅提供配置 CRUD 与调用日志查询。
**改**：实现 ERP 客户主数据同步、IM 系统通知转发、物流状态拉取、OA 审批联动 4 个 Adapter。

### Requirement: 前端响应式覆盖
**原**：仅 6/63 页面有媒体查询。
**改**：全部 63 个页面应用统一断点（PC≥1280px / 平板 768-1279px / 手机<768px），H5 页面替换硬编码颜色为 less 变量。

### Requirement: 前端通用组件
**原**：仅 5 个通用组件（PageContainer/StatisticCard/ProgressBar/StatusTag/EmptyState）。
**改**：新增 9 个通用组件（CrudTable/FormModal/FileUpload/RichEditor/OrgTree/ImportExport/Charts/Gantt/AMap），并推广至业务页面。

## REMOVED Requirements

### Requirement: `_placeholder/index.vue` 死代码
**Reason**：通用占位页未被 `routes.ts` 引用，属于遗留死代码。
**Migration**：直接删除文件，无影响。
