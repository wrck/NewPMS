package com.vibe.project.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 客户分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "客户分页查询")
public class CustomerQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户名称/编码（模糊）")
    private String keyword;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "行业")
    private String industry;
}
