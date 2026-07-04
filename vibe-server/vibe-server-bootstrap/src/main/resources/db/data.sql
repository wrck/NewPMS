-- =====================================================================================
-- 网络设备原厂实施项目管理系统 - MVP（Phase 1）初始化数据脚本
-- 数据库：vibe_db
-- 内容：默认超管、内置角色、菜单及权限、数据字典、默认项目模板、默认仓库、通知模板
-- 说明：种子数据使用固定小整数 ID（雪花算法由应用层生成，运行时不会与这些小 ID 冲突）
-- =====================================================================================

USE `vibe_db`;
SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空（按依赖倒序，便于重复执行）
DELETE FROM `sys_role_menu`;
DELETE FROM `sys_user_role`;
DELETE FROM `sys_notice_template`;
DELETE FROM `sys_notice`;
DELETE FROM `sys_dict_data`;
DELETE FROM `sys_dict_type`;
DELETE FROM `sys_menu`;
DELETE FROM `sys_role`;
DELETE FROM `sys_org`;
DELETE FROM `sys_user`;
DELETE FROM `project_template_task`;
DELETE FROM `project_template_phase`;
DELETE FROM `project_template`;
DELETE FROM `warehouse`;


-- =====================================================================================
-- 一、组织架构 & 系统用户 & 角色
-- =====================================================================================

-- 默认组织
INSERT INTO `sys_org` (`id`, `parent_id`, `org_name`, `org_code`, `sort_order`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 0, '网络设备实施本部', 'HQ', 1, 1, 1, NOW(), 1, NOW(), 0);

-- 默认超级管理员（username: admin / password: admin123，密码为 BCrypt 加密）
-- BCrypt hash 对应明文：admin123（已使用 Python bcrypt 库验证）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `avatar`, `status`, `tenant_type`, `tenant_id`, `org_id`, `last_login_time`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 'admin', '$2a$10$VV2l4ZaqyUWJJD4.HBC16.2LzQgkE2KJ0rS3s/dA44NmPa0KFu/rK', '超级管理员', '13800000000', 'admin@vibe.com', NULL, 'ACTIVE', 'INTERNAL', NULL, 1, NULL, 1, NOW(), 1, NOW(), 0);

-- 内置角色（10 个）
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1,  '超级管理员',   'SUPER_ADMIN',    '系统超级管理员，拥有全部权限',         1, 'ALL',    1, NOW(), 1, NOW(), 0),
(2,  '实施总监',     'DIRECTOR',       '实施业务总监，查看全部项目与报表',     1, 'ALL',    1, NOW(), 1, NOW(), 0),
(3,  '项目经理',     'PM',             '项目经理，管理本人负责的项目',         1, 'DEPT',   1, NOW(), 1, NOW(), 0),
(4,  '调度员',       'DISPATCHER',     '资源调度员，负责工程师排期与派单',     1, 'DEPT',   1, NOW(), 1, NOW(), 0),
(5,  '实施工程师',   'ENGINEER',       '自有实施工程师，执行派发任务',         1, 'SELF',   1, NOW(), 1, NOW(), 0),
(6,  '设备管理员',   'DEVICE_ADMIN',   '设备资产管理员，管理设备与库存',       1, 'DEPT',   1, NOW(), 1, NOW(), 0),
(7,  '财务',         'FINANCE',        '财务人员，负责成本与结算',             1, 'DEPT',   1, NOW(), 1, NOW(), 0),
(8,  '代理商管理员', 'AGENT_ADMIN',    '代理商公司管理员，管理本公司转包任务', 1, 'CUSTOM', 1, NOW(), 1, NOW(), 0),
(9,  '代理商工程师', 'AGENT_ENGINEER', '代理商工程师，执行代施任务',           1, 'SELF',   1, NOW(), 1, NOW(), 0),
(10, '客户',         'CUSTOMER',       '甲方客户，查看项目进度与签核',         1, 'CUSTOM', 1, NOW(), 1, NOW(), 0);

