# Checklist — 系统模块全面补全验证清单

> change-id：`complete-system-modules`
> 用于实现完成后逐项验证，通过后勾选

---

## 一、基础设施与中间件

- [x] Flowable 7 集成：父 POM 依赖就位、FlowableConfig 配置类正确、38 张 ACT_* 表自动创建
- [x] Flowable 4 个 BPMN 2.0 流程定义文件就位（acceptance/cutover/outsource/change）
- [x] FlowableProcessService 封装完整：启动/审批/回退/会签/催办/撤回/查询历史
- [x] 4 个业务 Controller（AcceptanceTask/CutoverPlan/OutsourceTask/ProjectChange）已接入 Flowable
- [ ] XXL-JOB 2.4 集成：父 POM 依赖就位、XxlJobConfig 配置类正确
- [ ] 7 个定时任务 Handler 实现：InventoryWarning/SparePartRestock/WorkOrderTimeout/AcceptanceOverdue/ProjectProgressSync/DataCleanup/FinanceReconciliation
- [ ] docker-compose.yml 新增 xxl-job-admin 服务可启动
- [x] ElasticSearch 8.x 集成：父 POM 依赖就位、ESConfig 配置类正确、application.yml 配置块完整（mvn compile BUILD SUCCESS 验证）
- [x] 3 个 ES 索引创建：vibe_project / vibe_device / vibe_work_order（EsIndexInitializer + 3 个 Index POJO + mapping JSON 就位）
- [x] ElasticSearchService<T> 通用服务实现：索引创建/批量写入/全文检索/聚合查询/单条写入/按 ID 删除/索引存在性检查 全部就位
- [x] MySQL → ES 增量同步通过 RabbitMQ 事件驱动（EntityEventListener 监听 vibe.es.sync.queue，按事件类型分发到 4 个 handler；DomainEventConstant/DomainEventRabbitConfig 配置 TopicExchange + Queue + Binding + @Primary MessageConverter）
- [x] 3 个 Controller 列表查询接口支持 ES 检索（保留 MySQL 兜底）（ProjectController/DeviceInstanceController/WorkOrderController 支持 useEs 参数 + ES 不可用时回退 MySQL）
- [x] docker-compose.yml 新增 elasticsearch 服务（elasticsearch:8.11.0，single-node，xpack.security 关闭，9200/9300 端口，healthcheck 已配置，vibe-server depends_on elasticsearch:service_healthy）
- [x] 领域事件总线：DomainEvent 抽象基类 + DomainEventPublisher 接口就位
- [x] RabbitMqDomainEventPublisher 实现（@Qualifier 指定 domainEventRabbitTemplate，路由键 vibe.domain.event.{eventType}，容错捕获 AmqpException）
- [x] 15 个领域事件定义完整（com.vibe.event.events 包下 15 个事件类：ProjectCreated/ProjectStatusChanged/TaskAssigned/TaskCompleted/DeviceStatusChanged/InventoryWarning/WorkOrderCompleted/DeliverableSubmitted/DeliverableReviewed/AcceptancePassed/CutoverApproved/ChangeApproved/RiskEscalated/AgentScored/NoticeSent）
- [ ] Flyway 9.x 引入并配置完成
- [ ] 5 个迁移脚本（V2-V5）执行成功：
  - [ ] 客户协作持久层 3 张表创建
  - [ ] 低代码 5 张表创建
  - [ ] Flowable 38 张表 DDL 就位
  - [ ] XXL-JOB 16 张表 DDL 就位
  - [ ] integration_config 表扩展字段就位

## 二、后端模块补全

