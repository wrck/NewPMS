package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目成员视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目成员")
public class ProjectMemberVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "成员记录ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名（关联查询）")
    private String userName;

    @Schema(description = "项目角色")
    private String role;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;
}
