package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 风险预警 VO
 *
 * <p>字段名对齐前端 {@code RiskWarning} 接口定义。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "风险预警")
public class RiskWarningVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 风险项 ID */
    @Schema(description = "风险项 ID")
    private Long id;

    /** 项目 ID */
    @Schema(description = "项目 ID")
    private Long projectId;

    /** 项目名称 */
    @Schema(description = "项目名称")
    private String projectName;

    /** 风险类型：PROGRESS/DEVICE/RESOURCE/AGENT/OTHER */
    @Schema(description = "风险类型")
    private String riskType;

    /** 风险描述 */
    @Schema(description = "风险描述")
    private String description;

    /** 风险等级：LOW/MEDIUM/HIGH */
    @Schema(description = "风险等级")
    private String level;

    /** 发现时间 */
    @Schema(description = "发现时间")
    private LocalDate detectedAt;

    public RiskWarningVO() {
    }

    public RiskWarningVO(Long id, Long projectId, String projectName, String riskType,
                         String description, String level, LocalDate detectedAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.riskType = riskType;
        this.description = description;
        this.level = level;
        this.detectedAt = detectedAt;
    }
}
