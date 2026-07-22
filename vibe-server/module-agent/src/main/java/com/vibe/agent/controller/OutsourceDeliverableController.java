package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.DeliverableReviewDTO;
import com.vibe.agent.dto.OutsourceDeliverableDTO;
import com.vibe.agent.dto.export.OutsourceDeliverableExportDTO;
import com.vibe.agent.service.OutsourceDeliverableService;
import com.vibe.agent.vo.OutsourceDeliverableVO;
import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代理商交付物管理 Controller
 *
 * <p>路径：{@code /api/v1/outsource-tasks/{taskId}/deliverables}</p>
 *
 * <p><b>权限分工：</b></p>
 * <ul>
 *   <li>AGENT_ADMIN / AGENT_ENGINEER：提交交付物（移动端/H5 调用）</li>
 *   <li>PM / SUPER_ADMIN：审核交付物（通过/退回）</li>
 * </ul>
 *
 * <p><b>必传校验：</b>施工照片≥3张、测试记录必传、签收单必传。
 * 校验在 DTO（@NotEmpty）+ Service（数量校验）双重保障。</p>
 *
 * @author vibe
 */
@Tag(name = "代理商交付物管理", description = "交付物提交/列表/审核")
@RestController
@RequestMapping("/api/v1/outsource-tasks/{taskId}/deliverables")
@RequiredArgsConstructor
public class OutsourceDeliverableController {

    private final OutsourceDeliverableService deliverableService;

    @Operation(summary = "代理商提交交付物（施工照片≥3张 + 测试记录 + 签收单必传）")
    @OperationLog(module = AgentConstant.MODULE_DELIVERABLE, type = "INSERT", description = "代理商提交交付物")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @PostMapping
    public Result<Integer> submit(@PathVariable Long taskId,
                                  @Valid @RequestBody OutsourceDeliverableDTO dto) {
        dto.setOutsourceTaskId(taskId);
        return Result.success(deliverableService.submit(dto));
    }

    @Operation(summary = "查询交付物列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping
    public Result<List<OutsourceDeliverableVO>> list(@PathVariable Long taskId) {
        return Result.success(deliverableService.listByTaskId(taskId));
    }

    @Operation(summary = "删除交付物（AGENT_ADMIN 仅删本公司任务下交付物）")
    @OperationLog(module = AgentConstant.MODULE_DELIVERABLE, type = "DELETE", description = "删除交付物")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PM','AGENT_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long taskId, @PathVariable Long id) {
        deliverableService.delete(taskId, id);
        return Result.success();
    }

    @Operation(summary = "PM 审核交付物（通过→CONFIRMED / 退回→RETURNED）")
    @OperationLog(module = AgentConstant.MODULE_DELIVERABLE, type = "UPDATE", description = "PM 审核交付物")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/review")
    public Result<Void> review(@PathVariable Long taskId,
                               @Valid @RequestBody DeliverableReviewDTO dto) {
        deliverableService.review(taskId, dto);
        return Result.success();
    }

    @Operation(summary = "按类型统计交付物数量")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping("/count-by-type")
    public Result<Map<String, Integer>> countByType(@PathVariable Long taskId) {
        return Result.success(deliverableService.countByType(taskId));
    }

    @Operation(summary = "导出交付物列表（Excel）")
    @OperationLog(module = AgentConstant.MODULE_DELIVERABLE, type = "EXPORT", description = "导出交付物列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @GetMapping("/export")
    public void export(@PathVariable Long taskId, HttpServletResponse response) throws IOException {
        List<OutsourceDeliverableVO> records = deliverableService.listByTaskId(taskId);
        List<OutsourceDeliverableExportDTO> data = records.stream().map(vo -> {
            OutsourceDeliverableExportDTO dto = new OutsourceDeliverableExportDTO();
            BeanUtils.copyProperties(vo, dto);
            return dto;
        }).collect(Collectors.toList());
        ExcelUtils.export(response, "交付物列表", "交付物", OutsourceDeliverableExportDTO.class, data);
    }
}
