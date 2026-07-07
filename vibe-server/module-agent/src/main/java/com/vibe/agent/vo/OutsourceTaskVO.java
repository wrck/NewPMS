package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转包任务视图对象
 *
 * <p><b>数据脱敏说明（代理商角色屏蔽敏感字段）：</b></p>
 * <p>本 VO 含从 project 表 JOIN 出的客户/合同/成本字段（customerName / contractAmount /
 * costAmount / projectCode）。通过 {@code @JsonInclude(NON_NULL)} + 全局 Jackson
 * {@code default-property-inclusion: non_null} 配置，对代理商角色（AGENT_ADMIN /
 * AGENT_ENGINEER）不填充这些字段（保持 null），从而自动从 JSON 响应中剔除。</p>
 *
 * <p>Service 层在返回 VO 前会调用 {@link #desensitizeForAgent()} 方法，
 * 将代理商角色不应看到的字段置为 null。</p>
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "转包任务信息")
public class OutsourceTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务编号")
    private String taskCode;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联项目任务ID")
    private Long taskId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "任务范围与要求")
    private String taskScope;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "提交次数")
    private Integer submitCount;

    @Schema(description = "确认人ID")
    private Long confirmedBy;

    @Schema(description = "确认人姓名")
    private String confirmedByName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    @Schema(description = "退回原因")
    private String rejectReason;

    @Schema(description = "附件列表（JSON 字符串）")
    private String attachments;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人ID")
    private Long updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /* ============ 以下为 JOIN 关联字段 ============ */

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目任务名称")
    private String taskName;

    @Schema(description = "代理商公司名称")
    private String agentCompanyName;

    @Schema(description = "代理商工程师姓名")
    private String agentEngineerName;

    /* ============ 以下为敏感字段（对代理商角色脱敏） ============ */

    @Schema(description = "项目编号（敏感，代理商不可见）")
    private String projectCode;

    @Schema(description = "客户名称（敏感，代理商不可见）")
    private String customerName;

    @Schema(description = "合同金额（敏感，代理商不可见）")
    private BigDecimal contractAmount;

    @Schema(description = "成本金额（敏感，代理商不可见）")
    private BigDecimal costAmount;

    /**
     * 对代理商角色脱敏：清除客户/合同/成本等敏感字段。
     *
     * <p>Service 层在判断当前用户为 AGENT_ADMIN / AGENT_ENGINEER 时调用此方法，
     * 配合全局 Jackson {@code non_null} 序列化策略，敏感字段不会出现在 JSON 响应中。</p>
     */
    public void desensitizeForAgent() {
        this.projectCode = null;
        this.customerName = null;
        this.contractAmount = null;
        this.costAmount = null;
    }
}
