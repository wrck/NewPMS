package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 通知模板分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知模板分页查询")
public class SysNoticeTemplateQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板编码/名称（模糊）")
    private String keyword;

    @Schema(description = "接收人类型")
    private String recipientType;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
