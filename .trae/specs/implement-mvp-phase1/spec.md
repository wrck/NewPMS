# 网络设备实施项目管理系统 — MVP（Phase 1）实施规格

> 来源：`系统设计文档.md`（V1.0）中 Phase 1（MVP · 2-3 个月）范围
> change-id：`implement-mvp-phase1`

## Why

原厂（卖方）目前缺乏统一的实施项目管理体系：自有工程师派单靠手工、设备到货安装进度靠表格跟踪、代理商转包靠邮件沟通、客户验收靠线下签纸。需要一个覆盖「立项→规划→执行→验收→结项」全生命周期的项目管理系统，打通原厂工程师、代理商、客户三方在线协作。

本 spec 聚焦 **MVP（Phase 1）**，先把核心主链路跑通：项目与任务管理、设备台账与出入库、工程师派单、移动端现场作业、代理商转包与交付、客户轻量进度查看、IM 通知、认证权限。Phase 2/3（割接、验收签核、预配、财务结算、外部系统集成、数据大屏等）作为后续独立 spec delta 推进。

## What Changes

### 新增（从零搭建）

**基础设施层**
- 搭建后端 Maven 多模块工程（Spring Boot 3.2.x + Java 17 + MyBatis-Plus），按限界上下文划分 12 个业务模块 + common 公共模块
- 搭建前端 PC 工程骨架（Vue 3.4 + TypeScript + Vite 5 + Ant Design Vue 4 + Pinia + Vue Router + Axios）
- 搭建前端移动端 H5 工程骨架（同上技术栈，移动端核心页面单独优化）
- 搭建客户/代理商外部入口 H5（精简版）
- 数据库初始化：MySQL 8.0，按设计文档规范建表（雪花算法主键、公共字段、逻辑删除、乐观锁）
- 公共能力：统一响应体、全局异常处理、错误码体系、MyBatis-Plus 自动填充、逻辑删除、JWT 认证、RBAC 权限、MyBatis 数据权限拦截器、MinIO 文件服务、Redis 缓存
- 开发环境 Docker Compose（vibe-server / vibe-web / mysql / redis / minio / rabbitmq）

**认证与权限**
- 账号密码登录（PC 8h / 移动端 7d Token）、Token 自动续期、强制下线（黑名单）
- RBAC 内部角色（SUPER_ADMIN/DIRECTOR/PM/DISPATCHER/ENGINEER/DEVICE_ADMIN/FINANCE）+ 外部角色（AGENT_ADMIN/AGENT_ENGINEER/CUSTOMER）
- 客户手机号 + 短信验证码临时 Token（2h）
- 注解式接口权限 `@PreAuthorize` + MyBatis 拦截器行级数据权限

**项目管理模块**
- 项目立项（手动创建 / 选择模板生成阶段与任务 / 选择执行模式 SELF/AGENT/MIXED / 关联客户 / 指定 PM / 优先级 P0-P3）
- 项目计划（阶段增删改、任务分解、里程碑、甘特图拖拽排期、依赖关系、交付物清单）
- 项目执行（任务派发、进度更新、进度自动同步、进度预警、沟通记录）
- 项目状态机 INIT→PLAN→EXECUTE→ACCEPT→CLOSE→ARCHIVED（含 ON_HOLD/CANCELLED）
- 项目视图（列表/看板/甘特图/详情页 Tab）
- 项目结项检查与归档
- 风险与问题登记跟踪

**设备资产管理模块**
- 设备型号库 CRUD（产品线分类、规格、配置模板）
- 项目 BOM（手动维护、BOM 变更、按型号维度进度查看）
- 设备实例管理（SN/MAC 录入、Excel 批量导入、状态流转、历史轨迹、搜索）
- 设备状态机 IN_FACTORY→SHIPPED→RECEIVED→PRE_CONFIG→INSTALLED→DEBUGGED→ONLINE（含 DAMAGED/LOST/RETURNED/REPAIR/REPLACED/EOL）
- 设备出入库（入库、出库领用、退库、调拨、库存台账、库存预警）
- 设备状态看板（项目维度完成率、状态分布、异常设备）
- 备件管理基础（入库/领用/归还/返修/台账）

