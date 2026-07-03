package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目设备清单（BOM）视图对象（含型号信息与进度数量）。
 *
 * @author vibe
 */
@Data
@Schema(description = "项目设备清单")
public class DeviceBomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "BOM ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "计划数量")
    private Integer plannedQty;

    @Schema(description = "已到货数量")
    private Integer receivedQty;

    @Schema(description = "已安装数量")
    private Integer installedQty;

    @Schema(description = "已验收数量")
    private Integer acceptedQty;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
