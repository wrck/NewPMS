-- =====================================================================================
-- V5 扩展 integration_config 表字段 —— Adapter 集成所需
-- =====================================================================================
-- 说明（complete-system-modules Task 4.5 / 8.x）：
--   为支撑 module-integration 实际对接 Adapter（ERP 客户主数据同步 / IM 通知转发 /
--   物流状态拉取 / OA 审批联动，见 Task 8），需扩展 integration_config 表新增字段：
--     * adapter_category  VARCHAR(50)   业务类型（ERP/IM/LOGISTICS/OA）
--     * last_sync_time     DATETIME     最后同步时间
--     * sync_status        VARCHAR(20)  同步状态（IDLE/RUNNING/SUCCESS/FAILED）
--     * sync_interval      INT          同步间隔（秒）
--
-- ── 关键冲突处理 ──────────────────────────────────────────────────────────────────────
--   原 integration_config 表（schema.sql）已有列 `adapter_type VARCHAR(64) NOT NULL
--   DEFAULT 'REST_API'`，其语义为"技术适配器类型"（REST_API/WEBHOOK/DATABASE/MESSAGE_QUEUE）。
--   spec 描述的"Adapter 类型（ERP/IM/LOGISTICS/OA）"是"业务领域分类"，二者语义不同。
--   若直接 ALTER ADD COLUMN adapter_type 会因"Duplicate column name"报错；
--   spec 注释也明确："如果列已存在会报错，需要捕获"，并建议"可用 stored procedure"。
--
--   决策：保留原 adapter_type（技术类型不动），新增 adapter_category 承载业务类型，
--         避免语义冲突且向后兼容既有代码与数据。
--
-- ── 幂等性 / 顺序鲁棒性 ────────────────────────────────────────────────────────────────
--   MySQL 8.0 不支持 ALTER TABLE ADD COLUMN IF NOT EXISTS（该语法仅 MariaDB 支持）。
--   故采用 stored procedure 检查 information_schema.columns，逐列判断是否存在后再 ADD：
--     * 表已存在 + 列不存在 → 安全 ADD
--     * 表已存在 + 列已存在 → 跳过（幂等）
--     * 表不存在（如全新库且 Flyway 先于 schema.sql 执行）→ 整体跳过，不报错
--   DELIMITER 语法由 flyway-mysql 解析器支持（Flyway 9.x MySQL parser 识别 DELIMITER）。
-- =====================================================================================

DROP PROCEDURE IF EXISTS `p_v5_alter_integration_config`;
DELIMITER //
CREATE PROCEDURE `p_v5_alter_integration_config`()
BEGIN
  -- 仅当 integration_config 表存在时执行（避免全新库 Flyway 先于 schema.sql 时报错）
  IF EXISTS (SELECT 1 FROM `information_schema`.`tables`
             WHERE `table_schema` = DATABASE()
               AND `table_name` = 'integration_config') THEN

    -- 1) adapter_category：业务类型（ERP/IM/LOGISTICS/OA）
    --    原 adapter_type（REST_API/WEBHOOK/DATABASE/MESSAGE_QUEUE）保留不动
    IF NOT EXISTS (SELECT 1 FROM `information_schema`.`columns`
                   WHERE `table_schema` = DATABASE()
                     AND `table_name` = 'integration_config'
                     AND `column_name` = 'adapter_category') THEN
      ALTER TABLE `integration_config`
        ADD COLUMN `adapter_category` VARCHAR(50) DEFAULT NULL
        COMMENT 'Adapter业务类型（ERP/IM/LOGISTICS/OA）' AFTER `system_name`;
    END IF;

    -- 2) last_sync_time：最后同步时间
    IF NOT EXISTS (SELECT 1 FROM `information_schema`.`columns`
                   WHERE `table_schema` = DATABASE()
                     AND `table_name` = 'integration_config'
                     AND `column_name` = 'last_sync_time') THEN
      ALTER TABLE `integration_config`
        ADD COLUMN `last_sync_time` DATETIME DEFAULT NULL
        COMMENT '最后同步时间';
    END IF;

    -- 3) sync_status：同步状态（IDLE/RUNNING/SUCCESS/FAILED）
    IF NOT EXISTS (SELECT 1 FROM `information_schema`.`columns`
                   WHERE `table_schema` = DATABASE()
                     AND `table_name` = 'integration_config'
                     AND `column_name` = 'sync_status') THEN
      ALTER TABLE `integration_config`
        ADD COLUMN `sync_status` VARCHAR(20) NOT NULL DEFAULT 'IDLE'
        COMMENT '同步状态（IDLE/RUNNING/SUCCESS/FAILED）';
    END IF;

    -- 4) sync_interval：同步间隔（秒）
    IF NOT EXISTS (SELECT 1 FROM `information_schema`.`columns`
                   WHERE `table_schema` = DATABASE()
                     AND `table_name` = 'integration_config'
                     AND `column_name` = 'sync_interval') THEN
      ALTER TABLE `integration_config`
        ADD COLUMN `sync_interval` INT NOT NULL DEFAULT 300
        COMMENT '同步间隔（秒）';
    END IF;

  END IF;
END //
DELIMITER ;

CALL `p_v5_alter_integration_config`();
DROP PROCEDURE IF EXISTS `p_v5_alter_integration_config`;

-- 兼容性备注：spec 原文给出的 ALTER ... ADD COLUMN adapter_type ... AFTER config_name
--   存在两处问题已由上方存储过程规避：
--     (1) adapter_type 列已存在（Duplicate column 错误）
--     (2) config_name 列不存在（原表为 system_code/system_name），AFTER config_name 会报错
--   新增的 adapter_category 置于 system_name 之后，语义清晰。
