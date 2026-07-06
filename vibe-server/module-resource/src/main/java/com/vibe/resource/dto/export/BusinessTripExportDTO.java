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
 * 出差记录导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.resource.vo.BusinessTripVO}，仅保留导出所需字段。
 * 费用字段使用 {@link NumberFormat} 千分位格式化。</p>
 *
 * @author vibe
 */
@Data
public class BusinessTripExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "出差ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "工程师姓名", index = 1)
    @ColumnWidth(14)
    private String engineerName;

    @ExcelProperty(value = "项目名称", index = 2)
    @ColumnWidth(24)
    private String projectName;

    @ExcelProperty(value = "出发地", index = 3)
    @ColumnWidth(16)
    private String origin;

    @ExcelProperty(value = "目的地", index = 4)
    @ColumnWidth(16)
    private String destination;

    @ExcelProperty(value = "开始日期", index = 5)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate startDate;

    @ExcelProperty(value = "结束日期", index = 6)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate endDate;

    @ExcelProperty(value = "交通方式", index = 7)
    @ColumnWidth(12)
    private String transportMode;

    @ExcelProperty(value = "住宿信息", index = 8)
    @ColumnWidth(20)
    private String accommodation;

    @ExcelProperty(value = "预估费用", index = 9)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal estimatedCost;

    @ExcelProperty(value = "实际费用", index = 10)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal actualCost;

    @ExcelProperty(value = "出差事由", index = 11)
    @ColumnWidth(30)
    private String reason;

    @ExcelProperty(value = "状态", index = 12)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "审批人", index = 13)
    @ColumnWidth(14)
    private String approverName;

    @ExcelProperty(value = "审批时间", index = 14)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime approveTime;

    @ExcelProperty(value = "备注", index = 15)
    @ColumnWidth(24)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 16)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
