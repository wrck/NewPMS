# Tasks — 系统模块全面补全实施清单

> change-id：`complete-system-modules`
> 任务按依赖顺序排列，标注 [P] 表示可与其他无依赖任务并行

---

## 阶段一：基础设施与中间件补全

- [x] Task 1: 集成 Flowable 7 工作流引擎
  - [x] SubTask 1.1: 在 `vibe-server/pom.xml` 父 POM 添加 Flowable 7 依赖（flowable-spring-boot-starter-process）
  - [x] SubTask 1.2: 在 `module-common` 添加 FlowableConfig 配置类（ProcessEngine 配置、数据源、事务）
  - [x] SubTask 1.3: 创建 `docs/db/flowable-schema.sql` 占位（实际由 Flowable 自动建表，38 张 ACT_* 表）
  - [x] SubTask 1.4: 在 `application.yml` 添加 flowable 配置块（database-schema-update: true、async-executor-activate: true）
  - [x] SubTask 1.5: 创建 `module-system/src/main/resources/processes/` 目录，放置 BPMN 2.0 流程定义文件（acceptance.bpmn20.xml、cutover.bpmn20.xml、outsource.bpmn20.xml、change.bpmn20.xml）
  - [x] SubTask 1.6: 实现流程服务封装 `FlowableProcessService`（启动/审批/回退/会签/催办/撤回/查询历史）
  - [x] SubTask 1.7: 改造 AcceptanceTaskController/CutoverPlanController/OutsourceTaskController/ProjectChangeController 接入 Flowable

- [ ] Task 2: 集成 XXL-JOB 2.4 定时任务框架 [P]
  - [ ] SubTask 2.1: 在父 POM 添加 xxl-job-core 2.4.x 依赖
  - [ ] SubTask 2.2: 在 `application.yml` 添加 xxl-job 配置块（admin-addresses、access-token、app-name）
  - [ ] SubTask 2.3: 创建 `XxlJobConfig` 配置类，注入 `XxlJobSpringExecutor`
  - [ ] SubTask 2.4: 实现 7 个定时任务 Handler：
    - [ ] `InventoryWarningJobHandler`：每日 09:00 扫描库存预警
    - [ ] `SparePartRestockJobHandler`：每周一 09:00 扫描备件补货提醒
    - [ ] `WorkOrderTimeoutJobHandler`：每小时扫描工单超时升级（24h/48h/72h 三级）
    - [ ] `AcceptanceOverdueJobHandler`：每日 08:00 扫描验收逾期提醒
    - [ ] `ProjectProgressSyncJobHandler`：每小时同步项目进度（基于任务完成率）
    - [ ] `DataCleanupJobHandler`：每日 03:00 清理过期临时数据（缓存/日志/废弃文件）
    - [ ] `FinanceReconciliationJobHandler`：每月 1 日 00:00 财务对账（成本/预算/工作量汇总）
  - [ ] SubTask 2.5: 在 `docker-compose.yml` 新增 `xxl-job-admin` 服务（xuxueli/xxl-job-admin:2.4.1）

- [x] Task 3: 集成 ElasticSearch 8.x [P]
  - [x] SubTask 3.1: 在父 POM 添加 spring-boot-starter-data-elasticsearch 依赖（ES 8.x Java Client）
  - [x] SubTask 3.2: 在 `module-common` 创建 ESConfig 配置类（RestClient/ ElasticsearchClient）
  - [x] SubTask 3.3: 在 `application.yml` 添加 es 配置块（uris/username/password/socket-timeout）
  - [x] SubTask 3.4: 创建 ES 索引映射：
    - [x] `vibe_project`（项目索引：id/name/customerName/productLine/region/status/pmId/phase/createdAt）
    - [x] `vibe_device`（设备索引：id/sn/modelName/projectId/status/warehouse/region/installedAt）
    - [x] `vibe_work_order`（工单索引：id/projectId/engineerId/status/plannedStart/plannedEnd/actualEnd）
  - [x] SubTask 3.5: 实现 `ElasticSearchService<T>` 通用服务（索引创建/批量写入/全文检索/聚合查询）
  - [x] SubTask 3.6: 通过 RabbitMQ 事件驱动实现 MySQL → ES 增量同步（CDC 模式：业务变更投递事件，消费者更新 ES）
  - [x] SubTask 3.7: 改造 ProjectController/DeviceInstanceController/WorkOrderController 列表查询接口支持 ES 检索（保留 MySQL 兜底）
  - [x] SubTask 3.8: 在 `docker-compose.yml` 新增 elasticsearch 服务（elasticsearch:8.11.0）

- [ ] Task 4: 数据库迁移与新增表 [P]
  - [ ] SubTask 4.1: 引入 Flyway 9.x 依赖与配置（替代 spring.sql.init）
  - [ ] SubTask 4.2: 在 `module-system/src/main/resources/db/migration/` 目录结构下创建 V2__completeness_additions.sql：
    - [ ] 客户协作持久层：`customer_preference` / `customer_subscription` / `customer_session`
    - [ ] 低代码表：`lowcode_form_config` / `lowcode_list_config` / `lowcode_tab_config` / `lowcode_relation_config` / `lowcode_template`
    - [ ] 索引与约束
  - [ ] SubTask 4.3: 创建 V3__flowable_schema.sql 引入 Flowable 38 张 ACT_* 表 DDL（实际可由 Flowable 自动建表，此处仅占位）
  - [ ] SubTask 4.4: 创建 V4__xxl_job_schema.sql 引入 XXL-JOB 16 张 xxl_job_* 表 DDL
  - [ ] SubTask 4.5: 创建 V5__integration_adapter.sql 扩展 integration_config 表字段（adapter_type/last_sync_time/sync_status）

---

## 阶段二：后端模块补全与重构

