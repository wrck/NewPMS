# Tasks — MVP（Phase 1）实施任务清单

> change-id：`implement-mvp-phase1`
> 任务按依赖顺序排列，标注 [P] 表示可与其他无依赖任务并行

---

## 阶段一：基础设施与工程骨架

- [x] Task 1: 初始化后端 Maven 多模块工程骨架
  - [ ] SubTask 1.1: 创建父 pom（Spring Boot 3.2.x / Java 17 / 统一依赖版本管理）
  - [ ] SubTask 1.2: 创建 module-common（工具类、常量、异常、基类、统一响应、全局异常处理器、MyBatis-Plus 配置、Redis 配置、MinIO 配置、雪花算法配置）
  - [ ] SubTask 1.3: 创建 module-auth（认证授权模块）
  - [ ] SubTask 1.4: 创建 module-project / module-device / module-resource / module-delivery / module-agent / module-system / module-report 业务模块骨架（controller/service/mapper/entity/dto/vo/bo/enums/constant/event 包结构）
  - [ ] SubTask 1.5: 创建 module-acceptance / module-finance / module-collaboration / module-integration 空骨架占位（Phase 2/3）
  - [ ] SubTask 1.6: 配置 application.yml（多环境 profile：dev/test/staging/prod）、logback、Knife4j 接口文档

- [x] Task 2: 初始化前端 PC 工程骨架 [P]
  - [ ] SubTask 2.1: Vite 5 + Vue 3.4 + TypeScript 脚手架，集成 Ant Design Vue 4 / Pinia / Vue Router / Axios
  - [ ] SubTask 2.2: 定制主题（品牌色 #1677FF、状态色映射）、全局样式（间距 8px 网格、字体、圆角、阴影）
  - [ ] SubTask 2.3: 实现全局布局（Header 面包屑 + 可折叠 Sider 菜单 + Content）、路由守卫、Axios 拦截器（统一处理响应体/401 跳登录/Token 续签）
  - [ ] SubTask 2.4: 封装通用组件（状态 Tag、统计卡片 StatisticCard、进度条、空状态、表格 CRUD 模板）

- [x] Task 3: 初始化前端移动端 H5 工程骨架 [P]
  - [ ] SubTask 3.1: 同技术栈脚手架，移动端适配（postcss-px-to-viewport / rem 方案）
  - [ ] SubTask 3.2: 底部 Tab 栏（首页/任务/现场/我的）、移动端 Axios 拦截器、Token 持久化
  - [ ] SubTask 3.3: 封装移动端组件（GPS 定位、拍照/相册、弱网上传队列、离线缓存）

- [x] Task 4: 初始化外部入口 H5（客户+代理商精简版）[P]
  - [ ] SubTask 4.1: 客户 H5 入口骨架（手机号+验证码登录、项目进度查看、割接审批占位、文档下载）
  - [ ] SubTask 4.2: 代理商 H5/PC 精简版骨架（账号登录、任务列表、交付物提交、结算查看占位）

- [x] Task 5: 数据库设计与初始化脚本
  - [ ] SubTask 5.1: 按设计文档建立全部 MVP 表结构 DDL（公共字段、索引、唯一约束），含 project / project_phase / project_task / project_milestone / project_change_log / project_risk / project_issue / project_member / project_comment / project_template / project_template_phase / project_template_task / customer
  - [ ] SubTask 5.2: 设备相关表 device_model / device_instance / device_bom / device_inventory_log / device_config_history / device_status_log / warehouse / spare_part / spare_part_log
  - [ ] SubTask 5.3: 资源相关表 engineer / engineer_skill / engineer_schedule / engineer_leave / work_order / work_order_step / work_order_photo / work_order_issue / timesheet
  - [ ] SubTask 5.4: 代理商相关表 agent_company / agent_engineer / outsource_task / outsource_deliverable / outsource_workload / agent_score_log
  - [ ] SubTask 5.5: 系统相关表 sys_user / sys_role / sys_user_role / sys_menu / sys_role_menu / sys_org / sys_position / sys_dict_type / sys_dict_data / sys_config / sys_log / sys_notice / sys_notice_template
  - [ ] SubTask 5.6: 初始化数据脚本（默认超管、内置角色与菜单、数据字典、默认项目模板）

