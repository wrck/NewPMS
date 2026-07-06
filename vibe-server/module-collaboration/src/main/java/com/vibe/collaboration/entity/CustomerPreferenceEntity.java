package com.vibe.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 客户偏好实体（customer_preference）
 *
 * <p>存储客户在 H5 门户中的个性化偏好设置（如语言/主题/通知频率等键值对）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_preference")
@Schema(description = "客户偏好")
public class CustomerPreferenceEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "偏好键（如 language/theme/notify_frequency）")
    private String preferenceKey;

    @Schema(description = "偏好值")
    private String preferenceValue;
}