- [x] Task 5: module-auth 多类型用户认证重构
  - [x] SubTask 5.1: 创建 `module-auth/dao/SysUserMapper.java`（独立 Mapper，从 module-system 解耦）
  - [x] SubTask 5.2: 创建 `AuthUser` 聚合根（统一内部/代理商/客户三类用户身份）
  - [x] SubTask 5.3: 扩展 AuthController 增加 `/auth/agent/login`、`/auth/customer/login` 端点
  - [x] SubTask 5.4: Token 载荷扩展 userType 字段（INTERNAL/AGENT/CUSTOMER），不同类型独立有效期配置

- [x] Task 6: module-collaboration 独立持久层 [P]
  - [x] SubTask 6.1: 创建 Entity：`CustomerPreferenceEntity` / `CustomerSubscriptionEntity` / `CustomerSessionEntity`
  - [x] SubTask 6.2: 创建对应 Mapper 与 Service
  - [x] SubTask 6.3: 扩展 CustomerPortalController 增加偏好/订阅/会话管理端点

- [ ] Task 7: module-report Mapper 拆分 [P]
  - [ ] SubTask 7.1: 拆分 `ReportMapper.java` 为 `ProjectReportMapper` / `DeviceReportMapper` / `ResourceReportMapper` / `FinanceReportMapper`
  - [ ] SubTask 7.2: 对应拆分 `ReportMapper.xml` 为 4 个 XML 文件
  - [ ] SubTask 7.3: 高并发聚合查询（项目趋势、设备分布、工时统计、利润分析）改走 ES 聚合
  - [ ] SubTask 7.4: 明细查询保留 MySQL，通过 Caffeine + Redis 二级缓存优化

- [ ] Task 8: module-integration 实际对接 Adapter [P]
  - [ ] SubTask 8.1: 引入 Spring Cloud OpenFeign 依赖
  - [ ] SubTask 8.2: 实现 `ErpCustomerSyncAdapter`（@FeignClient 调用 ERP 客户主数据接口，定时同步）
  - [ ] SubTask 8.3: 实现 `ImNotificationAdapter`（转发内部通知到 IM 系统：飞书/钉钉/企微）
  - [ ] SubTask 8.4: 实现 `LogisticsStatusAdapter`（拉取物流状态，更新设备 SHIPPED 阶段到货预计时间）
  - [ ] SubTask 8.5: 实现 `OaApprovalAdapter`（OA 审批联动：项目立项/验收/割接审批可联动 OA 系统）
  - [ ] SubTask 8.6: 集成 Resilience4j 实现熔断/降级/重试/限流（每个 Adapter 独立配置阈值）

- [ ] Task 9: 领域事件总线与模块间通信 [P]
  - [x] SubTask 9.1: 在 `module-common` 创建 `DomainEvent` 抽象基类与 `DomainEventPublisher` 接口
  - [x] SubTask 9.2: 实现 `RabbitMqDomainEventPublisher`（基于现有 RabbitMQ 配置）
  - [x] SubTask 9.3: 定义 15 个领域事件：ProjectCreated/ProjectStatusChanged/TaskAssigned/TaskCompleted/DeviceStatusChanged/InventoryWarning/WorkOrderCompleted/DeliverableSubmitted/DeliverableReviewed/AcceptancePassed/CutoverApproved/ChangeApproved/RiskEscalated/AgentScored/NoticeSent
  - [ ] SubTask 9.4: 各业务模块发布事件（在 Service 层关键业务操作后调用 publisher.publish）
  - [ ] SubTask 9.5: module-report 监听事件实时更新 ES 索引；module-system 通知引擎监听事件触发通知

- [ ] Task 10: 全局异常处理增强与链路追踪 [P]
  - [ ] SubTask 10.1: 扩展 `BusinessException` 为分级异常（BusinessException/PermissionException/DataException/ExternalException/SystemException）
  - [ ] SubTask 10.2: 修改 `GlobalExceptionHandler` 按异常类型返回不同错误码区间与恢复策略
  - [ ] SubTask 10.3: 集成 Micrometer Tracing + Zipkin（替代 Sleuth，适配 Spring Boot 3.2）
  - [ ] SubTask 10.4: 在 `module-common` 实现 `TraceContextFilter`，每个请求生成 TraceId 并放入 MDC
  - [ ] SubTask 10.5: 修改 `Result<T>` 响应体增加 `traceId` 字段
  - [ ] SubTask 10.6: 修改日志配置 logback-spring.xml 输出 TraceId

- [x] Task 11: EasyExcel 导出能力推广 [P]
  - [x] SubTask 11.1: 在 `ExcelUtils` 增加通用导出方法 `export(response, fileName, sheetName, headClazz, data)`
  - [x] SubTask 11.2: 财务预算/成本/结算/利润 4 个 Controller 增加 export 端点
  - [x] SubTask 11.3: 项目列表/项目任务清单 2 个 Controller 增加 export 端点
  - [x] SubTask 11.4: 资源工时/出差记录 2 个 Controller 增加 export 端点
  - [x] SubTask 11.5: 代理商工作量/交付物清单 2 个 Controller 增加 export 端点
  - [x] SubTask 11.6: 设备台账/库存日志/备件日志 3 个 Controller 增加 export 端点

---

## 阶段三：前端通用组件抽取

- [ ] Task 12: 抽取 CrudTable 通用 CRUD 表格组件
  - [ ] SubTask 12.1: 创建 `src/components/CrudTable/index.vue`（props: columns/searchFields/apiFunc/modelBinding）
  - [ ] SubTask 12.2: 支持搜索表单自动渲染（基于 searchFields 配置）+ 折叠展开
  - [ ] SubTask 12.3: 支持新增/编辑弹窗（基于 columns 自动生成 Form）+ 表单验证
  - [ ] SubTask 12.4: 支持删除确认 + 状态反馈（成功/失败 message）+ 异常处理（统一 try/catch）
  - [ ] SubTask 12.5: 支持分页、排序、行选择、批量操作
  - [ ] SubTask 12.6: 支持 actions 自定义操作按钮（编辑/删除/查看/自定义）
  - [ ] SubTask 12.7: 单元测试 Vitest 覆盖率 ≥80%