- [x] Task 6: 开发环境 Docker Compose [P]
  - [ ] SubTask 6.1: docker-compose.yml（mysql:8.0 / redis:7 / minio / rabbitmq:3.12 / vibe-server / vibe-web）
  - [ ] SubTask 6.2: 各中间件初始化配置（MySQL 字符集 utf8mb4、Redis 持久化、MinIO 默认 Bucket 创建、RabbitMQ vhost）

---

## 阶段二：公共能力与认证权限

- [x] Task 7: module-common 公共能力落地（由 Task 1 骨架一并交付）
  - [ ] SubTask 7.1: 统一响应体 Result<T> / PageResult<T>、错误码枚举（按区间）、BusinessException
  - [ ] SubTask 7.2: @RestControllerAdvice 全局异常处理（BusinessException / MethodArgumentNotValidException / AccessDeniedException / ConstraintViolationException / Exception）
  - [ ] SubTask 7.3: MyBatis-Plus 配置（自动填充 MetaObjectHandler create_by/create_time/update_by/update_time、逻辑删除 @TableLogic、乐观锁 @Version、分页插件、雪花算法）
  - [ ] SubTask 7.4: Redis 配置、Redisson、缓存工具类、Key 命名规范工具
  - [ ] SubTask 7.5: MinIO 配置、文件上传/下载/预签名 URL 工具、图片压缩+缩略图+水印工具（基于 Thumbnailator / imgscalr）
  - [ ] SubTask 7.6: MapStruct 配置、Hutool/Lombok 集成、EasyExcel 工具类

- [x] Task 8: module-auth 认证授权（骨架由 Task 1 交付，login 接入与数据权限拦截器由 Task 9 一并完成）
  - [x] SubTask 8.1: Spring Security + JWT 配置、登录接口（账号密码，区分 PC 8h / 移动端 7d Token 有效期）
  - [x] SubTask 8.2: JWT 签发/解析/校验过滤器、Token 自动续期（距过期<2h 续签）、Redis Token 黑名单与强制下线
  - [x] SubTask 8.3: 客户手机号+短信验证码登录临时 Token（2h）、短信发送适配器接口（MVP 可用日志/控制台模拟）
  - [x] SubTask 8.4: RBAC 权限：@PreAuthorize 注解鉴权、菜单/按钮权限、数据权限 MyBatis 拦截器（PM 自己项目 / ENGINEER 自己任务 / AGENT_ADMIN 本公司 / CUSTOMER 关联项目只读）
  - [x] SubTask 8.5: 当前登录用户上下文（ThreadLocal / RequestScope），权限缓存 Redis（vibe:auth:perm:{userId}）

- [x] Task 9: module-system 系统管理
  - [ ] SubTask 9.1: 用户管理 CRUD、角色分配、状态启停、密码重置（BCrypt）
  - [ ] SubTask 9.2: 角色权限管理、菜单/按钮权限分配、数据权限配置
  - [ ] SubTask 9.3: 组织架构（部门树、岗位）
  - [ ] SubTask 9.4: 数据字典（类型+数据 CRUD，缓存到 Redis）
  - [ ] SubTask 9.5: 系统配置（通知模板、项目模板管理）
  - [ ] SubTask 9.6: 日志管理（操作日志 AOP 切面 @OperationLog、登录日志、查询接口）
  - [ ] SubTask 9.7: 消息中心（站内信列表、已读未读、接收偏好设置）

---

## 阶段三：核心业务模块（按依赖顺序）

