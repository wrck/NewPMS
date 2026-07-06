package com.vibe.lowcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 低代码关联页配置 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "低代码关联页配置")
public class LowcodeRelationConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "配置编码（唯一）")
    private String configCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "JSON Schema（主从关联/级联规则/显示字段）")
    private String schemaJson;

    @Schema(description = "关联模板ID")
    private Long templateId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
