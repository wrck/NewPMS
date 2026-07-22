package com.vibe.agent.controller;

import com.vibe.agent.dto.OutsourceWorkloadQueryDTO;
import com.vibe.agent.service.OutsourceWorkloadService;
import com.vibe.agent.vo.OutsourceWorkloadVO;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代理商工作量全局查询 Controller
 *
 * <p>路径：{@code /api/v1/outsource-workloads}</p>
 *
 * <p>与 {@link OutsourceWorkloadController} 区分：后者为任务维度的提交/确认/驳回端点
 * （路径含 {@code {taskId}}），本控制器提供跨任务的全局工作量分页查询，
 * 供 PM 结算看板与代理商对账使用。</p>
 *
 * <p><b>数据权限：</b>AGENT_ADMIN 仅看本公司工作量（Service 层强制 agentCompanyId = tenantId），
 * PM/SUPER_ADMIN/DIRECTOR 查看全部。</p>
 *
 * @author vibe
 */
@Tag(name = "代理商工作量全局查询", description = "工作量跨任务分页查询")
@RestController
@RequestMapping("/api/v1/outsource-workloads")
@RequiredArgsConstructor
public class OutsourceWorkloadGlobalController {

    private final OutsourceWorkloadService workloadService;

    @Operation(summary = "全局分页查询工作量（含项目名/代理商名/确认人名）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping
    public Result<PageResult<OutsourceWorkloadVO>> page(@ParameterObject OutsourceWorkloadQueryDTO query) {
        return Result.success(workloadService.page(query));
    }
}
