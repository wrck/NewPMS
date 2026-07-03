package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目归档 DTO（含复盘记录）
 *
 * @author vibe
 */
@Data
@Schema(description = "项目归档")
public class ProjectArchiveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "复盘记录")
    private String reviewSummary;

    @Schema(description = "经验沉淀")
    private String lessonsLearned;
}
