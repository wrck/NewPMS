package com.vibe.resource.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 出差分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "出差分页查询")
public class BusinessTripQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED/COMPLETED")
    private String status;

    @Schema(description = "出差开始日期起")
    private LocalDate startDate;

    @Schema(description = "出差开始日期止")
    private LocalDate endDate;
}
