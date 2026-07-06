#!/usr/bin/env node
/**
 * Vibe 数据库初始化脚本（替代 Flyway，用于 MySQL 5.7 环境）
 *
 * 背景：Flyway 8.x+ Community Edition 不支持 MySQL 5.7，本地开发环境
 * 需手动执行 V2/V5 的幂等 DDL。本脚本通过 mysql2 连接 MySQL 执行：
 *   - V2: 客户协作持久层 + 低代码配置 8 张表（CREATE TABLE IF NOT EXISTS，幂等）
 *   - V5: integration_config 表字段扩展（ALTER TABLE ADD COLUMN，幂等）
 *
 * 用法：
 *   node scripts/init-db.js
 *
 * 环境变量（可选，默认与 application-dev.yml 一致）：
 *   VIBE_DB_HOST=localhost
 *   VIBE_DB_PORT=3306
 *   VIBE_DB_NAME=vibe_db
 *   VIBE_DB_USER=root
 *   VIBE_DB_PASSWORD=!Q@W3e4r
 */

const mysql = require('mysql2/promise');
const path = require('path');
const fs = require('fs');

// --- Configuration ---
const DB_HOST = process.env.VIBE_DB_HOST || 'localhost';
const DB_PORT = parseInt(process.env.VIBE_DB_PORT || '3306', 10);
const DB_NAME = process.env.VIBE_DB_NAME || 'vibe_db';
const DB_USER = process.env.VIBE_DB_USER || 'root';
const DB_PASSWORD = process.env.VIBE_DB_PASSWORD || '!Q@W3e4r';

const PROJECT_ROOT = path.resolve(__dirname, '..');
const MIGRATION_DIR = path.join(
  PROJECT_ROOT,
  'vibe-server',
  'vibe-server-bootstrap',
  'src',
  'main',
  'resources',
  'db',
  'migration'
);

// --- Logger ---
const c = {
  reset: '\x1b[0m', bold: '\x1b[1m',
  green: '\x1b[32m', red: '\x1b[31m', yellow: '\x1b[33m',
  cyan: '\x1b[36m', magenta: '\x1b[35m', gray: '\x1b[90m'
};
const log = (msg, color = c.reset) => console.log(`${color}${msg}${c.reset}`);
const ok = (msg) => log(`  [${c.green}OK${c.reset}] ${msg}`);
const skip = (msg) => log(`  [${c.gray}SKIP${c.reset}] ${msg}`);
const fail = (msg) => log(`  [${c.red}FAIL${c.reset}] ${msg}`);
const header = (msg) => log(`\n${c.magenta}${c.bold}==== ${msg} ====${c.reset}`);

// --- SQL statements ---

