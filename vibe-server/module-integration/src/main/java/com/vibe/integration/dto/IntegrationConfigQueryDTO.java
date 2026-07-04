package com.vibe.integration.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 集成配置分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "集成配置分页查询")
public class IntegrationConfigQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "系统编码/名称（模糊）")
    private String keyword;

    @Schema(description = "适配器类型")
    private String adapterType;

    @Schema(description = "是否启用 1-是 0-否")
    private Integer enabled;
}
