package com.vibe.lowcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 低代码列表配置 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "低代码列表配置")
public class LowcodeListConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "配置编码（唯一）")
    private String configCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "JSON Schema（列定义/筛选/操作按钮）")
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
