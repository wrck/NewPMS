package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 系统配置分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置分页查询")
public class SysConfigQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置名称/键（模糊）")
    private String keyword;

    @Schema(description = "配置类型 SYSTEM/CUSTOM")
    private String configType;
}
