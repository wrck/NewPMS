-- =====================================================================================
-- V11 关键业务表追加 version INT DEFAULT 0 列（乐观锁字段，幂等迁移）
-- =====================================================================================
-- 背景（Task C2）：
--   * 关键业务表通过 @Version 乐观锁防止并发修改冲突。
--   * BaseEntity 已声明 @Version private Integer version 字段，由 MyBatis-Plus
--     OptimisticLockerInnerInterceptor 自动维护。
--   * schema.sql 中 6 张核心业务表已包含 version INT NOT NULL DEFAULT 1 列；
--     本迁移用于已部署环境（schema.sql 旧版本未包含 version 列时）补齐。
--   * 由于 MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，使用存储过程做幂等检查。
--
-- 覆盖表（6 张关键业务表，对应 Task C2.1）：
--   * project              （项目）
--   * device_instance      （设备实例）
--   * outsource_task       （转包任务）
--   * work_order           （工单）
--   * acceptance_task      （验收任务）
--   * finance_budget       （项目预算）
-- =====================================================================================

DROP PROCEDURE IF EXISTS `vibe_add_version_column_v11`;
DELIMITER //
CREATE PROCEDURE `vibe_add_version_column_v11`(IN tbl VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = tbl
          AND column_name = 'version'
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT ''乐观锁版本号''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL `vibe_add_version_column_v11`('project');
CALL `vibe_add_version_column_v11`('device_instance');
CALL `vibe_add_version_column_v11`('outsource_task');
CALL `vibe_add_version_column_v11`('work_order');
CALL `vibe_add_version_column_v11`('acceptance_task');
CALL `vibe_add_version_column_v11`('finance_budget');

DROP PROCEDURE IF EXISTS `vibe_add_version_column_v11`;