// V2: 8 tables (CREATE TABLE IF NOT EXISTS, idempotent)
const V2_TABLES = [
  { name: 'customer_preference', sql: `CREATE TABLE IF NOT EXISTS \`customer_preference\` (
  \`id\` BIGINT NOT NULL COMMENT '主键（雪花算法）',
  \`customer_id\` BIGINT NOT NULL COMMENT '客户ID',
  \`preference_key\` VARCHAR(100) NOT NULL COMMENT '偏好键',
  \`preference_value\` VARCHAR(500) DEFAULT NULL COMMENT '偏好值',
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_customer_preference\` (\`customer_id\`, \`preference_key\`),
  KEY \`idx_customer_preference_customer_id\` (\`customer_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户偏好'` },

  { name: 'customer_subscription', sql: `CREATE TABLE IF NOT EXISTS \`customer_subscription\` (
  \`id\` BIGINT NOT NULL,
  \`customer_id\` BIGINT NOT NULL,
  \`event_type\` VARCHAR(50) NOT NULL COMMENT '事件类型',
  \`channels\` VARCHAR(100) DEFAULT NULL COMMENT '订阅渠道（逗号分隔）',
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  KEY \`idx_customer_subscription_lookup\` (\`customer_id\`, \`event_type\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户订阅关系'` },

  { name: 'customer_session', sql: `CREATE TABLE IF NOT EXISTS \`customer_session\` (
  \`id\` BIGINT NOT NULL,
  \`customer_id\` BIGINT NOT NULL,
  \`login_token\` VARCHAR(500) NOT NULL,
  \`login_ip\` VARCHAR(50) DEFAULT NULL,
  \`login_location\` VARCHAR(200) DEFAULT NULL,
  \`login_time\` DATETIME NOT NULL,
  \`expire_time\` DATETIME NOT NULL,
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  KEY \`idx_customer_session_lookup\` (\`customer_id\`, \`login_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户会话'` },

  { name: 'lowcode_form_config', sql: `CREATE TABLE IF NOT EXISTS \`lowcode_form_config\` (
  \`id\` BIGINT NOT NULL,
  \`config_code\` VARCHAR(100) NOT NULL,
  \`config_name\` VARCHAR(200) NOT NULL,
  \`schema_json\` LONGTEXT NOT NULL,
  \`template_id\` BIGINT DEFAULT NULL,
  \`version\` INT NOT NULL DEFAULT 1,
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`description\` VARCHAR(500) DEFAULT NULL,
  \`creator_id\` BIGINT DEFAULT NULL,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_form_config_code\` (\`config_code\`),
  KEY \`idx_form_template\` (\`template_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码表单配置'` },

  { name: 'lowcode_list_config', sql: `CREATE TABLE IF NOT EXISTS \`lowcode_list_config\` (
  \`id\` BIGINT NOT NULL,
  \`config_code\` VARCHAR(100) NOT NULL,
  \`config_name\` VARCHAR(200) NOT NULL,
  \`schema_json\` LONGTEXT NOT NULL,
  \`template_id\` BIGINT DEFAULT NULL,
  \`version\` INT NOT NULL DEFAULT 1,
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`description\` VARCHAR(500) DEFAULT NULL,
  \`creator_id\` BIGINT DEFAULT NULL,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_list_config_code\` (\`config_code\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码列表配置'` },

  { name: 'lowcode_tab_config', sql: `CREATE TABLE IF NOT EXISTS \`lowcode_tab_config\` (
  \`id\` BIGINT NOT NULL,
  \`config_code\` VARCHAR(100) NOT NULL,
  \`config_name\` VARCHAR(200) NOT NULL,
  \`schema_json\` LONGTEXT NOT NULL,
  \`template_id\` BIGINT DEFAULT NULL,
  \`version\` INT NOT NULL DEFAULT 1,
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`description\` VARCHAR(500) DEFAULT NULL,
  \`creator_id\` BIGINT DEFAULT NULL,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_tab_config_code\` (\`config_code\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码标签页配置'` },

  { name: 'lowcode_relation_config', sql: `CREATE TABLE IF NOT EXISTS \`lowcode_relation_config\` (
  \`id\` BIGINT NOT NULL,
  \`config_code\` VARCHAR(100) NOT NULL,
  \`config_name\` VARCHAR(200) NOT NULL,
  \`schema_json\` LONGTEXT NOT NULL,
  \`template_id\` BIGINT DEFAULT NULL,
  \`version\` INT NOT NULL DEFAULT 1,
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`description\` VARCHAR(500) DEFAULT NULL,
  \`creator_id\` BIGINT DEFAULT NULL,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_relation_config_code\` (\`config_code\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码关联页配置'` },

  { name: 'lowcode_template', sql: `CREATE TABLE IF NOT EXISTS \`lowcode_template\` (
  \`id\` BIGINT NOT NULL,
  \`template_code\` VARCHAR(100) NOT NULL,
  \`template_name\` VARCHAR(200) NOT NULL,
  \`template_type\` VARCHAR(20) NOT NULL COMMENT '类型（form/list/tab/relation）',
  \`schema_json\` LONGTEXT NOT NULL,
  \`description\` VARCHAR(500) DEFAULT NULL,
  \`usage_count\` INT NOT NULL DEFAULT 0,
  \`version\` INT NOT NULL DEFAULT 1,
  \`status\` TINYINT NOT NULL DEFAULT 1,
  \`creator_id\` BIGINT DEFAULT NULL,
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_template_code\` (\`template_code\`),
  KEY \`idx_template_type\` (\`template_type\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码模板'` }
];

// V5: integration_config 字段扩展（幂等 ALTER ADD COLUMN）
const V5_COLUMNS = [
  { table: 'integration_config', column: 'adapter_category', sql: `ALTER TABLE \`integration_config\` ADD COLUMN \`adapter_category\` VARCHAR(50) DEFAULT NULL COMMENT 'Adapter业务类型（ERP/IM/LOGISTICS/OA）' AFTER \`system_name\`` },
  { table: 'integration_config', column: 'last_sync_time', sql: `ALTER TABLE \`integration_config\` ADD COLUMN \`last_sync_time\` DATETIME DEFAULT NULL COMMENT '最后同步时间'` },
  { table: 'integration_config', column: 'sync_status', sql: `ALTER TABLE \`integration_config\` ADD COLUMN \`sync_status\` VARCHAR(20) NOT NULL DEFAULT 'IDLE' COMMENT '同步状态（IDLE/RUNNING/SUCCESS/FAILED）'` },
  { table: 'integration_config', column: 'sync_interval', sql: `ALTER TABLE \`integration_config\` ADD COLUMN \`sync_interval\` INT NOT NULL DEFAULT 300 COMMENT '同步间隔（秒）'` }
];

