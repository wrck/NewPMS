package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.AgentScoreDTO;
import com.vibe.agent.service.AgentScoreService;
import com.vibe.agent.vo.AgentRankingVO;
import com.vibe.agent.vo.AgentScoreLogVO;
import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代理商评分管理 Controller
 *
 * <p>路径：{@code /api/v1/agent-companies/{companyId}/scores}</p>
 *
 * <p><b>权限分工：</b></p>
 * <ul>
 *   <li>PM / SUPER_ADMIN：对代理商打分（多维：及时性/质量/沟通/问题处理）</li>
 *   <li>AGENT_ADMIN：查看本公司评分历史（数据权限拦截器自动隔离）</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "代理商评分管理", description = "PM 打分/评分历史/代理商排名")
@RestController
@RequestMapping("/api/v1/agent-companies/{companyId}/scores")
@RequiredArgsConstructor
public class AgentScoreController {

    private final AgentScoreService agentScoreService;

    @Operation(summary = "PM 对代理商打分（及时性/质量/沟通/问题处理，加权平均更新综合评分）")
    @OperationLog(module = AgentConstant.MODULE_SCORE, type = "INSERT", description = "PM 对代理商打分")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> score(@PathVariable Long companyId,
                              @Valid @RequestBody AgentScoreDTO dto) {
        dto.setAgentCompanyId(companyId);
        return Result.success(agentScoreService.score(dto));
    }

    @Operation(summary = "查询代理商评分历史（数据权限：AGENT_ADMIN 仅看本公司）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping
    public Result<List<AgentScoreLogVO>> history(@PathVariable Long companyId) {
        return Result.success(agentScoreService.getScoreHistory(companyId));
    }

    @Operation(summary = "按任务查询评分记录")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping("/by-task/{taskId}")
    public Result<AgentScoreLogVO> getByTaskId(@PathVariable Long companyId,
                                               @PathVariable Long taskId) {
        return Result.success(agentScoreService.getScoreByTaskId(taskId));
    }

    @Operation(summary = "代理商排名（影响分配优先级）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @GetMapping("/ranking")
    public Result<List<AgentRankingVO>> ranking(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(agentScoreService.ranking(limit));
    }
}
