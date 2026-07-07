-- =====================================================================================
-- V13 低代码业务接入示例种子数据（Task 19：3 个业务接入示例）
-- =====================================================================================
-- 内容（complete-production-readiness Task 19）：
--   1. 设备巡检表单（form-config）
--        - configCode = 'device-inspection'
--        - 字段：设备SN / 巡检日期 / 巡检人 / 外观检查 / 功能测试 / 备注 / 照片
--        - 提交接口：POST /api/v1/device-inspections
--   2. 客户回访列表（list-config）
--        - configCode = 'customer-followup'（区别于既有 'customer' 客户档案列表）
--        - 列：客户名称 / 回访日期 / 回访人 / 满意度 / 备注
--        - 操作：编辑 / 查看
--        - 数据源：/api/v1/customer-followups
--   3. 项目阶段交付关联页（relation-config）
--        - configCode = 'project-phase-delivery'
--        - 主表：项目阶段（apiUrl=/api/v1/projects/phases）
--        - 从表：阶段交付物（apiUrl=/api/v1/projects/deliverables，foreignKey=phaseId）
--
-- 同时插入对应模板（lowcode_template）3 条，便于在模板库中复用：
--   - tpl_device_inspection_form（FORM）
--   - tpl_customer_followup_list（LIST）
--   - tpl_project_phase_delivery_relation（RELATION）
--
-- 幂等策略：INSERT IGNORE 依赖唯一索引 uk_*_config_code / uk_template_code。
-- 兼容性：MySQL 5.7 / 8.0+，Flyway 8.x+ 与 scripts/init-db.js 均可应用。
-- =====================================================================================

-- =====================================================================================
-- 一、lowcode_form_config：设备巡检表单
-- =====================================================================================
-- FormSchema: type='object'，RuntimeRenderer 会按 properties 渲染 a-form。
-- 字段（按 SubTask 19.1）：
--   1. deviceSn        input   必填  设备SN
--   2. inspectionDate  date    必填  巡检日期
--   3. inspector       input   必填  巡检人
--   4. appearanceCheck select  必填  外观检查（正常/异常）
--   5. functionTest    select  必填  功能测试（通过/不通过）
--   6. remark          textarea 可选  备注
--   7. photos          file     可选  照片上传（多文件）

