package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 字典数据分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典数据分页查询")
public class SysDictDataQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字典类型编码")
    private String dictType;

    @Schema(description = "字典标签（模糊）")
    private String keyword;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