- [x] Task 10: module-project 项目管理
  - [ ] SubTask 10.1: 实体/Mapper/Service：project / project_phase / project_task / project_milestone / project_member / project_comment
  - [ ] SubTask 10.2: 项目立项接口（手动创建、选择模板生成阶段与任务、项目编号 PRJ-YYYYMM-XXX 生成器、关联客户/PM/执行模式/优先级）
  - [ ] SubTask 10.3: 项目状态机（INIT→PLAN→EXECUTE→ACCEPT→CLOSE→ARCHIVED + ON_HOLD/CANCELLED，流转校验，乐观锁）
  - [ ] SubTask 10.4: 项目计划接口（阶段增删改、任务分解父子任务、里程碑、依赖关系校验、交付物清单）
  - [ ] SubTask 10.5: 项目执行接口（任务派发、进度更新、进度自动同步、进度预警定时任务、沟通记录评论）
  - [ ] SubTask 10.6: 变更管理（变更申请、影响评估、审批流占位、执行记录）
  - [ ] SubTask 10.7: 风险与问题（登记、状态流转、超期自动升级定时任务）
  - [ ] SubTask 10.8: 项目结项检查（验收完成/文档归档/费用结算校验）、复盘记录、归档
  - [ ] SubTask 10.9: 项目查询接口（列表多维度筛选排序、看板分组、甘特图数据、地图视图数据、详情聚合）
  - [ ] SubTask 10.10: 项目模板管理（模板/模板阶段/模板任务 CRUD）

- [x] Task 11: module-device 设备资产管理
  - [ ] SubTask 11.1: 实体/Mapper/Service：device_model / device_instance / device_bom / device_inventory_log / device_status_log / warehouse / spare_part / spare_part_log
  - [ ] SubTask 11.2: 设备型号库 CRUD（产品线分类、规格 JSON、配置模板、手册链接）
  - [ ] SubTask 11.3: 设备实例管理（SN 唯一校验、单条录入、Excel 批量导入含错误报告、详情、编辑、搜索）
  - [ ] SubTask 11.4: 设备状态机（IN_FACTORY→...→ONLINE 及异常分支，流转校验，状态日志记录）
  - [ ] SubTask 11.5: 项目 BOM（手动维护、BOM 变更、按型号维度 to货/安装/验收数量统计）
  - [ ] SubTask 11.6: 出入库管理（入库/出库领用/退库/调拨/盘点，库存台账聚合查询，库存预警定时任务+通知）
  - [ ] SubTask 11.7: 备件管理（入库/领用/归还/返修/台账）
  - [ ] SubTask 11.8: 设备状态看板（项目维度完成率、状态分布饼图、异常设备列表、按型号/区域/仓库统计）

- [x] Task 12: module-resource 资源调度
  - [ ] SubTask 12.1: 实体/Mapper/Service：engineer / engineer_skill / engineer_schedule / engineer_leave / timesheet
  - [ ] SubTask 12.2: 工程师资源池档案 CRUD（技能标签、认证资质、区域、在职状态）
  - [ ] SubTask 12.3: 排期管理（日历视图周/月数据、负荷热力图、冲突检测、请假/培训时间块）
  - [ ] SubTask 12.4: 任务派发（手动指派、批量派单、智能推荐基于技能/区域/负荷、转派、退回、紧急调配）
  - [ ] SubTask 12.5: 工时管理（填报、出差/加班统计、PM 审批、人天统计多维查询）

- [x] Task 13: module-delivery 交付管理（移动端现场作业）
  - [ ] SubTask 13.1: 实体/Mapper/Service：work_order / work_order_step / work_order_photo / work_order_issue
  - [ ] SubTask 13.2: 现场签到（GPS 定位+客户现场范围校验、拍照签到、签到时间记录、签退）
  - [ ] SubTask 13.3: 施工步骤跟踪（按任务标准步骤列表、逐步标记完成+拍照+耗时记录、全部完成确认）
  - [ ] SubTask 13.4: 施工拍照（时间+GPS 水印、按步骤归类、批量上传、弱网本地缓存+断点续传）
  - [ ] SubTask 13.5: 异常问题上报（类型/影响/描述/照片、自动通知 PM、处理跟踪）
  - [ ] SubTask 13.6: 工作完成确认（工程师标记→PM 确认、自动推进项目进度）

