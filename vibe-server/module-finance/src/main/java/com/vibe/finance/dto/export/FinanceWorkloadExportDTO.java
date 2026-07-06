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
 * 代理商结算导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.finance.vo.FinanceWorkloadConfirmationVO}，
 * 仅保留导出所需字段，金额与人天字段使用 {@link NumberFormat} 千分位格式化。</p>
 *
 * @author vibe
 */
@Data
public class FinanceWorkloadExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "结算单ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "项目ID", index = 1)
    @ColumnWidth(12)
    private Long projectId;

    @ExcelProperty(value = "转包任务ID", index = 2)
    @ColumnWidth(14)
    private Long outsourceTaskId;

    @ExcelProperty(value = "代理商ID", index = 3)
    @ColumnWidth(12)
    private Long agentCompanyId;

    @ExcelProperty(value = "对账周期", index = 4)
    @ColumnWidth(14)
    private String period;

    @ExcelProperty(value = "工作量(人天)", index = 5)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal workloadDays;

    @ExcelProperty(value = "人天单价", index = 6)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal unitPrice;

    @ExcelProperty(value = "差旅费用", index = 7)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal travelAmount;

    @ExcelProperty(value = "其他费用", index = 8)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal otherAmount;

    @ExcelProperty(value = "结算总额", index = 9)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal totalAmount;

    @ExcelProperty(value = "审批状态", index = 10)
    @ColumnWidth(12)
    private String approvalStatus;

    @ExcelProperty(value = "付款状态", index = 11)
    @ColumnWidth(12)
    private String paymentStatus;

    @ExcelProperty(value = "PM确认时间", index = 12)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime pmConfirmTime;

    @ExcelProperty(value = "代理商确认时间", index = 13)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(22)
    private LocalDateTime agentConfirmTime;

    @ExcelProperty(value = "备注", index = 14)
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 15)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
