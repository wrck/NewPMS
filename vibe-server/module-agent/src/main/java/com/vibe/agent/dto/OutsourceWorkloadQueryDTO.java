package com.vibe.agent.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 代理商工作量分页查询 DTO
 *
 * <p>对齐前端 {@code WorkloadQueryParams}：page/size/agentCompanyId/projectId/status/beginTime/endTime。
 * 数据权限在 Service 层处理（AGENT_ADMIN 强制 agentCompanyId = tenantId），
 * 不使用 {@code @DataPermission}（工作量表无 agent_company_id 列，且 PM 需查看全部待确认工作量）。</p>
 *
 * <p>beginTime/endTime 使用 {@code String}（前端日期选择器输出 {@code yyyy-MM-dd} 字符串），
 * 避免日期字符串到 {@code LocalDateTime} 的绑定失败导致过滤条件静默失效。
 * XML 中按日期比较：beginTime 起当天 00:00:00，endTime 含当天（{@code < endTime + 1 天}）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作量分页查询")
public class OutsourceWorkloadQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "工作量状态 SUBMITTED/CONFIRMED/REJECTED")
    private String status;

    @Schema(description = "提交起始日期（yyyy-MM-dd）")
    private String beginTime;

    @Schema(description = "提交结束日期（yyyy-MM-dd，含当天）")
    private String endTime;
}

