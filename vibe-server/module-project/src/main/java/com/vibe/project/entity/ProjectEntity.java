package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 项目实体（project，含乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
@Schema(description = "项目")
public class ProjectEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目编号 PRJ-YYYYMM-XXX")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "项目类型 新建/扩容/改造/替换/安全")
    private String projectType;

    @Schema(description = "产品线 路由/交换/无线/安全/数据中心")
    private String productLine;

    @Schema(description = "执行模式 SELF/AGENT/MIXED")
    private String executeMode;

    @Schema(description = "优先级 P0/P1/P2/P3")
    private String priority;

    @Schema(description = "项目状态 INIT/PLAN/EXECUTE/ACCEPT/CLOSE/ARCHIVED/ON_HOLD/CANCELLED")
    private String status;

    @Schema(description = "当前阶段编码")
    private String currentPhase;

    @Schema(description = "项目经理ID")
    private Long pmId;

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

    @Schema(description = "进度百分比 0-100")
    private Integer progressPct;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "备注")
    private String remark;
}
