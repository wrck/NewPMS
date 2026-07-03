package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 登录日志分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志分页查询")
public class SysLoginLogQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号（模糊）")
    private String username;

    @Schema(description = "状态 1-成功 0-失败")
    private Integer status;

    @Schema(description = "登录开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "登录结束时间")
    private LocalDateTime endTime;
}
