package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.AgentCompanyDTO;
import com.vibe.agent.dto.AgentCompanyQueryDTO;
import com.vibe.agent.service.AgentCompanyService;
import com.vibe.agent.vo.AgentCompanyVO;
import com.vibe.agent.vo.AgentRankingVO;
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
 * 代理商公司管理 Controller
 *
 * <p>路径：{@code /api/v1/agent-companies}</p>
 *
 * <p>权限：</p>
 * <ul>
 *   <li>SUPER_ADMIN / DIRECTOR / PM：全部操作</li>
 *   <li>AGENT_ADMIN：仅查看本公司档案（数据权限拦截器自动隔离）</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "代理商档案管理", description = "代理商公司 CRUD、状态管理、排名")
@RestController
@RequestMapping("/api/v1/agent-companies")
@RequiredArgsConstructor
public class AgentCompanyController {

    private final AgentCompanyService agentCompanyService;

    @Operation(summary = "分页查询代理商公司")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping
    public Result<PageResult<AgentCompanyVO>> page(@ParameterObject AgentCompanyQueryDTO query) {
        return Result.success(agentCompanyService.page(query));
    }

    @Operation(summary = "代理商公司详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/{id}")
    public Result<AgentCompanyVO> detail(@PathVariable Long id) {
        return Result.success(agentCompanyService.getDetail(id));
    }

    @Operation(summary = "新增代理商公司")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "INSERT", description = "新增代理商公司")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AgentCompanyDTO dto) {
        return Result.success(agentCompanyService.create(dto));
    }

    @Operation(summary = "编辑代理商公司")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "UPDATE", description = "编辑代理商公司")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AgentCompanyDTO dto) {
        dto.setId(id);
        agentCompanyService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除代理商公司")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "DELETE", description = "删除代理商公司")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentCompanyService.delete(id);
        return Result.success();
    }

    @Operation(summary = "变更代理商合作状态")
    @OperationLog(module = AgentConstant.MODULE_AGENT, type = "UPDATE", description = "变更代理商合作状态")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        agentCompanyService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "代理商排名（按综合评分降序）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @GetMapping("/ranking")
    public Result<List<AgentRankingVO>> ranking(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(agentCompanyService.ranking(limit));
    }

    @Operation(summary = "按区域查询可用代理商（任务分配推荐）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @GetMapping("/by-region")
    public Result<List<AgentCompanyVO>> listByRegion(@RequestParam String region) {
        return Result.success(agentCompanyService.listByRegion(region));
    }
}
