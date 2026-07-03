package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 派单结果（含成功与失败明细，用于批量派单回执）
 *
 * @author vibe
 */
@Data
@Schema(description = "派单结果")
public class DispatchResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成功数量")
    private Integer successCount = 0;

    @Schema(description = "失败数量")
    private Integer failCount = 0;

    @Schema(description = "成功派发的排期ID列表")
    private List<Long> successIds = new ArrayList<>();

    @Schema(description = "失败明细（taskId + 失败原因）")
    private List<FailItem> failures = new ArrayList<>();

    /**
     * 失败明细项
     */
    @Data
    @Schema(description = "失败明细项")
    public static class FailItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "任务ID")
        private Long taskId;

        @Schema(description = "工程师ID")
        private Long engineerId;

        @Schema(description = "失败原因")
        private String reason;
    }
}
