package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 组织架构新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "组织架构新增/编辑")
public class SysOrgDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "组织ID（编辑时必填）")
    private Long id;

    @Schema(description = "父组织ID（0为根）")
    private Long parentId;

    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 64, message = "组织名称长度不能超过64")
    private String orgName;

    @Schema(description = "组织编码")
    @Size(max = 64, message = "组织编码长度不能超过64")
    private String orgCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