**资源调度模块**
- 工程师资源池档案（技能标签、认证资质、区域、在职状态）
- 排期管理（日历视图、负荷热力图、冲突检测、请假/培训时间块）
- 任务派发（手动指派、批量派单、智能推荐、转派、退回、紧急调配）
- 工时管理（填报、出差/加班统计、PM 审批、人天统计）

**代理商管理模块**
- 代理商企业档案（信息、合作区域、产品线、资质文件、合作状态）
- 代理商工程师管理（信息、技能、启用停用、项目历史）
- 转包管理（创建转包任务、代理商接单/拒绝、指派工程师、状态跟踪、退回重新指派）
- 转包任务状态机 PENDING→ACCEPTED→IN_PROGRESS→SUBMITTED→CONFIRMED（含 REJECTED/RETURNED/OVERDUE）
- 代理商交付管控（进度上报、交付物提交施工照片+测试记录+签收单、PM 审核、退回整改）
- 代理商数据权限边界（仅本公司任务、屏蔽客户/合同/成本敏感数据）

**交付管理模块（移动端）**
- 现场作业：移动端签到（GPS 定位校验 + 拍照防作弊）、施工步骤跟踪（按步标记+拍照+耗时）、施工拍照（时间+GPS 水印、按步骤归类、批量上传弱网优化）、异常问题上报（自动通知 PM）、工作完成确认（工程师标记→PM 确认）
- 客户协作通道（轻量）：H5 进度查看（手机号验证登录）

**消息通知**
- 通知引擎（业务事件→RabbitMQ→通知服务→飞书/钉钉/站内信）
- 通知模板渲染、渠道路由、频率控制
- Phase 1 MVP 通知模板：TASK_ASSIGNED / TASK_REMINDER / TASK_OVERDUE / DELIVERABLE_REVIEW / DELIVERABLE_RETURNED / DELIVERABLE_CONFIRMED / DEVICE_ARRIVED / DEVICE_ABNORMAL / RISK_WARNING

**系统管理模块**
- 用户管理、角色权限管理、菜单/按钮权限、数据权限配置
- 组织架构（部门树、岗位）
- 数据字典（项目类型/产品线/区域/设备类别/任务类型等）
- 系统配置（通知模板、项目模板）
- 日志管理（操作日志、登录日志、集成调用日志）
- 消息中心（站内信列表、已读未读、接收偏好）

**报表分析（基础）**
- 工作台首页（按角色差异化：总监/PM/工程师/代理商）
- 管理驾驶舱核心指标卡片 + 项目阶段分布 + 近 12 月趋势 + 风险项目列表

### 暂不在本 spec 范围（留待后续 spec delta）
- 割接管理全流程（Phase 2）
- 验收管理 + 客户在线签核（Phase 2）
- 设备预配与配置模板管理（Phase 2）
- CRM/ERP/NMS/物流/OA 集成（Phase 2/3）
- 代理商工作量确认与结算、财务核算（Phase 2）
- 完整报表与数据大屏（Phase 2/3）
- 设备拓扑、GIS 地图可视化、移动端离线模式（Phase 3）
- 验收签核通知模板（ACCEPTANCE_SIGN / ACCEPTANCE_SIGNED）、割接审批通知模板（CUTOVER_*）、工作量确认模板（WORKLOAD_CONFIRM）——随对应功能在 Phase 2 引入

## Impact
- 受影响规格：本系统为全新构建，无既有 spec
- 受影响代码：
  - 后端：`vibe-server/`（Maven 多模块，含 module-auth/common/project/device/resource/delivery/agent/acceptance-finance-collaboration-integration-report-system，其中 Phase 2/3 模块先建空骨架占位）
  - 前端 PC：`vibe-web/`
  - 前端移动端：`vibe-mobile/`
  - 外部入口 H5：`vibe-portal/`（客户 + 代理商）
  - 数据库：`vibe_db`（MySQL 8.0）
  - 部署：`docker-compose.yml`

