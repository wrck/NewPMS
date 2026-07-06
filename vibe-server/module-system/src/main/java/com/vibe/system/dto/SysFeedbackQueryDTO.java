package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 反馈分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "反馈分页查询")
public class SysFeedbackQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "反馈类型 BUG/SUGGESTION/QUESTION")
    private String type;

    @Schema(description = "状态 PENDING/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "标题关键字（模糊）")
    private String keyword;

    @Schema(description = "提交人 ID（管理员查询时使用）")
    private Long submitterId;
}
