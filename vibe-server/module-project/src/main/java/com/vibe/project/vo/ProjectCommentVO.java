package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目评论视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目评论")
public class ProjectCommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论人ID")
    private Long authorId;

    @Schema(description = "评论人姓名（关联查询）")
    private String authorName;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "回复列表")
    private List<ProjectCommentVO> replies;
}
