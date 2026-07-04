package com.vibe.acceptance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 验收标准创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "验收标准创建/更新")
public class AcceptanceStandardSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（更新时必填）")
    private Long id;

    @NotBlank(message = "标准名称不能为空")
    @Schema(description = "标准名称")
    private String name;

    @Schema(description = "适用项目类型")
    private String projectType;

    @Schema(description = "标准版本")
    private String standardVersion;

    @Schema(description = "标准说明")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "检查项列表")
    private java.util.List<ItemDTO> items;

    /**
     * 检查项
     */
    @Data
    @Schema(description = "检查项")
    public static class ItemDTO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "检查项ID（更新时必填）")
        private Long id;

        @NotBlank(message = "检查项名称不能为空")
        @Schema(description = "检查项名称")
        private String name;

        @Schema(description = "检查要求")
        private String requirement;

        @Schema(description = "测试方法")
        private String testMethod;

        @Schema(description = "权重")
        private BigDecimal weight;

        @Schema(description = "排序")
        private Integer sortOrder;
    }
}
