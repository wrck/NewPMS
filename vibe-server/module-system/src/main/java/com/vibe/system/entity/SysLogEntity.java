package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 操作日志实体（sys_log）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
@Schema(description = "操作日志")
public class SysLogEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "操作模块标题")
    private String title;

    @Schema(description = "业务类型 INSERT/UPDATE/DELETE/EXPORT/IMPORT/OTHER")
    private String businessType;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求URL")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParam;

    @Schema(description = "返回结果")
    private String responseResult;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    @Schema(description = "操作IP")
    private String operIp;
}
