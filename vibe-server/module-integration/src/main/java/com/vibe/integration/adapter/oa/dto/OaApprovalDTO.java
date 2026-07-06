package com.vibe.integration.adapter.oa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 审批 DTO
 *
 * <p>用于 {@code OaApprovalFeignClient} 联动 OA 系统审批流程
 * （项目立项/验收/割接审批可联动 OA 系统）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "OA 审批")
public class OaApprovalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "OA 流程实例 ID（启动时为空，启动后由 OA 侧返回）")
    private String oaProcessId;

    @Schema(description = "本系统业务流程编码（PROJECT_INIT/ACCEPTANCE/CUTOVER）")
    private String bizType;

    @Schema(description = "本系统业务流程实例 ID")
    private String bizProcessId;

    @Schema(description = "OA 流程定义编码")
    private String oaProcessCode;

    @Schema(description = "审批标题")
    private String title;

    @Schema(description = "审批摘要")
    private String summary;

    @Schema(description = "发起人 OA 用户 ID")
    private String starterOaUserId;

    @Schema(description = "发起人姓名")
    private String starterName;

    @Schema(description = "发起部门")
    private String starterDept;

    @Schema(description = "审批人列表（按顺序审批）")
    private List<OaApprover> approvers;

    @Schema(description = "抄送人 OA 用户 ID 列表")
    private List<String> ccOaUserIds;

    @Schema(description = "审批表单（JSON）")
    private String formData;

    @Schema(description = "附件 URL 列表")
    private List<String> attachments;

    @Schema(description = "优先级（NORMAL/URGENT/CRITICAL）")
    private String priority;

    @Schema(description = "发起时间")
    private LocalDateTime startedAt;

    @Schema(description = "流程状态（RUNNING/APPROVED/REJECTED/CANCELED）")
    private String status;

    @Schema(description = "审批完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "当前审批节点名称")
    private String currentNodeName;

    @Schema(description = "当前审批人 OA 用户 ID")
    private String currentApproverOaUserId;

    /**
     * OA 审批人。
     */
    @Data
    @Schema(description = "OA 审批人")
    public static class OaApprover implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "OA 用户 ID")
        private String oaUserId;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "部门")
        private String dept;

        @Schema(description = "审批顺序")
        private Integer order;
    }
}
