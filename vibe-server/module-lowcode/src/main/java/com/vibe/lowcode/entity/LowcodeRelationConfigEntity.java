package com.vibe.lowcode.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 低代码关联页配置实体（lowcode_relation_config 表）
 *
 * <p>存储关联页的 JSON Schema（Draft 7），定义主从关联/级联规则/显示字段，
 * 运行时由 RelationRenderer 解析并渲染为主从关联页面。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lowcode_relation_config")
@Schema(description = "低代码关联页配置")
public class LowcodeRelationConfigEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置编码（唯一）")
    private String configCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "JSON Schema（主从关联/级联规则/显示字段）")
    private String schemaJson;

    @Schema(description = "关联模板ID（lowcode_template.id，可空）")
    private Long templateId;

    // version 字段由 BaseEntity 继承（@Version 乐观锁 + 版本追踪，映射 version 列）

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人ID")
    private Long creatorId;
}
