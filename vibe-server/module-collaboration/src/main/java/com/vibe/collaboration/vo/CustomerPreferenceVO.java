package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户偏好视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "客户偏好")
public class CustomerPreferenceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "偏好ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "偏好键")
    private String preferenceKey;

    @Schema(description = "偏好值")
    private String preferenceValue;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
