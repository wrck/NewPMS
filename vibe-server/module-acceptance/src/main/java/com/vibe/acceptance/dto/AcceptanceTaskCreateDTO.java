package com.vibe.acceptance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 验收任务创建 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "验收任务创建")
public class AcceptanceTaskCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "适用的验收标准ID")
    private Long standardId;

    @NotBlank(message = "验收任务名称不能为空")
    @Schema(description = "验收任务名称")
    private String name;

    @Schema(description = "备注")
    private String remark;
}