// V12: sys_feedback 反馈与工单表（幂等 CREATE TABLE IF NOT EXISTS）
// 与 V12__sys_feedback.sql 保持一致，用于本地 MySQL 5.7 环境（Flyway 已禁用）。
const V12_TABLES = [
  { name: 'sys_feedback', sql: `CREATE TABLE IF NOT EXISTS \`sys_feedback\` (
  \`id\` BIGINT NOT NULL COMMENT '主键（雪花算法）',
  \`type\` VARCHAR(20) NOT NULL COMMENT '反馈类型 BUG/SUGGESTION/QUESTION',
  \`title\` VARCHAR(200) NOT NULL COMMENT '标题',
  \`content\` VARCHAR(2000) DEFAULT NULL COMMENT '内容描述',
  \`screenshot_url\` VARCHAR(1000) DEFAULT NULL COMMENT '截图 URL（多个用逗号分隔）',
  \`contact\` VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
  \`submitter_id\` BIGINT DEFAULT NULL COMMENT '提交人 ID',
  \`status\` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PROCESSING/RESOLVED/CLOSED',
  \`handler_id\` BIGINT DEFAULT NULL COMMENT '处理人 ID',
  \`handle_note\` VARCHAR(1000) DEFAULT NULL COMMENT '处理备注',
  \`handle_time\` DATETIME DEFAULT NULL COMMENT '处理时间',
  \`create_by\` BIGINT DEFAULT NULL,
  \`create_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_by\` BIGINT DEFAULT NULL,
  \`update_time\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \`deleted\` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (\`id\`),
  KEY \`idx_sys_feedback_submitter\` (\`submitter_id\`),
  KEY \`idx_sys_feedback_status\` (\`status\`),
  KEY \`idx_sys_feedback_type\` (\`type\`),
  KEY \`idx_sys_feedback_create_time\` (\`create_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='反馈与工单'` }
];

