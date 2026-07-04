package com.vibe.acceptance.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 验收任务分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "验收任务查询")
public class AcceptanceTaskQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "验收任务名称（模糊）")
    private String name;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "申请人ID")
    private Long applyUserId;
}
