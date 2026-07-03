package com.vibe.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * <p>自动填充字段：</p>
 * <ul>
 *   <li>INSERT 时：createBy、createTime、updateBy、updateTime</li>
 *   <li>UPDATE 时：updateBy、updateTime</li>
 *   <li>INSERT 时若 deleted 未设置，自动填充 0（未删除）</li>
 *   <li>INSERT 时若 version 未设置，自动填充 1</li>
 * </ul>
 *
 * @author vibe
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long currentUserId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        // 逻辑删除默认值
        Object deleted = getFieldValByName("deleted", metaObject);
        if (deleted == null) {
            this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        }
        // 乐观锁版本号默认值
        Object version = getFieldValByName("version", metaObject);
        if (version == null) {
            this.strictInsertFill(metaObject, "version", Integer.class, 1);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long currentUserId = currentUserId();
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 获取当前登录用户 ID（兼容未登录场景如初始化脚本）
     */
    private Long currentUserId() {
        try {
            UserContext ctx = UserContextHolder.get();
            if (ctx != null && ctx.getUserId() != null) {
                return ctx.getUserId();
            }
        } catch (Exception ignored) {
            // 上下文不可用时返回 null（数据库字段可为 NULL 或由调用方控制）
        }
        return null;
    }
}
