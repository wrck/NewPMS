package com.vibe.delivery.vo;

import com.vibe.delivery.bo.GpsLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单视图对象
 *
 * <p>列表/详情共用，列表场景 steps/photos/issues 为 null。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "工单")
public class WorkOrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long id;

    @Schema(description = "项目任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "签到时间")
    private LocalDateTime checkinTime;

    @Schema(description = "签退时间")
    private LocalDateTime checkoutTime;

    @Schema(description = "签到 GPS 坐标与地址")
    private GpsLocation checkinLocation;

    @Schema(description = "签退 GPS 坐标与地址")
    private GpsLocation checkoutLocation;

    @Schema(description = "签到照片预签名 URL")
    private String checkinPhotoUrl;

    @Schema(description = "工单状态")
    private String status;

    @Schema(description = "总工时（小时）")
    private BigDecimal totalDuration;

    @Schema(description = "照片数量")
    private Integer photoCount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "步骤完成数")
    private Integer completedStepCount;

    @Schema(description = "步骤总数")
    private Integer totalStepCount;

    @Schema(description = "施工步骤列表（详情场景）")
    private java.util.List<WorkOrderStepVO> steps;

    @Schema(description = "施工照片列表（详情场景）")
    private java.util.List<WorkOrderPhotoVO> photos;

    @Schema(description = "异常问题列表（详情场景）")
    private java.util.List<WorkOrderIssueVO> issues;
}