-- admin 关联超级管理员角色
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 1, 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 二、菜单 & 角色菜单关联（admin 关联全部菜单）
-- =====================================================================================

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 顶级
(1,  0,  '工作台',       'MENU',   '/dashboard',         'dashboard/index',        NULL,                  'Dashboard',  1,  1, 1, NOW(), 1, NOW(), 0),
(2,  0,  '项目管理',     'MENU',   '/project',           NULL,                     NULL,                  'Project',    2,  1, 1, NOW(), 1, NOW(), 0),
(7,  0,  '设备资产',     'MENU',   '/device',            NULL,                     NULL,                  'Device',     3,  1, 1, NOW(), 1, NOW(), 0),
(13, 0,  '资源调度',     'MENU',   '/resource',          NULL,                     NULL,                  'Resource',   4,  1, 1, NOW(), 1, NOW(), 0),
(18, 0,  '代理商管理',   'MENU',   '/agent',             NULL,                     NULL,                  'Agent',      5,  1, 1, NOW(), 1, NOW(), 0),
(22, 0,  '系统管理',     'MENU',   '/system',            NULL,                     NULL,                  'System',     6,  1, 1, NOW(), 1, NOW(), 0),
(30, 0,  '报表分析',     'MENU',   '/report',            NULL,                     NULL,                  'Report',     7,  1, 1, NOW(), 1, NOW(), 0),
-- 项目管理子菜单
(3,  2,  '项目列表',     'MENU',   'list',               'project/index',          'project:list',        NULL,         1,  1, 1, NOW(), 1, NOW(), 0),
(4,  2,  '项目模板',     'MENU',   'template',           'project/template',       'project:template',    NULL,         2,  1, 1, NOW(), 1, NOW(), 0),
-- 设备资产子菜单
(8,  7,  '设备型号库',   'MENU',   'model',              'device/model',           'device:model',        NULL,         1,  1, 1, NOW(), 1, NOW(), 0),
(9,  7,  '设备台账',     'MENU',   'instance',           'device/instance',        'device:instance',     NULL,         2,  1, 1, NOW(), 1, NOW(), 0),
(10, 7,  '出入库管理',   'MENU',   'inventory',          'device/inventory',       'device:inventory',    NULL,         3,  1, 1, NOW(), 1, NOW(), 0),
(11, 7,  '备件管理',     'MENU',   'spare',              'device/spare',           'device:spare',        NULL,         4,  1, 1, NOW(), 1, NOW(), 0),
(12, 7,  '设备看板',     'MENU',   'dashboard',          'device/board',           'device:board',        NULL,         5,  1, 1, NOW(), 1, NOW(), 0),
-- 资源调度子菜单
(14, 13, '工程师资源池', 'MENU',   'engineer',           'resource/engineer',      'resource:engineer',   NULL,         1,  1, 1, NOW(), 1, NOW(), 0),
(15, 13, '排期管理',     'MENU',   'schedule',           'resource/schedule',      'resource:schedule',   NULL,         2,  1, 1, NOW(), 1, NOW(), 0),
(16, 13, '任务派发',     'MENU',   'dispatch',           'resource/dispatch',      'resource:dispatch',   NULL,         3,  1, 1, NOW(), 1, NOW(), 0),
(17, 13, '工时管理',     'MENU',   'timesheet',          'resource/timesheet',     'resource:timesheet',  NULL,         4,  1, 1, NOW(), 1, NOW(), 0),
-- 代理商管理子菜单
(19, 18, '代理商档案',   'MENU',   'company',            'agent/company',          'agent:company',       NULL,         1,  1, 1, NOW(), 1, NOW(), 0),
(20, 18, '转包任务',     'MENU',   'outsource',          'agent/outsource',        'agent:outsource',     NULL,         2,  1, 1, NOW(), 1, NOW(), 0),
(21, 18, '质量评分',     'MENU',   'score',              'agent/score',            'agent:score',         NULL,         3,  1, 1, NOW(), 1, NOW(), 0),
-- 系统管理子菜单
(23, 22, '用户管理',     'MENU',   'user',               'system/user',            'system:user',         NULL,         1,  1, 1, NOW(), 1, NOW(), 0),
(24, 22, '角色管理',     'MENU',   'role',               'system/role',            'system:role',         NULL,         2,  1, 1, NOW(), 1, NOW(), 0),
(25, 22, '菜单管理',     'MENU',   'menu',               'system/menu',            'system:menu',         NULL,         3,  1, 1, NOW(), 1, NOW(), 0),
(26, 22, '组织架构',     'MENU',   'org',                'system/org',             'system:org',          NULL,         4,  1, 1, NOW(), 1, NOW(), 0),
(27, 22, '数据字典',     'MENU',   'dict',               'system/dict',            'system:dict',         NULL,         5,  1, 1, NOW(), 1, NOW(), 0),
(28, 22, '系统配置',     'MENU',   'config',             'system/config',          'system:config',       NULL,         6,  1, 1, NOW(), 1, NOW(), 0),
(29, 22, '操作日志',     'MENU',   'log',                'system/log',             'system:log',          NULL,         7,  1, 1, NOW(), 1, NOW(), 0),
-- 报表分析子菜单
(31, 30, '管理驾驶舱',   'MENU',   'dashboard',          'report/dashboard',       'report:dashboard',    NULL,         1,  1, 1, NOW(), 1, NOW(), 0),
(32, 30, '项目报表',     'MENU',   'project',            'report/project',         'report:project',      NULL,         2,  1, 1, NOW(), 1, NOW(), 0),
-- 按钮权限
(5,  2,  '项目立项',     'BUTTON', NULL,                 NULL,                     'project:add',         NULL,         10, 0, 1, NOW(), 1, NOW(), 0),
(6,  2,  '项目编辑',     'BUTTON', NULL,                 NULL,                     'project:edit',        NULL,         11, 0, 1, NOW(), 1, NOW(), 0),
(33, 2,  '项目删除',     'BUTTON', NULL,                 NULL,                     'project:remove',      NULL,         12, 0, 1, NOW(), 1, NOW(), 0),
(34, 22, '用户新增',     'BUTTON', NULL,                 NULL,                     'system:user:add',     NULL,         10, 0, 1, NOW(), 1, NOW(), 0),
(35, 22, '角色分配',     'BUTTON', NULL,                 NULL,                     'system:role:assign',  NULL,         11, 0, 1, NOW(), 1, NOW(), 0),
(36, 7,  '设备导入',     'BUTTON', NULL,                 NULL,                     'device:import',       NULL,         10, 0, 1, NOW(), 1, NOW(), 0);