## ADDED Requirements

### Requirement: 项目立项与状态机
系统 SHALL 支持手动创建项目并选择项目模板自动生成阶段与任务，记录项目基本信息（编号、名称、客户、类型、产品线、执行模式、优先级、PM、区域、合同号、计划周期）。

项目编号 SHALL 按 `PRJ-YYYYMM-XXX` 规则自动生成且唯一。

项目 SHALL 遵循状态机：INIT→PLAN→EXECUTE→ACCEPT→CLOSE→ARCHIVED，任意阶段可标记 ON_HOLD 或 CANCELLED；状态流转由对应触发条件驱动（PM 完成基本信息→PLAN；计划制定完成开始派单→EXECUTE；所有实施任务完成→ACCEPT；终验通过客户签核→CLOSE；复盘归档→ARCHIVED）。

#### Scenario: 手动立项
- **WHEN** PM 填写项目基本信息并提交
- **THEN** 系统生成唯一项目编号，项目进入 INIT 状态，关联客户与 PM，记录创建人与创建时间

#### Scenario: 选择模板生成阶段与任务
- **WHEN** PM 在立项时选择项目模板
- **THEN** 系统按模板自动生成阶段（SURVEY/DESIGN/DELIVER/INSTALL/DEBUG/ACCEPT）与对应任务，可在此基础上增删改

#### Scenario: 非法状态流转被拒绝
- **WHEN** 项目处于 ARCHIVED 状态时尝试流转到 EXECUTE
- **THEN** 系统返回 409xx 业务冲突错误，状态保持不变

### Requirement: 项目计划与甘特图
系统 SHALL 支持阶段规划（增删改、时间范围）、任务分解（阶段下子任务、支持父子任务）、里程碑设定、甘特图拖拽排期、任务依赖关系设置、交付物清单定义。

#### Scenario: 甘特图拖拽调整排期
- **WHEN** PM 在甘特图视图拖拽任务条调整时间
- **THEN** 系统更新任务计划开始/结束日期，并校验依赖关系（前置任务未完成则后续任务开始日期不能早于前置结束日期）

#### Scenario: 依赖冲突检测
- **WHEN** 任务 B 设置依赖任务 A，但 B 的计划开始日期早于 A 结束日期
- **THEN** 系统提示依赖冲突并阻止保存

### Requirement: 任务派发与执行模式
系统 SHALL 支持任务派发给自有工程师（execute_mode=SELF，填充 assignee_id）或转包给代理商（execute_mode=AGENT，创建 outsource_task 记录），支持批量派单、转派、退回。

#### Scenario: 派发给自有工程师
- **WHEN** PM 选择任务并指派工程师
- **THEN** 任务 execute_mode=SELF，assignee_id 填充，状态 PENDING→ASSIGNED，触发 TASK_ASSIGNED 通知

#### Scenario: 转包给代理商
- **WHEN** PM 选择任务并转包给代理商公司
- **THEN** 任务 execute_mode=AGENT，创建 outsource_task 记录（状态 PENDING），触发代理商接单通知

### Requirement: 设备全生命周期状态机
设备实例 SHALL 遵循状态机：IN_FACTORY→SHIPPED→RECEIVED→PRE_CONFIG→INSTALLED→DEBUGGED→ONLINE，并支持异常分支 DAMAGED/LOST/RETURNED/REPAIR/REPLACED/EOL。每次状态变更 SHALL 记录到 device_status_log（含变更前/后状态、操作人、时间、备注）。

#### Scenario: 设备出库发运
- **WHEN** 设备管理员对在库设备执行出库分配到项目
- **THEN** 设备状态 IN_FACTORY→SHIPPED，project_id 填充，记录状态日志

#### Scenario: 非法状态流转
- **WHEN** 对 IN_FACTORY 状态设备尝试直接标记 ONLINE
- **THEN** 系统返回 409xx 错误，状态保持 IN_FACTORY

