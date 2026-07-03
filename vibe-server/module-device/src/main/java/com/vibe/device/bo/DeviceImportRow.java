package com.vibe.device.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备批量导入 Excel 行模型（EasyExcel 读取）。
 *
 * <p>Excel 列：SN | MAC | 型号编码 | 固件版本 | 仓库编码 | 备注</p>
 *
 * @author vibe
 */
@Data
public class DeviceImportRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "序列号SN")
    private String serialNumber;

    @ExcelProperty(value = "MAC地址")
    private String macAddress;

    @ExcelProperty(value = "型号编码")
    private String modelCode;

    @ExcelProperty(value = "固件版本")
    private String firmwareVersion;

    @ExcelProperty(value = "仓库编码")
    private String warehouseCode;

    @ExcelProperty(value = "备注")
    private String remark;
}
