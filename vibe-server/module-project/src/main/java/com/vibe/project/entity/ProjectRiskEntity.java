package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 项目风险实体（project_risk）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_risk")
@Schema(description = "项目风险")
public class ProjectRiskEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "风险描述")
    private String riskDesc;

    @Schema(description = "影响程度 HIGH/MEDIUM/LOW")
    private String impact;

    @Schema(description = "发生概率 HIGH/MEDIUM/LOW")
    private String probability;

    @Schema(description = "应对措施")
    private String measure;

    @Schema(description = "责任人ID")
    private Long ownerId;

    @Schema(description = "状态 OPEN/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "截止日期")
    private LocalDate dueDate;
}