// V10: 低代码模块种子数据（Task A6.2 + A6.3 + A5.3）
// 10 个实体的 lowcode_list_config + sys_menu 低代码菜单 + sys_role_menu 关联
// 与 V10__lowcode_seed.sql 保持一致，用于本地 MySQL 5.7 环境（Flyway 已禁用）。
// 幂等：INSERT IGNORE 依赖 uk_list_config_code / 主键 id，重复执行安全。
const V10_LOWCODE_CONFIG_SEED = [
  // [id, config_code, config_name, schema_json, description]
  { id: 1001, code: 'customer', name: '客户档案列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"客户档案","description":"客户档案低代码列表配置","columns":[{"field":"customerCode","title":"客户编码","width":140},{"field":"customerName","title":"客户名称","width":160},{"field":"contactName","title":"联系人","width":100},{"field":"contactPhone","title":"联系电话","width":120},{"field":"region","title":"区域","width":80},{"field":"industry","title":"行业","width":100}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/customers","rowKey":"id","pageSize":10,"formFields":[{"field":"customerCode","label":"客户编码","type":"input","required":true,"width":12},{"field":"customerName","label":"客户名称","type":"input","required":true,"width":12},{"field":"contactName","label":"联系人","type":"input","width":12},{"field":"contactPhone","label":"联系电话","type":"input","width":12},{"field":"contactEmail","label":"联系邮箱","type":"input","width":12},{"field":"region","label":"区域","type":"input","width":12},{"field":"industry","label":"行业","type":"input","width":12},{"field":"address","label":"详细地址","type":"textarea","width":24},{"field":"remark","label":"备注","type":"textarea","width":24}]}',
    desc: '客户档案的低代码列表配置（Task A6.2 种子）' },
  { id: 1002, code: 'device-model', name: '设备型号列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"设备型号","description":"设备型号库低代码列表配置","columns":[{"field":"modelCode","title":"型号编码","width":140},{"field":"modelName","title":"型号名称","width":160},{"field":"productLine","title":"产品线","width":100},{"field":"vendor","title":"厂商","width":120},{"field":"category","title":"设备类别","width":100}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/devices/models","rowKey":"id","pageSize":10,"formFields":[{"field":"modelCode","label":"型号编码","type":"input","required":true,"width":12},{"field":"modelName","label":"型号名称","type":"input","required":true,"width":12},{"field":"productLine","label":"产品线","type":"input","width":12},{"field":"vendor","label":"厂商","type":"input","width":12},{"field":"category","label":"设备类别","type":"select","width":12,"options":[{"label":"路由器","value":"ROUTER"},{"label":"交换机","value":"SWITCH"},{"label":"无线AP","value":"AP"},{"label":"防火墙","value":"FIREWALL"}]},{"field":"manualUrl","label":"手册链接","type":"input","width":12},{"field":"imageUrl","label":"图片链接","type":"input","width":12}]}',
    desc: '设备型号库的低代码列表配置（Task A6.2 种子）' },
  { id: 1003, code: 'spare-part', name: '备件列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"备件管理","description":"备件台账低代码列表配置","columns":[{"field":"partCode","title":"备件编码","width":140},{"field":"partName","title":"备件名称","width":160},{"field":"quantity","title":"库存数量","width":100,"align":"right"},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/devices/spare-parts","rowKey":"id","pageSize":10,"formFields":[{"field":"partCode","label":"备件编码","type":"input","required":true,"width":12},{"field":"partName","label":"备件名称","type":"input","required":true,"width":12},{"field":"modelId","label":"型号ID","type":"number","width":12},{"field":"warehouseId","label":"仓库ID","type":"number","width":12},{"field":"quantity","label":"库存数量","type":"number","defaultValue":0,"width":12},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
    desc: '备件台账的低代码列表配置（Task A6.2 种子）' },
  { id: 1004, code: 'warehouse', name: '仓库列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"仓库管理","description":"仓库档案低代码列表配置","columns":[{"field":"warehouseCode","title":"仓库编码","width":140},{"field":"warehouseName","title":"仓库名称","width":160},{"field":"address","title":"仓库地址","width":240,"ellipsis":true},{"field":"region","title":"区域","width":80}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/devices/warehouses","rowKey":"id","pageSize":10,"formFields":[{"field":"warehouseCode","label":"仓库编码","type":"input","required":true,"width":12},{"field":"warehouseName","label":"仓库名称","type":"input","required":true,"width":12},{"field":"address","label":"仓库地址","type":"textarea","width":24},{"field":"region","label":"区域","type":"input","width":12},{"field":"managerId","label":"管理员ID","type":"number","width":12}]}',
    desc: '仓库档案的低代码列表配置（Task A6.2 种子）' },
  { id: 1005, code: 'engineer-skill', name: '工程师技能列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"工程师技能","description":"工程师技能低代码列表配置","columns":[{"field":"engineerId","title":"工程师ID","width":120},{"field":"skillTag","title":"技能标签","width":140},{"field":"level","title":"等级","width":100,"valueEnum":{"JUNIOR":{"text":"初级","status":"default"},"MIDDLE":{"text":"中级","status":"processing"},"SENIOR":{"text":"高级","status":"success"},"EXPERT":{"text":"专家","status":"warning"}}}],"searchFields":[{"field":"skillTag","label":"技能标签","type":"input","placeholder":"如 路由/交换"},{"field":"level","label":"等级","type":"select","options":[{"label":"初级","value":"JUNIOR"},{"label":"中级","value":"MIDDLE"},{"label":"高级","value":"SENIOR"},{"label":"专家","value":"EXPERT"}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/engineers","rowKey":"id","pageSize":10,"formFields":[{"field":"engineerId","label":"工程师ID","type":"number","required":true,"width":12},{"field":"skillTag","label":"技能标签","type":"input","required":true,"width":12},{"field":"level","label":"等级","type":"select","defaultValue":"MIDDLE","width":12,"options":[{"label":"初级","value":"JUNIOR"},{"label":"中级","value":"MIDDLE"},{"label":"高级","value":"SENIOR"},{"label":"专家","value":"EXPERT"}]}]}',
    desc: '工程师技能的低代码列表配置（Task A6.2 种子）' },
  { id: 1006, code: 'agent-engineer', name: '代理商工程师列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"代理商工程师","description":"代理商工程师档案低代码列表配置","columns":[{"field":"name","title":"姓名","width":120},{"field":"phone","title":"手机号","width":140},{"field":"status","title":"状态","width":100,"valueEnum":{"ACTIVE":{"text":"在职","status":"success"},"DISABLED":{"text":"停用","status":"error"}}},{"field":"qualityScore","title":"质量评分","width":100,"align":"right"}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"姓名/手机号"},{"field":"status","label":"状态","type":"select","options":[{"label":"在职","value":"ACTIVE"},{"label":"停用","value":"DISABLED"}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/agent-companies/0/engineers","rowKey":"id","pageSize":10,"formFields":[{"field":"agentCompanyId","label":"代理商ID","type":"number","required":true,"width":12},{"field":"name","label":"姓名","type":"input","required":true,"width":12},{"field":"phone","label":"手机号","type":"input","required":true,"width":12},{"field":"status","label":"状态","type":"select","defaultValue":"ACTIVE","width":12,"options":[{"label":"在职","value":"ACTIVE"},{"label":"停用","value":"DISABLED"}]},{"field":"qualityScore","label":"质量评分","type":"number","width":12}]}',
    desc: '代理商工程师的低代码列表配置（Task A6.2 种子）' },
  { id: 1007, code: 'acceptance-standard-item', name: '验收检查项列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"验收检查项","description":"验收标准检查项低代码列表配置","columns":[{"field":"name","title":"检查项名称","width":200},{"field":"requirement","title":"检查要求","width":240,"ellipsis":true},{"field":"testMethod","title":"测试方法","width":200,"ellipsis":true},{"field":"weight","title":"权重","width":80,"align":"right"},{"field":"sortOrder","title":"排序","width":80,"align":"right"}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"检查项名称"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/acceptance/standards","rowKey":"id","pageSize":10,"formFields":[{"field":"standardId","label":"标准ID","type":"number","required":true,"width":12},{"field":"name","label":"检查项名称","type":"input","required":true,"width":24},{"field":"requirement","label":"检查要求","type":"textarea","width":24},{"field":"testMethod","label":"测试方法","type":"textarea","width":24},{"field":"weight","label":"权重","type":"number","defaultValue":1,"width":12},{"field":"sortOrder","label":"排序","type":"number","defaultValue":0,"width":12}]}',
    desc: '验收检查项的低代码列表配置（Task A6.2 种子）' },
  { id: 1008, code: 'notice-template', name: '通知模板列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"通知模板","description":"通知模板低代码列表配置","columns":[{"field":"templateCode","title":"模板编码","width":140},{"field":"templateName","title":"模板名称","width":180},{"field":"recipientType","title":"接收人类型","width":120},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"},{"field":"status","label":"状态","type":"select","options":[{"label":"启用","value":1},{"label":"禁用","value":0}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/notice-templates","rowKey":"id","pageSize":10,"formFields":[{"field":"templateCode","label":"模板编码","type":"input","required":true,"width":12},{"field":"templateName","label":"模板名称","type":"input","required":true,"width":12},{"field":"titleTemplate","label":"标题模板","type":"input","width":24},{"field":"contentTemplate","label":"内容模板","type":"textarea","width":24,"rules":[{"required":true,"message":"请输入内容模板"}]},{"field":"recipientType","label":"接收人类型","type":"select","width":12,"options":[{"label":"工程师","value":"ENGINEER"},{"label":"项目经理","value":"PM"},{"label":"代理商","value":"AGENT"},{"label":"客户","value":"CUSTOMER"}]},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
    desc: '通知模板的低代码列表配置（Task A6.2 种子）' },
  { id: 1009, code: 'dict-data', name: '字典数据列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"字典数据","description":"数据字典低代码列表配置","columns":[{"field":"dictType","title":"字典类型","width":140},{"field":"dictLabel","title":"字典标签","width":160},{"field":"dictValue","title":"字典键值","width":160},{"field":"sortOrder","title":"排序","width":80,"align":"right"},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"dictType","label":"字典类型","type":"input","placeholder":"如 project_type"},{"field":"keyword","label":"标签/键值","type":"input","placeholder":"字典标签或键值"}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/dicts/data","rowKey":"id","pageSize":10,"formFields":[{"field":"dictType","label":"字典类型","type":"input","required":true,"width":12},{"field":"dictLabel","label":"字典标签","type":"input","required":true,"width":12},{"field":"dictValue","label":"字典键值","type":"input","required":true,"width":12},{"field":"sortOrder","label":"排序","type":"number","defaultValue":0,"width":12},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
    desc: '字典数据的低代码列表配置（Task A6.2 种子）' },
  { id: 1010, code: 'position', name: '岗位列表',
    schema: '{"$schema":"http://json-schema.org/draft-07/schema#","type":"list","title":"岗位管理","description":"岗位低代码列表配置","columns":[{"field":"positionCode","title":"岗位编码","width":140},{"field":"positionName","title":"岗位名称","width":160},{"field":"orgId","title":"组织ID","width":120,"align":"right"},{"field":"sortOrder","title":"排序","width":80,"align":"right"},{"field":"status","title":"状态","width":80,"valueEnum":{"1":{"text":"启用","status":"success"},"0":{"text":"禁用","status":"error"}}}],"searchFields":[{"field":"keyword","label":"关键字","type":"input","placeholder":"编码/名称"},{"field":"status","label":"状态","type":"select","options":[{"label":"启用","value":1},{"label":"禁用","value":0}]}],"actions":[{"type":"create","label":"新增"},{"type":"edit","label":"编辑"},{"type":"delete","label":"删除","danger":true}],"apiUrl":"/api/v1/positions","rowKey":"id","pageSize":10,"formFields":[{"field":"positionCode","label":"岗位编码","type":"input","required":true,"width":12},{"field":"positionName","label":"岗位名称","type":"input","required":true,"width":12},{"field":"orgId","label":"组织ID","type":"number","width":12},{"field":"sortOrder","label":"排序","type":"number","defaultValue":0,"width":12},{"field":"status","label":"状态","type":"switch","defaultValue":1,"width":12}]}',
    desc: '岗位的低代码列表配置（Task A6.2 种子）' }
];

