package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工程师技能视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师技能")
public class EngineerSkillVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "技能ID")
    private Long id;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "技能标签")
    private String skillTag;

    @Schema(description = "等级 JUNIOR/MIDDLE/SENIOR/EXPERT")
    private String level;
}
