package com.vibe.resource.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.resource.vo.TimesheetVO}，仅保留导出所需字段。
 * 工时字段使用 {@link NumberFormat} 千分位格式化。</p>
 *
 * @author vibe
 */
@Data
public class TimesheetExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "工时ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "工程师姓名", index = 1)
    @ColumnWidth(14)
    private String engineerName;

    @ExcelProperty(value = "项目名称", index = 2)
    @ColumnWidth(24)
    private String projectName;

    @ExcelProperty(value = "任务名称", index = 3)
    @ColumnWidth(20)
    private String taskName;

    @ExcelProperty(value = "工作日期", index = 4)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate workDate;

    @ExcelProperty(value = "工作时长(h)", index = 5)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal hours;

    @ExcelProperty(value = "加班时长(h)", index = 6)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal overtimeHours;

    @ExcelProperty(value = "出差天数", index = 7)
    @ColumnWidth(10)
    private Integer travelDays;

    @ExcelProperty(value = "工作内容说明", index = 8)
    @ColumnWidth(30)
    private String description;

    @ExcelProperty(value = "状态", index = 9)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "审批人", index = 10)
    @ColumnWidth(14)
    private String approverName;

    @ExcelProperty(value = "审批时间", index = 11)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime approveTime;

    @ExcelProperty(value = "创建时间", index = 12)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
