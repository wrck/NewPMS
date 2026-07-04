package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户待办事项 VO
 *
 * <p>聚合客户需要处理的待办（待审批的割接方案、待签核的验收任务）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户待办事项")
public class CustomerTodoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "待办类型 CUTOVER_APPROVAL/ACCEPTANCE_SIGN", example = "CUTOVER_APPROVAL")
    private String type;

    @Schema(description = "业务ID（割接方案ID或验收任务ID）")
    private Long businessId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "业务标题")
    private String title;

    @Schema(description = "客户签核链接token")
    private String signToken;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
