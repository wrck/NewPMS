package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 项目模板详情 VO（含阶段与任务）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目模板详情")
public class ProjectTemplateDetailVO extends ProjectTemplateVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板阶段列表")
    private List<ProjectTemplatePhaseVO> phases;

    @Schema(description = "模板任务列表")
    private List<ProjectTemplateTaskVO> tasks;
}
