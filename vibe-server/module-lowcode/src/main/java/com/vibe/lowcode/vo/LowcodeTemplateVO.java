package com.vibe.lowcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 低代码模板 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "低代码模板")
public class LowcodeTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

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

    @Schema(description = "被使用次数")
    private Integer usageCount;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
