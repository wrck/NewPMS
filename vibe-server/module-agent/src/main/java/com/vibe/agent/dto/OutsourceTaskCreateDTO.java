package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 转包任务创建 DTO
 *
 * <p>由 PM/SUPER_ADMIN 发起，指定代理商公司、任务范围、截止日期，
 * 关联项目任务ID。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "创建转包任务")
public class OutsourceTaskCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "关联项目任务ID（可选，自由外协任务可不传）")
    private Long taskId;

    @Schema(description = "代理商公司ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "代理商公司ID不能为空")
    private Long agentCompanyId;

    @Schema(description = "任务范围与要求", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务范围不能为空")
    private String taskScope;

    @Schema(description = "截止日期（必须晚于今天）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "截止日期不能为空")
    @Future(message = "截止日期必须晚于今天")
    private LocalDate deadline;

    @Schema(description = "附件列表（name+url）")
    private List<Attachment> attachments;

    /**
     * 附件项。
     */
    @Data
    @Schema(description = "附件项")
    public static class Attachment implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "文件名")
        private String name;

        @Schema(description = "文件地址")
        private String url;
    }
}
