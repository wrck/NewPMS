package com.vibe.delivery.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 割接方案分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "割接方案查询")
public class CutoverPlanQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "方案名称（模糊）")
    private String planName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "割接日期起（含）")
    private LocalDate dateFrom;

    @Schema(description = "割接日期止（含）")
    private LocalDate dateTo;

    @Schema(description = "编制人ID")
    private Long applyUserId;
}
