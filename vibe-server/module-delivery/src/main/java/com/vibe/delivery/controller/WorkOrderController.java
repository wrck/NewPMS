package com.vibe.delivery.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.delivery.dto.WorkOrderCheckinDTO;
import com.vibe.delivery.dto.WorkOrderCheckoutDTO;
import com.vibe.delivery.dto.WorkOrderConfirmDTO;
import com.vibe.delivery.dto.WorkOrderCreateDTO;
import com.vibe.delivery.dto.WorkOrderQueryDTO;
import com.vibe.delivery.service.WorkOrderService;
import com.vibe.delivery.vo.WorkOrderVO;
import com.vibe.es.ElasticSearchService;
import com.vibe.es.EsQueryHelper;
import com.vibe.es.index.EsIndexConstant;
import com.vibe.es.index.VibeWorkOrderIndex;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 工单 Controller（移动端现场作业）
 *
 * <p>路径：{@code /api/v1/work-orders}</p>
 *
 * <p>权限：</p>
 * <ul>
 *   <li>创建工单：PM / 调度员 / 管理员</li>
 *   <li>签到/签退/标记完成：ENGINEER（数据权限仅看自己的工单）</li>
 *   <li>PM 确认：PM</li>
 * </ul>
 *
 * <p>列表查询支持 ES 检索：{@code useEs=true} 时走 ElasticSearch 全文检索，
 * ES 不可用或返回空时自动回退 MySQL。</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "工单管理", description = "现场作业工单：创建/签到/签退/完成确认")
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final ElasticSearchService<VibeWorkOrderIndex> elasticSearchService;

    @Operation(summary = "创建工单")
    @OperationLog(module = "交付管理", type = "INSERT", description = "创建工单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WorkOrderCreateDTO dto) {
        return Result.success(workOrderService.createWorkOrder(dto));
    }

    @Operation(summary = "分页查询工单列表（支持 ES 检索）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER')")
    @GetMapping
    public Result<PageResult<WorkOrderVO>> page(@ParameterObject WorkOrderQueryDTO query,
                                                  @RequestParam(required = false, defaultValue = "false") Boolean useEs) {
        if (Boolean.TRUE.equals(useEs)) {
            PageResult<WorkOrderVO> esResult = searchByEs(query);
            if (esResult != null) {
                return Result.success(esResult);
            }
            log.info("ES 检索不可用或无结果，回退 MySQL: keyword={}", query.getKeyword());
        }
        return Result.success(workOrderService.page(query));
    }

    /**
     * 通过 ElasticSearch 检索工单（useEs=true 时调用）。
     *
     * @param query 查询条件
     * @return 检索结果（ES 不可用或异常时返回 null，触发 MySQL 回退）
     */
    private PageResult<WorkOrderVO> searchByEs(WorkOrderQueryDTO query) {
        try {
            String queryJson = EsQueryHelper.buildWorkOrderQuery(
                    query.getKeyword(), query.getStatus(), query.getProjectId(), query.getEngineerId());
            int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
            int size = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();
            int from = (page - 1) * size;
            List<VibeWorkOrderIndex> hits = elasticSearchService.search(
                    EsIndexConstant.INDEX_VIBE_WORK_ORDER, queryJson, from, size, VibeWorkOrderIndex.class);
            if (hits.isEmpty()) {
                return null;
            }
            List<WorkOrderVO> records = new ArrayList<>(hits.size());
            for (VibeWorkOrderIndex idx : hits) {
                WorkOrderVO vo = new WorkOrderVO();
                vo.setId(idx.getId());
                vo.setTaskName(idx.getTaskName());
                vo.setProjectId(idx.getProjectId());
                vo.setProjectName(idx.getProjectName());
                vo.setEngineerId(idx.getEngineerId());
                vo.setEngineerName(idx.getEngineerName());
                vo.setStatus(idx.getStatus());
                records.add(vo);
            }
            return PageResult.of(records, hits.size(), page, size);
        } catch (Exception e) {
            log.warn("ES 工单检索异常，将回退 MySQL: error={}", e.getMessage());
            return null;
        }
    }

    @Operation(summary = "工单详情（含步骤/照片/异常）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER')")
    @GetMapping("/{id}")
    public Result<WorkOrderVO> detail(@PathVariable Long id) {
        return Result.success(workOrderService.getDetail(id));
    }

    @Operation(summary = "移动端-我的工单（按状态筛选）")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER')")
    @GetMapping("/me")
    public Result<List<WorkOrderVO>> myWorkOrders(@RequestParam(required = false) String status) {
        return Result.success(workOrderService.listMyWorkOrders(status));
    }

    @Operation(summary = "现场签到（GPS 定位校验+拍照防作弊）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "工单签到", saveRequest = false)
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping(value = "/{id}/checkin", consumes = "multipart/form-data")
    public Result<WorkOrderVO> checkin(@PathVariable Long id,
                                       @Valid WorkOrderCheckinDTO dto,
                                       @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return Result.success(workOrderService.checkin(id, dto, photo));
    }

    @Operation(summary = "现场签退")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "工单签退", saveRequest = false)
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping(value = "/{id}/checkout", consumes = "multipart/form-data")
    public Result<WorkOrderVO> checkout(@PathVariable Long id,
                                        @Valid WorkOrderCheckoutDTO dto,
                                        @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return Result.success(workOrderService.checkout(id, dto, photo));
    }

    @Operation(summary = "工程师标记完成（要求所有步骤完成+已签退）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "工程师标记工单完成")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping("/{id}/complete")
    public Result<WorkOrderVO> complete(@PathVariable Long id,
                                        @RequestParam(required = false) String remark) {
        return Result.success(workOrderService.engineerComplete(id, remark));
    }

    @Operation(summary = "PM 确认完成（自动推进项目任务进度）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "PM 确认工单完成")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/confirm")
    public Result<WorkOrderVO> confirm(@PathVariable Long id,
                                       @RequestBody(required = false) WorkOrderConfirmDTO dto) {
        return Result.success(workOrderService.pmConfirm(id, dto));
    }
}
