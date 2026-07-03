package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源调度模块实体基类（无 version 乐观锁字段）
 *
 * <p>resource 模块中除 engineer_schedule / timesheet 表外，其余表（engineer / engineer_skill /
 * engineer_leave / business_trip）均无 version 字段。直接继承
 * {@link com.vibe.common.base.BaseEntity} 会导致 MyBatis-Plus 在 SELECT 时带上 version 列而报错，
 * 因此对这些表使用本基类。</p>
 *
 * <p>公共字段：id / create_by / create_time / update_by / update_time / deleted。
 * 自动填充、逻辑删除由 module-common 的 {@code MetaObjectHandlerImpl}
 * 与 MyBatis-Plus 全局配置提供。</p>
 *
 * @author vibe
 */
@Data
public abstract class ResourceBaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识 0-未删除 1-已删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
