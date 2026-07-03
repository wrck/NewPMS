package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.AgentEngineerDTO;
import com.vibe.agent.dto.AgentEngineerQueryDTO;
import com.vibe.agent.service.AgentEngineerService;
import com.vibe.agent.vo.AgentEngineerVO;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代理商工程师管理 Controller
 *
 * <p>路径：{@code /api/v1/agent-companies/{companyId}/engineers}</p>
 *
 * <p>权限：</p>
 * <ul>
 *   <li>SUPER_ADMIN / DIRECTOR / PM：全部操作</li>
 *   <li>AGENT_ADMIN：仅管理本公司工程师（数据权限拦截器自动隔离）</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "代理商工程师管理", description = "工程师 CRUD、启用停用、项目历史、质量评分")
@RestController
@RequestMapping("/api/v1/agent-companies/{companyId}/engineers")
@RequiredArgsConstructor
public class AgentEngineerController {

    private final AgentEngineerService agentEngineerService;

    @Operation(summary = "分页查询代理商工程师")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping
    public Result<PageResult<AgentEngineerVO>> page(@PathVariable Long companyId,
                                                     @ParameterObject AgentEngineerQueryDTO query) {
        query.setAgentCompanyId(companyId);
        return Result.success(agentEngineerService.page(query));
    }

    @Operation(summary = "按公司查询工程师列表（不分页）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping("/all")
    public Result<List<AgentEngineerVO>> listByCompanyId(@PathVariable Long companyId) {
        return Result.success(agentEngineerService.listByCompanyId(companyId));
    }

    @Operation(summary = "工程师详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/{engineerId}")
    public Result<AgentEngineerVO> detail(@PathVariable Long companyId,
                                          @PathVariable Long engineerId) {
        return Result.success(agentEngineerService.getDetail(engineerId));
    }

    @Operation(summary = "新增代理商工程师")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "INSERT", description = "新增代理商工程师")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','AGENT_ADMIN')")
    @PostMapping
    public Result<Long> create(@PathVariable Long companyId,
                               @Valid @RequestBody AgentEngineerDTO dto) {
        dto.setAgentCompanyId(companyId);
        return Result.success(agentEngineerService.create(dto));
    }

    @Operation(summary = "编辑代理商工程师")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "UPDATE", description = "编辑代理商工程师")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','AGENT_ADMIN')")
    @PutMapping("/{engineerId}")
    public Result<Void> update(@PathVariable Long companyId,
                               @PathVariable Long engineerId,
                               @Valid @RequestBody AgentEngineerDTO dto) {
        dto.setId(engineerId);
        dto.setAgentCompanyId(companyId);
        agentEngineerService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除代理商工程师")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "DELETE", description = "删除代理商工程师")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','AGENT_ADMIN')")
    @DeleteMapping("/{engineerId}")
    public Result<Void> delete(@PathVariable Long companyId,
                               @PathVariable Long engineerId) {
        agentEngineerService.delete(engineerId);
        return Result.success();
    }

    @Operation(summary = "启用/停用工程师")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "UPDATE", description = "变更工程师状态")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','AGENT_ADMIN')")
    @PutMapping("/{engineerId}/status")
    public Result<Void> changeStatus(@PathVariable Long companyId,
                                     @PathVariable Long engineerId,
                                     @RequestParam String status) {
        agentEngineerService.changeStatus(engineerId, status);
        return Result.success();
    }

    @Operation(summary = "工程师项目历史（参与过的转包任务）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/{engineerId}/task-history")
    public Result<List<OutsourceTaskVO>> taskHistory(@PathVariable Long companyId,
                                                     @PathVariable Long engineerId) {
        return Result.success(agentEngineerService.getTaskHistory(engineerId));
    }
}