- [x] module-auth 重构：独立 SysUserMapper、AuthUser 聚合根就位
- [x] 多类型用户认证：内部/代理商/客户三类独立认证，Token 载荷含 userType 字段
- [x] AuthController 新增 `/auth/agent/login`、`/auth/customer/login` 端点
- [x] module-collaboration 独立持久层：3 张表 + Entity + Mapper + Service 就位
- [x] CustomerPortalController 增加偏好/订阅/会话管理端点
- [ ] module-report ReportMapper 拆分为 4 个：ProjectReport/DeviceReport/ResourceReport/FinanceReport
- [ ] 对应 XML 拆分为 4 个文件
- [ ] 高并发聚合查询走 ES（验证：报表接口 P95 < 200ms）
- [ ] 明细查询走 MySQL + Caffeine + Redis 二级缓存
- [ ] module-integration 4 个 Adapter 实现：ErpCustomerSync/ImNotification/LogisticsStatus/OaApproval
- [ ] Resilience4j 集成：每个 Adapter 独立熔断/降级/重试/限流配置
- [ ] 领域事件总线：DomainEvent 抽象基类 + DomainEventPublisher 接口就位
- [ ] RabbitMqDomainEventPublisher 实现
- [ ] 15 个领域事件定义完整
- [ ] 各业务模块在关键操作后发布事件验证（grep `publisher.publish` 调用点）
- [ ] module-report 监听事件实时更新 ES 索引验证
- [ ] module-system 通知引擎监听事件触发通知验证
- [ ] 全局异常处理增强：5 级异常分类（Business/Permission/Data/External/System）
- [ ] GlobalExceptionHandler 按异常类型返回不同错误码区间与恢复策略
- [ ] Micrometer Tracing + Zipkin 集成，TraceId 贯穿日志
- [ ] TraceContextFilter 生成 TraceId 并放入 MDC
- [ ] Result<T> 响应体增加 traceId 字段
- [ ] logback-spring.xml 输出 TraceId
- [ ] EasyExcel 通用导出方法 `ExcelUtils.export` 就位
- [ ] 11 个 Controller 增加 export 端点：财务 4 个 + 项目 2 个 + 资源 2 个 + 代理商 2 个 + 设备 3 个

## 三、前端通用组件抽取

- [ ] CrudTable 组件就位：搜索表单 + 分页 + 新增/编辑/删除弹窗 + 表单验证 + 状态反馈 + 异常处理
- [ ] CrudTable 支持自定义 actions、行选择、批量操作
- [ ] CrudTable Vitest 单元测试覆盖率 ≥80%
- [ ] FormModal 组件就位：8 类字段（input/select/date/switch/radio/checkbox/cascader/upload/treeSelect）
- [ ] FormModal 支持字段联动、异步选项加载、表单验证规则
- [ ] FormModal Vitest 覆盖率 ≥80%
- [ ] FileUpload 组件就位：MinIO 预签名 URL 直传 + 图片预览 + 文件列表
- [ ] FileUpload 图片压缩（质量 85%、长边 ≤2048px）+ 缩略图 + 水印（时间+GPS+上传人）
- [ ] FileUpload 大文件分片上传（>10MB 自动分片 5MB）
- [ ] FileUpload 弱网环境本地缓存 + 断点续传
- [ ] RichEditor 富文本组件就位（wangEditor 5）
- [ ] RichEditor 集成 FileUpload 上传图片
- [ ] RichEditor 支持 HTML 输出与纯文本输出
- [ ] OrgTree 组件就位：调用 `/api/v1/orgs/tree` + 搜索 + 展开/折叠 + 节点选择
- [ ] OrgTree 支持嵌入表单作为树选择器
- [ ] Charts 组件就位（基于 ECharts 5 + vue-echarts）
- [ ] 8 个图表子组件：PieChart / LineChart / BarChart / StackedChart / FunnelChart / MapChart / RadarChart / GaugeChart
- [ ] Charts 统一主题（品牌色 #1677FF + 状态色映射）
- [ ] Charts 支持响应式 resize、loading 状态、空数据 EmptyState
- [ ] Gantt 组件就位（基于 dhtmlx-gantt 8.x）
- [ ] Gantt 支持任务拖拽调整时间、拖拽创建依赖关系
- [ ] Gantt 支持视图切换：日/周/月/季/年
- [ ] Gantt 支持自定义任务条颜色（按状态/优先级）、里程碑标记
- [ ] Gantt 暴露事件：onTaskDrag/onTaskSelect/onLinkCreate
- [ ] AMap 组件就位（基于 @amap/amap-jsapi-loader）
- [ ] AMap 配置 VITE_AMAP_KEY 与 VITE_AMAP_SECURITY_CODE
- [ ] AMap 支持标记点、点击弹窗、聚合（markerCluster）
- [ ] MapChart.vue ECharts 地图组件作为 AMap 轻量替代
- [ ] ImportExport 组件就位：导出 Excel + 导入文件 + 模板下载

## 四、前端可视化能力落地

