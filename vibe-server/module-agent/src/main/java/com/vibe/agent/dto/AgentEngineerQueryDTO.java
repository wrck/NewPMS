package com.vibe.agent.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 代理商工程师分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代理商工程师查询")
public class AgentEngineerQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（姓名/手机号）")
    private String keyword;

    @Schema(description = "所属代理商ID")
    private Long agentCompanyId;

    @Schema(description = "状态 ACTIVE/DISABLED")
    private String status;

    @Schema(description = "技能标签")
    private String skill;
}
