package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目视图对象（列表/详情通用）
 *
 * @author vibe
 */
@Data
@Schema(description = "项目信息")
public class ProjectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称（关联查询）")
    private String customerName;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "执行模式")
    private String executeMode;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "当前阶段编码")
    private String currentPhase;

    @Schema(description = "项目经理ID")
    private Long pmId;

    @Schema(description = "项目经理姓名（关联查询）")
    private String pmName;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "实际开始日期")
    private LocalDate actualStart;

    @Schema(description = "实际结束日期")
    private LocalDate actualEnd;

    @Schema(description = "进度百分比")
    private Integer progressPct;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