- [ ] `dashboard/my-tasks.vue` Bug 修复：按 role 取 `dashboard.pm.myProjects` 或 `dashboard.engineer.todayTasks`
- [ ] `project/detail.vue` 任务 Tab"新增任务"按钮绑定 FormModal 弹窗
- [ ] `report/project.vue` 导出按钮调用 ImportExport 组件
- [ ] `_placeholder/index.vue` 死代码已删除
- [ ] 项目列表 4 视图切换实现：列表/看板/甘特图/地图
- [ ] 项目看板视图：按状态分列、卡片拖拽跨列流转状态
- [ ] 项目甘特图视图：基于 Gantt 组件、调用 `getProjectGantt` API、拖拽排期
- [ ] 项目地图视图：基于 AMap 组件、按客户所在地渲染、气泡大小代表项目规模
- [ ] 视图切换保留搜索筛选条件
- [ ] 排期日历接入 Gantt 组件（替换原日历网格）
- [ ] 排期支持拖拽调整、冲突检测高亮、请假时段灰色背景
- [ ] 驾驶舱 `report/cockpit.vue` 重写为 ECharts 图表化：4 KPI 卡 + 饼图 + 堆叠折线 + 风险列表 + 仪表盘 + 地图
- [ ] `report/project.vue` ECharts 化：饼图 + 折线 + 柱状 + 明细 + ImportExport
- [ ] `report/device.vue` ECharts 化：饼图 + 柱状 + 趋势折线 + 明细
- [ ] `report/resource.vue` ECharts 化：堆叠柱状 + 雷达 + 地图 + 明细
- [ ] `report/finance.vue` ECharts 化：折线 + 对比柱状 + 漏斗 + 明细
- [ ] 数据大屏 `views/bigscreen/index.vue` 全屏布局就位
- [ ] 数据大屏 6 个区块完整：KPI 区 + 项目分布地图 + 设备仪表盘 + 项目趋势 + 待办滚动 + 风险预警滚动
- [ ] 数据大屏全屏 API + 自动刷新（5 分钟）+ 轮播高亮（30s）
- [ ] 数据大屏暗色主题适配
- [ ] 路由 `/bigscreen` 添加完成（无需 BasicLayout，全屏独立）
- [ ] 独立菜单管理页 `views/system/menu.vue` 就位
- [ ] 菜单树展示（a-tree-table）+ 新增/编辑/删除（目录/菜单/按钮）
- [ ] 菜单拖拽排序 + 关联权限码 + 关联角色
- [ ] 路由 `/system/menu` 添加完成

## 五、响应式与主题统一

- [ ] `src/styles/variables.less` 断点变量定义：@screen-pc/tablet/mobile
- [ ] `src/styles/mixins.less` 响应式 mixin `respond-to(@breakpoint)` 定义
- [ ] 57 个页面响应式适配完成（按阶段五 Task 27 清单逐项验证）：
  - [ ] project 4 个页面
  - [ ] device 5 个页面
  - [ ] resource 4 个页面
  - [ ] delivery 3 个页面
  - [ ] agent 4 个页面
  - [ ] acceptance 4 个页面
  - [ ] finance 4 个页面
  - [ ] report 5 个页面
  - [ ] system 11 个页面
  - [ ] integration 2 个页面
  - [ ] customer H5 8 个页面
  - [ ] agent H5 4 个页面
- [ ] 平板断点（768-1279px）：单列布局、搜索表单折叠抽屉、表格列优先级排序、操作按钮折叠下拉
- [ ] 移动端断点（<768px）：核心场景适配（H5 跳转或简化布局）
- [ ] H5 12 个页面硬编码颜色全部替换为 less 变量
- [ ] H5 硬编码字号全部替换为 less 变量
- [ ] `src/styles/variables.less` 扩展变量完整：颜色 5 梯度 + 间距 8px 网格 + 字号 + 圆角 + 阴影 + 动画曲线
- [ ] Ant Design Vue ConfigProvider 主题 token 覆盖对齐 less 变量
- [ ] `src/styles/utilities.less` 工具类就位（text-primary/bg-stripe/shadow-card/flex-center）
- [ ] grep 检查无残留硬编码颜色/字号/间距

## 六、低代码模块