// V10 sys_menu 低代码菜单与权限（id 37-58）
// [id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, visible]
const V10_LOWCODE_MENU_SEED = [
  // 顶级：低代码配置
  [37, 0, '低代码配置', 'MENU', '/lowcode', null, null, 'FormOutlined', 8, 1],
  // 5 个配置子菜单
  [38, 37, '表单配置', 'MENU', 'form', 'lowcode/form-config', 'lowcode:config:form', null, 1, 1],
  [39, 37, '列表配置', 'MENU', 'list', 'lowcode/list-config', 'lowcode:config:list', null, 2, 1],
  [40, 37, '标签页配置', 'MENU', 'tab', 'lowcode/tab-config', 'lowcode:config:tab', null, 3, 1],
  [41, 37, '关联页配置', 'MENU', 'relation', 'lowcode/relation-config', 'lowcode:config:relation', null, 4, 1],
  [42, 37, '模板库', 'MENU', 'template', 'lowcode/template-library', 'lowcode:config:template', null, 5, 1],
  // 10 个运行时视图子菜单（指向 /lowcode/runtime/<bizType>/0）
  [43, 37, '客户档案(低代码)', 'MENU', 'runtime/customer/0', 'lowcode/runtime-renderer', 'lowcode:runtime:customer', null, 11, 1],
  [44, 37, '设备型号(低代码)', 'MENU', 'runtime/device-model/0', 'lowcode/runtime-renderer', 'lowcode:runtime:device-model', null, 12, 1],
  [45, 37, '备件管理(低代码)', 'MENU', 'runtime/spare-part/0', 'lowcode/runtime-renderer', 'lowcode:runtime:spare-part', null, 13, 1],
  [46, 37, '仓库管理(低代码)', 'MENU', 'runtime/warehouse/0', 'lowcode/runtime-renderer', 'lowcode:runtime:warehouse', null, 14, 1],
  [47, 37, '工程师技能(低代码)', 'MENU', 'runtime/engineer-skill/0', 'lowcode/runtime-renderer', 'lowcode:runtime:engineer-skill', null, 15, 1],
  [48, 37, '代理商工程师(低代码)', 'MENU', 'runtime/agent-engineer/0', 'lowcode/runtime-renderer', 'lowcode:runtime:agent-engineer', null, 16, 1],
  [49, 37, '验收检查项(低代码)', 'MENU', 'runtime/acceptance-standard-item/0', 'lowcode/runtime-renderer', 'lowcode:runtime:acceptance-standard-item', null, 17, 1],
  [50, 37, '通知模板(低代码)', 'MENU', 'runtime/notice-template/0', 'lowcode/runtime-renderer', 'lowcode:runtime:notice-template', null, 18, 1],
  [51, 37, '字典数据(低代码)', 'MENU', 'runtime/dict-data/0', 'lowcode/runtime-renderer', 'lowcode:runtime:dict-data', null, 19, 1],
  [52, 37, '岗位管理(低代码)', 'MENU', 'runtime/position/0', 'lowcode/runtime-renderer', 'lowcode:runtime:position', null, 20, 1],
  // 按钮权限 lowcode:config:*
  [53, 37, '配置新增', 'BUTTON', null, null, 'lowcode:config:add', null, 30, 0],
  [54, 37, '配置编辑', 'BUTTON', null, null, 'lowcode:config:edit', null, 31, 0],
  [55, 37, '配置删除', 'BUTTON', null, null, 'lowcode:config:remove', null, 32, 0],
  [56, 37, '配置导出', 'BUTTON', null, null, 'lowcode:config:export', null, 33, 0],
  [57, 37, '配置导入', 'BUTTON', null, null, 'lowcode:config:import', null, 34, 0],
  [58, 37, '模板实例化', 'BUTTON', null, null, 'lowcode:config:instantiate', null, 35, 0]
];