- [x] Task 14: module-agent 代理商管理
  - [ ] SubTask 14.1: 实体/Mapper/Service：agent_company / agent_engineer / outsource_task / outsource_deliverable / outsource_workload / agent_score_log
  - [ ] SubTask 14.2: 代理商企业档案 CRUD（合作区域、产品线、资质文件、合作状态、综合评分）
  - [ ] SubTask 14.3: 代理商工程师管理（信息、技能、启用停用、项目历史、质量评分）
  - [ ] SubTask 14.4: 转包管理（创建转包任务、代理商接单/拒绝、指派工程师、状态跟踪、退回重新指派）
  - [ ] SubTask 14.5: 转包任务状态机（PENDING→ACCEPTED→IN_PROGRESS→SUBMITTED→CONFIRMED + REJECTED/RETURNED/OVERDUE，超期预警定时任务）
  - [ ] SubTask 14.6: 代理商交付管控（进度上报、交付物提交：施工照片必传+测试记录必传+签收单必传、PM 审核、退回整改、质量抽检记录）
  - [ ] SubTask 14.7: 代理商数据权限边界（数据拦截器 agent_company_id 隔离、VO 脱敏屏蔽客户/合同/成本敏感字段）
  - [ ] SubTask 14.8: 代理商质量评分（PM 打分、多维评分、综合排名、影响分配优先级）

---

## 阶段四：协作、通知、报表

- [x] Task 15: module-collaboration 客户协作（轻量）
  - [ ] SubTask 15.1: 客户 H5 进度查看（项目整体进度、各阶段进展、待客户处理事项）
  - [ ] SubTask 15.2: 客户文档下载（设计方案、报告等，按权限脱敏）
  - [ ] SubTask 15.3: 客户消息通知（短信/邮件，关键节点推送 H5 链接）

- [x] Task 16: 消息通知引擎
  - [ ] SubTask 16.1: 通知服务骨架（RabbitMQ 消费者、模板渲染引擎、渠道路由、频率控制）
  - [ ] SubTask 16.2: 通知模板管理（TASK_ASSIGNED / TASK_REMINDER / TASK_OVERDUE / DELIVERABLE_REVIEW / DELIVERABLE_RETURNED / DELIVERABLE_CONFIRMED / DEVICE_ARRIVED / DEVICE_ABNORMAL / RISK_WARNING）
  - [ ] SubTask 16.3: 飞书适配器（卡片消息+按钮、应用机器人）、钉钉适配器、企微适配器
  - [ ] SubTask 16.4: 站内信适配器（写 sys_notice 表）、短信适配器（接口占位）、邮件适配器
  - [ ] SubTask 16.5: 业务事件投递接入（任务派发/到期/超期、交付物审核、设备到货/异常、风险预警）

- [x] Task 17: module-report 报表分析（基础）
  - [ ] SubTask 17.1: 工作台首页接口（按角色差异化：总监/PM/ENGINEER/AGENT_ADMIN，待办事项、核心指标卡片、我负责的项目、近期任务、最近动态）
  - [ ] SubTask 17.2: 管理驾驶舱接口（核心指标卡片含环比、项目阶段分布饼图、近12月项目趋势折线、风险项目列表）

---

## 阶段五：前端页面实现

- [ ] Task 18: PC 端认证与布局
  - [ ] SubTask 18.1: 登录页（账号密码）、Token 存储、路由守卫、退出登录
  - [ ] SubTask 18.2: 全局布局（Header 面包屑+消息+用户菜单、Sider 按权限动态渲染菜单、Content 路由出口）
  - [ ] SubTask 18.3: 消息中心抽屉（站内信列表、未读计数、已读标记）

- [ ] Task 19: PC 端工作台与项目管理页
  - [ ] SubTask 19.1: 工作台首页（按角色差异化组件）
  - [ ] SubTask 19.2: 项目列表页（筛选栏、视图切换列表/看板/甘特图/地图、统计卡片、表格）
  - [ ] SubTask 19.3: 项目详情页（概要信息、Tab 页签：概览/阶段与任务/设备/人员/割接占位/验收占位/财务占位/文档/变更/风险与问题/动态）
  - [ ] SubTask 19.4: 项目立项表单、项目计划甘特图（dhtmlx-gantt）、阶段任务树
  - [ ] SubTask 19.5: 项目模板管理页

- [ ] Task 20: PC 端设备资产页
  - [ ] SubTask 20.1: 设备型号库管理页
  - [ ] SubTask 20.2: 设备台账页（搜索筛选、状态统计卡片、列表、Excel 导入、详情侧边抽屉含状态轨迹与配置历史）
  - [ ] SubTask 20.3: 出入库管理页（入库/出库/退库/调拨/盘点表单、库存台账、库存预警列表）
  - [ ] SubTask 20.4: 备件管理页
  - [ ] SubTask 20.5: 设备状态看板页

