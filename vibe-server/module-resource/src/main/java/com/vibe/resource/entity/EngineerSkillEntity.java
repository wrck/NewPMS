package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 工程师技能实体（engineer_skill）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("engineer_skill")
@Schema(description = "工程师技能")
public class EngineerSkillEntity extends ResourceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "技能标签（路由/交换/无线/安全/数据中心/布线）")
    private String skillTag;

    @Schema(description = "等级 JUNIOR/MIDDLE/SENIOR/EXPERT")
    private String level;
}
