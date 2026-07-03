package com.vibe.agent.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 代理商公司分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代理商公司查询")
public class AgentCompanyQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（公司名/编码/联系人/电话）")
    private String keyword;

    @Schema(description = "合作状态 ACTIVE/SUSPENDED/TERMINATED")
    private String status;

    @Schema(description = "服务区域")
    private String region;

    @Schema(description = "服务产品线")
    private String productLine;
}
