package com.vibe.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集成调用日志视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "集成调用日志")
public class IntegrationCallLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

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

    @Schema(description = "调用状态")
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
