package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 交付物提交 DTO
 *
 * <p>代理商通过移动端/H5 提交交付物。校验规则：</p>
 * <ul>
 *   <li>施工照片（PHOTO）必传，至少 {@code MIN_PHOTO_COUNT} 张</li>
 *   <li>测试记录（TEST_RECORD）必传</li>
 *   <li>签收单（RECEIPT）必传</li>
 *   <li>完成情况描述必填</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "交付物提交")
public class OutsourceDeliverableDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "转包任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转包任务ID不能为空")
    private Long outsourceTaskId;

    @Schema(description = "施工照片列表（必传，至少3张）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "施工照片不能为空，至少上传3张")
    @Valid
    private List<DeliverableItem> photos;

    @Schema(description = "测试记录列表（必传）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "测试记录不能为空")
    @Valid
    private List<DeliverableItem> testRecords;

    @Schema(description = "签收单列表（必传）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "签收单不能为空")
    @Valid
    private List<DeliverableItem> receipts;

    @Schema(description = "配置文件列表（可选）")
    private List<DeliverableItem> configs;

    @Schema(description = "完成情况描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "完成情况描述不能为空")
    private String completionDescription;

    /**
     * 单个交付物项。
     */
    @Data
    @Schema(description = "交付物项")
    public static class DeliverableItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "文件地址", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "文件地址不能为空")
        private String fileUrl;

        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "备注")
        private String remark;
    }
}
