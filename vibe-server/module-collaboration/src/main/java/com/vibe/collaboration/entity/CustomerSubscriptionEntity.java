package com.vibe.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 客户订阅实体（customer_subscription）
 *
 * <p>记录客户对不同事件类型的订阅关系及接收渠道（如 PROJECT_PROGRESS/CUTOVER_NOTICE 事件
 * 订阅 SMS/EMAIL/IM 渠道）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_subscription")
@Schema(description = "客户订阅")
public class CustomerSubscriptionEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "事件类型 PROJECT_PROGRESS/CUTOVER_NOTICE/ACCEPTANCE_NOTICE/DOCUMENT_UPLOAD")
    private String eventType;

    @Schema(description = "接收渠道（逗号分隔：SMS,EMAIL,IM）")
    private String channels;

    @Schema(description = "订阅状态 SUBSCRIBED/UNSUBSCRIBED")
    private String status;
}