- [ ] Task 13: 抽取 FormModal 通用表单弹窗组件 [P]
  - [ ] SubTask 13.1: 创建 `src/components/FormModal/index.vue`（props: fields/apiFunc/visible/title）
  - [ ] SubTask 13.2: 支持字段类型：input/select/date/switch/radio/checkbox/cascader/upload/treeSelect
  - [ ] SubTask 13.3: 支持字段联动（如选 A 后 B 字段必填，选 C 后 D 字段隐藏）
  - [ ] SubTask 13.4: 支持异步选项加载（select/cascader 从 API 拉取选项）
  - [ ] SubTask 13.5: 支持表单验证规则（required/pattern/min/max/custom）
  - [ ] SubTask 13.6: 单元测试覆盖率 ≥80%

- [ ] Task 14: 抽取 FileUpload 通用文件上传组件 [P]
  - [ ] SubTask 14.1: 创建 `src/components/FileUpload/index.vue`（props: accept/maxSize/maxCount/multiple/watermark）
  - [ ] SubTask 14.2: 调用后端 `/api/v1/files/presign` 获取 MinIO 预签名 URL，前端直传
  - [ ] SubTask 14.3: 支持图片预览、文件列表、删除、下载
  - [ ] SubTask 14.4: 支持图片压缩（质量 85%、长边 ≤2048px）+ 缩略图 + 水印（时间+GPS+上传人）
  - [ ] SubTask 14.5: 大文件分片上传（>10MB 自动分片 5MB）
  - [ ] SubTask 14.6: 弱网环境本地缓存 + 断点续传

- [ ] Task 15: 抽取 RichEditor 富文本编辑器组件 [P]
  - [ ] SubTask 15.1: 引入 wangEditor 5 依赖
  - [ ] SubTask 15.2: 创建 `src/components/RichEditor/index.vue`（props: modelValue/height/toolbar）
  - [ ] SubTask 15.3: 集成图片上传走 FileUpload 组件
  - [ ] SubTask 15.4: 支持 HTML 输出与纯文本输出两种模式

- [ ] Task 16: 抽取 OrgTree 组织树组件 [P]
  - [ ] SubTask 16.1: 创建 `src/components/OrgTree/index.vue`（props: orgType/single/multiple/defaultExpandedKeys）
  - [ ] SubTask 16.2: 调用 `/api/v1/orgs/tree` 加载组织树
  - [ ] SubTask 16.3: 支持搜索、展开/折叠、节点选择
  - [ ] SubTask 16.4: 支持嵌入表单作为树选择器

- [ ] Task 17: 抽取 Charts 图表包装组件（ECharts 5） [P]
  - [ ] SubTask 17.1: 引入 echarts 5 + vue-echarts 依赖（package.json 已声明 echarts）
  - [ ] SubTask 17.2: 创建 `src/components/charts/BaseChart.vue`（基础组件，处理 resize/dispose）
  - [ ] SubTask 17.3: 创建 8 个图表子组件：PieChart / LineChart / BarChart / StackedChart / FunnelChart / MapChart / RadarChart / GaugeChart
  - [ ] SubTask 17.4: 统一主题（品牌色 #1677FF + 状态色映射），统一 tooltip/legend/grid 样式
  - [ ] SubTask 17.5: 支持响应式 resize、loading 状态、空数据 EmptyState

- [ ] Task 18: 抽取 Gantt 甘特图组件（dhtmlx-gantt） [P]
  - [ ] SubTask 18.1: 确认 dhtmlx-gantt 8.x 依赖已声明
  - [ ] SubTask 18.2: 创建 `src/components/Gantt/index.vue`（props: tasks/links/viewMode/readonly）
  - [ ] SubTask 18.3: 支持任务拖拽调整时间、拖拽创建依赖关系
  - [ ] SubTask 18.4: 支持视图切换：日/周/月/季/年
  - [ ] SubTask 18.5: 支持自定义任务条颜色（按状态/优先级）、里程碑标记
  - [ ] SubTask 18.6: 暴露事件：onTaskDrag/onTaskSelect/onLinkCreate

