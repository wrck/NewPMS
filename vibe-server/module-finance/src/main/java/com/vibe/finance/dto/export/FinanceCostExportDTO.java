package com.vibe.finance.dto.export;

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
 * 成本归集导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.finance.vo.FinanceCostVO}，仅保留导出所需字段。</p>
 *
 * @author vibe
 */
@Data
public class FinanceCostExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "成本ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "项目ID", index = 1)
    @ColumnWidth(12)
    private Long projectId;

    @ExcelProperty(value = "成本类型", index = 2)
    @ColumnWidth(12)
    private String costType;

    @ExcelProperty(value = "金额", index = 3)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal amount;

    @ExcelProperty(value = "发生日期", index = 4)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate costDate;

    @ExcelProperty(value = "关联业务类型", index = 5)
    @ColumnWidth(16)
    private String refType;

    @ExcelProperty(value = "关联业务ID", index = 6)
    @ColumnWidth(14)
    private Long refId;

    @ExcelProperty(value = "费用说明", index = 7)
    @ColumnWidth(30)
    private String description;

    @ExcelProperty(value = "创建时间", index = 8)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
