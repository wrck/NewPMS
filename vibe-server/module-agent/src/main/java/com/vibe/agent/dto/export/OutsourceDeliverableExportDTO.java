package com.vibe.agent.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理商交付物导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.agent.vo.OutsourceDeliverableVO}，仅保留导出所需字段。</p>
 *
 * @author vibe
 */
@Data
public class OutsourceDeliverableExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "交付物ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "转包任务ID", index = 1)
    @ColumnWidth(14)
    private Long outsourceTaskId;

    @ExcelProperty(value = "交付物类型", index = 2)
    @ColumnWidth(14)
    private String deliverableType;

    @ExcelProperty(value = "文件名", index = 3)
    @ColumnWidth(30)
    private String fileName;

    @ExcelProperty(value = "文件地址", index = 4)
    @ColumnWidth(40)
    private String fileUrl;

    @ExcelProperty(value = "备注", index = 5)
    @ColumnWidth(24)
    private String remark;

    @ExcelProperty(value = "提交人ID", index = 6)
    @ColumnWidth(12)
    private Long createBy;

    @ExcelProperty(value = "提交时间", index = 7)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
