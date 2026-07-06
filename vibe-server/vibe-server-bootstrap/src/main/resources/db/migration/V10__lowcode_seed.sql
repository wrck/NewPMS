-- =====================================================================================
-- V10 低代码模块种子数据（Task A5.3 + A6.2 + A6.3）
-- =====================================================================================
-- 内容（enterprise-completion Task A6）：
--   1. 为 10 个通用 CRUD 实体插入 lowcode_list_config 记录（configCode = bizType）
--      覆盖实体：customer / device-model / spare-part / warehouse / engineer-skill /
--               agent-engineer / acceptance-standard-item / notice-template /
--               dict-data / position
--   2. 插入 sys_menu 低代码父菜单 + 5 个配置子菜单（lowcode:config:*）
--   3. 插入 sys_menu 10 个运行时视图子菜单（指向 /lowcode/runtime/<bizType>/0）
--   4. 插入 sys_role_menu 关联（admin role_id=1 拥有全部新菜单）
--
-- 幂等策略：
--   * lowcode_list_config: INSERT IGNORE（依赖 uk_list_config_code 唯一约束）
--   * sys_menu: INSERT IGNORE（依赖主键 id 唯一）
--   * sys_role_menu: INSERT IGNORE（依赖主键 id 唯一）
--   * 重复执行安全，不会覆盖既有配置
--
-- 兼容性：
--   * MySQL 5.7 / 8.0+ 均可执行
--   * Flyway 8.x+ 与 scripts/init-db.js（Node.js + mysql2）均可应用
-- =====================================================================================

-- =====================================================================================
-- 一、lowcode_list_config 种子数据（10 个实体）
-- =====================================================================================
-- 说明：schema_json 为前端约定的 ListSchema 结构（type='list'）。
-- 后端只校验 JSON 语法层面（Draft 7 兼容），具体字段语义由前端 RuntimeRenderer 解析。
-- bizId=0 表示新增态；列表态不依赖 bizId，runtime-renderer.vue 会用 configCode 匹配。

