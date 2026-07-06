package com.vibe.lowcode.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 低代码配置分页查询 DTO（表单/列表/标签页/关联页通用）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "低代码配置查询")
public class LowcodeConfigQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（配置编码或名称模糊匹配）")
    private String keyword;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "关联模板ID")
    private Long templateId;
}
