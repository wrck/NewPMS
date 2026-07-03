package com.vibe.resource.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 工程师请假分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工程师请假分页查询")
public class EngineerLeaveQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "请假类型 ANNUAL/SICK/PERSONAL/OTHER")
    private String leaveType;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "查询开始日期")
    private LocalDate startDate;

    @Schema(description = "查询结束日期")
    private LocalDate endDate;
}
