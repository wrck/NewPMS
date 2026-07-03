package com.vibe.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 代理商工作量确认实体（outsource_workload）
 *
 * <p>对应 schema.sql 中的 outsource_workload 表，存储代理商提交的工作量
 * （人天/站点数/设备台数），由 PM 确认。status 取值 SUBMITTED/CONFIRMED/REJECTED。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outsource_workload")
@Schema(description = "代理商工作量")
public class OutsourceWorkloadEntity extends AgentBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 转包任务ID */
    @Schema(description = "转包任务ID")
    private Long outsourceTaskId;

    /** 人天 */
    @Schema(description = "人天")
    private BigDecimal manDays;

    /** 站点数 */
    @Schema(description = "站点数")
    private Integer siteCount;

    /** 设备台数 */
    @Schema(description = "设备台数")
    private Integer deviceCount;

    /** 提交人ID */
    @Schema(description = "提交人ID")
    private Long submittedBy;

    /** 确认人ID */
    @Schema(description = "确认人ID")
    private Long confirmedBy;

    /** 状态 SUBMITTED/CONFIRMED/REJECTED */
    @Schema(description = "状态")
    private String status;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
