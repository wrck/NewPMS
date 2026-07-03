package com.vibe.collaboration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 项目整体进度 VO（客户视角，脱敏）
 *
 * @author vibe
 */
@Data
@Schema(description = "项目整体进度")
public class ProjectProgressVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "进度百分比 0-100")
    private Integer progressPct;

    @Schema(description = "当前阶段名称")
    private String currentPhaseName;

    @Schema(description = "项目整体状态 INIT/PLAN/EXECUTE/ACCEPT/CLOSE/ARCHIVED/ON_HOLD/CANCELLED")
    private String overallStatus;

    @Schema(description = "阶段时间线列表")
    private List<PhaseTimelineVO> phases;
}
