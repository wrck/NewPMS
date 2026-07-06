package com.vibe.device.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备件领用/归还流水导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.device.vo.SparePartLogVO}，仅保留导出所需字段。</p>
 *
 * @author vibe
 */
@Data
public class SparePartLogExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "日志ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "备件名称", index = 1)
    @ColumnWidth(20)
    private String partName;

    @ExcelProperty(value = "备件编码", index = 2)
    @ColumnWidth(18)
    private String partCode;

    @ExcelProperty(value = "操作类型", index = 3)
    @ColumnWidth(12)
    private String actionType;

    @ExcelProperty(value = "数量", index = 4)
    @ColumnWidth(10)
    private Integer quantity;

    @ExcelProperty(value = "关联项目", index = 5)
    @ColumnWidth(20)
    private String projectName;

    @ExcelProperty(value = "操作人ID", index = 6)
    @ColumnWidth(12)
    private Long operatorId;

    @ExcelProperty(value = "备注", index = 7)
    @ColumnWidth(24)
    private String remark;

    @ExcelProperty(value = "操作时间", index = 8)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
