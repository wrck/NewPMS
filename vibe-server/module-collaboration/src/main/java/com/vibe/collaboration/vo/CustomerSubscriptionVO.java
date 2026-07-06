package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户订阅视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "客户订阅")
public class CustomerSubscriptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订阅ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "接收渠道（逗号分隔）")
    private String channels;

    @Schema(description = "订阅状态 SUBSCRIBED/UNSUBSCRIBED")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