-- 超级管理员（role_id=1）关联全部菜单
SET @row_num := 0;
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
SELECT (@row_num := @row_num + 1) AS rn, 1, `id`, 1, NOW(), 1, NOW(), 0
FROM `sys_menu`
WHERE `deleted` = 0;


-- =====================================================================================
-- 三、数据字典（类型 + 数据）
-- =====================================================================================

-- 字典类型
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '项目类型', 'project_type',     1, '项目类型字典', 1, NOW(), 1, NOW(), 0),
(2, '产品线',   'product_line',     1, '产品线字典',   1, NOW(), 1, NOW(), 0),
(3, '区域',     'region',           1, '区域字典',     1, NOW(), 1, NOW(), 0),
(4, '设备类别', 'device_category',  1, '设备类别字典', 1, NOW(), 1, NOW(), 0),
(5, '任务类型', 'task_type',        1, '任务类型字典', 1, NOW(), 1, NOW(), 0),
(6, '优先级',   'priority',         1, '优先级字典',   1, NOW(), 1, NOW(), 0),
(7, '执行模式', 'execute_mode',     1, '执行模式字典', 1, NOW(), 1, NOW(), 0);

-- 字典数据
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort_order`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 项目类型
(1,  'project_type', '新建', '新建',     1, 1, 1, NOW(), 1, NOW(), 0),
(2,  'project_type', '扩容', '扩容',     2, 1, 1, NOW(), 1, NOW(), 0),
(3,  'project_type', '改造', '改造',     3, 1, 1, NOW(), 1, NOW(), 0),
(4,  'project_type', '替换', '替换',     4, 1, 1, NOW(), 1, NOW(), 0),
(5,  'project_type', '安全', '安全',     5, 1, 1, NOW(), 1, NOW(), 0),
-- 产品线
(6,  'product_line', '路由',     '路由',       1, 1, 1, NOW(), 1, NOW(), 0),
(7,  'product_line', '交换',     '交换',       2, 1, 1, NOW(), 1, NOW(), 0),
(8,  'product_line', '无线',     '无线',       3, 1, 1, NOW(), 1, NOW(), 0),
(9,  'product_line', '安全',     '安全',       4, 1, 1, NOW(), 1, NOW(), 0),
(10, 'product_line', '数据中心', '数据中心',   5, 1, 1, NOW(), 1, NOW(), 0),
-- 区域
(11, 'region', '华北', '华北', 1, 1, 1, NOW(), 1, NOW(), 0),
(12, 'region', '华东', '华东', 2, 1, 1, NOW(), 1, NOW(), 0),
(13, 'region', '华南', '华南', 3, 1, 1, NOW(), 1, NOW(), 0),
(14, 'region', '西南', '西南', 4, 1, 1, NOW(), 1, NOW(), 0),
(15, 'region', '西北', '西北', 5, 1, 1, NOW(), 1, NOW(), 0),
(16, 'region', '东北', '东北', 6, 1, 1, NOW(), 1, NOW(), 0),
-- 设备类别
(17, 'device_category', '路由器',   'ROUTER',   1, 1, 1, NOW(), 1, NOW(), 0),
(18, 'device_category', '交换机',   'SWITCH',   2, 1, 1, NOW(), 1, NOW(), 0),
(19, 'device_category', '无线AP',   'AP',       3, 1, 1, NOW(), 1, NOW(), 0),
(20, 'device_category', '防火墙',   'FIREWALL', 4, 1, 1, NOW(), 1, NOW(), 0),
(21, 'device_category', '无线控制器','WLC',     5, 1, 1, NOW(), 1, NOW(), 0),
(22, 'device_category', '负载均衡', 'LB',       6, 1, 1, NOW(), 1, NOW(), 0),
(23, 'device_category', '其他',     'OTHER',    7, 1, 1, NOW(), 1, NOW(), 0),
-- 任务类型
(24, 'task_type', '勘测', 'SURVEY',   1, 1, 1, NOW(), 1, NOW(), 0),
(25, 'task_type', '安装', 'INSTALL',  2, 1, 1, NOW(), 1, NOW(), 0),
(26, 'task_type', '调试', 'DEBUG',    3, 1, 1, NOW(), 1, NOW(), 0),
(27, 'task_type', '割接', 'CUTOVER',  4, 1, 1, NOW(), 1, NOW(), 0),
(28, 'task_type', '验收', 'ACCEPT',   5, 1, 1, NOW(), 1, NOW(), 0),
(29, 'task_type', '其他', 'OTHER',    6, 1, 1, NOW(), 1, NOW(), 0),
-- 优先级
(30, 'priority', 'P0-最高', 'P0', 1, 1, 1, NOW(), 1, NOW(), 0),
(31, 'priority', 'P1-高',   'P1', 2, 1, 1, NOW(), 1, NOW(), 0),
(32, 'priority', 'P2-中',   'P2', 3, 1, 1, NOW(), 1, NOW(), 0),
(33, 'priority', 'P3-低',   'P3', 4, 1, 1, NOW(), 1, NOW(), 0),
-- 执行模式
(34, 'execute_mode', '自施',     'SELF',  1, 1, 1, NOW(), 1, NOW(), 0),
(35, 'execute_mode', '代施',     'AGENT', 2, 1, 1, NOW(), 1, NOW(), 0),
(36, 'execute_mode', '混合实施', 'MIXED', 3, 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 四、默认项目模板（通用网络改造模板，6 阶段 + 标准任务）
-- =====================================================================================

INSERT INTO `project_template` (`id`, `template_name`, `project_type`, `product_line`, `description`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '通用网络改造模板', '改造', NULL, '适用于一般网络改造类项目的标准阶段与任务模板，包含勘测、设计、交付、安装、调试、验收 6 个阶段', 1, 1, NOW(), 1, NOW(), 0);

-- 模板阶段（6 阶段）
INSERT INTO `project_template_phase` (`id`, `template_id`, `phase_code`, `phase_name`, `sort_order`, `deliverables`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 1, 'SURVEY',  '勘测阶段', 1, '["现场勘测报告","网络拓扑现状图"]',                              1, NOW(), 1, NOW(), 0),
(2, 1, 'DESIGN',  '设计阶段', 2, '["详细设计方案","设备清单"]',                                    1, NOW(), 1, NOW(), 0),
(3, 1, 'DELIVER', '交付阶段', 3, '["到货签收单"]',                                                 1, NOW(), 1, NOW(), 0),
(4, 1, 'INSTALL', '安装阶段', 4, '["安装记录","机柜布置图"]',                                      1, NOW(), 1, NOW(), 0),
(5, 1, 'DEBUG',   '调试阶段', 5, '["调试报告","配置备份文件"]',                                    1, NOW(), 1, NOW(), 0),
(6, 1, 'ACCEPT',  '验收阶段', 6, '["测试报告","竣工文档","验收签核单"]',                            1, NOW(), 1, NOW(), 0);

-- 模板标准任务
INSERT INTO `project_template_task` (`id`, `template_id`, `phase_code`, `task_name`, `task_type`, `description`, `default_days`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
-- 勘测阶段
(1,  1, 'SURVEY',  '现场勘测',       'SURVEY', '现场点位勘测、机房环境确认、现有网络现状梳理', 2, 1, NOW(), 1, NOW(), 0),
(2,  1, 'SURVEY',  '勘测报告输出',   'OTHER',  '整理现场勘测记录，输出勘测报告与拓扑现状图',   1, 1, NOW(), 1, NOW(), 0),
-- 设计阶段
(3,  1, 'DESIGN',  '方案设计',       'OTHER',  '基于勘测结果编制详细实施方案与设备清单',       3, 1, NOW(), 1, NOW(), 0),
(4,  1, 'DESIGN',  '设计评审',       'OTHER',  '组织方案评审，确认技术方案与资源配置',         1, 1, NOW(), 1, NOW(), 0),
-- 交付阶段
(5,  1, 'DELIVER', '设备到货确认',   'OTHER',  '核对到货设备清单，确认 SN 与数量',             1, 1, NOW(), 1, NOW(), 0),
(6,  1, 'DELIVER', '设备入库登记',   'OTHER',  '设备扫码入库，登记仓库与货位',                 1, 1, NOW(), 1, NOW(), 0),
-- 安装阶段
(7,  1, 'INSTALL', '设备安装上架',   'INSTALL','设备上架、电源接入、初检',                     2, 1, NOW(), 1, NOW(), 0),
(8,  1, 'INSTALL', '线缆连接',       'INSTALL','电源线、网线、光纤布线与标签制作',             1, 1, NOW(), 1, NOW(), 0),
-- 调试阶段
(9,  1, 'DEBUG',   '设备加电调试',   'DEBUG',  '设备加电自检、基础配置',                       1, 1, NOW(), 1, NOW(), 0),
(10, 1, 'DEBUG',   '配置下发',       'DEBUG',  '按设计下发配置，校验配置正确性',               1, 1, NOW(), 1, NOW(), 0),
(11, 1, 'DEBUG',   '联调测试',       'DEBUG',  '端到端联调，验证业务连通性与冗余切换',         2, 1, NOW(), 1, NOW(), 0),
-- 验收阶段
(12, 1, 'ACCEPT',  '验收测试',       'ACCEPT', '功能测试、性能测试、冗余切换测试',             2, 1, NOW(), 1, NOW(), 0),
(13, 1, 'ACCEPT',  '竣工文档整理',   'OTHER',  '整理 As-Built 拓扑、配置备份、测试报告',       2, 1, NOW(), 1, NOW(), 0),
(14, 1, 'ACCEPT',  '终验签核',       'ACCEPT', '发起客户验收签核，归档项目文档',               1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 五、默认仓库（北京中心仓）
-- =====================================================================================

INSERT INTO `warehouse` (`id`, `warehouse_name`, `warehouse_code`, `address`, `region`, `manager_id`, `safety_stock`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '北京中心仓', 'WH-BJ-001', '北京市海淀区中关村软件园', '华北', NULL, NULL, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 六、通知模板初始数据（9 个 MVP 模板）
-- =====================================================================================

INSERT INTO `sys_notice_template` (`id`, `template_code`, `template_name`, `title_template`, `content_template`, `channels`, `recipient_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 'TASK_ASSIGNED',          '任务派发通知',     '您有新的实施任务：${taskName}',
   '您被指派执行任务【${taskName}】（项目：${projectName}）。计划开始：${plannedStart}，计划完成：${plannedEnd}。请及时登录系统查看任务详情并开始执行。',
   '["FEISHU","SITE"]', 'ENGINEER', 1, 1, NOW(), 1, NOW(), 0),