### Requirement: 设备 SN 唯一性与批量导入
设备实例的 serial_number SHALL 全局唯一，mac_address 可选。系统 SHALL 支持 Excel 批量导入 SN 清单（型号、SN、MAC、固件版本），导入前校验 SN 重复，重复行跳过并输出错误清单。

#### Scenario: 导入存在重复 SN
- **WHEN** Excel 中某 SN 已存在数据库
- **THEN** 该行跳过不导入，导入结果报告标注「SN 已存在」，其余有效行正常导入

### Requirement: 设备出入库与库存台账
系统 SHALL 支持入库登记（到货签收→录入 SN→入库）、出库领用（分配项目→出库）、退库归还、调拨（仓库间/项目间）、库存盘点、库存台账（各仓库各型号数量）、库存预警（低于安全库存告警）。

#### Scenario: 库存低于安全库存
- **WHEN** 某仓库某型号库存数量低于配置的安全库存阈值
- **THEN** 系统生成库存预警，通知设备管理员

### Requirement: 工程师排期与冲突检测
系统 SHALL 提供工程师日历视图（周/月）、负荷热力图，对同一工程师同一时段分配多个任务时 SHALL 检测冲突并告警，请假期间自动标记不可分配。

#### Scenario: 排期冲突告警
- **WHEN** 调度员将任务分配给某工程师，但该工程师该时段已有任务
- **THEN** 系统提示时间冲突，允许强制分配但标记冲突

### Requirement: 移动端现场签到与施工跟踪
工程师 SHALL 通过移动端 GPS 定位签到（校验是否在客户现场范围内）+ 拍照签到（防作弊），按任务配置的标准施工步骤逐项完成（标记+拍照+耗时），施工照片自动添加时间+GPS 水印并按步骤归类，支持弱网批量上传。

#### Scenario: GPS 不在允许范围
- **WHEN** 工程师签到时 GPS 定位距客户现场超出允许范围
- **THEN** 系统提示「不在客户现场范围」，阻止签到，记录尝试位置

#### Scenario: 弱网环境上传
- **WHEN** 工程师在弱网环境拍照上传
- **THEN** 照片先本地缓存，网络恢复后自动续传，不阻塞施工步骤继续操作

### Requirement: 代理商转包与交付物审核
PM 可创建转包任务（指定代理商、任务范围、截止日期、附加文档），代理商接单/拒绝后指派工程师执行，代理商通过移动端提交交付物（施工照片必传、测试记录必传、签收单必传），PM 审核通过则任务完成、退回则代理商补充后重新提交。

#### Scenario: 交付物审核退回
- **WHEN** PM 审核代理商交付物发现不达标，填写退回原因
- **THEN** 转包任务状态 SUBMITTED→RETURNED→IN_PROGRESS，submit_count +1，通知代理商

#### Scenario: 代理商数据权限隔离
- **WHEN** 代理商 A 登录查询转包任务列表
- **THEN** 系统仅返回 agent_company_id = A 的任务，绝不返回其他代理商任务或项目完整敏感信息（客户/合同/成本）

### Requirement: RBAC 权限与数据权限
系统 SHALL 采用 RBAC + 数据权限模型。接口权限通过 `@PreAuthorize` 注解控制；行级数据通过 MyBatis 拦截器自动拼接 WHERE 条件（PM 仅自己项目、ENGINEER 仅分配给自己的任务、AGENT_ADMIN 仅本公司数据、CUSTOMER 仅关联项目只读）。

#### Scenario: 工程师只能看自己的任务
- **WHEN** ENGINEER 角色用户查询任务列表
- **THEN** 系统自动追加 `WHERE assignee_id = 当前用户ID`，仅返回分配给该工程师的任务

#### Scenario: 代理商数据自动隔离
- **WHEN** AGENT_ADMIN 查询转包任务
- **THEN** 系统自动追加 `WHERE agent_company_id = 当前代理商ID`

### Requirement: 多端认证与 Token 机制
PC 管理后台账号密码登录 Token 有效期 8h，移动端 7d；Token 距过期 < 2h 时自动续签；强制下线通过 Redis Token 黑名单实现；客户入口手机号+短信验证码临时 Token 有效期 2h。

