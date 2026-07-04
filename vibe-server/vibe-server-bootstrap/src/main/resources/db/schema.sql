-- =====================================================================================
-- 网络设备原厂实施项目管理系统 - MVP（Phase 1）数据库建表脚本
-- 数据库：vibe_db  字符集：utf8mb4  排序规则：utf8mb4_general_ci  MySQL 8.0
-- 规范（设计文档 1.6）：
--   主键 id BIGINT（雪花算法，应用层生成，DDL 中 BIGINT NOT NULL）
--   公共字段（每张表必有）：id / create_by / create_time / update_by / update_time / deleted
--   乐观锁 version（关键业务表：project / project_task / device_instance / outsource_task / work_order）
--   命名：表名小写下划线、外键 {关联表}_id、索引 idx_表名_字段、唯一索引 uk_表名_字段
--   不使用物理外键约束，关系由应用层维护
-- =====================================================================================

CREATE DATABASE IF NOT EXISTS `vibe_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `vibe_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================================
-- 一、客户表
-- =====================================================================================

-- 客户表：项目关联的甲方客户
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `customer_name` VARCHAR(128) NOT NULL                 COMMENT '客户名称',
  `customer_code` VARCHAR(64)  NOT NULL                 COMMENT '客户编码',
  `contact_name`  VARCHAR(64)           DEFAULT NULL    COMMENT '联系人',
  `contact_phone` VARCHAR(32)           DEFAULT NULL    COMMENT '联系电话',
  `contact_email` VARCHAR(128)          DEFAULT NULL    COMMENT '联系邮箱',
  `address`       VARCHAR(255)          DEFAULT NULL    COMMENT '详细地址',
  `region`        VARCHAR(32)           DEFAULT NULL    COMMENT '区域',
  `industry`      VARCHAR(64)           DEFAULT NULL    COMMENT '行业',
  `remark`        VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_code` (`customer_code`),
  KEY `idx_customer_name` (`customer_name`),
  KEY `idx_customer_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户表';


-- =====================================================================================
-- 二、项目管理表（设计文档 2.2）
-- =====================================================================================

-- 项目表（核心业务表，含乐观锁）
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键（雪花算法）',
  `project_code`  VARCHAR(64)  NOT NULL                 COMMENT '项目编号（PRJ-YYYYMM-XXX 自动生成）',
  `project_name`  VARCHAR(128) NOT NULL                 COMMENT '项目名称',
  `customer_id`   BIGINT                DEFAULT NULL    COMMENT '客户ID',
  `project_type`  VARCHAR(32)           DEFAULT NULL    COMMENT '项目类型 新建/扩容/改造/替换/安全',
  `product_line`  VARCHAR(32)           DEFAULT NULL    COMMENT '产品线 路由/交换/无线/安全/数据中心',
  `execute_mode`  VARCHAR(16)           DEFAULT NULL    COMMENT '执行模式 SELF/AGENT/MIXED',
  `priority`      VARCHAR(8)            DEFAULT 'P2'    COMMENT '优先级 P0/P1/P2/P3',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'INIT'  COMMENT '项目状态 INIT/PLAN/EXECUTE/ACCEPT/CLOSE/ARCHIVED/ON_HOLD/CANCELLED',
  `current_phase` VARCHAR(32)           DEFAULT NULL    COMMENT '当前阶段编码',
  `pm_id`         BIGINT                DEFAULT NULL    COMMENT '项目经理ID',
  `region`        VARCHAR(32)           DEFAULT NULL    COMMENT '区域',
  `contract_no`   VARCHAR(64)           DEFAULT NULL    COMMENT '合同编号',
  `planned_start` DATE                  DEFAULT NULL    COMMENT '计划开始日期',
  `planned_end`   DATE                  DEFAULT NULL    COMMENT '计划结束日期',
  `actual_start`  DATE                  DEFAULT NULL    COMMENT '实际开始日期',
  `actual_end`    DATE                  DEFAULT NULL    COMMENT '实际结束日期',
  `progress_pct`  INT          NOT NULL DEFAULT 0       COMMENT '进度百分比 0-100',
  `description`   TEXT                  DEFAULT NULL    COMMENT '项目描述',
  `remark`        TEXT                  DEFAULT NULL    COMMENT '备注',
  `version`       INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_project_customer_id` (`customer_id`),
  KEY `idx_project_pm_id` (`pm_id`),
  KEY `idx_project_status` (`status`),
  KEY `idx_project_execute_mode` (`execute_mode`),
  KEY `idx_project_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目表';

