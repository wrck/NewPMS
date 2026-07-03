package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 项目看板视图 VO（按状态分组）
 *
 * @author vibe
 */
@Data
@Schema(description = "项目看板")
public class ProjectKanbanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态分组编码")
    private String status;

    @Schema(description = "状态分组名称")
    private String statusName;

    @Schema(description = "该状态下项目数量")
    private Integer count;

    @Schema(description = "该状态下的项目卡片")
    private List<ProjectVO> projects;
}
