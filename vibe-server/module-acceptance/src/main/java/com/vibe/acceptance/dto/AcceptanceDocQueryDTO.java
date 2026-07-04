package com.vibe.acceptance.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 竣工文档查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "竣工文档查询")
public class AcceptanceDocQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "文档类型")
    private String docType;
}