(2, 'TASK_REMINDER',          '任务到期提醒',     '任务即将到期：${taskName}',
   '您负责的任务【${taskName}】（项目：${projectName}）将于 ${plannedEnd} 到期，请尽快推进完成。',
   '["FEISHU","SITE"]', 'ENGINEER', 1, 1, NOW(), 1, NOW(), 0),

(3, 'TASK_OVERDUE',           '任务超期预警',     '任务已超期：${taskName}',
   '任务【${taskName}】（项目：${projectName}）已超过计划完成日期 ${plannedEnd}，请立即处理。该预警已同步通知项目经理与上级。',
   '["FEISHU","SITE","SMS"]', 'ENGINEER', 1, 1, NOW(), 1, NOW(), 0),

(4, 'DELIVERABLE_REVIEW',     '交付物审核通知',   '交付物待您审核：${taskName}',
   '代理商已提交任务【${taskName}】（项目：${projectName}）的交付物，提交次数：${submitCount}。请登录系统审核交付物。',
   '["FEISHU","SITE"]', 'PM', 1, 1, NOW(), 1, NOW(), 0),

(5, 'DELIVERABLE_RETURNED',   '交付物退回通知',   '交付物被退回：${taskName}',
   '您提交的任务【${taskName}】（项目：${projectName}）交付物被退回，退回原因：${rejectReason}。请整改后重新提交。',
   '["FEISHU","SITE"]', 'AGENT', 1, 1, NOW(), 1, NOW(), 0),