- [x] 后端 `module-lowcode` 模块创建：目录 + pom.xml + 父 POM 注册
- [x] 5 个 Entity 创建：LowcodeFormConfig/ListConfig/TabConfig/RelationConfig/Template
- [x] 5 个 Mapper + Service + ServiceImpl 创建
- [x] 5 个 Controller 创建：FormConfig/ListConfig/TabConfig/RelationConfig/Template
- [x] 每个 Controller 提供 9 个端点：分页/详情/创建/更新/删除/复制/导出 JSON/导入 JSON/基于模板实例化
- [x] json-schema-validator 集成，导入 JSON 时校验 JSON Schema Draft 7
- [x] `@PreAuthorize("@ss.hasPermi('lowcode:config:*')")` 权限控制生效
- [ ] 前端低代码表单配置器 `views/lowcode/form-config/` 就位
- [ ] 表单配置器三栏布局：左字段库 / 中拖拽画布 / 右属性配置
- [ ] 8 类字段可拖拽到画布生成字段卡片
- [ ] 字段属性配置完整：label/fieldName/required/placeholder/defaultValue/校验规则/选项/联动规则
- [ ] 预览面板实时渲染表单
- [ ] 保存导出 JSON Schema 并调用后端 save 接口
- [ ] 列表配置器 `views/lowcode/list-config/` 就位
- [ ] 列定义配置：字段/标题/对齐/宽/格式化/可排序/可搜索
- [ ] 筛选条件配置：字段/操作符/控件类型/默认值
- [ ] 操作按钮配置：编辑/删除/查看/自定义
- [ ] 列表预览实时渲染
- [ ] 标签页配置器 `views/lowcode/tab-config/` 就位
- [ ] Tab 定义 + Tab 内嵌内容引用（form/list/relation 配置）
- [ ] 布局配置：水平/垂直 Tab、高度、内边距
- [ ] 关联页配置器 `views/lowcode/relation-config/` 就位
- [ ] 主从关联配置 + 级联规则 + 显示字段
- [ ] 模板管理 `views/lowcode/template/` 就位
- [ ] 模板 CRUD + 分类筛选 + 导入 JSON + 导出 JSON + 基于模板实例化
- [ ] 运行时引擎 FormRenderer/ListRenderer/TabRenderer/RelationRenderer 4 个组件就位
- [ ] FormRenderer 支持 schema 驱动渲染、字段联动、字段权限、表单提交
- [ ] ListRenderer 支持 schema 驱动渲染、搜索、分页、操作按钮
- [ ] 动态路由 `/lowcode/render/:configCode` 添加完成
- [ ] 设备巡检表单示例创建并运行时渲染正常
- [ ] 客户回访列表示例创建并运行时渲染正常
- [ ] 项目阶段交付关联页示例创建并运行时渲染正常

## 七、交付部署体系

### 部署文档

- [ ] `docs/deployment/dev-deploy.md` 开发环境部署文档完整
- [ ] `docs/deployment/test-deploy.md` 测试环境部署文档完整
- [ ] `docs/deployment/prod-deploy.md` 生产环境部署文档完整（Nginx/Spring Boot 集群/MySQL 主从/Redis Sentinel/ES 集群/MinIO 分布式/RabbitMQ 集群/XXL-JOB/Flowable/Prometheus/Grafana/ELK）
- [ ] `docs/deployment/rollback.md` 回滚流程文档完整
- [ ] `docs/deployment/monitoring.md` 监控告警配置文档完整
- [ ] `docs/deployment/upgrade.md` 升级指南文档完整

### 自动化脚本

- [ ] `scripts/deploy/dev-up.sh` 一键启动开发环境成功
- [ ] `scripts/deploy/dev-down.sh` 清理开发环境成功
- [ ] `scripts/deploy/test-deploy.sh` 测试环境部署成功（拉镜像 → 滚动更新 → 健康检查 → 烟雾测试 → 通知飞书）
- [ ] `scripts/deploy/prod-deploy.sh` 生产环境蓝绿部署成功（备份 → 部署 → 流量切换 → 监控 → 失败回滚）
- [ ] `scripts/deploy/db-migrate.sh` Flyway 数据库迁移执行成功
- [ ] `scripts/deploy/backup-restore.sh` 备份恢复脚本验证（MySQL + ES + MinIO）
- [ ] `scripts/deploy/health-check.sh` 健康检查脚本验证（5 个核心接口）
- [ ] 所有脚本添加 `set -euo pipefail` 严格模式 + 颜色日志输出

### CI/CD 流水线

