package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备批量导入结果视图对象。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备批量导入结果")
public class DeviceImportResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "总行数")
    private int totalRows;

    @Schema(description = "成功导入数量")
    private int successCount;

    @Schema(description = "跳过数量（SN 重复或数据非法）")
    private int skippedCount;

    @Schema(description = "错误清单（行号 + 错误原因）")
    private List<ErrorItem> errors = new ArrayList<>();

    /**
     * 错误条目。
     */
    @Data
    @Schema(description = "导入错误条目")
    public static class ErrorItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Excel 行号（从 1 开始，含表头）")
        private int rowIndex;

        @Schema(description = "序列号 SN")
        private String serialNumber;

        @Schema(description = "错误原因")
        private String reason;

        public ErrorItem() {
        }

        public ErrorItem(int rowIndex, String serialNumber, String reason) {
            this.rowIndex = rowIndex;
            this.serialNumber = serialNumber;
            this.reason = reason;
        }
    }
}