#### Scenario: Token 自动续签
- **WHEN** 请求携带的 Token 距过期不足 2h
- **THEN** 系统签发新 Token 并通过响应头返回，旧 Token 在原有效期前继续有效

#### Scenario: 强制下线
- **WHEN** 管理员对某用户执行强制下线
- **THEN** 该用户 Token 加入 Redis 黑名单，后续请求返回 401xx

### Requirement: 通知引擎与模板渲染
业务事件通过 RabbitMQ 投递到通知服务，通知服务按模板编码渲染内容（变量替换），根据接收人类型与事件类型选择渠道（内部人员飞书/钉钉+站内信、代理商飞书/钉钉+站内信不暴露内部信息、客户短信/邮件），并做频率控制避免短时重复通知。

#### Scenario: 任务派发通知
- **WHEN** PM 派发任务给工程师
- **THEN** 系统投递 TASK_ASSIGNED 事件，通知服务渲染模板并通过飞书+站内信触达执行人，飞书卡片含项目/任务/地点/截止/派单人信息与「查看详情」「确认接单」按钮

### Requirement: 工作台与角色差异化首页
登录后首页 SHALL 按角色差异化展示：总监（全局总览+审批待办）、PM（我的项目+待派单+待审核+进度概览）、工程师（任务列表+今日待办+工时入口）、代理商（分配任务+待提交交付物）。首页含待办事项、核心指标卡片、我负责的项目进度、近期任务、最近动态时间线。

#### Scenario: 工程师首页
- **WHEN** ENGINEER 角色用户登录
- **THEN** 首页展示「今日任务」卡片列表、「本周工时」「本月出差」统计、工时填报入口，不显示项目全局数据

### Requirement: 文件存储与图片处理
文件存储采用 MinIO（S3 兼容），按 Bucket 分区（photos/documents/attachments/avatars/exports）。施工照片上传时自动压缩（质量 85%、长边≤2048px）、生成缩略图、添加水印（时间+GPS+上传人）。PC/移动端通过预签名 URL 直传 MinIO，大文件分片上传。

#### Scenario: 施工照片自动加水印
- **WHEN** 工程师移动端上传施工照片
- **THEN** 系统在图片右下角添加「时间+GPS 坐标+上传人」水印，生成缩略图供列表展示，原图与缩略图分别存储到 vibe-photos Bucket

### Requirement: 统一响应与错误码
所有接口 SHALL 返回统一响应体 `{code, message, data, timestamp}`，分页响应 data 含 `{records, total, page, size, pages}`。错误码按区间划分：200 成功、400xx 参数校验、401xx 认证、403xx 权限、404xx 资源不存在、409xx 业务冲突、500xx 系统错误、502xx 外部调用失败。全局异常处理器统一捕获 BusinessException/MethodArgumentNotValidException/AccessDeniedException/Exception。

#### Scenario: 参数校验失败
- **WHEN** 接口入参 `@Valid` 校验失败（如项目名称为空）
- **THEN** 返回 `{code: 40001, message: "项目名称不能为空", errors: [{field:"name", message:"项目名称不能为空"}], timestamp}`

#### Scenario: 业务冲突
- **WHEN** 对已归档项目执行编辑操作
- **THEN** 返回 409xx 业务冲突错误码，message 说明冲突原因

### Requirement: 数据库设计规范
所有表 SHALL 包含公共字段（id 雪花算法 BIGINT 主键、create_by、create_time、update_by、update_time、deleted 逻辑删除）。表名/字段名小写下划线，外键命名 `{关联表}_id`，索引 `idx_表名_字段`，唯一索引 `uk_表名_字段`。MyBatis-Plus 自动填充审计字段，`@TableLogic` 逻辑删除，关键业务表 `@Version` 乐观锁。

#### Scenario: 乐观锁并发控制
- **WHEN** 两个请求并发更新同一项目（version=5）
- **THEN** 第一个请求成功（version→6），第二个请求因 version 不匹配返回乐观锁冲突错误