- [ ] `.gitlab-ci.yml` 或 `Jenkinsfile` 创建并跑通
- [ ] Pipeline 8 个阶段定义：build/test/scan/image/deploy-test/approval/deploy-prod/notify
- [ ] GitHub Actions workflow `.github/workflows/ci.yml` 作为替代方案就位
- [ ] Maven 依赖缓存 + npm 依赖缓存 + Docker layer 缓存优化生效
- [ ] 后端构建 / 前端构建 / 镜像构建 并行执行验证
- [ ] 失败重试机制（网络相关步骤 3 次）生效
- [ ] 推送 main 分支自动触发流水线验证

### 用户引导系统

- [ ] `driver.js` 依赖引入
- [ ] `src/components/UserGuide/GuideOverlay.vue` 引导浮层组件就位
- [ ] `src/utils/guide-steps.ts` 5 个角色化引导步骤定义（director 5 步/pm 5 步/engineer 4 步/agent 4 步/customer 3 步）
- [ ] `src/components/UserGuide/FirstLoginModal.vue` 首次登录浮层就位（功能概览 + 5 张角色化卡片）
- [ ] `src/components/UserGuide/HelpDrawer.vue` 页面级帮助抽屉就位
- [ ] userStore 添加 `hasSeenGuide` 字段（localStorage 持久化）
- [ ] BasicLayout Header 帮助按钮触发 HelpDrawer
- [ ] 首次登录引导浮层自动弹出验证
- [ ] 角色化引导步骤分步执行验证

### 技术支持体系

- [ ] `docs/support/faq.md` FAQ 30+ 条，按角色分组
- [ ] `docs/support/troubleshooting.md` 故障排查指南完整
- [ ] `src/components/Feedback/FeedbackDrawer.vue` 问题反馈抽屉就位（标题/类型/优先级/截图上传/描述）
- [ ] 后端 `/api/v1/feedback` 接收反馈并投递到 GitLab Issue 或飞书工单
- [ ] BasicLayout Header 反馈按钮触发 FeedbackDrawer
- [ ] `docs/api/knife4j-export.yaml` OpenAPI 3 规范导出
- [ ] `docs/api/api-contract.md` 前后端 API 契约文档完整

## 八、测试与验证

### 后端单元测试

- [ ] Jacoco 集成 + Maven 插件 + 覆盖率规则 ≥80% 配置
- [ ] module-project 单测：项目状态机、任务派发、模板实例化覆盖率 ≥80%
- [ ] module-device 单测：设备状态机非法路径全覆盖
- [ ] module-resource 单测：排期冲突检测、智能派单算法
- [ ] module-delivery 单测：GPS 签到校验、施工步骤跟踪
- [ ] module-agent 单测：转包任务状态机、代理商数据权限隔离
- [ ] module-acceptance 单测：验收流程、签核、会签
- [ ] module-finance 单测：利润计算、工作量确认
- [ ] module-lowcode 单测：Schema 校验、模板实例化
- [ ] module-auth 单测：多类型用户认证、Token 续签
- [ ] 单元测试整体覆盖率 ≥80%（Jacoco 报告验证）

### 后端集成测试

- [ ] Testcontainers 集成（MySQL/Redis/ES/RabbitMQ/MinIO 容器化测试）
- [ ] Flowable 流程引擎集成测试：启动/审批/会签/回退/撤回
- [ ] XXL-JOB 调度集成测试：任务触发/失败重试/集群分片
- [ ] ES 检索集成测试：索引创建/增量同步/聚合查询
- [ ] 数据权限隔离测试：4 个角色数据边界
- [ ] 领域事件总线测试：事件投递/消费/幂等性
- [ ] 熔断降级测试：模拟外部依赖不可用，验证 Resilience4j 熔断

### 前端测试

- [ ] 通用组件 Vitest 测试：CrudTable/FormModal/FileUpload/Charts/Gantt/AMap
- [ ] 低代码渲染器测试：FormRenderer/ListRenderer/TabRenderer/RelationRenderer
- [ ] Playwright E2E 框架引入
- [ ] E2E 核心流程 10 个场景全部通过：
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

### 性能测试

- [ ] k6 性能测试框架引入
- [ ] 5 个核心接口压测脚本就位：登录/项目列表/ES 检索/报表聚合/文件上传
- [ ] 性能基线达成：
  - [ ] 登录接口：1000 并发，P95 < 500ms
  - [ ] 项目列表查询：500 并发，P95 < 200ms
  - [ ] 设备 ES 检索：500 并发，P95 < 200ms
  - [ ] 报表聚合查询：100 并发，P95 < 1s
  - [ ] 文件上传：50 并发，5MB 文件，P95 < 5s