- [ ] Task 19: 抽取 AMap 高德地图组件 [P]
  - [ ] SubTask 19.1: 安装 `@amap/amap-jsapi-loader` 依赖
  - [ ] SubTask 19.2: 在 `.env` 配置 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_CODE`
  - [ ] SubTask 19.3: 创建 `src/components/AMap/index.vue`（props: markers/center/zoom/cluster）
  - [ ] SubTask 19.4: 支持标记点、点击弹窗、聚合（markerCluster）
  - [ ] SubTask 19.5: 创建 `MapChart.vue`（嵌入 ECharts 地图，作为 AMap 的轻量替代）

- [ ] Task 20: 抽取 ImportExport 导入导出按钮组件 [P]
  - [ ] SubTask 20.1: 创建 `src/components/ImportExport/index.vue`（props: exportApi/importApi/templateUrl）
  - [ ] SubTask 20.2: 导出按钮调用后端 export 接口下载 Excel
  - [ ] SubTask 20.3: 导入按钮触发文件选择 + 上传到 import 接口 + 展示导入结果（成功/失败行数）
  - [ ] SubTask 20.4: 模板下载按钮下载 templateUrl

---

## 阶段四：前端可视化能力落地

- [ ] Task 21: 修复已知 Bug 与死代码清理
  - [ ] SubTask 21.1: 修复 `dashboard/my-tasks.vue` 字段不匹配 Bug：按 `dashboard.role` 取 `dashboard.pm.myProjects` 或 `dashboard.engineer.todayTasks`
  - [ ] SubTask 21.2: 修复 `project/detail.vue` 任务 Tab"新增任务"按钮绑定 FormModal 弹窗
  - [ ] SubTask 21.3: 修复 `report/project.vue` 导出按钮调用 ImportExport 组件
  - [ ] SubTask 21.4: 删除 `_placeholder/index.vue` 死代码

- [ ] Task 22: 项目列表 4 视图切换实现
  - [ ] SubTask 22.1: 修改 `project/list.vue` 增加视图切换 Radio（列表/看板/甘特图/地图）
  - [ ] SubTask 22.2: 实现看板视图（a-row + 多列分组，按状态分列，卡片拖拽跨列流转状态）
  - [ ] SubTask 22.3: 实现甘特图视图（基于 Gantt 组件，调用 `getProjectGantt` API，支持拖拽排期）
  - [ ] SubTask 22.4: 实现地图视图（基于 AMap 组件，按客户所在地渲染项目分布）
  - [ ] SubTask 22.5: 视图切换保留搜索筛选条件

- [ ] Task 23: 排期日历接入真正甘特图
  - [ ] SubTask 23.1: 修改 `resource/schedule.vue` 替换日历网格为 Gantt 组件
  - [ ] SubTask 23.2: 调用 `/api/v1/engineer-schedules/gantt` 接口返回甘特图数据
  - [ ] SubTask 23.3: 支持拖拽调整排期、冲突检测高亮、请假时段灰色背景

- [ ] Task 24: 驾驶舱与报表页全面 ECharts 化
  - [ ] SubTask 24.1: 重写 `report/cockpit.vue`：4 张 KPI 卡片（含环比）+ 项目阶段分布 PieChart + 近 12 月趋势 StackedChart + 风险项目列表 + 设备状态 GaugeChart + 区域分布 MapChart
  - [ ] SubTask 24.2: 重写 `report/project.vue`：项目状态分布 PieChart + 月度新增/完成 LineChart + PM 业绩 BarChart + 明细表格 + ImportExport
  - [ ] SubTask 24.3: 重写 `report/device.vue`：设备状态分布 PieChart + 型号分布 BarChart + 异常趋势 LineChart + 明细表格
  - [ ] SubTask 24.4: 重写 `report/resource.vue`：工时统计 StackedChart + 工程师负荷 RadarChart + 出差分布 MapChart + 明细表格
  - [ ] SubTask 24.5: 重写 `report/finance.vue`：利润趋势 LineChart + 收入成本对比 BarChart + 客户占比 FunnelChart + 明细表格

- [ ] Task 25: 数据大屏页面实现
  - [ ] SubTask 25.1: 创建 `src/views/bigscreen/index.vue` 全屏布局（1920x1080 设计稿）
  - [ ] SubTask 25.2: 6 个区块：核心 KPI 区（4 张大数字卡）+ 项目分布地图（AMap）+ 设备状态仪表盘（GaugeChart）+ 项目趋势（StackedChart）+ 待办滚动列表（每 30s 高亮切换）+ 风险预警滚动（红色高亮）
  - [ ] SubTask 25.3: 全屏 API（Fullscreen API）+ 自动刷新（每 5 分钟）+ 轮播高亮（每 30s 切换区块）
  - [ ] SubTask 25.4: 暗色主题适配
  - [ ] SubTask 25.5: 在 `routes.ts` 添加 `/bigscreen` 路由（无需 BasicLayout，全屏独立）

- [ ] Task 26: 独立菜单管理页实现
  - [ ] SubTask 26.1: 创建 `src/views/system/menu.vue` 页面
  - [ ] SubTask 26.2: 调用 `/api/v1/menus/tree` 加载菜单树（a-tree-table 展示）
  - [ ] SubTask 26.3: 支持新增/编辑/删除菜单（菜单类型：目录/菜单/按钮）
  - [ ] SubTask 26.4: 支持拖拽排序、关联权限码、关联角色
  - [ ] SubTask 26.5: 在 `routes.ts` 添加 `/system/menu` 路由

---

## 阶段五：前端响应式与主题统一

- [ ] Task 27: 响应式断点全面覆盖
  - [ ] SubTask 27.1: 在 `src/styles/variables.less` 统一定义断点变量（@screen-pc: 1280px、@screen-tablet: 768px、@screen-mobile: 576px）
  - [ ] SubTask 27.2: 在 `src/styles/mixins.less` 定义响应式 mixin（.respond-to(@breakpoint) { ... }）
  - [ ] SubTask 27.3: 逐页面适配（约 57 个页面）：
    - [ ] project（list/detail/task-detail/template）4 个
    - [ ] device（board/ledger/inout/model/spare）5 个
    - [ ] resource（schedule/engineer/dispatch/timesheet）4 个
    - [ ] delivery（cutover/field/board）3 个
    - [ ] agent（profile/outsource/review/settlement）4 个
    - [ ] acceptance（standard/task/issue/doc）4 个
    - [ ] finance（profit/budget/agent/cost）4 个
    - [ ] report（cockpit/project/device/resource/finance）5 个
    - [ ] system（user/role/dict/notice-template/config/org/position/notice/log/login-log/menu）11 个
    - [ ] integration（config/call-log）2 个
    - [ ] customer H5（cutover-approval/acceptance-sign/progress/projects/documents/todos/messages/login）8 个
    - [ ] agent H5（workbench/deliverable-submit/messages/login）4 个
  - [ ] SubTask 27.4: 平板断点（768-1279px）：单列布局、搜索表单折叠为抽屉、表格列优先级排序、操作按钮折叠下拉
  - [ ] SubTask 27.5: 移动端断点（<768px）：H5 跳转或简化布局（仅核心场景）

- [ ] Task 28: H5 页面 less 变量替换
  - [ ] SubTask 28.1: 在 `src/styles/h5-variables.less` 定义 H5 专用变量（颜色/字号/间距）
  - [ ] SubTask 28.2: 全 H5 页面（12 个）硬编码颜色 `#1677ff`/`#52c41a`/`#ff4d4f` 等替换为 `@primary-color`/`@success-color`/`@error-color`
  - [ ] SubTask 28.3: 替换硬编码字号 `12px/14px/16px` 为 `@font-size-sm`/`@font-size-base`/`@font-size-lg`

