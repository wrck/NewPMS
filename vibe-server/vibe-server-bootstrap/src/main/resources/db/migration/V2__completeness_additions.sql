-- =====================================================================================
-- V2 完整性补全 —— 客户协作持久层 + 低代码配置 5 表
-- =====================================================================================
-- 内容（complete-system-modules Task 4.2）：
--   A. 客户协作持久层（module-collaboration 独立持久层）：
--        customer_preference  客户偏好
--        customer_subscription 客户订阅关系
--        customer_session     客户会话
--   B. 低代码配置（module-lowcode 持久层，Task 30 依赖）：
--        lowcode_form_config     表单配置
--        lowcode_list_config     列表配置
--        lowcode_tab_config      标签页配置
--        lowcode_relation_config 关联页配置
--        lowcode_template        模板
--
-- 规范：
--   * 全部使用 CREATE TABLE IF NOT EXISTS（幂等，重复执行安全）
--   * 主键 BIGINT（雪花算法，应用层生成）
--   * 公共字段：create_by / create_time / update_by / update_time / deleted
--   * 字符集 utf8mb4 / 排序 utf8mb4_general_ci，引擎 InnoDB，与 schema.sql 对齐
--   * 表名小写下划线，索引 idx_表名_字段、唯一索引 uk_表名_字段
--   * 不使用物理外键约束（关系由应用层维护，与设计文档 1.6 一致）
-- =====================================================================================

-- =====================================================================================
-- A. 客户协作持久层
-- =====================================================================================

