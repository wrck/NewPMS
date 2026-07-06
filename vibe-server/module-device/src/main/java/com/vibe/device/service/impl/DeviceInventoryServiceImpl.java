package com.vibe.device.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.device.dto.DeviceInventoryActionDTO;
import com.vibe.device.entity.DeviceInstanceEntity;
import com.vibe.device.entity.DeviceInventoryLogEntity;
import com.vibe.device.entity.DeviceModelEntity;
import com.vibe.device.entity.WarehouseEntity;
import com.vibe.device.enums.DeviceStatus;
import com.vibe.device.enums.InventoryActionType;
import com.vibe.device.mapper.DeviceInstanceMapper;
import com.vibe.device.mapper.DeviceInventoryLogMapper;
import com.vibe.device.mapper.DeviceModelMapper;
import com.vibe.device.mapper.WarehouseMapper;
import com.vibe.device.service.DeviceInventoryService;
import com.vibe.device.vo.DeviceInventoryLogVO;
import com.vibe.device.vo.InventoryLedgerRow;
import com.vibe.device.vo.InventoryWarningVO;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.InventoryWarningEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备出入库与库存服务实现。
 *
 * <p>核心逻辑：</p>
 * <ul>
 *   <li><b>入库(IN)</b>：设备入仓库，更新 warehouse_id，状态置为 IN_FACTORY</li>
 *   <li><b>出库(OUT)</b>：设备出库到项目，更新 project_id，清空 warehouse_id</li>
 *   <li><b>退库(RETURN)</b>：设备从项目退回仓库，更新 warehouse_id，清空 project_id</li>
 *   <li><b>调拨(TRANSFER)</b>：设备在仓库间调拨，更新 warehouse_id</li>
 *   <li>每次操作均写入 device_inventory_log 流水</li>
 *   <li>库存台账：聚合各仓库各型号 IN_FACTORY 在库数量</li>
 *   <li>库存预警：解析 warehouse.safety_stock JSON，比对在库数量低于阈值时告警</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceInventoryServiceImpl implements DeviceInventoryService {

    private final DeviceInstanceMapper deviceInstanceMapper;
    private final DeviceInventoryLogMapper deviceInventoryLogMapper;
    private final DeviceModelMapper deviceModelMapper;
    private final WarehouseMapper warehouseMapper;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void action(DeviceInventoryActionDTO dto) {
        InventoryActionType actionType = InventoryActionType.parse(dto.getActionType());
        if (actionType == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "出入库操作类型非法: " + dto.getActionType());
        }
        DeviceInstanceEntity device = deviceInstanceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }

        switch (actionType) {
            case IN:
                handleIn(device, dto);
                break;
            case OUT:
                handleOut(device, dto);
                break;
            case RETURN:
                handleReturn(device, dto);
                break;
            case TRANSFER:
                handleTransfer(device, dto);
                break;
            default:
                throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的出入库操作类型");
        }

        deviceInstanceMapper.updateById(device);
        writeLog(device, dto, actionType);
    }

    @Override
    public List<DeviceInventoryLogVO> logList(Long warehouseId, Long projectId, String actionType) {
        return deviceInventoryLogMapper.selectLogList(warehouseId, projectId, actionType);
    }

    @Override
    public List<InventoryLedgerRow> ledger(Long warehouseId, Long modelId) {
        return deviceModelMapper.selectInventoryLedger(warehouseId, modelId);
    }

    @Override
    public List<InventoryWarningVO> warnings() {
        List<WarehouseEntity> warehouses = warehouseMapper.selectList(null);
        List<InventoryLedgerRow> ledgerRows = deviceModelMapper.selectInventoryLedger(null, null);
        List<DeviceModelEntity> models = deviceModelMapper.selectList(null);
        Map<Long, DeviceModelEntity> modelMap = new HashMap<>();
        for (DeviceModelEntity m : models) {
            modelMap.put(m.getId(), m);
        }

        // 按 warehouseId -> modelId -> inStockQty 建立索引
        Map<Long, Map<Long, Long>> stockIndex = new HashMap<>();
        for (InventoryLedgerRow row : ledgerRows) {
            if (row.getWarehouseId() == null) {
                continue;
            }
            stockIndex.computeIfAbsent(row.getWarehouseId(), k -> new HashMap<>())
                    .put(row.getModelId(), row.getInStockQty() == null ? 0L : row.getInStockQty());
        }

        List<InventoryWarningVO> warnings = new ArrayList<>();
        for (WarehouseEntity wh : warehouses) {
            Map<Long, Long> safetyMap = parseSafetyStock(wh.getSafetyStock());
            if (safetyMap.isEmpty()) {
                continue;
            }
            Map<Long, Long> whStock = stockIndex.getOrDefault(wh.getId(), new HashMap<>());
            for (Map.Entry<Long, Long> entry : safetyMap.entrySet()) {
                Long modelId = entry.getKey();
                long safetyQty = entry.getValue();
                long currentQty = whStock.getOrDefault(modelId, 0L);
                if (currentQty < safetyQty) {
                    InventoryWarningVO vo = new InventoryWarningVO();
                    vo.setWarehouseId(wh.getId());
                    vo.setWarehouseName(wh.getWarehouseName());
                    vo.setModelId(modelId);
                    DeviceModelEntity model = modelMap.get(modelId);
                    if (model != null) {
                        vo.setModelName(model.getModelName());
                        vo.setModelCode(model.getModelCode());
                    }
                    vo.setCurrentQty(currentQty);
                    vo.setSafetyQty(safetyQty);
                    vo.setGapQty(safetyQty - currentQty);
                    warnings.add(vo);

                    // 检测到低库存时发布库存预警领域事件
                    String level = (safetyQty > 0 && currentQty * 2 < safetyQty) ? "CRITICAL" : "LOW";
                    String modelName = vo.getModelName() == null ? "" : vo.getModelName();
                    String warehouseName = vo.getWarehouseName() == null ? "" : vo.getWarehouseName();
                    domainEventPublisher.publish(new InventoryWarningEvent(
                            modelId, modelName, wh.getId(), warehouseName,
                            (int) currentQty, (int) safetyQty, level));
                }
            }
        }
        return warnings;
    }

    // ============ 私有辅助方法 ============

    /**
     * 入库：设备进入目标仓库，状态置为 IN_FACTORY。
     */
    private void handleIn(DeviceInstanceEntity device, DeviceInventoryActionDTO dto) {
        if (dto.getToWarehouseId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "入库操作必须指定调入仓库ID");
        }
        validateWarehouseExist(dto.getToWarehouseId());
        device.setWarehouseId(dto.getToWarehouseId());
        device.setStatus(DeviceStatus.IN_FACTORY.name());
    }

    /**
     * 出库领用：设备出库到项目，清空仓库归属。
     */
    private void handleOut(DeviceInstanceEntity device, DeviceInventoryActionDTO dto) {
        if (dto.getToProjectId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "出库操作必须指定调入项目ID");
        }
        device.setProjectId(dto.getToProjectId());
        device.setWarehouseId(null);
    }

    /**
     * 退库归还：设备从项目退回仓库，清空项目归属，状态置为 IN_FACTORY。
     */
    private void handleReturn(DeviceInstanceEntity device, DeviceInventoryActionDTO dto) {
        if (dto.getToWarehouseId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "退库操作必须指定调入仓库ID");
        }
        validateWarehouseExist(dto.getToWarehouseId());
        device.setWarehouseId(dto.getToWarehouseId());
        device.setProjectId(null);
        device.setStatus(DeviceStatus.IN_FACTORY.name());
    }

    /**
     * 调拨：设备在仓库间调拨。
     */
    private void handleTransfer(DeviceInstanceEntity device, DeviceInventoryActionDTO dto) {
        if (dto.getFromWarehouseId() == null || dto.getToWarehouseId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "调拨操作必须指定调出与调入仓库ID");
        }
        if (!dto.getFromWarehouseId().equals(device.getWarehouseId())) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT,
                    "设备当前不在调出仓库: deviceId=" + device.getId()
                            + ", currentWarehouseId=" + device.getWarehouseId());
        }
        validateWarehouseExist(dto.getToWarehouseId());
        device.setWarehouseId(dto.getToWarehouseId());
    }

    /**
     * 校验仓库存在性。
     */
    private void validateWarehouseExist(Long warehouseId) {
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "仓库不存在: " + warehouseId);
        }
    }

    /**
     * 写入出入库流水。
     */
    private void writeLog(DeviceInstanceEntity device, DeviceInventoryActionDTO dto, InventoryActionType actionType) {
        DeviceInventoryLogEntity log = new DeviceInventoryLogEntity();
        log.setDeviceId(device.getId());
        log.setActionType(actionType.name());
        log.setFromWarehouseId(dto.getFromWarehouseId());
        log.setToWarehouseId(dto.getToWarehouseId());
        log.setFromProjectId(dto.getFromProjectId());
        log.setToProjectId(dto.getToProjectId());
        log.setOperatorId(UserContextHolder.getUserId());
        log.setQuantity(dto.getQuantity());
        log.setRemark(dto.getRemark());
        deviceInventoryLogMapper.insert(log);
    }

    /**
     * 解析仓库安全库存 JSON 配置。
     *
     * <p>JSON 格式：{"modelId1": safetyQty1, "modelId2": safetyQty2}</p>
     *
     * @return modelId -> 安全库存阈值 映射；解析失败或为空时返回空 Map
     */
    private Map<Long, Long> parseSafetyStock(String safetyStockJson) {
        if (safetyStockJson == null || safetyStockJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(safetyStockJson,
                    new TypeReference<Map<String, Object>>() {});
            Map<Long, Long> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                try {
                    Long modelId = Long.parseLong(entry.getKey());
                    long qty = ((Number) entry.getValue()).longValue();
                    result.put(modelId, qty);
                } catch (NumberFormatException e) {
                    log.warn("[库存预警] 安全库存配置 key 非法，已跳过: key={}, value={}",
                            entry.getKey(), entry.getValue());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[库存预警] 解析仓库安全库存 JSON 失败: json={}, err={}", safetyStockJson, e.getMessage());
            return new HashMap<>();
        }
    }
}