- [ ] Task 29: 品牌设计系统统一与扩展
  - [ ] SubTask 29.1: 扩展 `src/styles/variables.less` 变量：
    - 颜色：primary/success/warning/error/info + 5 个梯度（lighter/light/normal/dark/darker）
    - 间距：8px 网格（@spacing-xxs=4 / @spacing-xs=8 / @spacing-sm=12 / @spacing-md=16 / @spacing-lg=24 / @spacing-xl=32）
    - 字号：@font-size-xs=12 / sm=14 / base=14 / lg=16 / xl=18 / xxl=20 / heading-1=28 / heading-2=24 / heading-3=20
    - 圆角：@radius-sm=4 / radius-md=6 / radius-lg=8 / radius-pill=999
    - 阴影：@shadow-card / @shadow-popover / @shadow-modal
    - 动画：@duration-fast=100ms / @duration-base=200ms / @duration-slow=300ms + @ease-in / @ease-out / @ease-in-out
  - [ ] SubTask 29.2: 在 Ant Design Vue 主题定制（ConfigProvider）覆盖 token，对齐 less 变量
  - [ ] SubTask 29.3: 创建 `src/styles/utilities.less` 工具类（.text-primary / .bg-stripe / .shadow-card / .flex-center 等）
  - [ ] SubTask 29.4: 全页面替换硬编码值（grep 检查无残留硬编码颜色/字号/间距）

---

## 阶段六：低代码模块实现

- [x] Task 30: 后端 module-lowcode 模块搭建
  - [x] SubTask 30.1: 创建 `vibe-server/module-lowcode/` 模块目录与 pom.xml
  - [x] SubTask 30.2: 在父 POM 添加 module-lowcode 到 `<modules>`
  - [x] SubTask 30.3: 创建 Entity：
    - [x] `LowcodeFormConfigEntity`（id/configCode/configName/schema/templateId/version/status/creatorId/createdAt）
    - [x] `LowcodeListConfigEntity`（同上结构）
    - [x] `LowcodeTabConfigEntity`
    - [x] `LowcodeRelationConfigEntity`
    - [x] `LowcodeTemplateEntity`（id/templateCode/templateName/templateType/schema/description/usageCount）
  - [x] SubTask 30.4: 创建 Mapper（5 个）+ Service（5 个）+ ServiceImpl（5 个）
  - [x] SubTask 30.5: 创建 Controller（5 个）：FormConfigController / ListConfigController / TabConfigController / RelationConfigController / TemplateController
  - [x] SubTask 30.6: 每个 Controller 提供：分页查询/详情/创建/更新/删除/复制/导出 JSON/导入 JSON/基于模板实例化 端点
  - [x] SubTask 30.7: 引入 `com.networknt:json-schema-validator` 实现 JSON Schema Draft 7 校验
  - [x] SubTask 30.8: 权限控制：`@PreAuthorize("@ss.hasPermi('lowcode:config:*')")`

- [ ] Task 31: 前端低代码表单配置器
  - [ ] SubTask 31.1: 创建 `src/views/lowcode/form-config/index.vue` 列表页（CRUD + 导入导出）
  - [ ] SubTask 31.2: 创建 `src/views/lowcode/form-config/editor.vue` 配置器主页面（左字段库 / 中拖拽画布 / 右属性配置 三栏布局）
  - [ ] SubTask 31.3: 实现字段库面板：8 类字段（input/select/date/switch/radio/checkbox/cascader/upload/treeSelect）
  - [ ] SubTask 31.4: 拖拽到画布生成字段卡片（vue-draggable + vuedraggable）
  - [ ] SubTask 31.5: 字段属性配置面板：label/fieldName/required/placeholder/defaultValue/校验规则（pattern/min/max/custom）/选项配置（select/radio/checkbox）/联动规则
  - [ ] SubTask 31.6: 预览面板：根据 Schema 实时渲染表单（调用 FormRenderer）
  - [ ] SubTask 31.7: 保存：导出 JSON Schema + 调用后端 save 接口

- [ ] Task 32: 前端低代码列表配置器 [P]
  - [ ] SubTask 32.1: 创建 `src/views/lowcode/list-config/index.vue` 列表页
  - [ ] SubTask 32.2: 创建 `src/views/lowcode/list-config/editor.vue` 配置器
  - [ ] SubTask 32.3: 列定义配置：字段/标题/对齐/宽/格式化（日期/数字/枚举）/可排序/可搜索
  - [ ] SubTask 32.4: 筛选条件配置：字段/操作符（eq/ne/gt/lt/like/in）/控件类型/默认值
  - [ ] SubTask 32.5: 操作按钮配置：编辑/删除/查看/自定义（API/权限/图标）
  - [ ] SubTask 32.6: 预览：根据 Schema 实时渲染列表（调用 ListRenderer）

- [ ] Task 33: 前端低代码标签页配置器 [P]
  - [ ] SubTask 33.1: 创建 `src/views/lowcode/tab-config/index.vue` 与 `editor.vue`
  - [ ] SubTask 33.2: Tab 定义：tabCode/tabName/icon/order
  - [ ] SubTask 33.3: Tab 内嵌内容配置：表单配置引用 / 列表配置引用 / 关联页配置引用 / 自定义 HTML
  - [ ] SubTask 33.4: 布局配置：水平/垂直 Tab、Tab 高度、内边距
  - [ ] SubTask 33.5: 预览：根据 Schema 实时渲染多 Tab 页面

- [ ] Task 34: 前端低代码关联页配置器 [P]
  - [ ] SubTask 34.1: 创建 `src/views/lowcode/relation-config/index.vue` 与 `editor.vue`
  - [ ] SubTask 34.2: 主从关联配置：主表/从表、关联字段（外键）、显示模式（master-detail/master-grid）
  - [ ] SubTask 34.3: 级联规则：主表字段变更时联动从表筛选/赋值
  - [ ] SubTask 34.4: 显示字段配置：主表显示哪些字段、从表显示哪些字段
  - [ ] SubTask 34.5: 预览：根据 Schema 实时渲染主从关联页

