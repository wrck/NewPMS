package com.vibe.integration.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 集成调用日志分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "集成调用日志分页查询")
public class IntegrationCallLogQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联配置ID")
    private Long configId;

    @Schema(description = "系统编码")
    private String systemCode;

    @Schema(description = "调用场景")
    private String callScene;

    @Schema(description = "调用状态 SUCCESS/FAIL/TIMEOUT")
    private String status;

    @Schema(description = "起始时间")
    private String startBegin;

    @Schema(description = "结束时间")
    private String startEnd;
}
