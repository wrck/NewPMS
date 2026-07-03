package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 备件领用/归还记录实体（spare_part_log）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_part_log")
@Schema(description = "备件领用/归还记录")
public class SparePartLogEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备件ID")
    private Long sparePartId;

    @Schema(description = "操作类型 IN/OUT/RETURN/REPAIR")
    private String actionType;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "备注")
    private String remark;
}