- [ ] Task 35: 前端低代码模板管理 [P]
  - [ ] SubTask 35.1: 创建 `src/views/lowcode/template/index.vue` 模板列表页（CRUD）
  - [ ] SubTask 35.2: 模板分类筛选（按 templateType：form/list/tab/relation）
  - [ ] SubTask 35.3: 模板新建/编辑：复用对应类型配置器
  - [ ] SubTask 35.4: 模板导入 JSON：上传 JSON 文件 + 校验 JSON Schema + 保存
  - [ ] SubTask 35.5: 模板导出 JSON：下载 JSON 文件
  - [ ] SubTask 35.6: 基于模板实例化：点击"使用此模板"按钮 → 复制 Schema 到新配置 → 跳转到对应配置器

- [ ] Task 36: 前端低代码运行时引擎
  - [ ] SubTask 36.1: 创建 `src/components/Lowcode/FormRenderer.vue`（props: schema/dataSource）
  - [ ] SubTask 36.2: 根据 schema.fields 渲染对应字段组件（复用 FormModal 字段渲染逻辑）
  - [ ] SubTask 36.3: 字段联动规则运行时执行（基于 schema.rules）
  - [ ] SubTask 36.4: 字段权限控制（基于 schema.fieldPermissions 控制只读/隐藏）
  - [ ] SubTask 36.5: 表单提交调用 dataSource.saveApi
  - [ ] SubTask 36.6: 创建 `src/components/Lowcode/ListRenderer.vue`（props: schema/dataSource）
  - [ ] SubTask 36.7: 根据 schema.columns 渲染表格 + schema.searchFields 渲染搜索表单 + schema.actions 渲染按钮
  - [ ] SubTask 36.8: 创建 `src/components/Lowcode/TabRenderer.vue` 与 `RelationRenderer.vue`
  - [ ] SubTask 36.9: 在 `routes.ts` 添加动态路由：`/lowcode/render/:configCode`，根据 configCode 加载对应 Renderer

- [ ] Task 37: 低代码业务接入（3 个示例）
  - [ ] SubTask 37.1: 设备巡检表单（form-config）：基于 form 配置器创建"设备巡检表单"，包含字段：设备 SN/巡检日期/巡检人/外观检查/功能测试/备注/照片（upload）
  - [ ] SubTask 37.2: 客户回访列表（list-config）：基于 list 配置器创建"客户回访列表"，列：客户名称/回访日期/回访人/满意度/备注，操作：编辑/查看
  - [ ] SubTask 37.3: 项目阶段交付关联页（relation-config）：主表项目阶段、从表阶段交付物，关联字段 phase_id

---

## 阶段七：交付部署体系

- [ ] Task 38: 部署文档体系
  - [ ] SubTask 38.1: 创建 `docs/deployment/dev-deploy.md`：开发环境一键启动指南（前置依赖/Docker Desktop/IDE 配置/数据库初始化/前端启动/调试端口）
  - [ ] SubTask 38.2: 创建 `docs/deployment/test-deploy.md`：测试环境 Docker Compose 部署指南（环境变量/网络/数据卷/服务依赖/健康检查/回滚）
  - [ ] SubTask 38.3: 创建 `docs/deployment/prod-deploy.md`：生产环境部署指南
    - [ ] Nginx 反向代理配置（前端静态资源 + API 网关 + WebSocket）
    - [ ] Spring Boot 集群部署（Nacos 服务注册 + 负载均衡）
    - [ ] MySQL 主从配置（主写从读 + MHA 高可用）
    - [ ] Redis Sentinel 哨兵集群配置
    - [ ] ES 集群配置（3 节点）
    - [ ] MinIO 分布式部署（4 节点纠删码）
    - [ ] RabbitMQ 集群配置（镜像队列）
    - [ ] XXL-JOB 集群部署
    - [ ] Flowable 集群部署（共享数据库 + 异步执行器）
    - [ ] Prometheus + Grafana 监控
    - [ ] ELK 日志收集
  - [ ] SubTask 38.4: 创建 `docs/deployment/rollback.md`：回滚流程（蓝绿切换/数据库回滚/配置回滚/紧急修复）
  - [ ] SubTask 38.5: 创建 `docs/deployment/monitoring.md`：监控告警配置（Prometheus 抓取规则/Grafana 面板/Alertmanager 告警规则/钉钉告警机器人）
  - [ ] SubTask 38.6: 创建 `docs/deployment/upgrade.md`：升级指南（Flyway 迁移/ES 索引重建/Flowable 流程升级/前端静态资源清理）

- [ ] Task 39: 自动化部署脚本
  - [ ] SubTask 39.1: 创建 `scripts/deploy/dev-up.sh`：docker-compose up -d 一键启动 + 等待健康检查 + 数据库迁移 + 索引初始化
  - [ ] SubTask 39.2: 创建 `scripts/deploy/dev-down.sh`：docker-compose down -v 清理
  - [ ] SubTask 39.3: 创建 `scripts/deploy/test-deploy.sh`：测试环境部署脚本（拉镜像 → 滚动更新 → 健康检查 → 烟雾测试 → 通知飞书）
  - [ ] SubTask 39.4: 创建 `scripts/deploy/prod-deploy.sh`：生产环境蓝绿部署脚本（备份 → 部署蓝/绿 → 流量切换 → 监控 → 失败回滚）
  - [ ] SubTask 39.5: 创建 `scripts/deploy/db-migrate.sh`：调用 Flyway Maven 插件执行数据库迁移
  - [ ] SubTask 39.6: 创建 `scripts/deploy/backup-restore.sh`：MySQL 备份（mysqldump）+ ES 快照 + MinIO 备份 + 恢复脚本
  - [ ] SubTask 39.7: 创建 `scripts/deploy/health-check.sh`：健康检查脚本（curl 5 个核心接口，失败则告警）
  - [ ] SubTask 39.8: 所有脚本添加 `set -euo pipefail` 严格模式 + 颜色日志输出

