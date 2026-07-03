package com.vibe.agent.entity;

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
 * 代理商模块实体基类（无 @Version）
 *
 * <p>代理商模块的大部分表（agent_company / agent_engineer / outsource_deliverable /
 * outsource_workload / agent_score_log）在 schema.sql 中未定义 {@code version} 列，
 * 直接继承 {@link com.vibe.common.base.BaseEntity}（含 @Version 字段）会导致
 * MyBatis-Plus 在 SELECT 时带出 {@code version} 列而报错。</p>
 *
 * <p>因此本模块参照 module-system 的 SysBaseEntity 模式，提供一个不含 @Version 的基类，
 * 仅供上述无 version 列的表使用。{@link OutsourceTaskEntity} 对应表已含 version 列，
 * 直接继承 {@link com.vibe.common.base.BaseEntity}。</p>
 *
 * <p>公共字段：id / create_by / create_time / update_by / update_time / deleted。
 * 自动填充、逻辑删除由 module-common 的 MetaObjectHandlerImpl 与 MyBatis-Plus 全局配置提供。</p>
 *
 * @author vibe
 */
@Data
public abstract class AgentBaseEntity implements Serializable {

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
