package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典数据新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "字典数据新增/编辑")
public class SysDictDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字典数据ID（编辑时必填）")
    private Long id;

    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典类型编码不能为空")
    @Size(max = 64, message = "字典类型编码长度不能超过64")
    private String dictType;

    @Schema(description = "字典标签", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典标签不能为空")
    @Size(max = 128, message = "字典标签长度不能超过128")
    private String dictLabel;

    @Schema(description = "字典键值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典键值不能为空")
    @Size(max = 128, message = "字典键值长度不能超过128")
    private String dictValue;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
