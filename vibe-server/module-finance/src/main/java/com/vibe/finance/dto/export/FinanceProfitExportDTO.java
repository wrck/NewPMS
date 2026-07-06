package com.vibe.finance.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 利润分析导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.finance.vo.FinanceProfitVO}，
 * 仅保留导出所需字段，金额与百分比字段使用 {@link NumberFormat} 格式化。</p>
 *
 * @author vibe
 */
@Data
public class FinanceProfitExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "项目ID", index = 0)
    @ColumnWidth(12)
    private Long projectId;

    @ExcelProperty(value = "项目名称", index = 1)
    @ColumnWidth(30)
    private String projectName;

    @ExcelProperty(value = "合同收入", index = 2)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal revenue;

    @ExcelProperty(value = "自有成本", index = 3)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal selfCost;

    @ExcelProperty(value = "代理商成本", index = 4)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal agentCost;

    @ExcelProperty(value = "总成本", index = 5)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal totalCost;

    @ExcelProperty(value = "毛利润", index = 6)
    @NumberFormat("#,##0.00")
    @ColumnWidth(16)
    private BigDecimal profit;

    @ExcelProperty(value = "毛利率(%)", index = 7)
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal profitMargin;

    @ExcelProperty(value = "自施成本占比(%)", index = 8)
    @NumberFormat("#,##0.00")
    @ColumnWidth(18)
    private BigDecimal selfCostRatio;

    @ExcelProperty(value = "代施成本占比(%)", index = 9)
    @NumberFormat("#,##0.00")
    @ColumnWidth(18)
    private BigDecimal agentCostRatio;
}