-- 客户偏好：保存客户个性化偏好（通知语言/时区/界面主题/默认视图等键值对）
CREATE TABLE IF NOT EXISTS `customer_preference` (
  `id`               BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `customer_id`      BIGINT       NOT NULL                 COMMENT '客户ID（关联 customer.id）',
  `preference_key`   VARCHAR(100) NOT NULL                 COMMENT '偏好键（如 language/timezone/theme）',
  `preference_value` VARCHAR(500)          DEFAULT NULL    COMMENT '偏好值',
  `create_by`        BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_preference` (`customer_id`, `preference_key`),
  KEY `idx_customer_preference_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户偏好';

-- 客户订阅关系：客户订阅的事件类型与接收渠道
CREATE TABLE IF NOT EXISTS `customer_subscription` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `customer_id`  BIGINT       NOT NULL                 COMMENT '客户ID（关联 customer.id）',
  `event_type`   VARCHAR(50)  NOT NULL                 COMMENT '事件类型（如 PROJECT_PROGRESS/DELIVERABLE_REVIEW/ACCEPTANCE/CUTOVER_APPROVAL）',
  `channels`     VARCHAR(100)          DEFAULT NULL    COMMENT '订阅渠道（SMS/EMAIL/IN_APP，逗号分隔多选）',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_customer_subscription_lookup` (`customer_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户订阅关系';

-- 客户会话：客户门户登录会话（H5 客户端登录态）
CREATE TABLE IF NOT EXISTS `customer_session` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `customer_id`  BIGINT       NOT NULL                 COMMENT '客户ID（关联 customer.id）',
  `login_token`  VARCHAR(500) NOT NULL                 COMMENT '登录 Token（JWT）',
  `login_ip`     VARCHAR(50)           DEFAULT NULL    COMMENT '登录 IP',
  `login_location` VARCHAR(200)         DEFAULT NULL    COMMENT '登录地点',
  `login_time`   DATETIME     NOT NULL                 COMMENT '登录时间',
  `expire_time`  DATETIME     NOT NULL                 COMMENT '过期时间',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-有效 0-已登出/失效',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_customer_session_lookup` (`customer_id`, `login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户会话';

-- =====================================================================================
-- B. 低代码配置 5 表（module-lowcode）
-- =====================================================================================

-- 低代码表单配置
CREATE TABLE IF NOT EXISTS `lowcode_form_config` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `config_code`  VARCHAR(100) NOT NULL                 COMMENT '配置编码（唯一）',
  `config_name`  VARCHAR(200) NOT NULL                 COMMENT '配置名称',
  `schema_json`  LONGTEXT     NOT NULL                 COMMENT 'JSON Schema（Draft 7，定义字段/校验/联动）',
  `template_id`  BIGINT                DEFAULT NULL    COMMENT '关联模板ID（lowcode_template.id，可空）',
  `version`      INT          NOT NULL DEFAULT 1       COMMENT '版本号（乐观锁/版本追踪）',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `description`  VARCHAR(500)          DEFAULT NULL    COMMENT '描述',
  `creator_id`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID（公共字段）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_config_code` (`config_code`),
  KEY `idx_form_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码表单配置';

-- 低代码列表配置
CREATE TABLE IF NOT EXISTS `lowcode_list_config` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `config_code`  VARCHAR(100) NOT NULL                 COMMENT '配置编码（唯一）',
  `config_name`  VARCHAR(200) NOT NULL                 COMMENT '配置名称',
  `schema_json`  LONGTEXT     NOT NULL                 COMMENT 'JSON Schema（列定义/筛选/操作按钮）',
  `template_id`  BIGINT                DEFAULT NULL    COMMENT '关联模板ID',
  `version`      INT          NOT NULL DEFAULT 1       COMMENT '版本号',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `description`  VARCHAR(500)          DEFAULT NULL    COMMENT '描述',
  `creator_id`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID（公共字段）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_list_config_code` (`config_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码列表配置';

-- 低代码标签页配置
CREATE TABLE IF NOT EXISTS `lowcode_tab_config` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `config_code`  VARCHAR(100) NOT NULL                 COMMENT '配置编码（唯一）',
  `config_name`  VARCHAR(200) NOT NULL                 COMMENT '配置名称',
  `schema_json`  LONGTEXT     NOT NULL                 COMMENT 'JSON Schema（Tab 定义与内嵌内容引用）',
  `template_id`  BIGINT                DEFAULT NULL    COMMENT '关联模板ID',
  `version`      INT          NOT NULL DEFAULT 1       COMMENT '版本号',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `description`  VARCHAR(500)          DEFAULT NULL    COMMENT '描述',
  `creator_id`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID（公共字段）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tab_config_code` (`config_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码标签页配置';

-- 低代码关联页配置
CREATE TABLE IF NOT EXISTS `lowcode_relation_config` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `config_code`  VARCHAR(100) NOT NULL                 COMMENT '配置编码（唯一）',
  `config_name`  VARCHAR(200) NOT NULL                 COMMENT '配置名称',
  `schema_json`  LONGTEXT     NOT NULL                 COMMENT 'JSON Schema（主从关联/级联规则/显示字段）',
  `template_id`  BIGINT                DEFAULT NULL    COMMENT '关联模板ID',
  `version`      INT          NOT NULL DEFAULT 1       COMMENT '版本号',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `description`  VARCHAR(500)          DEFAULT NULL    COMMENT '描述',
  `creator_id`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID（公共字段）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation_config_code` (`config_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码关联页配置';

-- 低代码模板：可被各配置类型（form/list/tab/relation）复用的 Schema 模板
CREATE TABLE IF NOT EXISTS `lowcode_template` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `template_code`  VARCHAR(100) NOT NULL                 COMMENT '模板编码（唯一）',
  `template_name`  VARCHAR(200) NOT NULL                 COMMENT '模板名称',
  `template_type`  VARCHAR(20)  NOT NULL                 COMMENT '模板类型（form/list/tab/relation）',
  `schema_json`    LONGTEXT     NOT NULL                 COMMENT 'JSON Schema',
  `description`    VARCHAR(500)          DEFAULT NULL    COMMENT '描述',
  `usage_count`    INT          NOT NULL DEFAULT 0       COMMENT '被使用次数（实例化时 +1）',
  `status`         TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `creator_id`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID（公共字段）',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_template_type` (`template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低代码模板';