- [ ] Task 40: CI/CD 流水线
  - [ ] SubTask 40.1: 创建 `.gitlab-ci.yml`（如使用 GitLab）或 `Jenkinsfile`（如使用 Jenkins）
  - [ ] SubTask 40.2: Pipeline 阶段定义：
    - [ ] build：Maven 构建 + Vite 构建
    - [ ] test：单元测试（覆盖率 ≥80%）+ 集成测试
    - [ ] scan：SonarQube 静态扫描
    - [ ] image：Docker 镜像构建并推送 Harbor
    - [ ] deploy-test：自动部署测试环境 + 烟雾测试
    - [ ] approval：人工审批（生产环境）
    - [ ] deploy-prod：蓝绿部署生产环境
    - [ ] notify：成功/失败通知飞书
  - [ ] SubTask 40.3: 创建 `.github/workflows/ci.yml`（如使用 GitHub Actions）作为替代方案
  - [ ] SubTask 40.4: 缓存优化：Maven 依赖缓存 + npm 依赖缓存 + Docker layer 缓存
  - [ ] SubTask 40.5: 并行优化：后端构建 / 前端构建 / 镜像构建 可并行
  - [ ] SubTask 40.6: 失败重试：网络相关步骤自动重试 3 次

- [ ] Task 41: 用户引导系统
  - [ ] SubTask 41.1: 引入 `driver.js` 依赖（轻量级引导库）
  - [ ] SubTask 41.2: 创建 `src/components/UserGuide/GuideOverlay.vue` 引导浮层组件
  - [ ] SubTask 41.3: 创建 `src/utils/guide-steps.ts` 角色化引导步骤定义：
    - [ ] director-steps：5 步（首页概览 / 审批待办 / 驾驶舱 / 项目列表 / 数据大屏）
    - [ ] pm-steps：5 步（首页 / 我的项目 / 立项 / 派单 / 验收）
    - [ ] engineer-steps：4 步（首页今日任务 / 工时入口 / 现场作业 / 我的消息）
    - [ ] agent-steps：4 步（待接单 / 进行中 / 交付物提交 / 结算查看）
    - [ ] customer-steps：3 步（项目进度 / 文档下载 / 待办处理）
  - [ ] SubTask 41.4: 创建 `src/components/UserGuide/FirstLoginModal.vue` 首次登录浮层（功能概览 + 5 张角色化卡片）
  - [ ] SubTask 41.5: 创建 `src/components/UserGuide/HelpDrawer.vue` 页面级帮助抽屉（含操作步骤 + 截图占位）
  - [ ] SubTask 41.6: 在 `userStore` 添加 `hasSeenGuide` 字段（localStorage 持久化）
  - [ ] SubTask 41.7: 在 BasicLayout Header 添加帮助按钮（？图标）触发 HelpDrawer

- [ ] Task 42: 技术支持体系
  - [ ] SubTask 42.1: 创建 `docs/support/faq.md`：按角色分组常见问题（FAQ 30+ 条）
  - [ ] SubTask 42.2: 创建 `docs/support/troubleshooting.md`：故障排查指南（登录失败/权限错误/数据不显示/上传失败/通知未收到等）
  - [ ] SubTask 42.3: 创建 `src/components/Feedback/FeedbackDrawer.vue` 问题反馈抽屉（标题/类型/优先级/截图上传/描述）
  - [ ] SubTask 42.4: 后端 `/api/v1/feedback` 接收反馈并投递到 GitLab Issue 或飞书工单（配置驱动）
  - [ ] SubTask 42.5: 在 BasicLayout Header 添加反馈按钮触发 FeedbackDrawer
  - [ ] SubTask 42.6: 创建 `docs/api/knife4j-export.yaml`：Knife4j 导出 OpenAPI 3 规范，便于第三方集成
  - [ ] SubTask 42.7: 创建 `docs/api/api-contract.md`：前后端 API 契约文档（核心接口字段对齐说明）

---

## 阶段八：测试与验证

- [ ] Task 43: 后端单元测试覆盖率提升
  - [ ] SubTask 43.1: 引入 Jacoco 依赖与 Maven 插件，配置覆盖率检查规则（≥80%）
  - [ ] SubTask 43.2: module-project 核心业务测试：项目状态机、任务派发、模板实例化
  - [ ] SubTask 43.3: module-device 状态机测试：设备状态流转非法路径全覆盖
  - [ ] SubTask 43.4: module-resource 测试：排期冲突检测、智能派单算法
  - [ ] SubTask 43.5: module-delivery 测试：GPS 签到校验、施工步骤跟踪
  - [ ] SubTask 43.6: module-agent 测试：转包任务状态机、代理商数据权限隔离
  - [ ] SubTask 43.7: module-acceptance 测试：验收流程、签核、会签
  - [ ] SubTask 43.8: module-finance 测试：利润计算、工作量确认
  - [ ] SubTask 43.9: module-lowcode 测试：Schema 校验、模板实例化
  - [ ] SubTask 43.10: module-auth 测试：多类型用户认证、Token 续签

- [ ] Task 44: 后端集成测试
  - [ ] SubTask 44.1: 引入 Testcontainers 依赖（MySQL/Redis/ES/RabbitMQ/MinIO 容器化测试）
  - [ ] SubTask 44.2: Flowable 流程引擎集成测试（启动/审批/会签/回退/撤回）
  - [ ] SubTask 44.3: XXL-JOB 调度集成测试（任务触发/失败重试/集群分片）
  - [ ] SubTask 44.4: ES 检索集成测试（索引创建/增量同步/聚合查询）
  - [ ] SubTask 44.5: 数据权限隔离测试（PM/ENGINEER/AGENT_ADMIN/CUSTOMER 各角色数据边界）
  - [ ] SubTask 44.6: 领域事件总线测试（事件投递/消费/幂等性）
  - [ ] SubTask 44.7: 熔断降级测试（模拟外部依赖不可用，验证 Resilience4j 熔断）

