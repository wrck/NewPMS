package com.vibe.resource.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 工程师分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工程师分页查询")
public class EngineerQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "姓名/工号/手机号（模糊）")
    private String keyword;

    @Schema(description = "状态 ACTIVE/RESIGNED")
    private String status;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "技能标签")
    private String skillTag;

    @Schema(description = "技能等级 JUNIOR/MIDDLE/SENIOR/EXPERT")
    private String skillLevel;

    @Schema(description = "关联用户ID")
    private Long userId;
}