(6, 'DELIVERABLE_CONFIRMED',  '交付物审核通过',   '交付物审核通过：${taskName}',
   '您提交的任务【${taskName}】（项目：${projectName}）交付物已审核通过，任务确认完成。',
   '["FEISHU","SITE"]', 'AGENT', 1, 1, NOW(), 1, NOW(), 0),

(7, 'DEVICE_ARRIVED',         '设备到货通知',     '设备已到货：${modelName}',
   '项目【${projectName}】的设备【${modelName}】已到货 ${quantity} 台，请及时安排入库与后续安装。',
   '["FEISHU","SITE"]', 'PM', 1, 1, NOW(), 1, NOW(), 0),

(8, 'DEVICE_ABNORMAL',        '设备状态异常',     '设备状态异常：${serialNumber}',
   '设备【${serialNumber}】（型号：${modelName}，项目：${projectName}）出现状态异常：${abnormalDesc}。请及时处理。',
   '["FEISHU","SITE","SMS"]', 'PM', 1, 1, NOW(), 1, NOW(), 0),

(9, 'RISK_WARNING',           '项目风险预警',     '项目风险预警：${projectName}',
   '项目【${projectName}】存在风险：${riskDesc}（影响：${impact}，概率：${probability}）。请及时关注并采取应对措施。',
   '["FEISHU","SITE","SMS"]', 'PM', 1, 1, NOW(), 1, NOW(), 0);


SET FOREIGN_KEY_CHECKS = 1;