// V11: 关键业务表追加 version 列（乐观锁字段，幂等 ALTER ADD COLUMN）
// 对应 Task C2：project / device_instance / outsource_task / work_order / acceptance_task / finance_budget
const V11_COLUMNS = [
  { table: 'project', column: 'version', sql: `ALTER TABLE \`project\` ADD COLUMN \`version\` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'` },
  { table: 'device_instance', column: 'version', sql: `ALTER TABLE \`device_instance\` ADD COLUMN \`version\` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'` },
  { table: 'outsource_task', column: 'version', sql: `ALTER TABLE \`outsource_task\` ADD COLUMN \`version\` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'` },
  { table: 'work_order', column: 'version', sql: `ALTER TABLE \`work_order\` ADD COLUMN \`version\` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'` },
  { table: 'acceptance_task', column: 'version', sql: `ALTER TABLE \`acceptance_task\` ADD COLUMN \`version\` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'` },
  { table: 'finance_budget', column: 'version', sql: `ALTER TABLE \`finance_budget\` ADD COLUMN \`version\` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'` }
];

// --- Main ---
async function main() {
  log(`${c.bold}${c.cyan}Vibe 数据库初始化脚本${c.reset}（替代 Flyway，用于 MySQL 5.7）`);
  log(`  数据库: ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}`, c.gray);

  let conn;
  try {
    conn = await mysql.createConnection({
      host: DB_HOST,
      port: DB_PORT,
      user: DB_USER,
      password: DB_PASSWORD,
      database: DB_NAME,
      multipleStatements: true
    });
    log(`  ${c.green}连接成功${c.reset}`);

    // --- V2: 创建 8 张表 ---
    header('V2: 客户协作 + 低代码配置 8 张表');
    for (const t of V2_TABLES) {
      // Check if table exists
      const [rows] = await conn.execute(
        `SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?`,
        [DB_NAME, t.name]
      );
      if (rows.length > 0) {
        skip(`${t.name} 已存在`);
      } else {
        await conn.query(t.sql);
        ok(`创建表 ${t.name}`);
      }
    }

    // --- V5: integration_config 字段扩展 ---
    header('V5: integration_config 字段扩展');
    // Check if integration_config table exists
    const [tables] = await conn.execute(
      `SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name='integration_config'`,
      [DB_NAME]
    );
    if (tables.length === 0) {
      skip('integration_config 表不存在，跳过 V5');
    } else {
      for (const col of V5_COLUMNS) {
        const [cols] = await conn.execute(
          `SELECT 1 FROM information_schema.columns WHERE table_schema=? AND table_name=? AND column_name=?`,
          [DB_NAME, col.table, col.column]
        );
        if (cols.length > 0) {
          skip(`${col.table}.${col.column} 已存在`);
        } else {
          await conn.query(col.sql);
          ok(`添加列 ${col.table}.${col.column}`);
        }
      }
    }

    // --- V10: 低代码模块种子数据（10 条 list_config + 22 条 sys_menu + 22 条 sys_role_menu） ---
    header('V10: 低代码种子数据（10 实体 list_config + sys_menu + sys_role_menu）');
    // 1) 检查 lowcode_list_config 表是否存在
    const [v10TblRows] = await conn.execute(
      `SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name='lowcode_list_config'`,
      [DB_NAME]
    );
    if (v10TblRows.length === 0) {
      skip('lowcode_list_config 表不存在，跳过 V10');
    } else {
      // 1.1 插入 10 条 list_config 种子
      for (const item of V10_LOWCODE_CONFIG_SEED) {
        const [exist] = await conn.execute(
          `SELECT id FROM lowcode_list_config WHERE id=?`,
          [item.id]
        );
        if (exist.length > 0) {
          skip(`lowcode_list_config ${item.code} 已存在`);
        } else {
          await conn.query(
            `INSERT IGNORE INTO lowcode_list_config
              (id, config_code, config_name, schema_json, status, description, creator_id, create_by, create_time, update_by, update_time, deleted)
             VALUES (?, ?, ?, ?, 1, ?, 1, 1, NOW(), 1, NOW(), 0)`,
            [item.id, item.code, item.name, item.schema, item.desc]
          );
          ok(`插入 lowcode_list_config ${item.code}`);
        }
      }
      // 1.2 插入 22 条 sys_menu 种子
      for (const m of V10_LOWCODE_MENU_SEED) {
        const [menuExist] = await conn.execute(
          `SELECT id FROM sys_menu WHERE id=?`,
          [m[0]]
        );
        if (menuExist.length > 0) {
          skip(`sys_menu id=${m[0]} (${m[2]}) 已存在`);
        } else {
          await conn.query(
            `INSERT IGNORE INTO sys_menu
              (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, visible, create_by, create_time, update_by, update_time, deleted)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), 1, NOW(), 0)`,
            m
          );
          ok(`插入 sys_menu id=${m[0]} (${m[2]})`);
        }
      }
      // 1.3 插入 22 条 sys_role_menu 关联（role_id=1 管理员）
      for (const m of V10_LOWCODE_MENU_SEED) {
        const menuId = m[0];
        const rmId = 9000 + menuId; // 9001-9022，避开已有 id
        const [rmExist] = await conn.execute(
          `SELECT id FROM sys_role_menu WHERE id=?`,
          [rmId]
        );
        if (rmExist.length > 0) {
          skip(`sys_role_menu id=${rmId} (menu=${menuId}) 已存在`);
        } else {
          await conn.query(
            `INSERT IGNORE INTO sys_role_menu
              (id, role_id, menu_id, create_by, create_time, update_by, update_time, deleted)
             VALUES (?, 1, ?, 1, NOW(), 1, NOW(), 0)`,
            [rmId, menuId]
          );
          ok(`插入 sys_role_menu id=${rmId} (role=1, menu=${menuId})`);
        }
      }
    }

    // --- V11: 关键业务表追加 version 列（乐观锁字段） ---
    header('V11: 关键业务表追加 version 列（乐观锁）');
    for (const col of V11_COLUMNS) {
      const [tblRows] = await conn.execute(
        `SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?`,
        [DB_NAME, col.table]
      );
      if (tblRows.length === 0) {
        skip(`${col.table} 表不存在，跳过`);
        continue;
      }
      const [cols] = await conn.execute(
        `SELECT 1 FROM information_schema.columns WHERE table_schema=? AND table_name=? AND column_name=?`,
        [DB_NAME, col.table, col.column]
      );
      if (cols.length > 0) {
        skip(`${col.table}.${col.column} 已存在`);
      } else {
        await conn.query(col.sql);
        ok(`添加列 ${col.table}.${col.column}`);
      }
    }

    // --- V12: 反馈与工单表 ---
    header('V12: 反馈与工单表 sys_feedback');
    for (const t of V12_TABLES) {
      const [rows] = await conn.execute(
        `SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?`,
        [DB_NAME, t.name]
      );
      if (rows.length > 0) {
        skip(`${t.name} 已存在`);
      } else {
        await conn.query(t.sql);
        ok(`创建表 ${t.name}`);
      }
    }

    // --- Summary ---
    header('完成');
    log(`  V2 表: 8 张`);
    log(`  V5 列: 4 个`);
    log(`  V10 低代码种子: 10 条 list_config + 22 条 sys_menu + 22 条 sys_role_menu`);
    log(`  V11 列: 6 个（关键业务表乐观锁）`);
    log(`  V12 表: 1 张（sys_feedback 反馈与工单）`);
    log(`  所有迁移已应用（幂等，可重复执行）`, c.green);
    log(`\n  提示: 后端 application-dev.yml 已禁用 Flyway (spring.flyway.enabled=false)`, c.gray);
    log(`  生产环境用 MySQL 8.0+ 时可恢复 Flyway${c.reset}`, c.gray);

  } catch (err) {
    fail(`数据库初始化失败: ${err.message}`);
    console.error(err.stack);
    process.exit(1);
  } finally {
    if (conn) await conn.end();
  }
}

main();
