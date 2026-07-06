-- =====================================================================================
-- V12 反馈与工单表 sys_feedback
-- =====================================================================================
-- 说明（enterprise-completion Task D5）：
--   * 用于收集用户在使用过程中提交的 Bug 报告、功能建议、咨询等反馈
--   * 管理员可在「系统管理 > 反馈管理」中查看、处理并通知提交人
--
-- 字段说明：
--   * type           反馈类型 BUG/SUGGESTION/QUESTION
--   * title          反馈标题
--   * content        内容描述
--   * screenshot_url 截图 URL（多个用逗号分隔，MinIO 路径）
--   * contact        联系方式（手机号/邮箱/IM 账号，可选）
--   * submitter_id   提交人 ID（关联 sys_user.id）
--   * status         状态 PENDING/PROCESSING/RESOLVED/CLOSED
--   * handler_id     处理人 ID（关联 sys_user.id）
--   * handle_note    处理备注
--   * handle_time    处理时间
--
-- 幂等：使用 CREATE TABLE IF NOT EXISTS，重复执行安全。
-- =====================================================================================

CREATE TABLE IF NOT EXISTS `sys_feedback` (
  `id`             BIGINT       NOT NULL                COMMENT '主键（雪花算法）',
  `type`           VARCHAR(20)  NOT NULL                COMMENT '反馈类型 BUG/SUGGESTION/QUESTION',
  `title`          VARCHAR(200) NOT NULL                COMMENT '标题',
  `content`        VARCHAR(2000)         DEFAULT NULL    COMMENT '内容描述',
  `screenshot_url` VARCHAR(1000)         DEFAULT NULL    COMMENT '截图 URL（多个用逗号分隔）',
  `contact`        VARCHAR(100)          DEFAULT NULL    COMMENT '联系方式',
  `submitter_id`   BIGINT                DEFAULT NULL    COMMENT '提交人 ID',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PROCESSING/RESOLVED/CLOSED',
  `handler_id`     BIGINT                DEFAULT NULL    COMMENT '处理人 ID',
  `handle_note`    VARCHAR(1000)         DEFAULT NULL    COMMENT '处理备注',
  `handle_time`    DATETIME              DEFAULT NULL    COMMENT '处理时间',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_feedback_submitter` (`submitter_id`),
  KEY `idx_sys_feedback_status` (`status`),
  KEY `idx_sys_feedback_type` (`type`),
  KEY `idx_sys_feedback_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='反馈与工单';
