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

    // --- Summary ---
    header('完成');
    log(`  V2 表: 8 张`);
    log(`  V5 列: 4 个`);
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
