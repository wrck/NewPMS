package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工程师技能 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师技能")
public class EngineerSkillDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "技能ID（编辑时必填）")
    private Long id;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "技能标签（路由/交换/无线/安全/数据中心/布线）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "技能标签不能为空")
    @Size(max = 64, message = "技能标签长度不能超过64")
    private String skillTag;

    @Schema(description = "等级 JUNIOR/MIDDLE/SENIOR/EXPERT")
    @Size(max = 16, message = "等级长度不能超过16")
    private String level;
}
