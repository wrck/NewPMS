package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 代理商工作台 VO
 *
 * <p>聚合代理商 H5 端首页所需信息：</p>
 * <ul>
 *   <li>统计卡片：待接单 / 进行中 / 待审核 / 已超期 数量</li>
 *   <li>三类任务的最近列表（默认 top 5），H5 端可直接渲染工作台卡片</li>
 *   <li>未读消息数（可选，无消息表时为 0）</li>
 * </ul>
 *
 * <p>数据权限：仅返回当前代理商本公司任务（agent_company_id = tenantId）。</p>
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商工作台")
public class AgentWorkbenchVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "统计卡片")
    private Summary summary;

    @Schema(description = "待接单任务（top 5）")
    private List<OutsourceTaskVO> pendingTasks;

    @Schema(description = "进行中任务（top 5）")
    private List<OutsourceTaskVO> inProgressTasks;

    @Schema(description = "待审核任务（top 5）")
    private List<OutsourceTaskVO> submittedTasks;

    /**
     * 工作台统计卡片。
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "代理商任务统计")
    public static class Summary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "待接单数")
        private Integer pendingCount;

        @Schema(description = "进行中数")
        private Integer inProgressCount;

        @Schema(description = "待审核数")
        private Integer submittedCount;

        @Schema(description = "已超期数")
        private Integer overdueCount;

        @Schema(description = "未读消息数")
        private Integer unreadMessageCount;
    }
}
