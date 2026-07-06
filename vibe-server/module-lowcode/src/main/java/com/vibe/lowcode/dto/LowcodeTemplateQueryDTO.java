package com.vibe.lowcode.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 低代码模板分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "低代码模板查询")
public class LowcodeTemplateQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（模板编码或名称模糊匹配）")
    private String keyword;

    @Schema(description = "模板类型 FORM/LIST/TAB/RELATION")
    private String templateType;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
