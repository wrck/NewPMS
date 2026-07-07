package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理商工作量视图对象
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商工作量信息")
public class OutsourceWorkloadVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工作量记录ID")
    private Long id;

    @Schema(description = "转包任务ID")
    private Long taskId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商公司名称")
    private String agentCompanyName;

    @Schema(description = "人天")
    private BigDecimal manDays;

    @Schema(description = "站点数")
    private Integer siteCount;

    @Schema(description = "设备台数")
    private Integer deviceCount;

    @Schema(description = "差旅天数")
    private Integer travelDays;

    @Schema(description = "其他费用")
    private BigDecimal otherCost;

    @Schema(description = "结算总额")
    private BigDecimal totalAmount;

    @Schema(description = "提交人ID")
    private Long submittedBy;

    @Schema(description = "确认人ID")
    private Long confirmedBy;

    @Schema(description = "确认人姓名")
    private String confirmByName;

    @Schema(description = "状态 PENDING/CONFIRMED/APPROVED/INVOICED/PAID")
    private String status;

    @Schema(description = "确认时间")
    private LocalDateTime confirmAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
