package com.vibe.finance.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目预算导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.finance.vo.FinanceBudgetVO}，仅保留导出所需字段，
 * 列顺序由 {@code index} 显式指定，金额列使用 {@link NumberFormat} 千分位格式化。</p>
 *
 * @author vibe
 */
@Data
public class FinanceBudgetExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "预算ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "项目ID", index = 1)
    @ColumnWidth(12)
    private Long projectId;

    @ExcelProperty(value = "预算年度", index = 2)
    @ColumnWidth(10)
    private Integer year;

    @ExcelProperty(value = "人工预算", index = 3)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal laborAmount;

    @ExcelProperty(value = "差旅预算", index = 4)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal travelAmount;

    @ExcelProperty(value = "代理商预算", index = 5)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal agentAmount;

    @ExcelProperty(value = "其他预算", index = 6)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal otherAmount;

    @ExcelProperty(value = "预算总额", index = 7)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal totalAmount;

    @ExcelProperty(value = "审批状态", index = 8)
    @ColumnWidth(12)
    private String approvalStatus;

    @ExcelProperty(value = "实际人工成本", index = 9)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal actualLabor;

    @ExcelProperty(value = "实际差旅成本", index = 10)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal actualTravel;

    @ExcelProperty(value = "实际代理商成本", index = 11)
    @NumberFormat("#,##0.00")
    @ColumnWidth(18)
    private BigDecimal actualAgent;

    @ExcelProperty(value = "实际其他成本", index = 12)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal actualOther;

    @ExcelProperty(value = "实际总成本", index = 13)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal actualTotal;

    @ExcelProperty(value = "审批时间", index = 14)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime approveTime;

    @ExcelProperty(value = "备注", index = 15)
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 16)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
