package com.vibe.acceptance.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 验收标准分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "验收标准查询")
public class AcceptanceStandardQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标准名称（模糊）")
    private String name;

    @Schema(description = "适用项目类型")
    private String projectType;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
