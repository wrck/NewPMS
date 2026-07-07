-- =====================================================================================
-- V14 演示数据完整性补全（Task 7：SubTask 7.1 - 7.5）
-- =====================================================================================
-- 目标：为 10 个角色（7 内部 + 3 外部）各提供 1 个测试账号，并为 6 种业务实体
--      （项目 / 任务 / 设备 / 验收 / 财务 / 转包）补全各状态演示数据，形成全链路关联。
--
-- 角色体系（与 Task 7 要求对齐，7 内部 + 3 外部）：
--   内部：SUPER_ADMIN / DIRECTOR / PM / ENGINEER / FINANCE / WAREHOUSE / QUALITY
--   外部：AGENT_ADMIN / AGENT_ENGINEER / CUSTOMER
--   说明：data.sql 已有 10 个角色，其中 DISPATCHER / DEVICE_ADMIN 不在 Task 7 清单内，
--        本脚本新增 WAREHOUSE（id=11）/ QUALITY（id=12）两个角色，不修改既有角色。
--
-- 测试账号（10 个，密码统一为 admin123，BCrypt hash 与 admin 账号一致，便于测试登录）：
--   | username        | real_name        | role_code     | tenant_type |
--   | superadmin      | 超管测试账号     | SUPER_ADMIN   | INTERNAL    |
--   | director        | 总监测试账号     | DIRECTOR      | INTERNAL    |
--   | pm              | 项目经理测试账号 | PM            | INTERNAL    |
--   | engineer        | 工程师测试账号   | ENGINEER      | INTERNAL    |
--   | finance         | 财务测试账号     | FINANCE       | INTERNAL    |
--   | warehouse       | 仓库管理测试账号 | WAREHOUSE     | INTERNAL    |
--   | quality         | 质量管理测试账号 | QUALITY       | INTERNAL    |
--   | agent_admin     | 代理商管理测试   | AGENT_ADMIN   | AGENT       |
--   | agent_engineer  | 代理商工程师测试 | AGENT_ENGINEER| AGENT       |
--   | customer        | 客户测试账号     | CUSTOMER      | CUSTOMER    |
--
-- 业务实体状态覆盖：
--   1. 项目：PLAN / EXECUTE / ACCEPT / CLOSE / CANCELLED 各 2 条（共 10 条）
--      （schema 状态机：INIT/PLAN/EXECUTE/ACCEPT/CLOSE/ARCHIVED/ON_HOLD/CANCELLED）
--   2. 任务：PENDING / IN_PROGRESS / COMPLETED / CANCELLED 各 3 条（共 12 条）
--   3. 设备：IN_FACTORY / INSTALLED / ONLINE / REPAIR / EOL 各覆盖（共 8 条）
--      （schema 状态机：IN_FACTORY/SHIPPED/RECEIVED/PRE_CONFIG/INSTALLED/DEBUGGED/ONLINE
--       + 异常：DAMAGED/LOST/RETURNED/REPAIR/REPLACED/EOL）
--   4. 验收：DRAFT / APPLIED / INTERNAL_AUDITED / CUSTOMER_SIGNING / COMPLETED / REJECTED 各 1 条（共 6 条）
--   5. 财务：预算 DRAFT/PENDING/APPROVED/REJECTED；成本 LABOR/TRAVEL/AGENT/OTHER；工作量确认 DRAFT/PM_CONFIRMED/FINANCE_APPROVED
--   6. 转包任务：IN_PROGRESS / CONFIRMED 各 1 条
--
-- 实体关系全链路：
--   customer → project → project_phase → project_task → device_instance
--                                                ↓
--                                          outsource_task → agent_company → agent_engineer
--                                                ↓
--                                          acceptance_task ← project
--                                                ↓
--                                          finance_budget / finance_cost / finance_workload_confirmation
--
-- 幂等策略：所有 INSERT 使用 INSERT IGNORE，依赖主键（id）或唯一索引（uk_*）冲突忽略。
--          重复执行安全，不会产生重复数据。
-- 兼容性：MySQL 5.7 / 8.0+，Flyway 8.x+ 与 scripts/init-db.js 均可应用。
-- =====================================================================================

-- =====================================================================================
-- 一、角色补全：新增 WAREHOUSE / QUALITY 两个内部角色
-- =====================================================================================
-- data.sql 已有 10 个角色（含 DISPATCHER / DEVICE_ADMIN），本脚本不修改既有角色，
-- 仅新增 Task 7 要求的 WAREHOUSE / QUALITY 两个角色（id=11/12）。
-- 幂等：依赖 uk_sys_role_role_code 唯一索引。

INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(11, '仓库管理员', 'WAREHOUSE', '仓库管理员，管理设备出入库与备件库存', 1, 'DEPT', 1, NOW(), 1, NOW(), 0),
(12, '质量管理员', 'QUALITY',   '质量管理员，负责验收审核与质量评分',  1, 'DEPT', 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 二、测试账号：10 个角色各 1 个测试账号
-- =====================================================================================
-- 密码统一使用 admin 的 BCrypt hash（明文 admin123），便于测试人员快速登录。
-- 用户 ID 规划：100-109，避开既有 admin(id=1) 与 V10/V13 使用的 1。
-- 幂等：依赖 uk_sys_user_username 唯一索引。

INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `avatar`, `status`, `tenant_type`, `tenant_id`, `org_id`, `last_login_time`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(100, 'superadmin',     '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '超管测试账号',     '13800000001', 'superadmin@vibe.com',     NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(101, 'director',       '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '总监测试账号',     '13800000002', 'director@vibe.com',       NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(102, 'pm',             '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '项目经理测试账号', '13800000003', 'pm@vibe.com',             NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(103, 'engineer',       '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '工程师测试账号',   '13800000004', 'engineer@vibe.com',       NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(104, 'finance',        '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '财务测试账号',     '13800000005', 'finance@vibe.com',        NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(105, 'warehouse',      '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '仓库管理测试账号', '13800000006', 'warehouse@vibe.com',      NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(106, 'quality',        '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '质量管理测试账号', '13800000007', 'quality@vibe.com',        NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(107, 'agent_admin',    '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '代理商管理测试',   '13800000008', 'agent_admin@vibe.com',    NULL, 'ACTIVE', 'AGENT',    1,    NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(108, 'agent_engineer', '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '代理商工程师测试', '13800000009', 'agent_engineer@vibe.com', NULL, 'ACTIVE', 'AGENT',    1,    NULL, 1, NULL, 1, NOW(), 1, NOW(), 0),
(109, 'customer',       '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '客户测试账号',     '13800000010', 'customer@vibe.com',       NULL, 'ACTIVE', 'CUSTOMER', 1,    NULL, 1, NULL, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 三、用户-角色关联：10 个测试账号各绑定对应角色
-- =====================================================================================
-- 幂等：依赖 uk_sys_user_role(user_id, role_id) 唯一索引。
-- 关联关系：
--   superadmin(100) → SUPER_ADMIN(1)
--   director(101) → DIRECTOR(2)
--   pm(102) → PM(3)
--   engineer(103) → ENGINEER(5)
--   finance(104) → FINANCE(7)
--   warehouse(105) → WAREHOUSE(11)
--   quality(106) → QUALITY(12)
--   agent_admin(107) → AGENT_ADMIN(8)
--   agent_engineer(108) → AGENT_ENGINEER(9)
--   customer(109) → CUSTOMER(10)

INSERT IGNORE INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(100, 100, 1,  1, NOW(), 1, NOW(), 0),
(101, 101, 2,  1, NOW(), 1, NOW(), 0),
(102, 102, 3,  1, NOW(), 1, NOW(), 0),
(103, 103, 5,  1, NOW(), 1, NOW(), 0),
(104, 104, 7,  1, NOW(), 1, NOW(), 0),
(105, 105, 11, 1, NOW(), 1, NOW(), 0),
(106, 106, 12, 1, NOW(), 1, NOW(), 0),
(107, 107, 8,  1, NOW(), 1, NOW(), 0),
(108, 108, 9,  1, NOW(), 1, NOW(), 0),
(109, 109, 10, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 四、角色-菜单关联：新角色（WAREHOUSE/QUALITY）关联部分菜单
-- =====================================================================================
-- data.sql 中 admin(SUPER_ADMIN) 已关联全部菜单。本脚本为 WAREHOUSE / QUALITY 角色
-- 关联设备资产、项目管理、验收相关菜单，确保登录后能看到与权限匹配的非空数据。
-- sys_role_menu ID 规划：10000-10099，避开既有 1-36 与 V10/V13 的 9001-9025。
-- 幂等：依赖 uk_sys_role_menu(role_id, menu_id) 唯一索引。

INSERT IGNORE INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- WAREHOUSE(11)：设备资产菜单 7/8/9/10/11/12 + 系统管理子菜单 22/23/29
(10001, 11, 7,  1, NOW(), 1, NOW(), 0),
(10002, 11, 8,  1, NOW(), 1, NOW(), 0),
(10003, 11, 9,  1, NOW(), 1, NOW(), 0),
(10004, 11, 10, 1, NOW(), 1, NOW(), 0),
(10005, 11, 11, 1, NOW(), 1, NOW(), 0),
(10006, 11, 12, 1, NOW(), 1, NOW(), 0),
(10007, 11, 22, 1, NOW(), 1, NOW(), 0),
(10008, 11, 23, 1, NOW(), 1, NOW(), 0),
(10009, 11, 29, 1, NOW(), 1, NOW(), 0),
-- QUALITY(12)：项目管理 + 设备资产 + 验收（项目管理菜单 2/3 + 设备 7/9 + 系统 22/29）
(10010, 12, 2,  1, NOW(), 1, NOW(), 0),
(10011, 12, 3,  1, NOW(), 1, NOW(), 0),
(10012, 12, 7,  1, NOW(), 1, NOW(), 0),
(10013, 12, 9,  1, NOW(), 1, NOW(), 0),
(10014, 12, 22, 1, NOW(), 1, NOW(), 0),
(10015, 12, 29, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 五、客户档案（3 条，为项目提供 customer_id 外键）
-- =====================================================================================
INSERT IGNORE INTO `customer` (`id`, `customer_name`, `customer_code`, `contact_name`, `contact_phone`, `contact_email`, `address`, `region`, `industry`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '北京华夏科技有限公司',   'CUST-001', '王经理', '13900000010', 'wang@huaxia.com',   '北京市海淀区中关村大街1号', '华北', '互联网',     'VIP 客户，年度框架协议', 1, NOW(), 1, NOW(), 0),
(2, '上海金融数据服务公司',   'CUST-002', '李总监', '13900000011', 'li@jrcj.com',       '上海市浦东新区世纪大道100号', '华东', '金融',       '金融行业客户，对安全要求高', 1, NOW(), 1, NOW(), 0),
(3, '深圳智能科技公司',       'CUST-003', '张总',   '13900000012', 'zhang@znkj.com',    '深圳市南山区科技园路8号',     '华南', '智能制造',   '新签客户，首期合作', 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 六、代理商公司（2 条，为转包任务与代理商用户提供 tenant_id）
-- =====================================================================================
INSERT IGNORE INTO `agent_company` (`id`, `company_name`, `company_code`, `qualification`, `contact_name`, `contact_phone`, `service_regions`, `product_lines`, `status`, `overall_score`, `cooperation_start`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '北京网络工程服务有限公司', 'AGT-001', 'A级', '赵总', '13700000001', '["华北","东北"]',     '["路由","交换","无线"]', 'ACTIVE', 4.50, '2024-01-01', 1, NOW(), 1, NOW(), 0),
(2, '上海IT实施服务公司',       'AGT-002', 'B级', '钱总', '13700000002', '["华东","华南"]',     '["路由","交换","安全"]', 'ACTIVE', 4.20, '2024-06-01', 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 七、代理商工程师（2 条，关联代理商公司）
-- =====================================================================================
INSERT IGNORE INTO `agent_engineer` (`id`, `agent_company_id`, `name`, `phone`, `skills`, `certifications`, `status`, `quality_score`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 1, '张代理',   '13700000003', '["路由","交换"]',     '["HCIP"]',     'ACTIVE', 4.30, 1, NOW(), 1, NOW(), 0),
(2, 2, '李工程师', '13700000004', '["无线","安全"]',     '["HCIE"]',     'ACTIVE', 4.60, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 八、自有工程师档案（1 条，关联 engineer 测试账号 user_id=103）
-- =====================================================================================
INSERT IGNORE INTO `engineer` (`id`, `user_id`, `employee_no`, `name`, `phone`, `region`, `status`, `hire_date`, `skills`, `certifications`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 103, 'EMP-001', '工程师测试', '13800000004', '华北', 'ACTIVE', '2024-01-15', '["路由","交换","无线"]', '["HCIE"]', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 九、设备型号（4 条，覆盖路由/交换/无线/安全 4 类）
-- =====================================================================================
INSERT IGNORE INTO `device_model` (`id`, `model_code`, `model_name`, `product_line`, `vendor`, `category`, `specifications`, `config_template`, `manual_url`, `image_url`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 'MDL-ROUTER-001', 'NetRouter AR-2280',  '路由', '华为', 'ROUTER',   '{"ports":"4xGE+2x10GE","throughput":"10Gbps"}',  NULL, 'https://docs.vibe.com/router-ar2280',  NULL, 1, NOW(), 1, NOW(), 0),
(2, 'MDL-SWITCH-001', 'NetSwitch S-5700',   '交换', '华为', 'SWITCH',   '{"ports":"24xGE+4x10GE","stack":"支持"}',         NULL, 'https://docs.vibe.com/switch-s5700',   NULL, 1, NOW(), 1, NOW(), 0),
(3, 'MDL-AP-001',     'AirWave AP-660',     '无线', '华为', 'AP',       '{"standard":"Wi-Fi 6","mimo":"4x4"}',             NULL, 'https://docs.vibe.com/ap-660',        NULL, 1, NOW(), 1, NOW(), 0),
(4, 'MDL-FW-001',     'SecureWall FW-2000', '安全', '华为', 'FIREWALL', '{"throughput":"5Gbps","sessions":"2M"}',          NULL, 'https://docs.vibe.com/fw-2000',       NULL, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十、设备实例（8 条，覆盖 4 类状态：IN_FACTORY / INSTALLED / ONLINE / REPAIR / EOL）
-- =====================================================================================
-- 状态映射（Task 7 要求 → schema 实际值）：
--   IN_STORAGE  → IN_FACTORY（在库）
--   DEPLOYED    → ONLINE（在网运行）
--   MAINTENANCE → REPAIR（返修中）
--   SCRAPPED    → EOL（退网/报废）
-- 设备 ID 规划：1001-1008，避开既有数据。

INSERT IGNORE INTO `device_instance` (`id`, `serial_number`, `mac_address`, `model_id`, `firmware_version`, `project_id`, `phase_id`, `site_name`, `install_location`, `status`, `warehouse_id`, `agent_company_id`, `config_file_url`, `config_version`, `install_date`, `online_date`, `installer_id`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 2 条在库（IN_FACTORY）
(1001, 'SN2026R0001', '00:1A:2B:3C:4D:01', 1, 'V5.1.0', NULL, NULL, NULL, NULL, 'IN_FACTORY', 1, NULL, NULL, 0, NULL, NULL, NULL, '北京中心仓库存', 1, 1, NOW(), 1, NOW(), 0),
(1002, 'SN2026S0002', '00:1A:2B:3C:4D:02', 2, 'V7.2.1', NULL, NULL, NULL, NULL, 'IN_FACTORY', 1, NULL, NULL, 0, NULL, NULL, NULL, '北京中心仓库存', 1, 1, NOW(), 1, NOW(), 0),
-- 2 条已安装（INSTALLED）
(1003, 'SN2026R0003', '00:1A:2B:3C:4D:03', 1, 'V5.1.0', 1003, 1003, '北京华夏机房', 'A栋-3层-机柜01-U12', 'INSTALLED', NULL, NULL, '/configs/sn2026r0003_v1.cfg', 1, '2026-06-15', NULL, 103, '项目安装中', 1, 1, NOW(), 1, NOW(), 0),
(1004, 'SN2026S0004', '00:1A:2B:3C:4D:04', 2, 'V7.2.1', 1004, 1004, '深圳智能机房', 'B栋-2层-机柜03-U08', 'INSTALLED', NULL, NULL, '/configs/sn2026s0004_v1.cfg', 1, '2026-06-20', NULL, 103, '项目安装中', 1, 1, NOW(), 1, NOW(), 0),
-- 2 条在网运行（ONLINE）
(1005, 'SN2026R0005', '00:1A:2B:3C:4D:05', 1, 'V5.1.2', 1007, 1007, '北京华夏机房', 'A栋-3层-机柜01-U12', 'ONLINE', NULL, NULL, '/configs/sn2026r0005_v2.cfg', 2, '2026-05-10', '2026-05-15', 103, '已割接上线', 1, 1, NOW(), 1, NOW(), 0),
(1006, 'SN2026S0006', '00:1A:2B:3C:4D:06', 2, 'V7.2.3', 1008, 1008, '深圳智能机房', 'B栋-2层-机柜03-U08', 'ONLINE', NULL, NULL, '/configs/sn2026s0006_v2.cfg', 2, '2026-05-12', '2026-05-18', 103, '已割接上线', 1, 1, NOW(), 1, NOW(), 0),
-- 1 条返修中（REPAIR）
(1007, 'SN2026R0007', '00:1A:2B:3C:4D:07', 3, 'V6.0.0', 1003, 1003, '北京华夏机房', 'A栋-3层-机柜02-U05', 'REPAIR', 1, NULL, '/configs/sn2026r0007_v1.cfg', 1, '2026-06-18', NULL, 103, '设备故障返修', 1, 1, NOW(), 1, NOW(), 0),
-- 1 条退网/报废（EOL）
(1008, 'SN2026R0008', '00:1A:2B:3C:4D:08', 4, 'V4.0.0', NULL, NULL, NULL, NULL, 'EOL', 1, NULL, NULL, 0, NULL, NULL, NULL, '设备超期退役', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十一、项目（10 条，5 状态 × 2 条）
-- =====================================================================================
-- 状态映射（Task 7 要求 → schema 实际值）：
--   PLANNING   → PLAN
--   IN_PROGRESS → EXECUTE
--   ACCEPTANCE  → ACCEPT
--   COMPLETED   → CLOSE
--   CANCELLED   → CANCELLED
-- 项目 ID 规划：1001-1010，避开既有数据。
-- PM 统一为 pm 测试账号(102)，customer_id 关联客户表。

INSERT IGNORE INTO `project` (`id`, `project_code`, `project_name`, `customer_id`, `project_type`, `product_line`, `execute_mode`, `priority`, `status`, `current_phase`, `pm_id`, `region`, `contract_no`, `planned_start`, `planned_end`, `actual_start`, `actual_end`, `progress_pct`, `description`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 2 条 PLAN（规划中）
(1001, 'PRJ-202606-001', '华夏北京核心网改造项目',     1, '改造', '路由', 'SELF',  'P1', 'PLAN',     'SURVEY', 102, '华北', 'HT-2026-001', '2026-07-01', '2026-09-30', NULL,        NULL,        10,  '核心路由器替换与链路升级，涉及 4 个机房',         NULL, 1, 1, NOW(), 1, NOW(), 0),
(1002, 'PRJ-202606-002', '上海金融数据中心扩容项目',   2, '扩容', '交换', 'MIXED', 'P0', 'PLAN',     'SURVEY', 102, '华东', 'HT-2026-002', '2026-07-05', '2026-10-15', NULL,        NULL,         5,  '数据中心交换机扩容，含 24 台设备',                 NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 2 条 EXECUTE（执行中）
(1003, 'PRJ-202605-003', '华夏北京无线网络新建项目',   1, '新建', '无线', 'MIXED', 'P1', 'EXECUTE',  'INSTALL', 102, '华北', 'HT-2026-003', '2026-05-15', '2026-08-30', '2026-05-15', NULL,        45, '办公区无线覆盖，120 台 AP 部署',                   NULL, 1, 1, NOW(), 1, NOW(), 0),
(1004, 'PRJ-202605-004', '深圳智能园区安全建设项目',   3, '安全', '安全', 'AGENT', 'P2', 'EXECUTE',  'DEBUG',   102, '华南', 'HT-2026-004', '2026-05-20', '2026-09-10', '2026-05-20', NULL,        60, '园区防火墙与入侵检测系统部署',                     NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 2 条 ACCEPT（验收中）
(1005, 'PRJ-202604-005', '华夏北京核心路由替换项目',   1, '替换', '路由', 'SELF',  'P1', 'ACCEPT',   'ACCEPT', 102, '华北', 'HT-2026-005', '2026-04-01', '2026-07-15', '2026-04-01', NULL,        90, '2 台核心路由器替换与割接，已完成调试进入验收',     NULL, 1, 1, NOW(), 1, NOW(), 0),
(1006, 'PRJ-202604-006', '上海金融数据中心交换升级',   2, '改造', '交换', 'MIXED', 'P1', 'ACCEPT',   'ACCEPT', 102, '华东', 'HT-2026-006', '2026-04-10', '2026-07-25', '2026-04-10', NULL,        85, '数据中心核心交换机升级，含 8 台设备',               NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 2 条 CLOSE（已结项）
(1007, 'PRJ-202603-007', '华夏北京网络优化项目',       1, '改造', '路由', 'SELF',  'P2', 'CLOSE',    'ACCEPT', 102, '华北', 'HT-2026-007', '2026-03-01', '2026-06-15', '2026-03-01', '2026-06-10', 100, '已完成路由优化与割接，验收通过',                     NULL, 1, 1, NOW(), 1, NOW(), 0),
(1008, 'PRJ-202603-008', '深圳智能园区网络建设项目',   3, '新建', '交换', 'SELF',  'P2', 'CLOSE',    'ACCEPT', 102, '华南', 'HT-2026-008', '2026-03-05', '2026-06-20', '2026-03-05', '2026-06-15', 100, '园区基础网络建设，含 30 台交换机部署',             NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 2 条 CANCELLED（已取消）
(1009, 'PRJ-202606-009', '上海金融数据备份网络项目',   2, '新建', '路由', 'SELF',  'P3', 'CANCELLED', 'SURVEY', 102, '华东', 'HT-2026-009', '2026-06-01', '2026-09-30', NULL,        NULL,         0,  '客户预算调整，项目取消',                             '客户原因取消', 1, 1, NOW(), 1, NOW(), 0),
(1010, 'PRJ-202606-010', '深圳智能无线扩展项目',       3, '扩容', '无线', 'AGENT', 'P3', 'CANCELLED', 'SURVEY', 102, '华南', 'HT-2026-010', '2026-06-10', '2026-10-10', NULL,        NULL,         0,  '需求变更，项目暂停后取消',                           '需求变更取消', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十二、项目阶段（10 条，每个项目 1 个关键阶段）
-- =====================================================================================
-- 阶段状态：NOT_STARTED / IN_PROGRESS / COMPLETED
-- 阶段编码：SURVEY / DESIGN / DELIVER / INSTALL / DEBUG / ACCEPT
-- ID 规划：1001-1010，与项目 ID 一一对应，便于追踪。

INSERT IGNORE INTO `project_phase` (`id`, `project_id`, `phase_code`, `phase_name`, `sort_order`, `status`, `planned_start`, `planned_end`, `actual_start`, `actual_end`, `deliverables`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1001, 1001, 'SURVEY', '勘测阶段', 1, 'IN_PROGRESS', '2026-07-01', '2026-07-15', '2026-07-02', NULL,       '["现场勘测报告"]',             1, NOW(), 1, NOW(), 0),
(1002, 1002, 'SURVEY', '勘测阶段', 1, 'IN_PROGRESS', '2026-07-05', '2026-07-20', '2026-07-06', NULL,       '["现场勘测报告"]',             1, NOW(), 1, NOW(), 0),
(1003, 1003, 'INSTALL','安装阶段', 4, 'IN_PROGRESS', '2026-06-01', '2026-07-30', '2026-06-02', NULL,       '["安装记录","机柜布置图"]',    1, NOW(), 1, NOW(), 0),
(1004, 1004, 'DEBUG',  '调试阶段', 5, 'IN_PROGRESS', '2026-06-20', '2026-08-10', '2026-06-22', NULL,       '["调试报告"]',                 1, NOW(), 1, NOW(), 0),
(1005, 1005, 'ACCEPT', '验收阶段', 6, 'IN_PROGRESS', '2026-07-01', '2026-07-15', '2026-07-01', NULL,       '["测试报告","验收签核单"]',    1, NOW(), 1, NOW(), 0),
(1006, 1006, 'ACCEPT', '验收阶段', 6, 'IN_PROGRESS', '2026-07-10', '2026-07-25', '2026-07-10', NULL,       '["测试报告","验收签核单"]',    1, NOW(), 1, NOW(), 0),
(1007, 1007, 'ACCEPT', '验收阶段', 6, 'COMPLETED',   '2026-05-15', '2026-06-10', '2026-05-15', '2026-06-10', '["测试报告","竣工文档"]',    1, NOW(), 1, NOW(), 0),
(1008, 1008, 'ACCEPT', '验收阶段', 6, 'COMPLETED',   '2026-05-20', '2026-06-15', '2026-05-20', '2026-06-15', '["测试报告","竣工文档"]',    1, NOW(), 1, NOW(), 0),
(1009, 1009, 'SURVEY', '勘测阶段', 1, 'NOT_STARTED', '2026-06-01', '2026-06-30', NULL,        NULL,       NULL,                            1, NOW(), 1, NOW(), 0),
(1010, 1010, 'SURVEY', '勘测阶段', 1, 'NOT_STARTED', '2026-06-10', '2026-07-10', NULL,        NULL,       NULL,                            1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十三、项目任务（12 条，4 状态 × 3 条）
-- =====================================================================================
-- 任务状态：PENDING / IN_PROGRESS / COMPLETED / CANCELLED
-- 注意：schema 任务状态机为 PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CONFIRMED，
--       Task 7 要求的 CANCELLED 在 schema 中未定义，但字段为 VARCHAR(16) 不强制校验，
--       本脚本使用 CANCELLED 字面值以满足 Task 7 状态覆盖要求。
-- ID 规划：2001-2012，避开既有数据。

INSERT IGNORE INTO `project_task` (`id`, `project_id`, `phase_id`, `parent_task_id`, `task_name`, `task_type`, `status`, `execute_mode`, `assignee_id`, `agent_company_id`, `agent_engineer_id`, `site_info`, `device_ids`, `planned_start`, `planned_end`, `actual_start`, `actual_end`, `priority`, `description`, `attachments`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 3 条 PENDING（待分配）
(2001, 1001, 1001, NULL, '现场勘测',         'SURVEY',  'PENDING',    'SELF',  NULL, NULL, NULL, '{"site":"北京华夏机房"}',    NULL,         '2026-07-05', '2026-07-15', NULL,        NULL,        'MEDIUM', '华夏北京核心网改造项目首次勘测', NULL, 1, 1, NOW(), 1, NOW(), 0),
(2002, 1002, 1002, NULL, '现场勘测',         'SURVEY',  'PENDING',    'SELF',  NULL, NULL, NULL, '{"site":"上海金融机房"}',    NULL,         '2026-07-10', '2026-07-20', NULL,        NULL,        'HIGH',   '上海金融数据中心扩容勘测',       NULL, 1, 1, NOW(), 1, NOW(), 0),
(2003, 1009, 1009, NULL, '项目启动',         'OTHER',   'PENDING',    'SELF',  NULL, NULL, NULL, NULL,                          NULL,         '2026-06-05', '2026-06-15', NULL,        NULL,        'LOW',    '已取消项目的启动任务（待清理）', NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 3 条 IN_PROGRESS（进行中）
(2004, 1003, 1003, NULL, 'AP 安装上架',      'INSTALL', 'IN_PROGRESS','MIXED', 103,  1,    1,    '{"site":"北京华夏办公区"}',  '[1003]',     '2026-06-15', '2026-07-15', '2026-06-15', NULL,        'HIGH',   '120 台 AP 安装上架，含代理商协助', NULL, 1, 1, NOW(), 1, NOW(), 0),
(2005, 1004, 1004, NULL, '防火墙调试',       'DEBUG',   'IN_PROGRESS','AGENT', 103,  2,    2,    '{"site":"深圳智能园区"}',    '[1004]',     '2026-06-22', '2026-07-30', '2026-06-22', NULL,        'MEDIUM', '防火墙策略调试与入侵检测配置',     NULL, 1, 1, NOW(), 1, NOW(), 0),
(2006, 1005, 1005, NULL, '验收测试',         'ACCEPT',  'IN_PROGRESS','SELF',  103,  NULL, NULL, '{"site":"北京华夏机房"}',    '[1005]',     '2026-07-01', '2026-07-10', '2026-07-01', NULL,        'HIGH',   '路由器替换后端到端验收测试',       NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 3 条 COMPLETED（已完成）
(2007, 1007, 1007, NULL, '路由优化割接',     'CUTOVER', 'COMPLETED',  'SELF',  103,  NULL, NULL, '{"site":"北京华夏机房"}',    '[1005]',     '2026-05-10', '2026-05-15', '2026-05-10', '2026-05-15', 'HIGH',   '核心路由优化割接，已通过',         NULL, 1, 1, NOW(), 1, NOW(), 0),
(2008, 1008, 1008, NULL, '交换机部署',       'INSTALL', 'COMPLETED',  'SELF',  103,  NULL, NULL, '{"site":"深圳智能园区"}',    '[1006]',     '2026-05-15', '2026-05-25', '2026-05-15', '2026-05-25', 'MEDIUM', '30 台交换机部署完成',             NULL, 1, 1, NOW(), 1, NOW(), 0),
(2009, 1005, 1005, NULL, '设备到货确认',     'OTHER',   'COMPLETED',  'SELF',  103,  NULL, NULL, '{"site":"北京华夏机房"}',    '[1005]',     '2026-04-05', '2026-04-15', '2026-04-05', '2026-04-15', 'MEDIUM', '设备到货验收完成',                 NULL, 1, 1, NOW(), 1, NOW(), 0),
-- 3 条 CANCELLED（已取消）
(2010, 1009, 1009, NULL, '设备采购',         'OTHER',   'CANCELLED',  'SELF',  NULL, NULL, NULL, NULL,                          NULL,         '2026-06-10', '2026-06-30', NULL,        NULL,        'LOW',    '项目取消，采购任务同步取消',       NULL, 1, 1, NOW(), 1, NOW(), 0),
(2011, 1010, 1010, NULL, '现场勘测',         'SURVEY',  'CANCELLED',  'AGENT', NULL, 2,    NULL, '{"site":"深圳智能园区"}',    NULL,         '2026-06-15', '2026-06-25', NULL,        NULL,        'LOW',    '项目取消，勘测任务同步取消',       NULL, 1, 1, NOW(), 1, NOW(), 0),
(2012, 1002, 1002, NULL, '设计方案编制',     'OTHER',   'CANCELLED',  'SELF',  NULL, NULL, NULL, NULL,                          NULL,         '2026-07-15', '2026-07-30', NULL,        NULL,        'MEDIUM', '方案调整，原设计任务取消',         NULL, 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十四、项目成员（关联用户与项目，确保各角色登录后能看到非空数据）
-- =====================================================================================
-- 每个项目关联 PM(102) / ENGINEER(103) / CUSTOMER(109)，确保各角色登录后能看到项目。
-- 对于代施项目，关联 AGENT(107) / AGENT_ENGINEER(108)。
-- ID 规划：100-149，避开既有数据。

INSERT IGNORE INTO `project_member` (`id`, `project_id`, `user_id`, `role`, `join_time`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 项目 1001（PLAN，规划中）
(100, 1001, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(101, 1001, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(102, 1001, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1002（PLAN，规划中，代施）
(103, 1002, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(104, 1002, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
(105, 1002, 107, 'AGENT',    NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1003（EXECUTE，执行中，代施）
(106, 1003, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(107, 1003, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(108, 1003, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
(109, 1003, 107, 'AGENT',    NOW(), 1, NOW(), 1, NOW(), 0),
(110, 1003, 108, 'AGENT',    NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1004（EXECUTE，执行中，代施）
(111, 1004, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(112, 1004, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(113, 1004, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
(114, 1004, 107, 'AGENT',    NOW(), 1, NOW(), 1, NOW(), 0),
(115, 1004, 108, 'AGENT',    NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1005（ACCEPT，验收中）
(116, 1005, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(117, 1005, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(118, 1005, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1006（ACCEPT，验收中）
(119, 1006, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(120, 1006, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(121, 1006, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1007（CLOSE，已结项）
(122, 1007, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(123, 1007, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(124, 1007, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1008（CLOSE，已结项）
(125, 1008, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(126, 1008, 103, 'ENGINEER', NOW(), 1, NOW(), 1, NOW(), 0),
(127, 1008, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1009（CANCELLED，已取消）
(128, 1009, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(129, 1009, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0),
-- 项目 1010（CANCELLED，已取消）
(130, 1010, 102, 'PM',       NOW(), 1, NOW(), 1, NOW(), 0),
(131, 1010, 109, 'CUSTOMER', NOW(), 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十五、转包任务（2 条，覆盖 IN_PROGRESS / CONFIRMED 状态）
-- =====================================================================================
-- 转包任务状态：PENDING / ACCEPTED / REJECTED / IN_PROGRESS / SUBMITTED / CONFIRMED / RETURNED / OVERDUE
-- ID 规划：3001-3002，避开既有数据。

INSERT IGNORE INTO `outsource_task` (`id`, `project_id`, `task_id`, `agent_company_id`, `agent_engineer_id`, `task_scope`, `deadline`, `status`, `submit_count`, `confirmed_by`, `confirmed_time`, `reject_reason`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(3001, 1003, 2004, 1, 1, '北京华夏办公区 AP 安装上架 60 台（含线缆连接与标签制作）', '2026-07-15', 'IN_PROGRESS', 0, NULL, NULL, NULL, 1, 1, NOW(), 1, NOW(), 0),
(3002, 1004, 2005, 2, 2, '深圳智能园区防火墙策略调试与入侵检测配置',               '2026-07-30', 'CONFIRMED',   1, 102,    NOW(), NULL, 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十六、验收任务（6 条，6 状态 × 1 条）
-- =====================================================================================
-- 验收状态：DRAFT / APPLIED / INTERNAL_AUDITED / CUSTOMER_SIGNING / COMPLETED / REJECTED
-- ID 规划：4001-4006，避开既有数据。
-- 关联项目：1005(ACCEPT) / 1006(ACCEPT) / 1007(CLOSE) / 1008(CLOSE)

INSERT IGNORE INTO `acceptance_task` (`id`, `project_id`, `standard_id`, `name`, `apply_user_id`, `apply_time`, `internal_audit_user_id`, `internal_audit_time`, `internal_audit_result`, `customer_sign_link`, `customer_sign_user`, `customer_sign_time`, `customer_sign_result`, `customer_sign_remark`, `score`, `status`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- DRAFT：草稿，未提交
(4001, 1005, NULL, '华夏北京核心路由替换-验收',  102, NULL,       NULL, NULL,       NULL,    NULL,                 NULL, NULL,       NULL,              NULL,                 NULL,  'DRAFT',             '草稿状态，待提交内部审核', 1, 1, NOW(), 1, NOW(), 0),
-- APPLIED：已申请，待内部审核
(4002, 1006, NULL, '上海金融数据中心交换升级-验收', 102, NOW(),       NULL, NULL,       NULL,    NULL,                 NULL, NULL,       NULL,              NULL,                 NULL,  'APPLIED',           '已申请，等待内部审核',     1, 1, NOW(), 1, NOW(), 0),
-- INTERNAL_AUDITED：内部审核通过，待客户签核
(4003, 1007, NULL, '华夏北京网络优化-验收',     102, NOW(),       106,  NOW(),       'PASS',  '/acceptance/sign/4003', '王经理', NULL,       NULL,              NULL,                 88.50, 'INTERNAL_AUDITED',  '内部审核通过，待客户签核', 1, 1, NOW(), 1, NOW(), 0),
-- CUSTOMER_SIGNING：客户签核中
(4004, 1008, NULL, '深圳智能园区网络建设-验收', 102, NOW(),       106,  NOW(),       'PASS',  '/acceptance/sign/4004', '张总',   NULL,       NULL,              NULL,                 92.00, 'CUSTOMER_SIGNING',  '客户签核中',               1, 1, NOW(), 1, NOW(), 0),
-- COMPLETED：验收完成
(4005, 1007, NULL, '华夏北京网络优化-终验',     102, NOW(),       106,  NOW(),       'PASS',  '/acceptance/sign/4005', '王经理', NOW(),       'PASS',            '验收通过，质量优秀', 95.00, 'COMPLETED',         '验收完成，已归档',         1, 1, NOW(), 1, NOW(), 0),
-- REJECTED：验收驳回
(4006, 1006, NULL, '上海金融数据中心交换升级-初验', 102, NOW(),       106,  NOW(),       'REJECT', NULL,                   NULL,     NULL,       NULL,              NULL,                 60.00, 'REJECTED',           '验收未通过，需整改',       1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十七、财务预算（4 条，4 种审批状态）
-- =====================================================================================
-- 预算审批状态：DRAFT / PENDING / APPROVED / REJECTED
-- ID 规划：5001-5004，避开既有数据。

INSERT IGNORE INTO `finance_budget` (`id`, `project_id`, `year`, `labor_amount`, `travel_amount`, `agent_amount`, `other_amount`, `total_amount`, `approval_status`, `approver_id`, `approve_time`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(5001, 1001, 2026, 80000.00,  30000.00,  0.00,      5000.00,   115000.00, 'DRAFT',    NULL, NULL,       '草稿预算，待提交审批',           1, 1, NOW(), 1, NOW(), 0),
(5002, 1003, 2026, 150000.00, 50000.00,  60000.00,  10000.00,  270000.00, 'PENDING',  NULL, NULL,       '已提交，等待审批',                 1, 1, NOW(), 1, NOW(), 0),
(5003, 1005, 2026, 120000.00, 40000.00,  0.00,      8000.00,   168000.00, 'APPROVED', 104,  NOW(),       '审批通过',                         1, 1, NOW(), 1, NOW(), 0),
(5004, 1009, 2026, 60000.00,  20000.00,  30000.00,  5000.00,   115000.00, 'REJECTED', 104,  NOW(),       '项目已取消，预算驳回',             1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十八、财务成本归集（4 条，4 种成本类型）
-- =====================================================================================
-- 成本类型：LABOR（人工） / TRAVEL（差旅） / AGENT（代理商） / OTHER（其他）
-- ID 规划：5101-5104，避开既有数据。

INSERT IGNORE INTO `finance_cost` (`id`, `project_id`, `cost_type`, `amount`, `cost_date`, `ref_type`, `ref_id`, `description`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(5101, 1003, 'LABOR',  45000.00, '2026-06-30', 'TIMESHEET',      NULL, '6 月工程师人工成本',     1, 1, NOW(), 1, NOW(), 0),
(5102, 1003, 'TRAVEL', 12000.00, '2026-06-30', 'BUSINESS_TRIP',  NULL, '6 月差旅费用',           1, 1, NOW(), 1, NOW(), 0),
(5103, 1004, 'AGENT',  30000.00, '2026-06-30', 'OUTSOURCE_TASK', 3002, '代理商实施费用',         1, 1, NOW(), 1, NOW(), 0),
(5104, 1005, 'OTHER',  3000.00,  '2026-06-30', 'MANUAL',         NULL, '验收测试其他费用',       1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 十九、财务工作量确认单（3 条，3 种审批状态）
-- =====================================================================================
-- 审批状态：DRAFT / PM_CONFIRMED / AGENT_CONFIRMED / PENDING / DIRECTOR_APPROVED / FINANCE_APPROVED / REJECTED
-- 付款状态：UNPAID / PAYING / PAID
-- ID 规划：5201-5203，避开既有数据。

INSERT IGNORE INTO `finance_workload_confirmation` (`id`, `project_id`, `outsource_task_id`, `agent_company_id`, `period`, `workload_days`, `unit_price`, `travel_amount`, `other_amount`, `total_amount`, `pm_confirm_user_id`, `pm_confirm_time`, `agent_confirm_user_id`, `agent_confirm_time`, `approval_status`, `payment_status`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(5201, 1003, 3001, 1, '2026-06', 30.00, 1500.00, 5000.00, 1000.00, 51000.00, NULL, NULL, NULL, NULL,                   'DRAFT',            'UNPAID', '草稿，待 PM 确认',           1, 1, NOW(), 1, NOW(), 0),
(5202, 1004, 3002, 2, '2026-06', 20.00, 1800.00, 3000.00, 500.00,  39500.00, 102,  NOW(), 107,  NOW(),                   'PM_CONFIRMED',    'UNPAID', 'PM 已确认，待代理商确认',   1, 1, NOW(), 1, NOW(), 0),
(5203, 1005, NULL, 1, '2026-05', 15.00, 1500.00, 2000.00, 500.00,  25000.00, 102,  NOW(), 107,  NOW(),                   'FINANCE_APPROVED', 'PAID',   '财务已审批并完成付款',       1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 二十、工单（2 条，覆盖现场作业状态）
-- =====================================================================================
-- 工单状态：CREATED / CHECKED_IN / IN_PROGRESS / COMPLETED / CONFIRMED
-- ID 规划：6001-6002，避开既有数据。

INSERT IGNORE INTO `work_order` (`id`, `task_id`, `engineer_id`, `checkin_time`, `checkout_time`, `checkin_location`, `checkout_location`, `checkin_photo`, `status`, `total_duration`, `photo_count`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(6001, 2007, 1, '2026-05-10 09:00:00', '2026-05-10 17:00:00', '{"lat":39.98,"lng":116.30,"addr":"北京华夏机房"}', '{"lat":39.98,"lng":116.30,"addr":"北京华夏机房"}', '/photos/wo6001-checkin.jpg',  'COMPLETED',   8.00, 5, '路由优化割接完成',     1, 1, NOW(), 1, NOW(), 0),
(6002, 2006, 1, '2026-07-01 09:30:00', NULL,                  '{"lat":39.98,"lng":116.30,"addr":"北京华夏机房"}', NULL,                                                NULL,                            'IN_PROGRESS', NULL, 2, '验收测试进行中',       1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 完成总结
-- =====================================================================================
-- 本脚本共插入演示数据：
--   - 角色：2 个新增（WAREHOUSE / QUALITY）
--   - 用户：10 个测试账号（覆盖 7 内部 + 3 外部角色）
--   - 用户角色关联：10 条
--   - 角色菜单关联：15 条（WAREHOUSE 9 条 + QUALITY 6 条）
--   - 客户：3 条
--   - 代理商公司：2 条
--   - 代理商工程师：2 条
--   - 自有工程师：1 条
--   - 设备型号：4 条
--   - 设备实例：8 条（IN_FACTORY 2 + INSTALLED 2 + ONLINE 2 + REPAIR 1 + EOL 1）
--   - 项目：10 条（PLAN 2 + EXECUTE 2 + ACCEPT 2 + CLOSE 2 + CANCELLED 2）
--   - 项目阶段：10 条
--   - 项目任务：12 条（PENDING 3 + IN_PROGRESS 3 + COMPLETED 3 + CANCELLED 3）
--   - 项目成员：32 条
--   - 转包任务：2 条（IN_PROGRESS 1 + CONFIRMED 1）
--   - 验收任务：6 条（DRAFT/APPLIED/INTERNAL_AUDITED/CUSTOMER_SIGNING/COMPLETED/REJECTED 各 1）
--   - 财务预算：4 条（DRAFT/PENDING/APPROVED/REJECTED 各 1）
--   - 财务成本：4 条（LABOR/TRAVEL/AGENT/OTHER 各 1）
--   - 财务工作量确认：3 条（DRAFT/PM_CONFIRMED/FINANCE_APPROVED 各 1）
--   - 工单：2 条（COMPLETED 1 + IN_PROGRESS 1）
-- 实体关系全链路：
--   客户(1-3) → 项目(1001-1010) → 阶段(1001-1010) → 任务(2001-2012) → 设备(1003-1008)
--                                                          ↓
--                                                    转包任务(3001-3002) → 代理商(1-2) → 代理商工程师(1-2)
--   项目(1005/1007) → 验收(4001-4006) → 财务预算(5001-5004) / 财务成本(5101-5104) / 财务工作量(5201-5203)
-- 幂等性：所有 INSERT 使用 INSERT IGNORE，可重复执行。