INSERT IGNORE INTO `lowcode_list_config`
  (`id`, `config_code`, `config_name`, `schema_json`, `status`, `description`, `creator_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
-- 1. 客户档案
(1001, 'customer', '客户档案列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"客户档案","description":"客户档案低代码列表配置","columns":[{"field":"customerCode","title":"客户编码","width":140},{"field":"customerName","title":"客户名称","width":160},{"field":"contactName","title":"联系人","width":100},{"field":"contactPhone","title":"联系电话","width":120},{"field":"region","title":"区域","width":80},{"field":"industry","title":"行业","width":100}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/customers","rowKey":"id","pageSize":10,"formFields":[{"field":"customerCode","label":"客户编码","type":"input","required":true,"width":12},{"field":"customerName","label":"客户名称","type":"input","required":true,"width":12},{"field":"contactName","label":"联系人","type":"input","width":12},{"field":"contactPhone","label":"联系电话","type":"input","width":12},{"field":"contactEmail","label":"联系邮箱","type":"input","width":12},{"field":"region","label":"区域","type":"input","width":12},{"field":"industry","label":"行业","type":"input","width":12},{"field":"address","label":"详细地址","type":"textarea","width":24},{"field":"remark","label":"备注","type":"textarea","width":24}]}',
1, '客户档案的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 2. 设备型号
(1002, 'device-model', '设备型号列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"设备型号","description":"设备型号库低代码列表配置","columns":[{"field":"modelCode","title":"型号编码","width":140},{"field":"modelName","title":"型号名称","width":160},{"field":"productLine","title":"产品线","width":100},{"field":"vendor","title":"厂商","width":120},{"field":"category","title":"设备类别","width":100}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/devices/models","rowKey":"id","pageSize":10,"formFields":[{"field":"modelCode","label":"型号编码","type":"input","required":true,"width":12},{"field":"modelName","label":"型号名称","type":"input","required":true,"width":12},{"field":"productLine","label":"产品线","type":"input","width":12},{"field":"vendor","label":"厂商","type":"input","width":12},{"field":"category","label":"设备类别","type":"select","width":12,"options":[{"label":"路由器","value":"ROUTER"},{"label":"交换机","value":"SWITCH"},{"label":"无线AP","value":"AP"},{"label":"防火墙","value":"FIREWALL"}]},{"field":"manualUrl","label":"手册链接","type":"input","width":12},{"field":"imageUrl","label":"图片链接","type":"input","width":12}]}',
1, '设备型号库的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 3. 备件
(1003, 'spare-part', '备件列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"备件管理","description":"备件台账低代码列表配置","columns":[{"field":"partCode","title":"备件编码","width":140},{"field":"partName","title":"备件名称","width":160},{"field":"quantity","title":"库存数量","width":100,"align":"right"},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/devices/spare-parts","rowKey":"id","pageSize":10,"formFields":[{"field":"partCode","label":"备件编码","type":"input","required":true,"width":12},{"field":"partName","label":"备件名称","type":"input","required":true,"width":12},{"field":"modelId","label":"型号ID","type":"number","width":12},{"field":"warehouseId","label":"仓库ID","type":"number","width":12},{"field":"quantity","label":"库存数量","type":"number","defaultValue":0,"width":12},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
1, '备件台账的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 4. 仓库
(1004, 'warehouse', '仓库列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"仓库管理","description":"仓库档案低代码列表配置","columns":[{"field":"warehouseCode","title":"仓库编码","width":140},{"field":"warehouseName","title":"仓库名称","width":160},{"field":"address","title":"仓库地址","width":240,"ellipsis":true},{"field":"region","title":"区域","width":80}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/devices/warehouses","rowKey":"id","pageSize":10,"formFields":[{"field":"warehouseCode","label":"仓库编码","type":"input","required":true,"width":12},{"field":"warehouseName","label":"仓库名称","type":"input","required":true,"width":12},{"field":"address","label":"仓库地址","type":"textarea","width":24},{"field":"region","label":"区域","type":"input","width":12},{"field":"managerId","label":"管理员ID","type":"number","width":12}]}',
1, '仓库档案的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 5. 工程师技能
(1005, 'engineer-skill', '工程师技能列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"工程师技能","description":"工程师技能低代码列表配置","columns":[{"field":"engineerId","title":"工程师ID","width":120},{"field":"skillTag","title":"技能标签","width":140},{"field":"level","title":"等级","width":100,"valueEnum":{"JUNIOR":{"text":"初级","status":"default"},"MIDDLE":{"text":"中级","status":"processing"},"SENIOR":{"text":"高级","status":"success"},"EXPERT":{"text":"专家","status":"warning"}}}],"searchFields":[{"field":"skillTag","label":"技能标签","type":"input","placeholder":"如 路由/交换"},{"field":"level","label":"等级","type":"select","options":[{"label":"初级","value":"JUNIOR"},{"label":"中级","value":"MIDDLE"},{"label":"高级","value":"SENIOR"},{"label":"专家","value":"EXPERT"}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/engineers","rowKey":"id","pageSize":10,"formFields":[{"field":"engineerId","label":"工程师ID","type":"number","required":true,"width":12},{"field":"skillTag","label":"技能标签","type":"input","required":true,"width":12},{"field":"level","label":"等级","type":"select","defaultValue":"MIDDLE","width":12,"options":[{"label":"初级","value":"JUNIOR"},{"label":"中级","value":"MIDDLE"},{"label":"高级","value":"SENIOR"},{"label":"专家","value":"EXPERT"}]}]}',
1, '工程师技能的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 6. 代理商工程师
(1006, 'agent-engineer', '代理商工程师列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"代理商工程师","description":"代理商工程师档案低代码列表配置","columns":[{"field":"name","title":"姓名","width":120},{"field":"phone","title":"手机号","width":140},{"field":"status","title":"状态","width":100,"valueEnum":{"ACTIVE":{"text":"在职","status":"success"},"DISABLED":{"text":"停用","status":"error"}}},{"field":"qualityScore","title":"质量评分","width":100,"align":"right"}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"姓名/手机号"},{"field":"status","label":"状态","type":"select","options":[{"label":"在职","value":"ACTIVE"},{"label":"停用","value":"DISABLED"}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/agent-companies/0/engineers","rowKey":"id","pageSize":10,"formFields":[{"field":"agentCompanyId","label":"代理商ID","type":"number","required":true,"width":12},{"field":"name","label":"姓名","type":"input","required":true,"width":12},{"field":"phone","label":"手机号","type":"input","required":true,"width":12},{"field":"status","label":"状态","type":"select","defaultValue":"ACTIVE","width":12,"options":[{"label":"在职","value":"ACTIVE"},{"label":"停用","value":"DISABLED"}]},{"field":"qualityScore","label":"质量评分","type":"number","width":12}]}',
1, '代理商工程师的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 7. 验收检查项
(1007, 'acceptance-standard-item', '验收检查项列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"验收检查项","description":"验收标准检查项低代码列表配置","columns":[{"field":"name","title":"检查项名称","width":200},{"field":"requirement","title":"检查要求","width":240,"ellipsis":true},{"field":"testMethod","title":"测试方法","width":200,"ellipsis":true},{"field":"weight","title":"权重","width":80,"align":"right"},{"field":"sortOrder","title":"排序","width":80,"align":"right"}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"检查项名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/acceptance/standards","rowKey":"id","pageSize":10,"formFields":[{"field":"standardId","label":"标准ID","type":"number","required":true,"width":12},{"field":"name","label":"检查项名称","type":"input","required":true,"width":24},{"field":"requirement","label":"检查要求","type":"textarea","width":24},{"field":"testMethod","label":"测试方法","type":"textarea","width":24},{"field":"weight","label":"权重","type":"number","defaultValue":1,"width":12},{"field":"sortOrder","label":"排序","type":"number","defaultValue":0,"width":12}]}',
1, '验收检查项的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 8. 通知模板
(1008, 'notice-template', '通知模板列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"通知模板","description":"通知模板低代码列表配置","columns":[{"field":"templateCode","title":"模板编码","width":140},{"field":"templateName","title":"模板名称","width":180},{"field":"recipientType","title":"接收人类型","width":120},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"},{"field":"status","label":"状态","type":"select","options":[{"label":"启用","value":1},{"label":"禁用","value":0}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/notice-templates","rowKey":"id","pageSize":10,"formFields":[{"field":"templateCode","label":"模板编码","type":"input","required":true,"width":12},{"field":"templateName","label":"模板名称","type":"input","required":true,"width":12},{"field":"titleTemplate","label":"标题模板","type":"input","width":24},{"field":"contentTemplate","label":"内容模板","type":"textarea","width":24,"rules":[{"required":true,"message":"请输入内容模板"}]},{"field":"recipientType","label":"接收人类型","type":"select","width":12,"options":[{"label":"工程师","value":"ENGINEER"},{"label":"项目经理","value":"PM"},{"label":"代理商","value":"AGENT"},{"label":"客户","value":"CUSTOMER"}]},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
1, '通知模板的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 9. 字典数据
(1009, 'dict-data', '字典数据列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"字典数据","description":"数据字典低代码列表配置","columns":[{"field":"dictType","title":"字典类型","width":140},{"field":"dictLabel","title":"字典标签","width":160},{"field":"dictValue","title":"字典键值","width":160},{"field":"sortOrder","title":"排序","width":80,"align":"right"},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"dictType","label":"字典类型","type":"input","placeholder":"如 project_type"},{"field":"keyword","label":"标签/键值","type":"input","placeholder":"字典标签或键值"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/dicts/data","rowKey":"id","pageSize":10,"formFields":[{"field":"dictType","label":"字典类型","type":"input","required":true,"width":12},{"field":"dictLabel","label":"字典标签","type":"input","required":true,"width":12},{"field":"dictValue","label":"字典键值","type":"input","required":true,"width":12},{"field":"sortOrder","label":"排序","type":"number","defaultValue":0,"width":12},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
1, '字典数据的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0),

-- 10. 岗位
(1010, 'position', '岗位列表',
'{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"岗位管理","description":"岗位低代码列表配置","columns":[{"field":"positionCode","title":"岗位编码","width":140},{"field":"positionName","title":"岗位名称","width":160},{"field":"orgId","title":"组织ID","width":120,"align":"right"},{"field":"sortOrder","title":"排序","width":80,"align":"right"},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"},{"field":"status","label":"状态","type":"select","options":[{"label":"启用","value":1},{"label":"禁用","value":0}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/positions","rowKey":"id","pageSize":10,"formFields":[{"field":"positionCode","label":"岗位编码","type":"input","required":true,"width":12},{"field":"positionName","label":"岗位名称","type":"input","required":true,"width":12},{"field":"orgId","label":"组织ID","type":"number","width":12},{"field":"sortOrder","label":"排序","type":"number","defaultValue":0,"width":12},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
1, '岗位的低代码列表配置（Task A6.2 种子）', 1, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 二、sys_menu 低代码菜单（Task A5.3）
-- =====================================================================================
-- 菜单 ID 规划（避开既有 1-36）：
--   37        低代码配置（顶级）
--   38-42     5 个配置子菜单（form/list/tab/relation/template）
--   43-52     10 个运行时视图子菜单（customer/device-model/spare-part/warehouse/
--             engineer-skill/agent-engineer/acceptance-standard-item/notice-template/
--             dict-data/position）
-- 路径说明：
--   * 顶级菜单 path 为绝对路径（/lowcode）
--   * 子菜单 path 为相对路径（form / list / runtime/customer/0 等）
--   * 运行时视图 path='runtime/<bizType>/0' 解析为 /lowcode/runtime/<bizType>/0
--     对应前端路由 /lowcode/runtime/:bizType/:bizId

INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort_order`, `visible`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
-- 顶级：低代码配置
(37, 0,  '低代码配置',     'MENU',   '/lowcode',         NULL,                       NULL,                   'FormOutlined', 8,  1, 1, NOW(), 1, NOW(), 0),
-- 配置子菜单（5 项）
(38, 37, '表单配置',       'MENU',   'form',             'lowcode/form-config',      'lowcode:config:form',    NULL,           1,  1, 1, NOW(), 1, NOW(), 0),
(39, 37, '列表配置',       'MENU',   'list',             'lowcode/list-config',      'lowcode:config:list',    NULL,           2,  1, 1, NOW(), 1, NOW(), 0),
(40, 37, '标签页配置',     'MENU',   'tab',              'lowcode/tab-config',       'lowcode:config:tab',     NULL,           3,  1, 1, NOW(), 1, NOW(), 0),
(41, 37, '关联页配置',     'MENU',   'relation',         'lowcode/relation-config',  'lowcode:config:relation',NULL,           4,  1, 1, NOW(), 1, NOW(), 0),
(42, 37, '模板库',         'MENU',   'template',         'lowcode/template-library', 'lowcode:config:template',NULL,           5,  1, 1, NOW(), 1, NOW(), 0),
-- 运行时视图子菜单（10 项，对应 Task A6.3 改造菜单指向 /lowcode/runtime/:bizType）
(43, 37, '客户档案(低代码)',         'MENU', 'runtime/customer/0',                'lowcode/runtime-renderer', 'lowcode:runtime:customer',                NULL, 11, 1, 1, NOW(), 1, NOW(), 0),
(44, 37, '设备型号(低代码)',         'MENU', 'runtime/device-model/0',            'lowcode/runtime-renderer', 'lowcode:runtime:device-model',            NULL, 12, 1, 1, NOW(), 1, NOW(), 0),
(45, 37, '备件管理(低代码)',         'MENU', 'runtime/spare-part/0',              'lowcode/runtime-renderer', 'lowcode:runtime:spare-part',              NULL, 13, 1, 1, NOW(), 1, NOW(), 0),
(46, 37, '仓库管理(低代码)',         'MENU', 'runtime/warehouse/0',               'lowcode/runtime-renderer', 'lowcode:runtime:warehouse',               NULL, 14, 1, 1, NOW(), 1, NOW(), 0),
(47, 37, '工程师技能(低代码)',       'MENU', 'runtime/engineer-skill/0',          'lowcode/runtime-renderer', 'lowcode:runtime:engineer-skill',          NULL, 15, 1, 1, NOW(), 1, NOW(), 0),
(48, 37, '代理商工程师(低代码)',     'MENU', 'runtime/agent-engineer/0',          'lowcode/runtime-renderer', 'lowcode:runtime:agent-engineer',          NULL, 16, 1, 1, NOW(), 1, NOW(), 0),
(49, 37, '验收检查项(低代码)',       'MENU', 'runtime/acceptance-standard-item/0','lowcode/runtime-renderer', 'lowcode:runtime:acceptance-standard-item',NULL, 17, 1, 1, NOW(), 1, NOW(), 0),
(50, 37, '通知模板(低代码)',         'MENU', 'runtime/notice-template/0',        'lowcode/runtime-renderer', 'lowcode:runtime:notice-template',         NULL, 18, 1, 1, NOW(), 1, NOW(), 0),
(51, 37, '字典数据(低代码)',         'MENU', 'runtime/dict-data/0',               'lowcode/runtime-renderer', 'lowcode:runtime:dict-data',               NULL, 19, 1, 1, NOW(), 1, NOW(), 0),
(52, 37, '岗位管理(低代码)',         'MENU', 'runtime/position/0',                'lowcode/runtime-renderer', 'lowcode:runtime:position',                NULL, 20, 1, 1, NOW(), 1, NOW(), 0),
-- 按钮权限（lowcode:config:* 用于细粒度授权）
(53, 37, '配置新增', 'BUTTON', NULL, NULL, 'lowcode:config:add',    NULL, 30, 0, 1, NOW(), 1, NOW(), 0),
(54, 37, '配置编辑', 'BUTTON', NULL, NULL, 'lowcode:config:edit',   NULL, 31, 0, 1, NOW(), 1, NOW(), 0),
(55, 37, '配置删除', 'BUTTON', NULL, NULL, 'lowcode:config:remove', NULL, 32, 0, 1, NOW(), 1, NOW(), 0),
(56, 37, '配置导出', 'BUTTON', NULL, NULL, 'lowcode:config:export', NULL, 33, 0, 1, NOW(), 1, NOW(), 0),
(57, 37, '配置导入', 'BUTTON', NULL, NULL, 'lowcode:config:import', NULL, 34, 0, 1, NOW(), 1, NOW(), 0),
(58, 37, '模板实例化', 'BUTTON', NULL, NULL, 'lowcode:config:instantiate', NULL, 35, 0, 1, NOW(), 1, NOW(), 0);


-- =====================================================================================
-- 三、sys_role_menu 关联（admin role_id=1 拥有全部低代码菜单）
-- =====================================================================================
-- 说明：data.sql 中的 SET @row_num := 0; INSERT...SELECT 仅在 schema.sql+data.sql
--       全量初始化时执行；本迁移为增量补丁，单独 INSERT IGNORE 关联记录。

INSERT IGNORE INTO `sys_role_menu`
  (`id`, `role_id`, `menu_id`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
(9001, 1, 37, 1, NOW(), 1, NOW(), 0),
(9002, 1, 38, 1, NOW(), 1, NOW(), 0),
(9003, 1, 39, 1, NOW(), 1, NOW(), 0),
(9004, 1, 40, 1, NOW(), 1, NOW(), 0),
(9005, 1, 41, 1, NOW(), 1, NOW(), 0),
(9006, 1, 42, 1, NOW(), 1, NOW(), 0),
(9007, 1, 43, 1, NOW(), 1, NOW(), 0),
(9008, 1, 44, 1, NOW(), 1, NOW(), 0),
(9009, 1, 45, 1, NOW(), 1, NOW(), 0),
(9010, 1, 46, 1, NOW(), 1, NOW(), 0),
(9011, 1, 47, 1, NOW(), 1, NOW(), 0),
(9012, 1, 48, 1, NOW(), 1, NOW(), 0),
(9013, 1, 49, 1, NOW(), 1, NOW(), 0),
(9014, 1, 50, 1, NOW(), 1, NOW(), 0),
(9015, 1, 51, 1, NOW(), 1, NOW(), 0),
(9016, 1, 52, 1, NOW(), 1, NOW(), 0),
(9017, 1, 53, 1, NOW(), 1, NOW(), 0),
(9018, 1, 54, 1, NOW(), 1, NOW(), 0),
(9019, 1, 55, 1, NOW(), 1, NOW(), 0),
(9020, 1, 56, 1, NOW(), 1, NOW(), 0),
(9021, 1, 57, 1, NOW(), 1, NOW(), 0),
(9022, 1, 58, 1, NOW(), 1, NOW(), 0);
