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
 * 项目任务导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.project.vo.ProjectTaskVO}，仅保留导出所需字段。</p>
 *
 * @author vibe
 */
@Data
public class ProjectTaskExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "任务ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "项目名称", index = 1)
    @ColumnWidth(24)
    private String projectName;

    @ExcelProperty(value = "阶段名称", index = 2)
    @ColumnWidth(16)
    private String phaseName;

    @ExcelProperty(value = "任务名称", index = 3)
    @ColumnWidth(24)
    private String taskName;

    @ExcelProperty(value = "任务类型", index = 4)
    @ColumnWidth(12)
    private String taskType;

    @ExcelProperty(value = "状态", index = 5)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "执行模式", index = 6)
    @ColumnWidth(12)
    private String executeMode;

    @ExcelProperty(value = "执行人", index = 7)
    @ColumnWidth(14)
    private String assigneeName;

    @ExcelProperty(value = "代理商公司", index = 8)
    @ColumnWidth(20)
    private String agentCompanyName;

    @ExcelProperty(value = "计划开始", index = 9)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate plannedStart;

    @ExcelProperty(value = "计划结束", index = 10)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate plannedEnd;

    @ExcelProperty(value = "实际开始", index = 11)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate actualStart;

    @ExcelProperty(value = "实际结束", index = 12)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate actualEnd;

    @ExcelProperty(value = "优先级", index = 13)
    @ColumnWidth(10)
    private String priority;

    @ExcelProperty(value = "任务描述", index = 14)
    @ColumnWidth(40)
    private String description;

    @ExcelProperty(value = "创建时间", index = 15)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
