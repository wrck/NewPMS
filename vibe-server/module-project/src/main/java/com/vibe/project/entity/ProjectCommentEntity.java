package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目沟通记录实体（project_comment）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_comment")
@Schema(description = "项目沟通记录")
public class ProjectCommentEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论人ID")
    private Long authorId;

    @Schema(description = "父评论ID（支持回复）")
    private Long parentId;
}
