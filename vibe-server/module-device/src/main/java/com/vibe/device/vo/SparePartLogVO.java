package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备件领用/归还记录视图对象。
 *
 * @author vibe
 */
@Data
@Schema(description = "备件领用/归还记录")
public class SparePartLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "备件ID")
    private Long sparePartId;

    @Schema(description = "备件名称")
    private String partName;

    @Schema(description = "备件编码")
    private String partCode;

    @Schema(description = "操作类型 IN/OUT/RETURN/REPAIR")
    private String actionType;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
