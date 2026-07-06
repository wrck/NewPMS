package com.vibe.device.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备台账导出 DTO（EasyExcel 行模型）。
 *
 * <p>对应 {@link com.vibe.device.vo.DeviceInstanceVO}，仅保留导出所需字段。</p>
 *
 * @author vibe
 */
@Data
public class DeviceInstanceExportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "设备ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "序列号SN", index = 1)
    @ColumnWidth(20)
    private String serialNumber;

    @ExcelProperty(value = "MAC地址", index = 2)
    @ColumnWidth(20)
    private String macAddress;

    @ExcelProperty(value = "型号名称", index = 3)
    @ColumnWidth(20)
    private String modelName;

    @ExcelProperty(value = "型号编码", index = 4)
    @ColumnWidth(16)
    private String modelCode;

    @ExcelProperty(value = "固件版本", index = 5)
    @ColumnWidth(14)
    private String firmwareVersion;

    @ExcelProperty(value = "项目名称", index = 6)
    @ColumnWidth(24)
    private String projectName;

    @ExcelProperty(value = "项目编号", index = 7)
    @ColumnWidth(18)
    private String projectCode;

    @ExcelProperty(value = "安装站点", index = 8)
    @ColumnWidth(16)
    private String siteName;

    @ExcelProperty(value = "安装位置", index = 9)
    @ColumnWidth(20)
    private String installLocation;

    @ExcelProperty(value = "设备状态", index = 10)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "所属仓库", index = 11)
    @ColumnWidth(16)
    private String warehouseName;

    @ExcelProperty(value = "安装日期", index = 12)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate installDate;

    @ExcelProperty(value = "入网日期", index = 13)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    private LocalDate onlineDate;

    @ExcelProperty(value = "备注", index = 14)
    @ColumnWidth(24)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 15)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
