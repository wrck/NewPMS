package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.DeviceInstanceDTO;
import com.vibe.device.dto.DeviceInstanceQueryDTO;
import com.vibe.device.dto.DeviceStatusTransitionDTO;
import com.vibe.device.dto.export.DeviceInstanceExportDTO;
import com.vibe.device.service.DeviceInstanceService;
import com.vibe.device.vo.DeviceImportResultVO;
import com.vibe.device.vo.DeviceInstanceDetailVO;
import com.vibe.device.vo.DeviceInstanceVO;
import com.vibe.es.ElasticSearchService;
import com.vibe.es.EsQueryHelper;
import com.vibe.es.index.EsIndexConstant;
import com.vibe.es.index.VibeDeviceIndex;
import com.vibe.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.BeanUtils;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备实例 Controller
 *
 * <p>设备实例的录入、编辑、搜索、Excel 批量导入、状态流转。
 * DEVICE_ADMIN/SUPER_ADMIN 可增删改与状态流转，PM/ENGINEER 只读查询。</p>
 *
 * <p>列表查询受 @DataPermission 数据权限控制：PM 仅看自己负责项目下的设备，
 * 客户看自己关联项目的设备。</p>
 *
 * <p>列表查询支持 ES 检索：{@code useEs=true} 时走 ElasticSearch 全文检索，
 * ES 不可用或返回空时自动回退 MySQL。</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "设备实例", description = "录入、导入、状态机、搜索")
@RestController
@RequestMapping("/api/v1/devices/instances")
@RequiredArgsConstructor
public class DeviceInstanceController {

    private final DeviceInstanceService deviceInstanceService;
    private final ExcelUtils excelUtils;
    private final ElasticSearchService<VibeDeviceIndex> elasticSearchService;

    /** 单次导出最大行数 */
    private static final int EXPORT_MAX_ROWS = 10000;

    @Operation(summary = "分页查询设备实例（受数据权限控制，支持 ES 检索）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<PageResult<DeviceInstanceVO>> page(@ParameterObject DeviceInstanceQueryDTO query,
                                                       @RequestParam(required = false, defaultValue = "false") Boolean useEs) {
        if (Boolean.TRUE.equals(useEs)) {
            PageResult<DeviceInstanceVO> esResult = searchByEs(query);
            if (esResult != null) {
                return Result.success(esResult);
            }
            log.info("ES 检索不可用或无结果，回退 MySQL: keyword={}", query.getKeyword());
        }
        return Result.success(deviceInstanceService.page(query));
    }

    /**
     * 通过 ElasticSearch 检索设备实例（useEs=true 时调用）。
     *
     * @param query 查询条件
     * @return 检索结果（ES 不可用或异常时返回 null，触发 MySQL 回退）
     */
    private PageResult<DeviceInstanceVO> searchByEs(DeviceInstanceQueryDTO query) {
        try {
            String queryJson = EsQueryHelper.buildDeviceQuery(
                    query.getKeyword(), query.getStatus(), query.getProjectId());
            int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
            int size = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();
            int from = (page - 1) * size;
            List<VibeDeviceIndex> hits = elasticSearchService.search(
                    EsIndexConstant.INDEX_VIBE_DEVICE, queryJson, from, size, VibeDeviceIndex.class);
            if (hits.isEmpty()) {
                return null;
            }
            List<DeviceInstanceVO> records = new ArrayList<>(hits.size());
            for (VibeDeviceIndex idx : hits) {
                DeviceInstanceVO vo = new DeviceInstanceVO();
                vo.setId(idx.getId());
                vo.setSerialNumber(idx.getSn());
                vo.setMacAddress(idx.getMacAddress());
                vo.setModelName(idx.getModelName());
                vo.setProjectId(idx.getProjectId());
                vo.setProjectName(idx.getProjectName());
                vo.setStatus(idx.getStatus());
                vo.setWarehouseName(idx.getWarehouse());
                records.add(vo);
            }
            return PageResult.of(records, hits.size(), page, size);
        } catch (Exception e) {
            log.warn("ES 设备检索异常，将回退 MySQL: error={}", e.getMessage());
            return null;
        }
    }

    @Operation(summary = "设备详情（含状态轨迹与出入库历史）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/{id}")
    public Result<DeviceInstanceDetailVO> detail(@PathVariable Long id) {
        return Result.success(deviceInstanceService.getDetail(id));
    }

    @Operation(summary = "单条录入设备（SN 唯一校验，初始状态 IN_FACTORY）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INSTANCE, type = "CREATE", description = "录入设备")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DeviceInstanceDTO dto) {
        return Result.success(deviceInstanceService.create(dto));
    }

    @Operation(summary = "编辑设备信息")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INSTANCE, type = "UPDATE", description = "编辑设备")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeviceInstanceDTO dto) {
        dto.setId(id);
        deviceInstanceService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除设备")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INSTANCE, type = "DELETE", description = "删除设备")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceInstanceService.delete(id);
        return Result.success();
    }

    @Operation(summary = "设备状态流转（校验状态机合法性，记录状态变更日志）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_STATUS, type = "UPDATE", description = "设备状态流转")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> transition(@PathVariable Long id, @Valid @RequestBody DeviceStatusTransitionDTO dto) {
        deviceInstanceService.transition(id, dto);
        return Result.success();
    }

    @Operation(summary = "Excel 批量导入设备（导入前校验 SN 重复，重复行跳过并输出错误清单）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INSTANCE, type = "IMPORT", description = "批量导入设备")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping("/import")
    public Result<DeviceImportResultVO> importDevices(@RequestParam("file") MultipartFile file) {
        return Result.success(deviceInstanceService.importDevices(file));
    }

    @Operation(summary = "下载设备导入模板")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @GetMapping("/import-template")
    public void downloadTemplate(HttpServletResponse response) {
        excelUtils.download(response, "设备导入模板",
                deviceInstanceService.importRowClass(), "设备清单", Collections.emptyList());
    }

    @Operation(summary = "导出设备台账（Excel）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INSTANCE, type = "EXPORT", description = "导出设备台账")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/export")
    public void export(HttpServletResponse response, @ParameterObject DeviceInstanceQueryDTO query) throws IOException {
        query.setPage(1);
        query.setSize(EXPORT_MAX_ROWS);
        List<DeviceInstanceVO> records = deviceInstanceService.page(query).getRecords();
        List<DeviceInstanceExportDTO> data = records.stream().map(vo -> {
            DeviceInstanceExportDTO dto = new DeviceInstanceExportDTO();
            BeanUtils.copyProperties(vo, dto);
            return dto;
        }).collect(Collectors.toList());
        ExcelUtils.export(response, "设备台账", "设备", DeviceInstanceExportDTO.class, data);
    }
}
