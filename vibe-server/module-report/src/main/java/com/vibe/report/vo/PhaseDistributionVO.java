package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目阶段分布 VO
 *
 * <p>字段名对齐前端 {@code PhaseDistribution} 接口定义。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "项目阶段分布")
public class PhaseDistributionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 阶段编码 */
    @Schema(description = "阶段编码")
    private String phase;

    /** 阶段名称 */
    @Schema(description = "阶段名称")
    private String phaseName;

    /** 项目数 */
    @Schema(description = "项目数")
    private Long count;

    public PhaseDistributionVO() {
    }

    public PhaseDistributionVO(String phase, String phaseName, Long count) {
        this.phase = phase;
        this.phaseName = phaseName;
        this.count = count;
    }
}
