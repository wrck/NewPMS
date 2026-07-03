package com.vibe.collaboration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 客户视角的项目列表项 VO（脱敏）
 *
 * <p>仅暴露客户可见字段，不含 PM/工程师/成本等内部信息。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户项目列表项")
public class CustomerProjectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类型 新建/扩容/改造/替换/安全")
    private String projectType;

    @Schema(description = "当前阶段编码")
    private String currentPhase;

    @Schema(description = "进度百分比 0-100")
    private Integer progressPct;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "项目状态 INIT/PLAN/EXECUTE/ACCEPT/CLOSE/ARCHIVED/ON_HOLD/CANCELLED")
    private String status;
}
