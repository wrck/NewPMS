-- ============================================================================
-- MySQL 初始化脚本 - 容器首次启动时自动执行
-- 挂载路径：/docker-entrypoint-initdb.d/init.sql
-- ============================================================================

-- 1. 创建数据库（若不存在），使用 utf8mb4 字符集
CREATE DATABASE IF NOT EXISTS `vibe_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 2. 切换到目标数据库
USE `vibe_db`;

-- 3. 加载表结构脚本
-- 脚本来源：vibe-server/vibe-server-bootstrap/src/main/resources/db/schema.sql
-- 通过 docker-compose 卷挂载到 /docker-entrypoint-initdb.d/db-scripts/schema.sql
SOURCE /docker-entrypoint-initdb.d/db-scripts/schema.sql;

-- 4. 加载初始化数据脚本
-- 脚本来源：vibe-server/vibe-server-bootstrap/src/main/resources/db/data.sql
SOURCE /docker-entrypoint-initdb.d/db-scripts/data.sql;

-- 5. 完成提示（写入到标准输出，可在容器日志中查看）
SELECT 'vibe_db 初始化完成：表结构与初始化数据已加载。' AS init_status;