- [ ] p6spy 慢 SQL 日志启用 + 慢 SQL 报警
- [ ] JVM 性能调优文档完整（堆内存/GC 策略/线程池）

### 异常处理专项测试

- [ ] 网络超时场景：前端 loading + 超时提示验证
- [ ] 并发冲突场景：乐观锁冲突验证
- [ ] 数据校验失败场景：必填/格式/长度验证
- [ ] 权限拒绝场景：403 + 数据权限越权拦截
- [ ] 外部依赖不可用场景：熔断降级验证
- [ ] 弱网环境：H5 文件上传断点续传验证

## 九、联调与最终验收

- [ ] Knife4j 接口文档更新导出
- [ ] 全模块主链路联调通过：
  - [ ] 立项 → 计划 → 派单 → 执行 → 验收 → 结项（含 Flowable 审批）
  - [ ] 设备入库 → 发运 → 到货 → 安装 → 在网（含 ES 同步）
  - [ ] 代理商转包 → 交付 → 审核（含会签）
  - [ ] 库存预警定时任务触发 → 通知（XXL-JOB）
  - [ ] 低代码配置 → 运行时渲染 → 业务接入
  - [ ] 数据大屏全屏展示
  - [ ] 用户引导首次登录
- [ ] 接口契约对齐检查：前端 API 类型与后端 VO 字段一致性
- [ ] 全部 checklist 验证通过
- [ ] 部署文档评审（dev/test/prod 三套）
- [ ] 用户引导视频教程占位（5 个角色各 1 个）
- [ ] 技术支持体系试运行（FAQ + 反馈入口验证）
- [ ] 性能基线报告（核心接口 P95 延迟）
- [ ] Git tag 标记版本 v2.0.0

## 十、行业最佳实践核对

### 功能模块完善度

- [ ] 各功能模块达到行业领先水平，包含完整功能点清单
- [ ] 业务逻辑流程图与状态转换机制清晰（项目/设备/转包/验收/割接 5 个状态机文档化）
- [ ] 全面的异常处理策略：错误捕获、错误提示、错误恢复（5 级异常分类 + 熔断降级 + 链路追踪）
- [ ] 完整的功能测试用例和验证标准（单元测试 ≥80% + 集成测试 + E2E + 性能测试 + 异常专项测试）

### 界面设计与用户体验

- [ ] 界面美观度符合现代 UI 设计标准（ECharts 图表化 + 数据大屏 + 品牌设计系统统一）
- [ ] 设计风格一致性与品牌统一性（less 变量扩展 + ConfigProvider 主题对齐 + H5 变量替换）
- [ ] 用户体验流程优化（4 视图切换 + 通用组件抽取减少重复代码 + 用户引导系统）
- [ ] 响应式设计（PC≥1280px / 平板 768-1279px / 手机<768px 三档断点全面覆盖）

### 系统架构与可扩展性

- [ ] 模块化设计和松耦合架构（module-lowcode 独立 + module-auth 解耦 + 领域事件总线）
- [ ] 可扩展接口和组件（Flowable 流程定义可扩展 + 低代码 Schema 驱动 + 通用组件可复用）
- [ ] 模块间通信机制清晰（领域事件总线 + RabbitMQ 投递 + 15 个领域事件）
- [ ] 高内聚低耦合（module-report 拆分 + module-integration Adapter 模式）

### 交付与部署标准

- [ ] 部署流程文档和自动化部署脚本（dev/test/prod 三套 + 8 个脚本）
- [ ] 用户引导系统（首次登录 + 角色化教程 + 页面级帮助）
- [ ] 技术支持体系和问题反馈机制（FAQ + 故障排查 + FeedbackDrawer）
- [ ] 交付过程无缝高效（CI/CD 流水线 + 蓝绿部署 + 监控告警）

### 低代码模块实现

- [ ] 通用内容界面配置功能支持可视化配置（4 类配置器）
- [ ] 表单/列表/标签页/关联页基础组件配置能力
- [ ] 配置项导入导出功能（JSON Schema Draft 7 校验）
- [ ] 模板复用（基于 templateId 实例化）
- [ ] 低代码配置生成界面符合设计与体验标准（运行时引擎 + 业务接入 3 个示例）