- [ ] Task 21: PC 端资源调度页
  - [ ] SubTask 21.1: 工程师资源池管理页
  - [ ] SubTask 21.2: 排期日历页（周/月视图、负荷热力图、点击创建任务、拖拽调整、冲突提示）
  - [ ] SubTask 21.3: 任务派发页（手动指派、批量派单、智能推荐、转派）
  - [ ] SubTask 21.4: 工时管理页（填报、审批、统计）

- [x] Task 22: PC 端代理商管理页
  - [ ] SubTask 22.1: 代理商档案管理页
  - [ ] SubTask 22.2: 转包任务管理页（创建转包、状态跟踪、交付物审核）
  - [ ] SubTask 22.3: 代理商质量评分页

- [x] Task 23: PC 端系统管理与报表
  - [ ] SubTask 23.1: 用户/角色/菜单/组织架构/数据字典/系统配置/操作日志页
  - [ ] SubTask 23.2: 管理驾驶舱页（指标卡片、ECharts 饼图/折线图、风险项目列表）

- [x] Task 24: 移动端 H5 页面
  - [ ] SubTask 24.1: 工程师首页（今日任务、工时入口、消息）
  - [ ] SubTask 24.2: 任务列表/详情页
  - [ ] SubTask 24.3: 现场作业页（签到 GPS+拍照、施工步骤跟踪、拍照水印上传、异常上报、完成确认、签退）
  - [ ] SubTask 24.4: 我的页（个人信息、工时填报、出差记录、消息中心、设置）

- [x] Task 25: 外部门户 H5 页面
  - [ ] SubTask 25.1: 客户 H5（手机号验证码登录、项目进度查看、文档下载）
  - [ ] SubTask 25.2: 代理商 H5/PC 精简版（登录、任务列表、交付物提交、结算查看占位）

---

## 阶段六：联调、测试、部署

- [x] Task 26: 前后端联调与接口契约对齐
  - [ ] SubTask 26.1: Knife4j 接口文档导出，前端按文档对接
  - [ ] SubTask 26.2: 全模块主链路联调（立项→计划→派单→执行→交付→归档；设备入库→发运→到货→安装→在网；代理商转包→交付→审核）

- [x] Task 27: 测试
  - [ ] SubTask 27.1: 后端单元测试（Service 层核心业务逻辑、状态机、权限拦截器，覆盖率≥60%）
  - [ ] SubTask 27.2: 后端集成测试（Controller 层 MockMvc、RBAC 权限、数据权限隔离）
  - [ ] SubTask 27.3: 前端组件测试与 E2E（Vitest + Playwright 核心流程：登录/立项/派单/移动端签到/代理商交付）
  - [ ] SubTask 27.4: 状态机专项测试（项目/设备/转包任务状态流转非法路径全覆盖）

- [x] Task 28: 部署与 CI/CD
  - [ ] SubTask 28.1: 完善开发环境 docker-compose 一键启动
  - [ ] SubTask 28.2: 测试环境 Docker Compose 部署脚本、GitLab CI/Jenkins 流水线（构建→单测→镜像推送→部署）
  - [ ] SubTask 28.3: 生产环境部署文档（Nginx 配置、Spring Boot 集群、MySQL 主从、Redis Sentinel、ES、MinIO、RabbitMQ）

---

# Task Dependencies
- Task 5（数据库）→ Task 7/8/9/10/11/12/13/14（所有后端业务模块依赖表结构）
- Task 1（后端骨架）/ Task 2（PC 骨架）/ Task 3（移动端骨架）/ Task 4（外部入口骨架）/ Task 6（Docker）可并行
- Task 7（common）→ Task 8（auth）→ Task 9（system）→ Task 10-14（业务模块，可按模块并行）
- Task 13（交付管理 work_order）依赖 Task 10（项目任务）与 Task 12（工程师派单）
- Task 14（代理商 outsource_task）依赖 Task 10（项目任务）
- Task 16（通知引擎）被 Task 10/11/12/13/14 业务事件依赖，需先完成骨架与适配器接口
- Task 17（报表）依赖 Task 10/11/12/14 数据
- 前端 Task 18-25 依赖对应后端模块接口就绪，可按模块并行
- Task 26-28 依赖全部功能模块完成