INSERT IGNORE INTO `lowcode_form_config`
  (`id`, `config_code`, `config_name`, `schema_json`, `template_id`, `version`, `status`, `description`, `creator_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
(2001, 'device-inspection', '设备巡检表单',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"object","title":"设备巡检表单","description":"现场设备巡检记录表单（Task 19.1 业务接入示例）","layout":"vertical","apiUrl":"/api/v1/device-inspections","apiMethod":"POST","properties":{"deviceSn":{"field":"deviceSn","label":"设备SN","type":"input","dataType":"string","required":true,"placeholder":"请输入设备SN","width":12,"order":0,"rules":[{"required":true,"message":"请输入设备SN","trigger":"blur"}]},"inspectionDate":{"field":"inspectionDate","label":"巡检日期","type":"date","dataType":"string","required":true,"placeholder":"请选择巡检日期","width":12,"order":1},"inspector":{"field":"inspector","label":"巡检人","type":"input","dataType":"string","required":true,"placeholder":"请输入巡检人姓名","width":12,"order":2},"appearanceCheck":{"field":"appearanceCheck","label":"外观检查","type":"select","dataType":"string","required":true,"placeholder":"请选择外观检查结果","width":12,"order":3,"options":[{"label":"正常","value":"PASS"},{"label":"异常","value":"FAIL"}]},"functionTest":{"field":"functionTest","label":"功能测试","type":"select","dataType":"string","required":true,"placeholder":"请选择功能测试结果","width":12,"order":4,"options":[{"label":"通过","value":"PASS"},{"label":"不通过","value":"FAIL"}]},"remark":{"field":"remark","label":"备注","type":"textarea","dataType":"string","required":false,"placeholder":"补充说明（可选）","width":24,"order":5},"photos":{"field":"photos","label":"现场照片","type":"file","dataType":"array","required":false,"placeholder":"上传现场照片","width":24,"order":6}}}',
4001, 1, 1, '设备巡检表单（Task 19.1 业务接入示例）：含 7 个字段，覆盖 input/date/select/textarea/file 五类组件', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 二、lowcode_list_config：客户回访列表
-- =====================================================================================
-- ListSchema: type='list'，RuntimeRenderer 会渲染搜索表单 + a-table + 操作按钮。
-- 列（按 SubTask 19.2）：客户名称 / 回访日期 / 回访人 / 满意度 / 备注
-- 操作：编辑 / 查看（不提供删除，避免误操作）

INSERT IGNORE INTO `lowcode_list_config`
  (`id`, `config_code`, `config_name`, `schema_json`, `template_id`, `version`, `status`, `description`, `creator_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
(1011, 'customer-followup', '客户回访列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"客户回访列表","description":"客户回访记录低代码列表配置（Task 19.2 业务接入示例）","columns":[{"field":"customerName","title":"客户名称","width":180},{"field":"followupDate","title":"回访日期","width":120},{"field":"followupBy","title":"回访人","width":120},{"field":"satisfaction","title":"满意度","width":100,"align":"center","valueEnum":{"5":{"text":"★★★★★","status":"success"},"4":{"text":"★★★★","status":"processing"},"3":{"text":"★★★","status":"warning"},"2":{"text":"★★","status":"warning"},"1":{"text":"★","status":"error"}}},{"field":"remark","title":"备注","width":240,"ellipsis":true}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"客户名称/回访人"},{"field":"satisfaction","label":"满意度","type":"select","options":[{"label":"5星","value":5},{"label":"4星","value":4},{"label":"3星","value":3},{"label":"2星","value":2},{"label":"1星","value":1}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"view","label":"查看"}],"apiUrl":"/api/v1/customer-followups","rowKey":"id","pageSize":10,"scrollX":900,"formFields":[{"field":"customerName","label":"客户名称","type":"input","required":true,"width":12},{"field":"followupDate","label":"回访日期","type":"date","required":true,"width":12},{"field":"followupBy","label":"回访人","type":"input","required":true,"width":12},{"field":"satisfaction","label":"满意度","type":"select","required":true,"defaultValue":5,"width":12,"options":[{"label":"5星","value":5},{"label":"4星","value":4},{"label":"3星","value":3},{"label":"2星","value":2},{"label":"1星","value":1}]},{"field":"remark","label":"备注","type":"textarea","width":24}]}',
4002, 1, 1, '客户回访列表（Task 19.2 业务接入示例）：5 列 + 2 个搜索条件 + 3 个操作按钮', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 三、lowcode_relation_config：项目阶段交付关联页
-- =====================================================================================
-- RelationSchema: type='relation'，RuntimeRenderer 会渲染主从关联页。
-- 主表：项目阶段（bizType=project-phase, apiUrl=/api/v1/projects/phases）
-- 从表：阶段交付物（bizType=phase-deliverable, apiUrl=/api/v1/projects/deliverables, foreignKey=phaseId）
-- 显示字段：主表显示 阶段名/计划开始/计划结束/状态；从表显示 交付物名/类型/提交时间/状态

INSERT IGNORE INTO `lowcode_relation_config`
  (`id`, `config_code`, `config_name`, `schema_json`, `template_id`, `version`, `status`, `description`, `creator_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
(3001, 'project-phase-delivery', '项目阶段交付关联页',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"relation","title":"项目阶段交付关联页","description":"主表项目阶段 + 从表阶段交付物（Task 19.3 业务接入示例）","master":{"bizType":"project-phase","label":"项目阶段","apiUrl":"/api/v1/projects/phases","rowKey":"id","displayField":"phaseName","columns":[{"field":"phaseName","title":"阶段名称","width":160},{"field":"planStart","title":"计划开始","width":120},{"field":"planEnd","title":"计划结束","width":120},{"field":"status","title":"状态","width":100,"valueEnum":{"PENDING":{"text":"未开始","status":"default"},"IN_PROGRESS":{"text":"进行中","status":"processing"},"DONE":{"text":"已完成","status":"success"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"阶段名称"}]},"details":[{"bizType":"phase-deliverable","label":"阶段交付物","apiUrl":"/api/v1/projects/deliverables","rowKey":"id","foreignKey":"phaseId","defaultExpand":true,"columns":[{"field":"deliverableName","title":"交付物名称","width":200},{"field":"deliverableType","title":"类型","width":100,"valueEnum":{"DOCUMENT":{"text":"文档","status":"processing"},"CODE":{"text":"代码","status":"success"},"ARTIFACT":{"text":"实物","status":"warning"}}},{"field":"submitTime","title":"提交时间","width":140},{"field":"status","title":"状态","width":100,"valueEnum":{"DRAFT":{"text":"草稿","status":"default"},"SUBMITTED":{"text":"已提交","status":"processing"},"ACCEPTED":{"text":"已验收","status":"success"},"REJECTED":{"text":"已驳回","status":"error"}}}],"formFields":[{"field":"deliverableName","label":"交付物名称","type":"input","required":true,"width":24},{"field":"deliverableType","label":"类型","type":"select","required":true,"width":12,"options":[{"label":"文档","value":"DOCUMENT"},{"label":"代码","value":"CODE"},{"label":"实物","value":"ARTIFACT"}]},{"field":"submitTime","label":"提交时间","type":"date","width":12},{"field":"status","label":"状态","type":"select","defaultValue":"DRAFT","width":12,"options":[{"label":"草稿","value":"DRAFT"},{"label":"已提交","value":"SUBMITTED"},{"label":"已验收","value":"ACCEPTED"},{"label":"已驳回","value":"REJECTED"}]}]}]}',
4003, 1, 1, '项目阶段交付关联页（Task 19.3 业务接入示例）：主表 4 列 + 从表 4 列 + 外键 phaseId', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 四、lowcode_template：3 个对应模板（便于模板库复用）
-- =====================================================================================
-- 模板 schema_json 与上述配置完全一致，template_type 区分类型。

INSERT IGNORE INTO `lowcode_template`
  (`id`, `template_code`, `template_name`, `template_type`, `schema_json`, `description`, `usage_count`, `version`, `status`, `creator_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
-- 1. 设备巡检表单模板（FORM）
(4001, 'tpl_device_inspection_form', '设备巡检表单模板', 'FORM',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"object","title":"设备巡检表单","description":"现场设备巡检记录表单模板","layout":"vertical","apiUrl":"/api/v1/device-inspections","apiMethod":"POST","properties":{"deviceSn":{"field":"deviceSn","label":"设备SN","type":"input","dataType":"string","required":true,"placeholder":"请输入设备SN","width":12,"order":0,"rules":[{"required":true,"message":"请输入设备SN","trigger":"blur"}]},"inspectionDate":{"field":"inspectionDate","label":"巡检日期","type":"date","dataType":"string","required":true,"placeholder":"请选择巡检日期","width":12,"order":1},"inspector":{"field":"inspector","label":"巡检人","type":"input","dataType":"string","required":true,"placeholder":"请输入巡检人姓名","width":12,"order":2},"appearanceCheck":{"field":"appearanceCheck","label":"外观检查","type":"select","dataType":"string","required":true,"placeholder":"请选择外观检查结果","width":12,"order":3,"options":[{"label":"正常","value":"PASS"},{"label":"异常","value":"FAIL"}]},"functionTest":{"field":"functionTest","label":"功能测试","type":"select","dataType":"string","required":true,"placeholder":"请选择功能测试结果","width":12,"order":4,"options":[{"label":"通过","value":"PASS"},{"label":"不通过","value":"FAIL"}]},"remark":{"field":"remark","label":"备注","type":"textarea","dataType":"string","required":false,"placeholder":"补充说明（可选）","width":24,"order":5},"photos":{"field":"photos","label":"现场照片","type":"file","dataType":"array","required":false,"placeholder":"上传现场照片","width":24,"order":6}}}',
'设备巡检表单模板（Task 19.1）：含 7 字段，覆盖 input/date/select/textarea/file 五类组件', 0, 1, 1, 1, 1, NOW(), 1, NOW(), 0),

-- 2. 客户回访列表模板（LIST）
(4002, 'tpl_customer_followup_list', '客户回访列表模板', 'LIST',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"客户回访列表","description":"客户回访记录低代码列表模板","columns":[{"field":"customerName","title":"客户名称","width":180},{"field":"followupDate","title":"回访日期","width":120},{"field":"followupBy","title":"回访人","width":120},{"field":"satisfaction","title":"满意度","width":100,"align":"center","valueEnum":{"5":{"text":"★★★★★","status":"success"},"4":{"text":"★★★★","status":"processing"},"3":{"text":"★★★","status":"warning"},"2":{"text":"★★","status":"warning"},"1":{"text":"★","status":"error"}}},{"field":"remark","title":"备注","width":240,"ellipsis":true}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"客户名称/回访人"},{"field":"satisfaction","label":"满意度","type":"select","options":[{"label":"5星","value":5},{"label":"4星","value":4},{"label":"3星","value":3},{"label":"2星","value":2},{"label":"1星","value":1}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"view","label":"查看"}],"apiUrl":"/api/v1/customer-followups","rowKey":"id","pageSize":10,"scrollX":900,"formFields":[{"field":"customerName","label":"客户名称","type":"input","required":true,"width":12},{"field":"followupDate","label":"回访日期","type":"date","required":true,"width":12},{"field":"followupBy","label":"回访人","type":"input","required":true,"width":12},{"field":"satisfaction","label":"满意度","type":"select","required":true,"defaultValue":5,"width":12,"options":[{"label":"5星","value":5},{"label":"4星","value":4},{"label":"3星","value":3},{"label":"2星","value":2},{"label":"1星","value":1}]},{"field":"remark","label":"备注","type":"textarea","width":24}]}',
'客户回访列表模板（Task 19.2）：5 列 + 2 搜索 + 3 操作', 0, 1, 1, 1, 1, NOW(), 1, NOW(), 0),

-- 3. 项目阶段交付关联页模板（RELATION）
(4003, 'tpl_project_phase_delivery_relation', '项目阶段交付关联页模板', 'RELATION',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"relation","title":"项目阶段交付关联页","description":"主表项目阶段 + 从表阶段交付物模板","master":{"bizType":"project-phase","label":"项目阶段","apiUrl":"/api/v1/projects/phases","rowKey":"id","displayField":"phaseName","columns":[{"field":"phaseName","title":"阶段名称","width":160},{"field":"planStart","title":"计划开始","width":120},{"field":"planEnd","title":"计划结束","width":120},{"field":"status","title":"状态","width":100,"valueEnum":{"PENDING":{"text":"未开始","status":"default"},"IN_PROGRESS":{"text":"进行中","status":"processing"},"DONE":{"text":"已完成","status":"success"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"阶段名称"}]},"details":[{"bizType":"phase-deliverable","label":"阶段交付物","apiUrl":"/api/v1/projects/deliverables","rowKey":"id","foreignKey":"phaseId","defaultExpand":true,"columns":[{"field":"deliverableName","title":"交付物名称","width":200},{"field":"deliverableType","title":"类型","width":100,"valueEnum":{"DOCUMENT":{"text":"文档","status":"processing"},"CODE":{"text":"代码","status":"success"},"ARTIFACT":{"text":"实物","status":"warning"}}},{"field":"submitTime","title":"提交时间","width":140},{"field":"status","title":"状态","width":100,"valueEnum":{"DRAFT":{"text":"草稿","status":"default"},"SUBMITTED":{"text":"已提交","status":"processing"},"ACCEPTED":{"text":"已验收","status":"success"},"REJECTED":{"text":"已驳回","status":"error"}}}],"formFields":[{"field":"deliverableName","label":"交付物名称","type":"input","required":true,"width":24},{"field":"deliverableType","label":"类型","type":"select","required":true,"width":12,"options":[{"label":"文档","value":"DOCUMENT"},{"label":"代码","value":"CODE"},{"label":"实物","value":"ARTIFACT"}]},{"field":"submitTime","label":"提交时间","type":"date","width":12},{"field":"status","label":"状态","type":"select","defaultValue":"DRAFT","width":12,"options":[{"label":"草稿","value":"DRAFT"},{"label":"已提交","value":"SUBMITTED"},{"label":"已验收","value":"ACCEPTED"},{"label":"已驳回","value":"REJECTED"}]}]}]}',
'项目阶段交付关联页模板（Task 19.3）：主表 4 列 + 从表 4 列 + 外键 phaseId', 0, 1, 1, 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 五、sys_menu 与 sys_role_menu：3 个示例运行时入口菜单
-- =====================================================================================
-- 菜单 ID 规划：避开 V10 的 37-58，使用 60-62。
-- 路径：
--   * /lowcode/examples/device-inspection
--   * /lowcode/examples/customer-followup
--   * /lowcode/examples/project-phase-delivery
-- 对应前端路由（在 routes.ts 中新增）。

INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
(60, 37, '设备巡检表单(示例)',     'MENU', 'examples/device-inspection',         'lowcode/examples/device-inspection',         'lowcode:example:device-inspection',       'FormOutlined',    41, 1, 1, NOW(), 1, NOW(), 0),
(61, 37, '客户回访列表(示例)',     'MENU', 'examples/customer-followup',         'lowcode/examples/customer-followup',         'lowcode:example:customer-followup',        'TableOutlined',   42, 1, 1, NOW(), 1, NOW(), 0),
(62, 37, '项目阶段交付(示例)',     'MENU', 'examples/project-phase-delivery',    'lowcode/examples/project-phase-delivery',    'lowcode:example:project-phase-delivery',   'ApartmentOutlined',43, 1, 1, NOW(), 1, NOW(), 0);

INSERT IGNORE INTO `sys_role_menu`
  (`id`, `role_id`, `menu_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
(9023, 1, 60, 1, NOW(), 1, NOW(), 0),
(9024, 1, 61, 1, NOW(), 1, NOW(), 0),
(9025, 1, 62, 1, NOW(), 1, NOW(), 0);
