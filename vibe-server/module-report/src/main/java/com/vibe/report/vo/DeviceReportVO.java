package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 设备报表 VO
 *
 * <p>对齐前端 {@code report.ts -> getDeviceReport} 返回结构。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "设备报表")
public class DeviceReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "汇总")
    private Summary summary;

    @Schema(description = "状态分布")
    private List<StatusDist> statusDistribution;

    @Schema(description = "产品线分布")
    private List<ProductLineDist> productLineDistribution;

    @Schema(description = "各项目 BOM 完成率")
    private List<BomCompletion> bomCompletion;

    @Schema(description = "库存状态")
    private List<InventoryStatus> inventoryStatus;

    @Data
    @Schema(description = "设备汇总")
    public static class Summary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "设备总数") private Long total;
        @Schema(description = "在网") private Long online;
        @Schema(description = "离线") private Long offline;
        @Schema(description = "异常") private Long abnormal;
    }

    @Data
    @Schema(description = "设备状态分布")
    public static class StatusDist implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "状态码") private String status;
        @Schema(description = "设备数") private Long count;
    }

    @Data
    @Schema(description = "产品线分布")
    public static class ProductLineDist implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "产品线") private String productLine;
        @Schema(description = "设备数") private Long count;
    }

    @Data
    @Schema(description = "BOM 完成率")
    public static class BomCompletion implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "项目ID") private Long projectId;
        @Schema(description = "项目名称") private String projectName;
        @Schema(description = "BOM 总数") private Long totalQty;
        @Schema(description = "已到货数") private Long completedQty;
        @Schema(description = "完成率（百分比）") private BigDecimal rate;
    }

    @Data
    @Schema(description = "库存状态")
    public static class InventoryStatus implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "仓库名称") private String warehouseName;
        @Schema(description = "总库存") private Long totalQty;
        @Schema(description = "预警库存") private Long warningQty;
    }
}