-- 项目阶段表
DROP TABLE IF EXISTS `project_phase`;
CREATE TABLE `project_phase` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`    BIGINT       NOT NULL                 COMMENT '项目ID',
  `phase_code`    VARCHAR(32)  NOT NULL                 COMMENT '阶段编码 SURVEY/DESIGN/DELIVER/INSTALL/DEBUG/ACCEPT',
  `phase_name`    VARCHAR(64)  NOT NULL                 COMMENT '阶段名称',
  `sort_order`    INT          NOT NULL DEFAULT 0       COMMENT '排序',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT '状态 NOT_STARTED/IN_PROGRESS/COMPLETED',
  `planned_start` DATE                  DEFAULT NULL    COMMENT '计划开始',
  `planned_end`   DATE                  DEFAULT NULL    COMMENT '计划结束',
  `actual_start`  DATE                  DEFAULT NULL    COMMENT '实际开始',
  `actual_end`    DATE                  DEFAULT NULL    COMMENT '实际结束',
  `deliverables`  JSON                  DEFAULT NULL    COMMENT '交付物清单',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_phase_project_id` (`project_id`),
  KEY `idx_project_phase_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目阶段表';

-- 项目任务表（核心业务表，含乐观锁）
DROP TABLE IF EXISTS `project_task`;
CREATE TABLE `project_task` (
  `id`                BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`        BIGINT       NOT NULL                 COMMENT '项目ID',
  `phase_id`          BIGINT                DEFAULT NULL    COMMENT '所属阶段ID',
  `parent_task_id`    BIGINT                DEFAULT NULL    COMMENT '父任务ID（支持子任务）',
  `task_name`         VARCHAR(128) NOT NULL                 COMMENT '任务名称',
  `task_type`         VARCHAR(32)           DEFAULT NULL    COMMENT '任务类型 SURVEY/INSTALL/DEBUG/CUTOVER/ACCEPT/OTHER',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CONFIRMED',
  `execute_mode`      VARCHAR(16)           DEFAULT NULL    COMMENT '执行模式 SELF/AGENT',
  `assignee_id`       BIGINT                DEFAULT NULL    COMMENT '执行人ID（自有工程师）',
  `agent_company_id`  BIGINT                DEFAULT NULL    COMMENT '代理商公司ID（代施时）',
  `agent_engineer_id` BIGINT                DEFAULT NULL    COMMENT '代理商工程师ID（代施时）',
  `site_info`         JSON                  DEFAULT NULL    COMMENT '关联站点信息（站点名/地址/联系人）',
  `device_ids`        JSON                  DEFAULT NULL    COMMENT '关联设备ID列表',
  `planned_start`     DATE                  DEFAULT NULL    COMMENT '计划开始',
  `planned_end`       DATE                  DEFAULT NULL    COMMENT '计划结束',
  `actual_start`      DATE                  DEFAULT NULL    COMMENT '实际开始',
  `actual_end`        DATE                  DEFAULT NULL    COMMENT '实际结束',
  `priority`          VARCHAR(8)            DEFAULT 'MEDIUM' COMMENT '优先级 HIGH/MEDIUM/LOW',
  `description`       TEXT                  DEFAULT NULL    COMMENT '任务描述与要求',
  `attachments`       JSON                  DEFAULT NULL    COMMENT '附件列表',
  `version`           INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`         BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_task_project_id` (`project_id`),
  KEY `idx_project_task_phase_id` (`phase_id`),
  KEY `idx_project_task_parent_task_id` (`parent_task_id`),
  KEY `idx_project_task_assignee_id` (`assignee_id`),
  KEY `idx_project_task_status` (`status`),
  KEY `idx_project_task_agent_company_id` (`agent_company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目任务表';

-- 项目里程碑表
DROP TABLE IF EXISTS `project_milestone`;
CREATE TABLE `project_milestone` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`     BIGINT       NOT NULL                 COMMENT '项目ID',
  `milestone_name` VARCHAR(128) NOT NULL                 COMMENT '里程碑名称',
  `planned_date`   DATE                  DEFAULT NULL    COMMENT '预计日期',
  `actual_date`    DATE                  DEFAULT NULL    COMMENT '实际日期',
  `deliverables`   JSON                  DEFAULT NULL    COMMENT '交付物清单',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/REACHED/DELAYED',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_milestone_project_id` (`project_id`),
  KEY `idx_project_milestone_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目里程碑表';

-- 项目变更记录表
DROP TABLE IF EXISTS `project_change_log`;
CREATE TABLE `project_change_log` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`      BIGINT       NOT NULL                 COMMENT '项目ID',
  `change_type`     VARCHAR(32)           DEFAULT NULL    COMMENT '变更类型 SCOPE/TIME/RESOURCE/OTHER',
  `change_content`  TEXT                  DEFAULT NULL    COMMENT '变更内容',
  `reason`          VARCHAR(512)          DEFAULT NULL    COMMENT '变更原因',
  `impact_analysis` TEXT                  DEFAULT NULL    COMMENT '影响评估',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/REJECTED/EXECUTED',
  `applicant_id`    BIGINT                DEFAULT NULL    COMMENT '申请人ID',
  `approver_id`     BIGINT                DEFAULT NULL    COMMENT '审批人ID',
  `approve_time`    DATETIME              DEFAULT NULL    COMMENT '审批时间',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_change_log_project_id` (`project_id`),
  KEY `idx_project_change_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目变更记录表';

-- 项目风险登记表
DROP TABLE IF EXISTS `project_risk`;
CREATE TABLE `project_risk` (
  `id`          BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`  BIGINT       NOT NULL                 COMMENT '项目ID',
  `risk_desc`   VARCHAR(512) NOT NULL                 COMMENT '风险描述',
  `impact`      VARCHAR(16)           DEFAULT NULL    COMMENT '影响程度 HIGH/MEDIUM/LOW',
  `probability` VARCHAR(16)           DEFAULT NULL    COMMENT '发生概率 HIGH/MEDIUM/LOW',
  `measure`     VARCHAR(512)          DEFAULT NULL    COMMENT '应对措施',
  `owner_id`    BIGINT                DEFAULT NULL    COMMENT '责任人ID',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'OPEN'  COMMENT '状态 OPEN/PROCESSING/RESOLVED/CLOSED',
  `due_date`    DATE                  DEFAULT NULL    COMMENT '截止日期',
  `create_by`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_risk_project_id` (`project_id`),
  KEY `idx_project_risk_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目风险登记表';

-- 项目问题跟踪表
DROP TABLE IF EXISTS `project_issue`;
CREATE TABLE `project_issue` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`    BIGINT       NOT NULL                 COMMENT '项目ID',
  `task_id`       BIGINT                DEFAULT NULL    COMMENT '关联任务ID',
  `issue_desc`    VARCHAR(512) NOT NULL                 COMMENT '问题描述',
  `impact`        VARCHAR(255)          DEFAULT NULL    COMMENT '影响',
  `owner_id`      BIGINT                DEFAULT NULL    COMMENT '责任人ID',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'OPEN'  COMMENT '状态 OPEN/PROCESSING/RESOLVED/CLOSED',
  `due_date`      DATE                  DEFAULT NULL    COMMENT '截止日期',
  `resolved_time` DATETIME              DEFAULT NULL    COMMENT '解决时间',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_issue_project_id` (`project_id`),
  KEY `idx_project_issue_task_id` (`task_id`),
  KEY `idx_project_issue_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目问题跟踪表';

-- 项目成员表
DROP TABLE IF EXISTS `project_member`;
CREATE TABLE `project_member` (
  `id`          BIGINT      NOT NULL                 COMMENT '主键',
  `project_id`  BIGINT      NOT NULL                 COMMENT '项目ID',
  `user_id`     BIGINT      NOT NULL                 COMMENT '用户ID',
  `role`        VARCHAR(32)          DEFAULT NULL    COMMENT '项目角色 PM/ENGINEER/AGENT/CUSTOMER',
  `join_time`   DATETIME             DEFAULT NULL    COMMENT '加入时间',
  `create_by`   BIGINT               DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT               DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_member` (`project_id`, `user_id`),
  KEY `idx_project_member_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目成员表';

-- 项目沟通记录表（评论/讨论）
DROP TABLE IF EXISTS `project_comment`;
CREATE TABLE `project_comment` (
  `id`          BIGINT   NOT NULL                 COMMENT '主键',
  `project_id`  BIGINT   NOT NULL                 COMMENT '项目ID',
  `task_id`     BIGINT            DEFAULT NULL    COMMENT '关联任务ID',
  `content`     TEXT     NOT NULL                 COMMENT '评论内容',
  `author_id`   BIGINT   NOT NULL                 COMMENT '评论人ID',
  `parent_id`   BIGINT            DEFAULT NULL    COMMENT '父评论ID（支持回复）',
  `create_by`   BIGINT            DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT            DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT  NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_comment_project_id` (`project_id`),
  KEY `idx_project_comment_task_id` (`task_id`),
  KEY `idx_project_comment_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目沟通记录表';

-- 项目模板表
DROP TABLE IF EXISTS `project_template`;
CREATE TABLE `project_template` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键',
  `template_name` VARCHAR(128) NOT NULL                COMMENT '模板名称',
  `project_type` VARCHAR(32)           DEFAULT NULL    COMMENT '项目类型',
  `product_line` VARCHAR(32)           DEFAULT NULL    COMMENT '产品线',
  `description`  VARCHAR(512)          DEFAULT NULL    COMMENT '描述',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_template_project_type` (`project_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目模板表';

-- 项目模板阶段表
DROP TABLE IF EXISTS `project_template_phase`;
CREATE TABLE `project_template_phase` (
  `id`           BIGINT      NOT NULL                 COMMENT '主键',
  `template_id`  BIGINT      NOT NULL                 COMMENT '项目模板ID',
  `phase_code`   VARCHAR(32) NOT NULL                 COMMENT '阶段编码',
  `phase_name`   VARCHAR(64) NOT NULL                 COMMENT '阶段名称',
  `sort_order`   INT         NOT NULL DEFAULT 0       COMMENT '排序',
  `deliverables` JSON                 DEFAULT NULL    COMMENT '交付物清单',
  `create_by`    BIGINT               DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT               DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT     NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_template_phase_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目模板阶段表';

-- 项目模板任务表
DROP TABLE IF EXISTS `project_template_task`;
CREATE TABLE `project_template_task` (
  `id`          BIGINT       NOT NULL                 COMMENT '主键',
  `template_id` BIGINT       NOT NULL                 COMMENT '项目模板ID',
  `phase_code`  VARCHAR(32)  NOT NULL                 COMMENT '所属阶段编码',
  `task_name`   VARCHAR(128) NOT NULL                 COMMENT '任务名称',
  `task_type`   VARCHAR(32)           DEFAULT NULL    COMMENT '任务类型',
  `description` VARCHAR(512)          DEFAULT NULL    COMMENT '任务描述',
  `default_days` INT         NOT NULL DEFAULT 1       COMMENT '默认工期（天）',
  `create_by`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_project_template_task_template_id` (`template_id`),
  KEY `idx_project_template_task_phase_code` (`phase_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目模板任务表';


-- =====================================================================================
-- 三、设备资产表（设计文档 2.3）
-- =====================================================================================

-- 设备型号表
DROP TABLE IF EXISTS `device_model`;
CREATE TABLE `device_model` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `model_code`      VARCHAR(64)  NOT NULL                 COMMENT '型号编码',
  `model_name`      VARCHAR(128) NOT NULL                 COMMENT '型号名称',
  `product_line`    VARCHAR(32)           DEFAULT NULL    COMMENT '产品线',
  `vendor`          VARCHAR(64)           DEFAULT NULL    COMMENT '厂商',
  `category`        VARCHAR(32)           DEFAULT NULL    COMMENT '设备类别 ROUTER/SWITCH/AP/FIREWALL/WLC/LB/OTHER',
  `specifications`  JSON                  DEFAULT NULL    COMMENT '技术规格（键值对）',
  `config_template` TEXT                  DEFAULT NULL    COMMENT '默认配置模板',
  `manual_url`      VARCHAR(255)          DEFAULT NULL    COMMENT '安装手册链接',
  `image_url`       VARCHAR(255)          DEFAULT NULL    COMMENT '产品图片',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_model_code` (`model_code`),
  KEY `idx_device_model_product_line` (`product_line`),
  KEY `idx_device_model_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备型号表';

-- 设备实例表（核心表，含乐观锁）
DROP TABLE IF EXISTS `device_instance`;
CREATE TABLE `device_instance` (
  `id`               BIGINT       NOT NULL                 COMMENT '主键',
  `serial_number`    VARCHAR(64)  NOT NULL                 COMMENT '序列号 SN（全局唯一）',
  `mac_address`      VARCHAR(32)           DEFAULT NULL    COMMENT 'MAC 地址',
  `model_id`         BIGINT       NOT NULL                 COMMENT '设备型号ID',
  `firmware_version` VARCHAR(64)           DEFAULT NULL    COMMENT '固件版本',
  `project_id`       BIGINT                DEFAULT NULL    COMMENT '所属项目ID（未分配时为空）',
  `phase_id`         BIGINT                DEFAULT NULL    COMMENT '关联项目阶段ID',
  `site_name`        VARCHAR(128)          DEFAULT NULL    COMMENT '安装站点名称',
  `install_location` VARCHAR(255)          DEFAULT NULL    COMMENT '安装位置（机房-机柜-层位）',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'IN_FACTORY' COMMENT '设备状态（见状态机）',
  `warehouse_id`     BIGINT                DEFAULT NULL    COMMENT '所属仓库ID',
  `agent_company_id` BIGINT                DEFAULT NULL    COMMENT '当前保管代理商ID',
  `config_file_url`  VARCHAR(255)          DEFAULT NULL    COMMENT '当前配置文件地址',
  `config_version`   INT                   DEFAULT 0       COMMENT '配置版本号',
  `install_date`     DATE                  DEFAULT NULL    COMMENT '安装日期',
  `online_date`      DATE                  DEFAULT NULL    COMMENT '入网日期',
  `installer_id`     BIGINT                DEFAULT NULL    COMMENT '安装人员ID',
  `remark`           TEXT                  DEFAULT NULL    COMMENT '备注',
  `version`          INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`        BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_instance_sn` (`serial_number`),
  KEY `idx_device_instance_model_id` (`model_id`),
  KEY `idx_device_instance_project_id` (`project_id`),
  KEY `idx_device_instance_status` (`status`),
  KEY `idx_device_instance_warehouse_id` (`warehouse_id`),
  KEY `idx_device_instance_agent_company_id` (`agent_company_id`),
  KEY `idx_device_instance_mac_address` (`mac_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备实例表';

-- 项目设备清单表（BOM）
DROP TABLE IF EXISTS `device_bom`;
CREATE TABLE `device_bom` (
  `id`            BIGINT   NOT NULL                 COMMENT '主键',
  `project_id`    BIGINT   NOT NULL                 COMMENT '项目ID',
  `model_id`      BIGINT   NOT NULL                 COMMENT '设备型号ID',
  `planned_qty`   INT      NOT NULL DEFAULT 0       COMMENT '计划数量',
  `received_qty`  INT      NOT NULL DEFAULT 0       COMMENT '已到货数量',
  `installed_qty` INT      NOT NULL DEFAULT 0       COMMENT '已安装数量',
  `accepted_qty`  INT      NOT NULL DEFAULT 0       COMMENT '已验收数量',
  `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
  `create_by`     BIGINT            DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT            DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT  NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_bom` (`project_id`, `model_id`),
  KEY `idx_device_bom_project_id` (`project_id`),
  KEY `idx_device_bom_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目设备清单表（BOM）';

-- 设备出入库记录表
DROP TABLE IF EXISTS `device_inventory_log`;
CREATE TABLE `device_inventory_log` (
  `id`               BIGINT       NOT NULL                 COMMENT '主键',
  `device_id`        BIGINT       NOT NULL                 COMMENT '设备实例ID',
  `action_type`      VARCHAR(16)  NOT NULL                 COMMENT '操作类型 IN/OUT/RETURN/TRANSFER',
  `from_warehouse_id` BIGINT               DEFAULT NULL    COMMENT '调出仓库ID',
  `to_warehouse_id`  BIGINT                DEFAULT NULL    COMMENT '调入仓库ID',
  `from_project_id`  BIGINT                DEFAULT NULL    COMMENT '调出项目ID',
  `to_project_id`    BIGINT                DEFAULT NULL    COMMENT '调入项目ID',
  `operator_id`      BIGINT                DEFAULT NULL    COMMENT '操作人ID',
  `quantity`         INT          NOT NULL DEFAULT 1       COMMENT '数量',
  `remark`           VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`        BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_device_inventory_log_device_id` (`device_id`),
  KEY `idx_device_inventory_log_action_type` (`action_type`),
  KEY `idx_device_inventory_log_from_warehouse_id` (`from_warehouse_id`),
  KEY `idx_device_inventory_log_to_warehouse_id` (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备出入库记录表';

-- 设备配置历史表
DROP TABLE IF EXISTS `device_config_history`;
CREATE TABLE `device_config_history` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `device_id`      BIGINT       NOT NULL                 COMMENT '设备实例ID',
  `version`        INT          NOT NULL                 COMMENT '配置版本号',
  `config_file_url` VARCHAR(255)          DEFAULT NULL    COMMENT '配置文件地址',
  `operator_id`    BIGINT                DEFAULT NULL    COMMENT '操作人ID',
  `change_desc`    VARCHAR(512)          DEFAULT NULL    COMMENT '变更说明',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_device_config_history_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备配置历史表';

-- 设备状态变更日志表
DROP TABLE IF EXISTS `device_status_log`;
CREATE TABLE `device_status_log` (
  `id`          BIGINT       NOT NULL                 COMMENT '主键',
  `device_id`   BIGINT       NOT NULL                 COMMENT '设备实例ID',
  `from_status` VARCHAR(16)           DEFAULT NULL    COMMENT '变更前状态',
  `to_status`   VARCHAR(16)  NOT NULL                 COMMENT '变更后状态',
  `operator_id` BIGINT                DEFAULT NULL    COMMENT '操作人ID',
  `remark`      VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_device_status_log_device_id` (`device_id`),
  KEY `idx_device_status_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备状态变更日志表';

-- 仓库表
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `warehouse_name` VARCHAR(128) NOT NULL                COMMENT '仓库名称',
  `warehouse_code` VARCHAR(64)  NOT NULL                COMMENT '仓库编码',
  `address`       VARCHAR(255)          DEFAULT NULL    COMMENT '仓库地址',
  `region`        VARCHAR(32)           DEFAULT NULL    COMMENT '区域',
  `manager_id`    BIGINT                DEFAULT NULL    COMMENT '仓库管理员ID',
  `safety_stock`  JSON                  DEFAULT NULL    COMMENT '安全库存配置（按型号键值对）',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`),
  KEY `idx_warehouse_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='仓库表';

-- 备件表
DROP TABLE IF EXISTS `spare_part`;
CREATE TABLE `spare_part` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键',
  `part_name`    VARCHAR(128) NOT NULL                 COMMENT '备件名称',
  `part_code`    VARCHAR(64)  NOT NULL                 COMMENT '备件编码',
  `model_id`     BIGINT                DEFAULT NULL    COMMENT '关联设备型号ID',
  `warehouse_id` BIGINT                DEFAULT NULL    COMMENT '所属仓库ID',
  `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `quantity`     INT          NOT NULL DEFAULT 0       COMMENT '库存数量',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spare_part_code` (`part_code`),
  KEY `idx_spare_part_warehouse_id` (`warehouse_id`),
  KEY `idx_spare_part_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='备件表';

-- 备件领用/归还记录表
DROP TABLE IF EXISTS `spare_part_log`;
CREATE TABLE `spare_part_log` (
  `id`          BIGINT       NOT NULL                 COMMENT '主键',
  `spare_part_id` BIGINT     NOT NULL                 COMMENT '备件ID',
  `action_type` VARCHAR(16)  NOT NULL                 COMMENT '操作类型 IN/OUT/RETURN/REPAIR',
  `quantity`    INT          NOT NULL                 COMMENT '数量',
  `project_id`  BIGINT                DEFAULT NULL    COMMENT '关联项目ID',
  `operator_id` BIGINT                DEFAULT NULL    COMMENT '操作人ID',
  `remark`      VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_spare_part_log_spare_part_id` (`spare_part_id`),
  KEY `idx_spare_part_log_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='备件领用/归还记录表';


-- =====================================================================================
-- 四、资源调度表（设计文档 2.4）
-- =====================================================================================

-- 工程师资源池表（含乐观锁）
DROP TABLE IF EXISTS `engineer`;
CREATE TABLE `engineer` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `user_id`        BIGINT       NOT NULL                 COMMENT '关联用户ID',
  `employee_no`    VARCHAR(64)           DEFAULT NULL    COMMENT '工号',
  `name`           VARCHAR(64)  NOT NULL                 COMMENT '姓名',
  `phone`          VARCHAR(32)           DEFAULT NULL    COMMENT '手机号',
  `region`         VARCHAR(32)           DEFAULT NULL    COMMENT '所属区域',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE-在职 RESIGNED-离职',
  `hire_date`      DATE                  DEFAULT NULL    COMMENT '入职日期',
  `skills`         JSON                  DEFAULT NULL    COMMENT '技能标签（产品线+等级）',
  `certifications` JSON                  DEFAULT NULL    COMMENT '认证资质（HCIE/HCIP/CCIE 等）',
  `version`        INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_engineer_user_id` (`user_id`),
  KEY `idx_engineer_status` (`status`),
  KEY `idx_engineer_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工程师资源池表';

-- 工程师技能表
DROP TABLE IF EXISTS `engineer_skill`;
CREATE TABLE `engineer_skill` (
  `id`          BIGINT      NOT NULL                 COMMENT '主键',
  `engineer_id` BIGINT      NOT NULL                 COMMENT '工程师ID',
  `skill_tag`   VARCHAR(64) NOT NULL                 COMMENT '技能标签（路由/交换/无线/安全/数据中心/布线）',
  `level`       VARCHAR(16) NOT NULL DEFAULT 'MIDDLE' COMMENT '等级 JUNIOR/MIDDLE/SENIOR/EXPERT',
  `create_by`   BIGINT               DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT               DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_engineer_skill` (`engineer_id`, `skill_tag`),
  KEY `idx_engineer_skill_skill_tag` (`skill_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工程师技能表';

-- 工程师排期表（含乐观锁）
DROP TABLE IF EXISTS `engineer_schedule`;
CREATE TABLE `engineer_schedule` (
  `id`            BIGINT      NOT NULL                 COMMENT '主键',
  `engineer_id`   BIGINT      NOT NULL                 COMMENT '工程师ID',
  `task_id`       BIGINT                DEFAULT NULL    COMMENT '关联任务ID',
  `start_time`    DATETIME             DEFAULT NULL    COMMENT '开始时间',
  `end_time`      DATETIME             DEFAULT NULL    COMMENT '结束时间',
  `schedule_type` VARCHAR(16) NOT NULL DEFAULT 'TASK'  COMMENT '排期类型 TASK/LEAVE/TRAINING/MEETING',
  `remark`        VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `version`       INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`     BIGINT               DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT               DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT     NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_engineer_schedule_engineer_id` (`engineer_id`),
  KEY `idx_engineer_schedule_task_id` (`task_id`),
  KEY `idx_engineer_schedule_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工程师排期表';

-- 工程师请假表
DROP TABLE IF EXISTS `engineer_leave`;
CREATE TABLE `engineer_leave` (
  `id`          BIGINT       NOT NULL                 COMMENT '主键',
  `engineer_id` BIGINT       NOT NULL                 COMMENT '工程师ID',
  `start_date`  DATE         NOT NULL                 COMMENT '开始日期',
  `end_date`    DATE         NOT NULL                 COMMENT '结束日期',
  `leave_type`  VARCHAR(32)           DEFAULT NULL    COMMENT '请假类型 ANNUAL/SICK/PERSONAL/OTHER',
  `reason`      VARCHAR(255)          DEFAULT NULL    COMMENT '请假原因',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/REJECTED',
  `create_by`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_engineer_leave_engineer_id` (`engineer_id`),
  KEY `idx_engineer_leave_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工程师请假表';

-- 工单表（现场作业，核心表，含乐观锁）
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id`                BIGINT       NOT NULL                 COMMENT '主键',
  `task_id`           BIGINT       NOT NULL                 COMMENT '关联项目任务ID',
  `engineer_id`       BIGINT                DEFAULT NULL    COMMENT '执行工程师ID',
  `checkin_time`      DATETIME              DEFAULT NULL    COMMENT '签到时间',
  `checkout_time`     DATETIME              DEFAULT NULL    COMMENT '签退时间',
  `checkin_location`  JSON                  DEFAULT NULL    COMMENT '签到GPS坐标与地址',
  `checkout_location` JSON                  DEFAULT NULL    COMMENT '签退GPS坐标与地址',
  `checkin_photo`     VARCHAR(255)          DEFAULT NULL    COMMENT '签到照片地址',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'CREATED' COMMENT '状态 CREATED/CHECKED_IN/IN_PROGRESS/COMPLETED/CONFIRMED',
  `total_duration`    DECIMAL(8,2)          DEFAULT NULL    COMMENT '总工时（小时）',
  `photo_count`       INT          NOT NULL DEFAULT 0       COMMENT '照片数量',
  `remark`            VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `version`           INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`         BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_task_id` (`task_id`),
  KEY `idx_work_order_engineer_id` (`engineer_id`),
  KEY `idx_work_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单表（现场作业）';

-- 工单施工步骤表
DROP TABLE IF EXISTS `work_order_step`;
CREATE TABLE `work_order_step` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `work_order_id`  BIGINT       NOT NULL                 COMMENT '工单ID',
  `step_no`        INT          NOT NULL                 COMMENT '步骤序号',
  `step_name`      VARCHAR(128) NOT NULL                 COMMENT '步骤名称',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/COMPLETED/SKIPPED',
  `completed_time` DATETIME              DEFAULT NULL    COMMENT '完成时间',
  `duration`       INT                   DEFAULT NULL    COMMENT '耗时（秒）',
  `remark`         VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_step_work_order_id` (`work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单施工步骤表';

-- 工单施工照片表
DROP TABLE IF EXISTS `work_order_photo`;
CREATE TABLE `work_order_photo` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `work_order_id` BIGINT       NOT NULL                 COMMENT '工单ID',
  `step_id`       BIGINT                DEFAULT NULL    COMMENT '关联步骤ID',
  `photo_url`     VARCHAR(255)          DEFAULT NULL    COMMENT '照片地址',
  `thumbnail_url` VARCHAR(255)          DEFAULT NULL    COMMENT '缩略图地址',
  `gps`           JSON                  DEFAULT NULL    COMMENT 'GPS 信息（经纬度+地址+水印时间）',
  `taken_time`    DATETIME              DEFAULT NULL    COMMENT '拍摄时间',
  `uploaded_by`   BIGINT                DEFAULT NULL    COMMENT '上传人ID',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_photo_work_order_id` (`work_order_id`),
  KEY `idx_work_order_photo_step_id` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单施工照片表';

-- 工单异常问题表
DROP TABLE IF EXISTS `work_order_issue`;
CREATE TABLE `work_order_issue` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `work_order_id` BIGINT       NOT NULL                 COMMENT '工单ID',
  `issue_type`    VARCHAR(32)           DEFAULT NULL    COMMENT '问题类型',
  `severity`      VARCHAR(16)  NOT NULL DEFAULT 'MINOR' COMMENT '严重程度 MINOR/MAJOR/BLOCKING',
  `description`   TEXT                  DEFAULT NULL    COMMENT '问题描述',
  `photos`        JSON                  DEFAULT NULL    COMMENT '问题照片地址列表',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'OPEN'  COMMENT '状态 OPEN/PROCESSING/RESOLVED/CLOSED',
  `resolved_time` DATETIME              DEFAULT NULL    COMMENT '解决时间',
  `remark`        VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_issue_work_order_id` (`work_order_id`),
  KEY `idx_work_order_issue_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单异常问题表';

-- 工时表（含乐观锁）
DROP TABLE IF EXISTS `timesheet`;
CREATE TABLE `timesheet` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `engineer_id`    BIGINT       NOT NULL                 COMMENT '工程师ID',
  `project_id`     BIGINT                DEFAULT NULL    COMMENT '项目ID',
  `task_id`        BIGINT                DEFAULT NULL    COMMENT '任务ID',
  `work_date`      DATE         NOT NULL                 COMMENT '工作日期',
  `hours`          DECIMAL(5,2) NOT NULL DEFAULT 0       COMMENT '工作时长（小时）',
  `overtime_hours` DECIMAL(5,2) NOT NULL DEFAULT 0       COMMENT '加班时长（小时）',
  `travel_days`    INT          NOT NULL DEFAULT 0       COMMENT '出差天数',
  `description`    VARCHAR(512)          DEFAULT NULL    COMMENT '工作内容说明',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态 SUBMITTED/APPROVED/REJECTED',
  `approver_id`    BIGINT                DEFAULT NULL    COMMENT '审批人ID',
  `approve_time`   DATETIME              DEFAULT NULL    COMMENT '审批时间',
  `version`        INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_timesheet_engineer_id` (`engineer_id`),
  KEY `idx_timesheet_project_id` (`project_id`),
  KEY `idx_timesheet_task_id` (`task_id`),
  KEY `idx_timesheet_work_date` (`work_date`),
  KEY `idx_timesheet_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工时表';

-- 出差表（差旅管理）
DROP TABLE IF EXISTS `business_trip`;
CREATE TABLE `business_trip` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `engineer_id`     BIGINT       NOT NULL                 COMMENT '工程师ID',
  `project_id`      BIGINT                DEFAULT NULL    COMMENT '关联项目ID',
  `task_id`         BIGINT                DEFAULT NULL    COMMENT '关联任务ID',
  `origin`          VARCHAR(128)          DEFAULT NULL    COMMENT '出发地',
  `destination`     VARCHAR(128) NOT NULL                 COMMENT '目的地',
  `start_date`      DATE         NOT NULL                 COMMENT '出差开始日期',
  `end_date`        DATE         NOT NULL                 COMMENT '出差结束日期',
  `transport_mode`  VARCHAR(32)           DEFAULT NULL    COMMENT '交通方式 PLANE/TRAIN/CAR/OTHER',
  `accommodation`   VARCHAR(255)          DEFAULT NULL    COMMENT '住宿信息',
  `estimated_cost`  DECIMAL(10,2)         DEFAULT NULL    COMMENT '预估费用',
  `actual_cost`     DECIMAL(10,2)         DEFAULT NULL    COMMENT '实际费用',
  `reason`          VARCHAR(512)          DEFAULT NULL    COMMENT '出差事由',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/REJECTED/COMPLETED',
  `approver_id`     BIGINT                DEFAULT NULL    COMMENT '审批人ID',
  `approve_time`    DATETIME              DEFAULT NULL    COMMENT '审批时间',
  `remark`          VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_business_trip_engineer_id` (`engineer_id`),
  KEY `idx_business_trip_project_id` (`project_id`),
  KEY `idx_business_trip_task_id` (`task_id`),
  KEY `idx_business_trip_status` (`status`),
  KEY `idx_business_trip_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出差表';


-- =====================================================================================
-- 五、代理商管理表（设计文档 2.5）
-- =====================================================================================

-- 代理商公司表
DROP TABLE IF EXISTS `agent_company`;
CREATE TABLE `agent_company` (
  `id`               BIGINT        NOT NULL                 COMMENT '主键',
  `company_name`     VARCHAR(128)  NOT NULL                 COMMENT '公司名称',
  `company_code`     VARCHAR(64)   NOT NULL                 COMMENT '公司编码',
  `qualification`    VARCHAR(64)            DEFAULT NULL    COMMENT '资质等级',
  `contact_name`     VARCHAR(64)            DEFAULT NULL    COMMENT '联系人',
  `contact_phone`    VARCHAR(32)            DEFAULT NULL    COMMENT '联系电话',
  `service_regions`  JSON                   DEFAULT NULL    COMMENT '服务区域列表',
  `product_lines`    JSON                   DEFAULT NULL    COMMENT '服务产品线列表',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/SUSPENDED/TERMINATED',
  `overall_score`    DECIMAL(5,2)           DEFAULT NULL    COMMENT '综合评分',
  `cooperation_start` DATE                   DEFAULT NULL    COMMENT '合作开始日期',
  `create_by`        BIGINT                 DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                 DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_company_name` (`company_name`),
  UNIQUE KEY `uk_agent_company_code` (`company_code`),
  KEY `idx_agent_company_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理商公司表';

-- 代理商工程师表
DROP TABLE IF EXISTS `agent_engineer`;
CREATE TABLE `agent_engineer` (
  `id`               BIGINT        NOT NULL                 COMMENT '主键',
  `agent_company_id` BIGINT        NOT NULL                 COMMENT '所属代理商ID',
  `name`             VARCHAR(64)   NOT NULL                 COMMENT '姓名',
  `phone`            VARCHAR(32)   NOT NULL                 COMMENT '手机号（登录账号）',
  `skills`           JSON                   DEFAULT NULL    COMMENT '技能标签',
  `certifications`   JSON                   DEFAULT NULL    COMMENT '认证资质',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/DISABLED',
  `quality_score`    DECIMAL(5,2)           DEFAULT NULL    COMMENT '质量评分',
  `create_by`        BIGINT                 DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                 DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_agent_engineer_company_id` (`agent_company_id`),
  KEY `idx_agent_engineer_phone` (`phone`),
  KEY `idx_agent_engineer_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理商工程师表';

-- 转包任务表（核心表，含乐观锁与状态机）
DROP TABLE IF EXISTS `outsource_task`;
CREATE TABLE `outsource_task` (
  `id`                BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`        BIGINT       NOT NULL                 COMMENT '项目ID',
  `task_id`           BIGINT       NOT NULL                 COMMENT '关联项目任务ID',
  `agent_company_id`  BIGINT       NOT NULL                 COMMENT '代理商公司ID',
  `agent_engineer_id` BIGINT                DEFAULT NULL    COMMENT '代理商工程师ID（接单后填充）',
  `task_scope`        TEXT                  DEFAULT NULL    COMMENT '任务范围与要求',
  `deadline`          DATE                  DEFAULT NULL    COMMENT '截止日期',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/ACCEPTED/REJECTED/IN_PROGRESS/SUBMITTED/CONFIRMED/RETURNED/OVERDUE',
  `submit_count`      INT          NOT NULL DEFAULT 0       COMMENT '提交次数',
  `confirmed_by`      BIGINT                DEFAULT NULL    COMMENT '确认人ID',
  `confirmed_time`    DATETIME              DEFAULT NULL    COMMENT '确认时间',
  `reject_reason`     TEXT                  DEFAULT NULL    COMMENT '退回原因',
  `version`           INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`         BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_outsource_task_project_id` (`project_id`),
  KEY `idx_outsource_task_task_id` (`task_id`),
  KEY `idx_outsource_task_agent_company_id` (`agent_company_id`),
  KEY `idx_outsource_task_agent_engineer_id` (`agent_engineer_id`),
  KEY `idx_outsource_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='转包任务表';

-- 代理商交付物表
DROP TABLE IF EXISTS `outsource_deliverable`;
CREATE TABLE `outsource_deliverable` (
  `id`                BIGINT       NOT NULL                 COMMENT '主键',
  `outsource_task_id` BIGINT       NOT NULL                 COMMENT '转包任务ID',
  `deliverable_type`  VARCHAR(32)  NOT NULL                 COMMENT '交付物类型 PHOTO/TEST_RECORD/RECEIPT/CONFIG/OTHER',
  `file_url`          VARCHAR(255)          DEFAULT NULL    COMMENT '文件地址',
  `file_name`         VARCHAR(255)          DEFAULT NULL    COMMENT '文件名',
  `remark`            VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`         BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_outsource_deliverable_task_id` (`outsource_task_id`),
  KEY `idx_outsource_deliverable_type` (`deliverable_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理商交付物表';

-- 代理商工作量确认表
DROP TABLE IF EXISTS `outsource_workload`;
CREATE TABLE `outsource_workload` (
  `id`                BIGINT       NOT NULL                 COMMENT '主键',
  `outsource_task_id` BIGINT       NOT NULL                 COMMENT '转包任务ID',
  `man_days`          DECIMAL(8,2)          DEFAULT NULL    COMMENT '人天',
  `site_count`        INT                   DEFAULT NULL    COMMENT '站点数',
  `device_count`      INT                   DEFAULT NULL    COMMENT '设备台数',
  `submitted_by`      BIGINT                DEFAULT NULL    COMMENT '提交人ID',
  `confirmed_by`      BIGINT                DEFAULT NULL    COMMENT '确认人ID',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态 SUBMITTED/CONFIRMED/REJECTED',
  `remark`            VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `create_by`         BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_outsource_workload_task_id` (`outsource_task_id`),
  KEY `idx_outsource_workload_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理商工作量确认表';

-- 代理商评分记录表
DROP TABLE IF EXISTS `agent_score_log`;
CREATE TABLE `agent_score_log` (
  `id`                 BIGINT       NOT NULL                 COMMENT '主键',
  `agent_company_id`   BIGINT       NOT NULL                 COMMENT '代理商公司ID',
  `outsource_task_id`  BIGINT                DEFAULT NULL    COMMENT '关联转包任务ID',
  `score_timeliness`   DECIMAL(5,2)          DEFAULT NULL    COMMENT '交付及时性评分',
  `score_quality`      DECIMAL(5,2)          DEFAULT NULL    COMMENT '交付质量评分',
  `score_communication` DECIMAL(5,2)         DEFAULT NULL    COMMENT '沟通协作评分',
  `score_issue`        DECIMAL(5,2)          DEFAULT NULL    COMMENT '问题发生率评分',
  `scorer_id`          BIGINT                DEFAULT NULL    COMMENT '评分人ID',
  `remark`             VARCHAR(512)          DEFAULT NULL    COMMENT '评语',
  `create_by`          BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`          BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`            TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_agent_score_log_company_id` (`agent_company_id`),
  KEY `idx_agent_score_log_task_id` (`outsource_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理商评分记录表';


-- =====================================================================================
-- 六、系统管理表（设计文档 2.9）
-- =====================================================================================

-- 系统用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `username`      VARCHAR(64)  NOT NULL                 COMMENT '登录账号',
  `password`      VARCHAR(128) NOT NULL                 COMMENT '密码（BCrypt 加密）',
  `real_name`     VARCHAR(64)           DEFAULT NULL    COMMENT '真实姓名',
  `phone`         VARCHAR(20)           DEFAULT NULL    COMMENT '手机号',
  `email`         VARCHAR(128)          DEFAULT NULL    COMMENT '邮箱',
  `avatar`        VARCHAR(255)          DEFAULT NULL    COMMENT '头像地址',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/DISABLED',
  `tenant_type`   VARCHAR(16)  NOT NULL DEFAULT 'INTERNAL' COMMENT '租户类型 INTERNAL/AGENT/CUSTOMER',
  `tenant_id`     BIGINT                DEFAULT NULL    COMMENT '租户ID（代理商公司ID/客户ID）',
  `org_id`        BIGINT                DEFAULT NULL    COMMENT '所属组织ID',
  `last_login_time` DATETIME            DEFAULT NULL    COMMENT '最后登录时间',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_phone` (`phone`),
  KEY `idx_sys_user_org_id` (`org_id`),
  KEY `idx_sys_user_tenant_type` (`tenant_type`),
  KEY `idx_sys_user_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- 系统角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL                 COMMENT '主键',
  `role_name`   VARCHAR(64)  NOT NULL                 COMMENT '角色名称',
  `role_code`   VARCHAR(64)  NOT NULL                 COMMENT '角色编码',
  `description` VARCHAR(255)          DEFAULT NULL    COMMENT '描述',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `data_scope`  VARCHAR(16)           DEFAULT 'ALL'   COMMENT '数据权限范围 ALL/DEPT/SELF/CUSTOM',
  `create_by`   BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统角色表';

-- 用户角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`          BIGINT   NOT NULL                 COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                 COMMENT '用户ID',
  `role_id`     BIGINT   NOT NULL                 COMMENT '角色ID',
  `create_by`   BIGINT            DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT            DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT  NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 菜单权限表（目录/菜单/按钮）
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`         BIGINT       NOT NULL                 COMMENT '主键',
  `parent_id`  BIGINT       NOT NULL DEFAULT 0       COMMENT '父菜单ID（0为根）',
  `menu_name`  VARCHAR(64)  NOT NULL                 COMMENT '菜单名称',
  `menu_type`  VARCHAR(16)  NOT NULL DEFAULT 'MENU'  COMMENT '菜单类型 MENU-菜单 BUTTON-按钮',
  `path`       VARCHAR(128)          DEFAULT NULL    COMMENT '路由路径',
  `component`  VARCHAR(128)          DEFAULT NULL    COMMENT '前端组件路径',
  `perms`      VARCHAR(128)          DEFAULT NULL    COMMENT '权限标识',
  `icon`       VARCHAR(64)           DEFAULT NULL    COMMENT '图标',
  `sort_order` INT          NOT NULL DEFAULT 0       COMMENT '排序',
  `visible`    TINYINT      NOT NULL DEFAULT 1       COMMENT '是否可见 1-是 0-否',
  `create_by`  BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`  BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`    TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';

-- 角色菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`          BIGINT   NOT NULL                 COMMENT '主键',
  `role_id`     BIGINT   NOT NULL                 COMMENT '角色ID',
  `menu_id`     BIGINT   NOT NULL                 COMMENT '菜单ID',
  `create_by`   BIGINT            DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   BIGINT            DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`     TINYINT  NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 组织架构表（部门树）
DROP TABLE IF EXISTS `sys_org`;
CREATE TABLE `sys_org` (
  `id`         BIGINT       NOT NULL                 COMMENT '主键',
  `parent_id`  BIGINT       NOT NULL DEFAULT 0       COMMENT '父组织ID（0为根）',
  `org_name`   VARCHAR(64)  NOT NULL                 COMMENT '组织名称',
  `org_code`   VARCHAR(64)           DEFAULT NULL    COMMENT '组织编码',
  `sort_order` INT          NOT NULL DEFAULT 0       COMMENT '排序',
  `status`     TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `create_by`  BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`  BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`    TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_org_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织架构表';

-- 岗位表
DROP TABLE IF EXISTS `sys_position`;
CREATE TABLE `sys_position` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `org_id`        BIGINT                DEFAULT NULL    COMMENT '所属组织ID',
  `position_name` VARCHAR(64)  NOT NULL                 COMMENT '岗位名称',
  `position_code` VARCHAR(64)  NOT NULL                 COMMENT '岗位编码',
  `sort_order`    INT          NOT NULL DEFAULT 0       COMMENT '排序',
  `status`        TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_position_org_id` (`org_id`),
  UNIQUE KEY `uk_sys_position_code` (`position_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位表';

-- 字典类型表
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`         BIGINT       NOT NULL                 COMMENT '主键',
  `dict_name`  VARCHAR(64)  NOT NULL                 COMMENT '字典名称',
  `dict_type`  VARCHAR(64)  NOT NULL                 COMMENT '字典类型编码',
  `status`     TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `remark`     VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`  BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`  BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`    TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- 字典数据表
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`         BIGINT       NOT NULL                 COMMENT '主键',
  `dict_type`  VARCHAR(64)  NOT NULL                 COMMENT '字典类型编码',
  `dict_label` VARCHAR(128) NOT NULL                 COMMENT '字典标签',
  `dict_value` VARCHAR(128) NOT NULL                 COMMENT '字典键值',
  `sort_order` INT          NOT NULL DEFAULT 0       COMMENT '排序',
  `status`     TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `create_by`  BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`  BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`    TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_data` (`dict_type`, `dict_value`),
  KEY `idx_sys_dict_data_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- 系统配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL                 COMMENT '主键',
  `config_name`  VARCHAR(128)          DEFAULT NULL    COMMENT '配置名称',
  `config_key`   VARCHAR(128) NOT NULL                 COMMENT '配置键',
  `config_value` VARCHAR(512)          DEFAULT NULL    COMMENT '配置值',
  `config_type`  VARCHAR(32)           DEFAULT 'SYSTEM' COMMENT '配置类型 SYSTEM/CUSTOM',
  `remark`       VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- 操作日志表
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `title`          VARCHAR(128)          DEFAULT NULL    COMMENT '操作模块标题',
  `business_type`  VARCHAR(32)           DEFAULT NULL    COMMENT '业务类型 INSERT/UPDATE/DELETE/EXPORT/IMPORT/OTHER',
  `method`         VARCHAR(255)          DEFAULT NULL    COMMENT '请求方法',
  `request_url`    VARCHAR(255)          DEFAULT NULL    COMMENT '请求URL',
  `request_param`  TEXT                  DEFAULT NULL    COMMENT '请求参数',
  `response_result` TEXT                 DEFAULT NULL    COMMENT '返回结果',
  `operator_id`    BIGINT                DEFAULT NULL    COMMENT '操作人ID',
  `oper_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `oper_ip`        VARCHAR(64)           DEFAULT NULL    COMMENT '操作IP',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_log_operator_id` (`operator_id`),
  KEY `idx_sys_log_oper_time` (`oper_time`),
  KEY `idx_sys_log_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- 登录日志表
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id`             BIGINT       NOT NULL                 COMMENT '主键',
  `username`       VARCHAR(64)           DEFAULT NULL    COMMENT '登录账号',
  `login_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `login_ip`       VARCHAR(64)           DEFAULT NULL    COMMENT '登录IP',
  `login_location` VARCHAR(128)          DEFAULT NULL    COMMENT '登录地点',
  `browser`        VARCHAR(64)           DEFAULT NULL    COMMENT '浏览器',
  `os`             VARCHAR(64)           DEFAULT NULL    COMMENT '操作系统',
  `status`         TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-成功 0-失败',
  `msg`            VARCHAR(255)          DEFAULT NULL    COMMENT '提示消息',
  `create_by`      BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_login_log_username` (`username`),
  KEY `idx_sys_login_log_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- 站内信表
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `notice_title`  VARCHAR(128) NOT NULL                 COMMENT '通知标题',
  `notice_type`   TINYINT      NOT NULL DEFAULT 1       COMMENT '通知类型 1-通知 2-消息',
  `notice_content` TEXT                  DEFAULT NULL    COMMENT '通知内容',
  `recipient_id`  BIGINT       NOT NULL                 COMMENT '接收人ID',
  `read_status`   TINYINT      NOT NULL DEFAULT 0       COMMENT '已读状态 0-未读 1-已读',
  `send_time`     DATETIME              DEFAULT NULL    COMMENT '发送时间',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_sys_notice_recipient_id` (`recipient_id`),
  KEY `idx_sys_notice_read_status` (`recipient_id`, `read_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内信表';

-- 通知模板表
DROP TABLE IF EXISTS `sys_notice_template`;
CREATE TABLE `sys_notice_template` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `template_code`   VARCHAR(64)  NOT NULL                 COMMENT '模板编码',
  `template_name`   VARCHAR(128) NOT NULL                 COMMENT '模板名称',
  `title_template`  VARCHAR(255)          DEFAULT NULL    COMMENT '标题模板（含变量占位符）',
  `content_template` TEXT        NOT NULL                 COMMENT '内容模板（含变量占位符 ${var}）',
  `channels`        JSON                  DEFAULT NULL    COMMENT '触达渠道 FEISHU/DINGTALK/SMS/EMAIL/SITE',
  `recipient_type`  VARCHAR(32)           DEFAULT NULL    COMMENT '接收人类型 ENGINEER/PM/AGENT/CUSTOMER/MANAGER',
  `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_notice_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知模板表';

-- =====================================================================================
-- 验收管理模块（设计文档 2.7）
-- =====================================================================================

-- 验收标准模板表
DROP TABLE IF EXISTS `acceptance_standard`;
CREATE TABLE `acceptance_standard` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `name`          VARCHAR(128) NOT NULL                 COMMENT '标准名称',
  `project_type`  VARCHAR(32)           DEFAULT NULL    COMMENT '适用项目类型（按产品线/项目类型预设）',
  `standard_version` VARCHAR(16)  NOT NULL DEFAULT '1.0.0' COMMENT '标准版本',
  `description`   VARCHAR(512)          DEFAULT NULL    COMMENT '标准说明',
  `status`        TINYINT      NOT NULL DEFAULT 1       COMMENT '状态 1-启用 0-禁用',
  `version`       INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`     BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_standard_type` (`project_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收标准模板表';

-- 验收检查项表
DROP TABLE IF EXISTS `acceptance_standard_item`;
CREATE TABLE `acceptance_standard_item` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `standard_id`     BIGINT       NOT NULL                 COMMENT '所属验收标准ID',
  `name`            VARCHAR(128) NOT NULL                 COMMENT '检查项名称',
  `requirement`     VARCHAR(512)          DEFAULT NULL    COMMENT '检查要求',
  `test_method`     VARCHAR(256)          DEFAULT NULL    COMMENT '测试方法',
  `weight`          DECIMAL(5,2)          DEFAULT 1.00    COMMENT '权重',
  `sort_order`      INT          NOT NULL DEFAULT 0       COMMENT '排序',
  `version`         INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_item_standard` (`standard_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收检查项表';

-- 验收任务表（验收流程：申请→内部审核→客户验收→签核）
DROP TABLE IF EXISTS `acceptance_task`;
CREATE TABLE `acceptance_task` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`      BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `standard_id`     BIGINT                DEFAULT NULL    COMMENT '适用的验收标准ID',
  `name`            VARCHAR(128) NOT NULL                 COMMENT '验收任务名称',
  `apply_user_id`   BIGINT                DEFAULT NULL    COMMENT '验收申请人（PM）ID',
  `apply_time`      DATETIME              DEFAULT NULL    COMMENT '申请时间',
  `internal_audit_user_id` BIGINT         DEFAULT NULL    COMMENT '内部技术审核人ID',
  `internal_audit_time`    DATETIME       DEFAULT NULL    COMMENT '内部审核时间',
  `internal_audit_result`  VARCHAR(16)    DEFAULT NULL    COMMENT '内部审核结果 PASS/REJECT',
  `customer_sign_link`     VARCHAR(255)   DEFAULT NULL    COMMENT '客户签核链接token',
  `customer_sign_user`     VARCHAR(64)    DEFAULT NULL    COMMENT '客户签核人姓名',
  `customer_sign_time`    DATETIME       DEFAULT NULL    COMMENT '客户签核时间',
  `customer_sign_result`  VARCHAR(16)    DEFAULT NULL    COMMENT '客户签核结果 PASS/CONDITIONAL_PASS/REJECT',
  `customer_sign_remark`  VARCHAR(512)   DEFAULT NULL    COMMENT '客户签核意见',
  `score`           DECIMAL(5,2)          DEFAULT NULL    COMMENT '自动评分（根据检查项结果）',
  `status`          VARCHAR(24)  NOT NULL DEFAULT 'DRAFT'  COMMENT '状态 DRAFT/APPLIED/INTERNAL_AUDITED/CUSTOMER_SIGNING/COMPLETED/REJECTED',
  `remark`          VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`         INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_task_project` (`project_id`),
  KEY `idx_acceptance_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收任务表';

-- 测试记录表（功能/性能/冗余切换等测试项结果记录）
DROP TABLE IF EXISTS `acceptance_test_record`;
CREATE TABLE `acceptance_test_record` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `task_id`         BIGINT       NOT NULL                 COMMENT '所属验收任务ID',
  `item_id`         BIGINT                DEFAULT NULL    COMMENT '关联检查项ID（可选）',
  `test_type`       VARCHAR(32)  NOT NULL DEFAULT 'FUNCTION' COMMENT '测试类型 FUNCTION/PERFORMANCE/REDUNDANCY/OTHER',
  `test_name`       VARCHAR(128) NOT NULL                 COMMENT '测试项名称',
  `test_result`     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '测试结果 PENDING/PASS/FAIL/NA',
  `test_value`      VARCHAR(255)          DEFAULT NULL    COMMENT '测试值（性能指标等）',
  `evidence_url`   VARCHAR(512)          DEFAULT NULL    COMMENT '测试截图/证明材料URL（MinIO）',
  `tester_id`       BIGINT                DEFAULT NULL    COMMENT '测试人ID',
  `test_time`       DATETIME              DEFAULT NULL    COMMENT '测试时间',
  `remark`          VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`         INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_record_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收测试记录表';

-- 验收遗留问题表
DROP TABLE IF EXISTS `acceptance_issue`;
CREATE TABLE `acceptance_issue` (
  `id`               BIGINT       NOT NULL                 COMMENT '主键',
  `task_id`          BIGINT       NOT NULL                 COMMENT '所属验收任务ID',
  `project_id`       BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `name`             VARCHAR(128) NOT NULL                 COMMENT '遗留问题名称',
  `description`      VARCHAR(512)          DEFAULT NULL    COMMENT '问题描述',
  `severity`         VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT '严重等级 LOW/MEDIUM/HIGH/CRITICAL',
  `assignee_id`      BIGINT                DEFAULT NULL    COMMENT '整改责任人ID',
  `due_date`         DATE                  DEFAULT NULL    COMMENT '整改截止日期',
  `resolved_time`    DATETIME              DEFAULT NULL    COMMENT '整改完成时间',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'OPEN'  COMMENT '状态 OPEN/IN_PROGRESS/RESOLVED/CLOSED',
  `close_user_id`    BIGINT                DEFAULT NULL    COMMENT '闭环确认人ID',
  `close_time`       DATETIME              DEFAULT NULL    COMMENT '闭环确认时间',
  `remark`           VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`          INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`        BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_issue_task` (`task_id`),
  KEY `idx_acceptance_issue_project` (`project_id`),
  KEY `idx_acceptance_issue_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='验收遗留问题表';

-- 竣工文档表
DROP TABLE IF EXISTS `acceptance_doc`;
CREATE TABLE `acceptance_doc` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `task_id`       BIGINT       NOT NULL                 COMMENT '所属验收任务ID',
  `project_id`   BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `doc_type`     VARCHAR(32)  NOT NULL                 COMMENT '文档类型 TOPOLOGY/DEVICE_LIST/CONFIG_BACKUP/TEST_REPORT/MAINTENANCE_MANUAL/OTHER',
  `name`         VARCHAR(128) NOT NULL                 COMMENT '文档名称',
  `file_url`     VARCHAR(512) NOT NULL                 COMMENT '文档URL（MinIO objectName）',
  `file_size`    BIGINT                DEFAULT 0       COMMENT '文件大小（字节）',
  `doc_version`  VARCHAR(16)           DEFAULT '1.0.0' COMMENT '文档版本',
  `uploader_id`  BIGINT                DEFAULT NULL    COMMENT '上传人ID',
  `remark`       VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`      INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_doc_task` (`task_id`),
  KEY `idx_acceptance_doc_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='竣工文档表';

-- =====================================================================================
-- 财务核算模块（设计文档 2.8）
-- =====================================================================================

-- 项目预算表
DROP TABLE IF EXISTS `finance_budget`;
CREATE TABLE `finance_budget` (
  `id`               BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`       BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `year`             INT          NOT NULL                 COMMENT '预算年度',
  `labor_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0.00  COMMENT '人工预算',
  `travel_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0.00  COMMENT '差旅预算',
  `agent_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0.00  COMMENT '代理商预算',
  `other_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0.00  COMMENT '其他预算',
  `total_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0.00  COMMENT '预算总额（冗余字段，自动计算）',
  `approval_status`  VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态 DRAFT/PENDING/APPROVED/REJECTED',
  `approver_id`      BIGINT                DEFAULT NULL    COMMENT '审批人ID',
  `approve_time`     DATETIME              DEFAULT NULL    COMMENT '审批时间',
  `remark`           VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`          INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`        BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_finance_budget_project_year` (`project_id`, `year`),
  KEY `idx_finance_budget_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目预算表';

-- 成本归集表（人工/差旅/代理商/其他费用）
DROP TABLE IF EXISTS `finance_cost`;
CREATE TABLE `finance_cost` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`   BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `cost_type`    VARCHAR(32)  NOT NULL                 COMMENT '成本类型 LABOR/TRAVEL/AGENT/OTHER',
  `amount`       DECIMAL(14,2) NOT NULL               COMMENT '金额',
  `cost_date`    DATE         NOT NULL                 COMMENT '发生日期',
  `ref_type`    VARCHAR(32)           DEFAULT NULL    COMMENT '关联业务类型 TIMESHEET/BUSINESS_TRIP/OUTSOURCE_TASK/MANUAL',
  `ref_id`      BIGINT                DEFAULT NULL    COMMENT '关联业务ID',
  `description`  VARCHAR(256)          DEFAULT NULL    COMMENT '费用说明',
  `version`      INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`    BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_finance_cost_project` (`project_id`),
  KEY `idx_finance_cost_type` (`cost_type`),
  KEY `idx_finance_cost_date` (`cost_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成本归集表';

-- 代理商工作量确认单表
DROP TABLE IF EXISTS `finance_workload_confirmation`;
CREATE TABLE `finance_workload_confirmation` (
  `id`               BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`       BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `outsource_task_id` BIGINT                DEFAULT NULL   COMMENT '关联转包任务ID',
  `agent_company_id` BIGINT       NOT NULL                 COMMENT '代理商ID',
  `period`           VARCHAR(16)  NOT NULL                 COMMENT '对账周期 YYYY-MM 或 PROJECT',
  `workload_days`    DECIMAL(8,2) NOT NULL                 COMMENT '工作量（人天）',
  `unit_price`       DECIMAL(14,2) NOT NULL                 COMMENT '人天单价',
  `travel_amount`    DECIMAL(14,2) NOT NULL DEFAULT 0.00   COMMENT '差旅费用',
  `other_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0.00   COMMENT '其他费用',
  `total_amount`     DECIMAL(14,2) NOT NULL DEFAULT 0.00   COMMENT '结算总额（自动计算）',
  `pm_confirm_user_id`  BIGINT              DEFAULT NULL   COMMENT 'PM 确认人ID',
  `pm_confirm_time`     DATETIME            DEFAULT NULL   COMMENT 'PM 确认时间',
  `agent_confirm_user_id` BIGINT            DEFAULT NULL   COMMENT '代理商确认人ID',
  `agent_confirm_time`   DATETIME          DEFAULT NULL   COMMENT '代理商确认时间',
  `approval_status`  VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态 DRAFT/PM_CONFIRMED/AGENT_CONFIRMED/PENDING/DIRECTOR_APPROVED/FINANCE_APPROVED/REJECTED',
  `payment_status`   VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' COMMENT '付款状态 UNPAID/PAYING/PAID',
  `remark`           VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`          INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`        BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_finance_wc_project` (`project_id`),
  KEY `idx_finance_wc_agent` (`agent_company_id`),
  KEY `idx_finance_wc_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代理商工作量确认单表';

-- =====================================================================================
-- 六、集成管理表
-- =====================================================================================

-- 集成配置表：外部系统连接信息
DROP TABLE IF EXISTS `integration_config`;
CREATE TABLE `integration_config` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `system_code`     VARCHAR(64)  NOT NULL                 COMMENT '系统编码（如 ERP/NMS/FEISHU/DINGTALK/OA/LOGISTICS）',
  `system_name`     VARCHAR(128) NOT NULL                COMMENT '系统名称',
  `adapter_type`    VARCHAR(64)  NOT NULL DEFAULT 'REST_API' COMMENT '适配器类型 REST_API/WEBHOOK/DATABASE/MESSAGE_QUEUE',
  `endpoint_url`    VARCHAR(512) NOT NULL                COMMENT '接入点 URL',
  `auth_type`       VARCHAR(32)  NOT NULL DEFAULT 'NONE' COMMENT '认证方式 NONE/BASIC/BEARER/API_KEY/OAUTH2',
  `auth_config`     TEXT                  DEFAULT NULL    COMMENT '认证配置（JSON，敏感字段加密存储）',
  `timeout_ms`      INT          NOT NULL DEFAULT 10000  COMMENT '调用超时（毫秒）',
  `retry_count`     INT          NOT NULL DEFAULT 0       COMMENT '重试次数',
  `enabled`         TINYINT      NOT NULL DEFAULT 1       COMMENT '是否启用 1-是 0-否',
  `description`     VARCHAR(512)          DEFAULT NULL    COMMENT '描述',
  `last_call_time`  DATETIME              DEFAULT NULL    COMMENT '最近调用时间',
  `last_call_status` VARCHAR(16)          DEFAULT NULL    COMMENT '最近调用状态 SUCCESS/FAIL',
  `version`         INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_integration_config_code` (`system_code`),
  KEY `idx_integration_config_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='集成配置表';

-- 集成调用日志表：外部系统调用历史
DROP TABLE IF EXISTS `integration_call_log`;
CREATE TABLE `integration_call_log` (
  `id`              BIGINT       NOT NULL                 COMMENT '主键',
  `config_id`       BIGINT                DEFAULT NULL    COMMENT '关联配置ID',
  `system_code`     VARCHAR(64)  NOT NULL                 COMMENT '系统编码（冗余）',
  `call_scene`      VARCHAR(64)  NOT NULL                 COMMENT '调用场景（如 PUSH_EVENT/SYNC_DATA/QUERY/AUTH）',
  `request_method`  VARCHAR(16)           DEFAULT NULL    COMMENT 'HTTP 方法',
  `request_url`     VARCHAR(512)          DEFAULT NULL    COMMENT '请求 URL',
  `request_headers` TEXT                  DEFAULT NULL    COMMENT '请求头（脱敏）',
  `request_body`    TEXT                  DEFAULT NULL    COMMENT '请求体（脱敏）',
  `response_status` INT                   DEFAULT NULL    COMMENT 'HTTP 响应码',
  `response_body`   TEXT                  DEFAULT NULL    COMMENT '响应体（截断）',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT '调用状态 SUCCESS/FAIL/TIMEOUT',
  `error_msg`       VARCHAR(1024)         DEFAULT NULL    COMMENT '错误信息',
  `cost_ms`         INT                   DEFAULT NULL    COMMENT '耗时（毫秒）',
  `caller_ip`       VARCHAR(64)           DEFAULT NULL    COMMENT '调用方 IP',
  `operated_at`     DATETIME     NOT NULL                COMMENT '调用时间',
  `version`         INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`       BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_integration_call_log_config` (`config_id`),
  KEY `idx_integration_call_log_system` (`system_code`),
  KEY `idx_integration_call_log_status` (`status`),
  KEY `idx_integration_call_log_time` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='集成调用日志表';

-- =====================================================================================
-- 割接管理模块（设计文档 2.6.2）
-- =====================================================================================

-- 割接方案表（核心表，含乐观锁，状态机管理割接全流程）
DROP TABLE IF EXISTS `cutover_plan`;
CREATE TABLE `cutover_plan` (
  `id`                    BIGINT       NOT NULL                 COMMENT '主键',
  `project_id`            BIGINT       NOT NULL                 COMMENT '关联项目ID',
  `plan_name`             VARCHAR(128) NOT NULL                 COMMENT '割接方案名称',
  `cutover_date`          DATE         NOT NULL                 COMMENT '割接日期',
  `start_time`            DATETIME     NOT NULL                 COMMENT '计划开始时间',
  `end_time`              DATETIME     NOT NULL                 COMMENT '计划结束时间',
  `impact_scope`          TEXT                  DEFAULT NULL    COMMENT '影响范围说明',
  `emergency_contact`     VARCHAR(128)          DEFAULT NULL    COMMENT '应急联系人（姓名+电话）',
  `status`                VARCHAR(32)  NOT NULL DEFAULT 'DRAFT'   COMMENT '状态 DRAFT/PENDING_INTERNAL_APPROVAL/INTERNAL_APPROVED/INTERNAL_REJECTED/PENDING_CUSTOMER_APPROVAL/CUSTOMER_APPROVED/CUSTOMER_REJECTED/EXECUTING/COMPLETED/ABORTED',
  `apply_user_id`         BIGINT                DEFAULT NULL    COMMENT '编制人ID（PM）',
  `apply_time`            DATETIME              DEFAULT NULL    COMMENT '编制时间',
  `approval_user_id`      BIGINT                DEFAULT NULL    COMMENT '内部审批人ID（技术主管/总监）',
  `approval_time`         DATETIME              DEFAULT NULL    COMMENT '内部审批时间',
  `approval_remark`       VARCHAR(512)          DEFAULT NULL    COMMENT '内部审批意见',
  `customer_sign_link`    VARCHAR(255)          DEFAULT NULL    COMMENT '客户审批链接token',
  `customer_sign_user`    VARCHAR(64)           DEFAULT NULL    COMMENT '客户签核人姓名',
  `customer_sign_time`    DATETIME              DEFAULT NULL    COMMENT '客户签核时间',
  `customer_sign_result`  VARCHAR(16)           DEFAULT NULL    COMMENT '客户签核结果 APPROVED/REJECTED',
  `customer_sign_remark`  VARCHAR(512)          DEFAULT NULL    COMMENT '客户审批意见',
  `actual_start_time`     DATETIME              DEFAULT NULL    COMMENT '实际开始时间',
  `actual_end_time`       DATETIME              DEFAULT NULL    COMMENT '实际结束时间',
  `summary`               TEXT                  DEFAULT NULL    COMMENT '割接总结',
  `problem_improvement`   TEXT                  DEFAULT NULL    COMMENT '问题与改进',
  `remark`                VARCHAR(512)          DEFAULT NULL    COMMENT '备注',
  `version`               INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`             BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`             BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`               TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_cutover_plan_project_id` (`project_id`),
  KEY `idx_cutover_plan_status` (`status`),
  KEY `idx_cutover_plan_cutover_date` (`cutover_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='割接方案表';

-- 割接步骤表（有序操作步骤，每步含回退方案）
DROP TABLE IF EXISTS `cutover_step`;
CREATE TABLE `cutover_step` (
  `id`                BIGINT       NOT NULL                 COMMENT '主键',
  `plan_id`           BIGINT       NOT NULL                 COMMENT '所属割接方案ID',
  `sort_order`        INT          NOT NULL                 COMMENT '步骤序号（从1开始）',
  `step_name`         VARCHAR(128) NOT NULL                 COMMENT '步骤名称',
  `description`       TEXT                  DEFAULT NULL    COMMENT '详细操作说明',
  `estimated_duration` INT                  DEFAULT NULL    COMMENT '预估耗时（分钟）',
  `owner_id`          BIGINT                DEFAULT NULL    COMMENT '负责人ID',
  `owner_name`        VARCHAR(64)           DEFAULT NULL    COMMENT '负责人姓名',
  `rollback_plan`     TEXT                  DEFAULT NULL    COMMENT '回退方案（异常时执行）',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/EXECUTING/COMPLETED/ROLLED_BACK/ABORTED',
  `actual_start_time` DATETIME              DEFAULT NULL    COMMENT '实际开始时间',
  `actual_end_time`   DATETIME              DEFAULT NULL    COMMENT '实际结束时间',
  `actual_duration`   INT                   DEFAULT NULL    COMMENT '实际耗时（分钟）',
  `execution_remark`  VARCHAR(512)          DEFAULT NULL    COMMENT '执行备注',
  `exception_remark`  VARCHAR(512)          DEFAULT NULL    COMMENT '异常说明',
  `version`           INT          NOT NULL DEFAULT 1       COMMENT '乐观锁版本号',
  `create_by`         BIGINT                DEFAULT NULL    COMMENT '创建人ID',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         BIGINT                DEFAULT NULL    COMMENT '最后修改人ID',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_cutover_step_plan_id` (`plan_id`),
  KEY `idx_cutover_step_status` (`status`),
  KEY `idx_cutover_step_sort` (`plan_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='割接步骤表';

-- 割接操作日志表（全程操作留痕，支持异常追溯）
DROP TABLE IF EXISTS `cutover_execution_log`;
CREATE TABLE `cutover_execution_log` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `plan_id`       BIGINT       NOT NULL                 COMMENT '所属割接方案ID',
  `step_id`       BIGINT                DEFAULT NULL    COMMENT '关联步骤ID（方案级日志为空）',
  `operator_id`   BIGINT                DEFAULT NULL    COMMENT '操作人ID',
  `operator_name` VARCHAR(64)           DEFAULT NULL    COMMENT '操作人姓名',
  `action`        VARCHAR(32)  NOT NULL                 COMMENT '操作动作 CREATE/SUBMIT_INTERNAL_APPROVAL/INTERNAL_APPROVE/INTERNAL_REJECT/START_CUSTOMER_APPROVAL/CUSTOMER_APPROVE/CUSTOMER_REJECT/START_EXECUTION/STEP_EXECUTE/STEP_ROLLBACK/STEP_EXCEPTION/COMPLETE/ABORT',
  `log_time`      DATETIME     NOT NULL                 COMMENT '操作时间',
  `log_content`   TEXT                  DEFAULT NULL    COMMENT '操作内容/详情',
  `log_level`     VARCHAR(8)   NOT NULL DEFAULT 'INFO'   COMMENT '日志级别 INFO/WARN/ERROR',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_cutover_log_plan_id` (`plan_id`),
  KEY `idx_cutover_log_step_id` (`step_id`),
  KEY `idx_cutover_log_time` (`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='割接操作日志表';

--- 客户消息表：客户收到的通知消息（项目进度/割接/验收提醒）
DROP TABLE IF EXISTS `customer_message`;
CREATE TABLE `customer_message` (
  `id`            BIGINT       NOT NULL                 COMMENT '主键',
  `customer_id`   BIGINT       NOT NULL                 COMMENT '客户ID',
  `message_type`  VARCHAR(32)  NOT NULL                 COMMENT '消息类型 PROJECT_PROGRESS/CUTOVER_NOTICE/ACCEPTANCE_NOTICE/DOCUMENT_UPLOAD',
  `business_id`   BIGINT                DEFAULT NULL    COMMENT '业务ID（项目ID/方案ID/任务ID）',
  `project_id`    BIGINT                DEFAULT NULL    COMMENT '关联项目ID',
  `title`         VARCHAR(128) NOT NULL                 COMMENT '消息标题',
  `content`       TEXT                  DEFAULT NULL    COMMENT '消息内容',
  `is_read`       TINYINT      NOT NULL DEFAULT 0       COMMENT '是否已读 0-未读 1-已读',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_customer_message_customer_id` (`customer_id`),
  KEY `idx_customer_message_is_read` (`customer_id`, `is_read`),
  KEY `idx_customer_message_type` (`customer_id`, `message_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户消息表';

SET FOREIGN_KEY_CHECKS = 1;
