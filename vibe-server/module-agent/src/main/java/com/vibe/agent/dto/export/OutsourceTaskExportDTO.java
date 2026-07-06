package com.vibe.agent.dto.export;

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
 * 转包任务导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.agent.vo.OutsourceTaskVO}，仅保留 PM/总监视角的导出字段
 * （含合同金额/成本等敏感字段，代理商角色不应使用本 DTO 导出）。</p>
 *
 * @author vibe
 */
@Data
public class OutsourceTaskExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "任务ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "项目名称", index = 1)
    @ColumnWidth(24)
    private String projectName;

    @ExcelProperty(value = "项目编号", index = 2)
    @ColumnWidth(18)
    private String projectCode;

    @ExcelProperty(value = "客户名称", index = 3)
    @ColumnWidth(20)
    private String customerName;

    @ExcelProperty(value = "关联任务名称", index = 4)
    @ColumnWidth(20)
    private String taskName;

    @ExcelProperty(value = "代理商公司", index = 5)
    @ColumnWidth(20)
    private String agentCompanyName;

    @ExcelProperty(value = "代理商工程师", index = 6)
    @ColumnWidth(14)
    private String agentEngineerName;

    @ExcelProperty(value = "任务范围与要求", index = 7)
    @ColumnWidth(40)
    private String taskScope;

    @ExcelProperty(value = "截止日期", index = 8)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate deadline;

    @ExcelProperty(value = "任务状态", index = 9)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "提交次数", index = 10)
    @ColumnWidth(10)
    private Integer submitCount;

    @ExcelProperty(value = "退回原因", index = 11)
    @ColumnWidth(30)
    private String rejectReason;

    @ExcelProperty(value = "合同金额", index = 12)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal contractAmount;

    @ExcelProperty(value = "成本金额", index = 13)
    @NumberFormat("#,##0.00")
    @ColumnWidth(14)
    private BigDecimal costAmount;

    @ExcelProperty(value = "确认时间", index = 14)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime confirmedTime;

    @ExcelProperty(value = "创建时间", index = 15)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
