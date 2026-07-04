package com.vibe.acceptance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 验收标准 VO（含检查项列表）
 *
 * @author vibe
 */
@Data
@Schema(description = "验收标准")
public class AcceptanceStandardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

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
    private List<ItemVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 检查项 VO
     */
    @Data
    @Schema(description = "检查项")
    public static class ItemVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "主键")
        private Long id;

        @Schema(description = "所属验收标准ID")
        private Long standardId;

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
