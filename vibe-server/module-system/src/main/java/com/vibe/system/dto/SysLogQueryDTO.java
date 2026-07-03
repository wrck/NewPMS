package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 操作日志分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志分页查询")
public class SysLogQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "操作模块标题（模糊）")
    private String title;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "操作结束时间")
    private LocalDateTime endTime;
}
