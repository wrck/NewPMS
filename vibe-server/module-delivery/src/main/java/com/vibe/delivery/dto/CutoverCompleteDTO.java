package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 割接完成 DTO
 *
 * <p>所有步骤完成后，PM 提交割接总结。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "割接完成")
public class CutoverCompleteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "割接方案ID不能为空")
    @Schema(description = "割接方案ID")
    private Long planId;

    @Schema(description = "割接总结")
    private String summary;

    @Schema(description = "问题与改进")
    private String problemImprovement;
}
