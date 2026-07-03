package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目评论 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目评论")
public class ProjectCommentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @Schema(description = "父评论ID（回复评论时填）")
    private Long parentId;
}
