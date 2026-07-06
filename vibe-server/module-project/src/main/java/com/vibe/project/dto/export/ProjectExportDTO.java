package com.vibe.project.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目列表导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.project.vo.ProjectVO}，仅保留导出所需字段。
 * 日期字段使用 {@link DateTimeFormat} 格式化，按业务字段顺序排列。</p>
 *
 * @author vibe
 */
@Data
public class ProjectExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "项目ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "项目编号", index = 1)
    @ColumnWidth(18)
    private String projectCode;

    @ExcelProperty(value = "项目名称", index = 2)
    @ColumnWidth(30)
    private String projectName;

    @ExcelProperty(value = "客户名称", index = 3)
    @ColumnWidth(24)
    private String customerName;

    @ExcelProperty(value = "项目类型", index = 4)
    @ColumnWidth(12)
    private String projectType;

    @ExcelProperty(value = "产品线", index = 5)
    @ColumnWidth(14)
    private String productLine;

    @ExcelProperty(value = "执行模式", index = 6)
    @ColumnWidth(12)
    private String executeMode;

    @ExcelProperty(value = "优先级", index = 7)
    @ColumnWidth(10)
    private String priority;

    @ExcelProperty(value = "项目状态", index = 8)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "当前阶段", index = 9)
    @ColumnWidth(12)
    private String currentPhase;

    @ExcelProperty(value = "项目经理", index = 10)
    @ColumnWidth(14)
    private String pmName;

    @ExcelProperty(value = "区域", index = 11)
    @ColumnWidth(12)
    private String region;

    @ExcelProperty(value = "合同编号", index = 12)
    @ColumnWidth(18)
    private String contractNo;

    @ExcelProperty(value = "计划开始", index = 13)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate plannedStart;

    @ExcelProperty(value = "计划结束", index = 14)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate plannedEnd;

    @ExcelProperty(value = "实际开始", index = 15)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate actualStart;

    @ExcelProperty(value = "实际结束", index = 16)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate actualEnd;

    @ExcelProperty(value = "进度(%)", index = 17)
    @ColumnWidth(10)
    private Integer progressPct;

    @ExcelProperty(value = "备注", index = 18)
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 19)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
