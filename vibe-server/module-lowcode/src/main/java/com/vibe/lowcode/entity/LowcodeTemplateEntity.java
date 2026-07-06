package com.vibe.lowcode.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 低代码模板实体（lowcode_template 表）
 *
 * <p>可被各配置类型（FORM/LIST/TAB/RELATION）复用的 Schema 模板。
 * 实例化时复制 Schema 到新配置，并将 {@link #usageCount} +1。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lowcode_template")
@Schema(description = "低代码模板")
public class LowcodeTemplateEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板编码（唯一）")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板类型 FORM/LIST/TAB/RELATION")
    private String templateType;

    @Schema(description = "JSON Schema")
    private String schemaJson;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "被使用次数（实例化时 +1）")
    private Integer usageCount;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建人ID")
    private Long creatorId;
}
