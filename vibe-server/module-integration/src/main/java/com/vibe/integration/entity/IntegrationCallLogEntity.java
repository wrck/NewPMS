package com.vibe.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 集成调用日志实体（integration_call_log 表）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_call_log")
@Schema(description = "集成调用日志")
public class IntegrationCallLogEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联配置ID")
    private Long configId;

    @Schema(description = "系统编码")
    private String systemCode;

    @Schema(description = "调用场景")
    private String callScene;

    @Schema(description = "HTTP 方法")
    private String requestMethod;

    @Schema(description = "请求 URL")
    private String requestUrl;

    @Schema(description = "请求头（脱敏）")
    private String requestHeaders;

    @Schema(description = "请求体（脱敏）")
    private String requestBody;

    @Schema(description = "HTTP 响应码")
    private Integer responseStatus;

    @Schema(description = "响应体（截断）")
    private String responseBody;

    @Schema(description = "调用状态 SUCCESS/FAIL/TIMEOUT")
    private String status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "耗时毫秒")
    private Integer costMs;

    @Schema(description = "调用方 IP")
    private String callerIp;

    @Schema(description = "调用时间")
    private LocalDateTime operatedAt;
}