- [ ] Task 45: 前端组件测试与 E2E
  - [ ] SubTask 45.1: 通用组件 Vitest 测试：CrudTable / FormModal / FileUpload / Charts / Gantt / AMap
  - [ ] SubTask 45.2: 低代码渲染器测试：FormRenderer / ListRenderer / TabRenderer / RelationRenderer
  - [ ] SubTask 45.3: 引入 Playwright E2E 框架
  - [ ] SubTask 45.4: E2E 核心流程：
    - [ ] 登录流程（PC + H5 客户 + H5 代理商）
    - [ ] 项目立项 → 4 视图切换（列表/看板/甘特/地图）
    - [ ] 任务派发 → 工程师移动端签到 → 完成确认
    - [ ] 代理商接单 → 交付物提交 → PM 审核
    - [ ] 验收签核（含会签节点）
    - [ ] 割接审批
    - [ ] 低代码配置 → 预览 → 保存 → 运行时渲染
    - [ ] 数据大屏全屏展示
    - [ ] 用户引导首次登录
    - [ ] 问题反馈提交

- [ ] Task 46: 性能测试
  - [ ] SubTask 46.1: 引入 k6 性能测试框架
  - [ ] SubTask 46.2: 核心接口压测脚本：
    - [ ] 登录接口（1000 并发，P95 < 500ms）
    - [ ] 项目列表查询（500 并发，P95 < 200ms）
    - [ ] 设备 ES 检索（500 并发，P95 < 200ms）
    - [ ] 报表聚合查询（100 并发，P95 < 1s）
    - [ ] 文件上传（50 并发，5MB 文件，P95 < 5s）
  - [ ] SubTask 46.3: 数据库慢查询日志分析（启用 p6spy + 慢 SQL 报警）
  - [ ] SubTask 46.4: JVM 性能调优文档（堆内存/GC 策略/线程池）

- [ ] Task 47: 异常处理专项测试
  - [ ] SubTask 47.1: 网络超时场景：模拟后端响应慢（5s+），前端 loading 状态 + 超时提示
  - [ ] SubTask 47.2: 并发冲突场景：两个用户同时编辑同一项目（乐观锁冲突）
  - [ ] SubTask 47.3: 数据校验失败场景：必填字段空、格式错误、长度超限
  - [ ] SubTask 47.4: 权限拒绝场景：未授权访问受限接口（403）、数据权限越权（数据拦截器拦截）
  - [ ] SubTask 47.5: 外部依赖不可用场景：ERP/IM/物流/OA 不可用时熔断降级验证
  - [ ] SubTask 47.6: 弱网环境：H5 移动端在 2G/3G 网络下文件上传断点续传

---

## 阶段九：联调与最终验收

- [ ] Task 48: 前后端联调与接口契约对齐
  - [ ] SubTask 48.1: Knife4j 接口文档更新导出
  - [ ] SubTask 48.2: 全模块主链路联调：
    - [ ] 立项 → 计划 → 派单 → 执行 → 验收 → 结项（含 Flowable 审批）
    - [ ] 设备入库 → 发运 → 到货 → 安装 → 在网（含 ES 同步）
    - [ ] 代理商转包 → 交付 → 审核（含会签）
    - [ ] 库存预警定时任务触发 → 通知（XXL-JOB）
    - [ ] 低代码配置 → 运行时渲染 → 业务接入
    - [ ] 数据大屏全屏展示
    - [ ] 用户引导首次登录
  - [ ] SubTask 48.3: 接口契约对齐检查（前端 API 类型与后端 VO 字段一致性）

- [ ] Task 49: 最终验收与文档交付
  - [ ] SubTask 49.1: 全部 checklist 验证通过
  - [ ] SubTask 49.2: 部署文档评审（dev/test/prod 三套）
  - [ ] SubTask 49.3: 用户引导视频教程占位（5 个角色各 1 个）
  - [ ] SubTask 49.4: 技术支持体系试运行（FAQ + 反馈入口验证）
  - [ ] SubTask 49.5: 性能基线报告（核心接口 P95 延迟）
  - [ ] SubTask 49.6: Git tag 标记版本 v2.0.0

---

# Task Dependencies

## 阶段依赖关系
- Task 1-4（基础设施）→ 所有后续任务依赖基础设施就绪
  - Task 1（Flowable）→ Task 5（auth 不依赖）/ Task 8（integration 不依赖）/ Task 48 联调依赖
  - Task 2（XXL-JOB）→ Task 48 联调依赖
  - Task 3（ES）→ Task 7（report 拆分依赖 ES）/ Task 48 联调依赖
  - Task 4（DB 迁移）→ Task 6 / Task 30 依赖新表

## 阶段间依赖
- 阶段一（Task 1-4）→ 阶段二（Task 5-11 后端补全）→ 阶段八（Task 43-47 测试）
- 阶段三（Task 12-20 前端组件）→ 阶段四（Task 21-26 可视化落地）→ 阶段五（Task 27-29 响应式）
- 阶段六（Task 30-37 低代码）独立但依赖 Task 4（新表）与 Task 13（FormModal 复用）
- 阶段七（Task 38-42 部署交付）依赖阶段一至六完成度
- 阶段八（Task 43-47 测试）依赖阶段一至六全部完成
- 阶段九（Task 48-49 联调验收）依赖全部任务

## 并行任务
- 阶段一：Task 2/3/4 可与 Task 1 并行
- 阶段二：Task 5/6/7/8/9/10/11 互不依赖，全部可并行
- 阶段三：Task 13-20 互不依赖，全部可并行（Task 12 CrudTable 略早做参考）
- 阶段四：Task 22/23/24/25/26 可并行（依赖阶段三组件就绪）
- 阶段五：Task 27/28/29 可并行
- 阶段六：Task 31-37 可并行（Task 30 必须先做后端）
- 阶段七：Task 38/39/40/41/42 可并行
- 阶段八：Task 43/44/45/46/47 可并行（覆盖不同模块）

## 关键路径（最长依赖链）
Task 4（DB）→ Task 30（lowcode 后端）→ Task 31（form 配置器）→ Task 36（运行时）→ Task 37（业务接入）→ Task 48（联调）→ Task 49（验收）
预计 8-12 周（4-6 名开发人员并行）
