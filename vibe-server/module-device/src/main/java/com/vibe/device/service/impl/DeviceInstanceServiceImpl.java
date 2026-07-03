package com.vibe.device.service.impl;

import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.device.bo.DeviceImportRow;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.DeviceInstanceDTO;
import com.vibe.device.dto.DeviceInstanceQueryDTO;
import com.vibe.device.dto.DeviceStatusTransitionDTO;
import com.vibe.device.entity.DeviceInstanceEntity;
import com.vibe.device.entity.DeviceModelEntity;
import com.vibe.device.entity.DeviceStatusLogEntity;
import com.vibe.device.entity.WarehouseEntity;
import com.vibe.device.enums.DeviceStatus;
import com.vibe.device.mapper.DeviceInstanceMapper;
import com.vibe.device.mapper.DeviceInventoryLogMapper;
import com.vibe.device.mapper.DeviceModelMapper;
import com.vibe.device.mapper.DeviceStatusLogMapper;
import com.vibe.device.mapper.WarehouseMapper;
import com.vibe.device.service.DeviceInstanceService;
import com.vibe.device.vo.DeviceImportResultVO;
import com.vibe.device.vo.DeviceInstanceDetailVO;
import com.vibe.device.vo.DeviceInstanceVO;
import com.vibe.device.vo.DeviceInventoryLogVO;
import com.vibe.device.vo.DeviceStatusLogVO;
import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import com.vibe.system.notification.producer.NotificationProducer;
import com.vibe.utils.ExcelUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 设备实例服务实现。
 *
 * <p>核心能力：</p>
 * <ul>
 *   <li>SN 唯一校验、单条录入、详情、编辑、搜索</li>
 *   <li>Excel 批量导入（EasyExcel，导入前校验 SN 重复，重复行跳过并输出错误清单）</li>
 *   <li>设备状态机管理（{@link DeviceStatus#canTransition}）</li>
 *   <li>状态变更记录到 device_status_log</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceInstanceServiceImpl implements DeviceInstanceService {

    private final DeviceInstanceMapper deviceInstanceMapper;
    private final DeviceModelMapper deviceModelMapper;
    private final WarehouseMapper warehouseMapper;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final DeviceInventoryLogMapper deviceInventoryLogMapper;
    private final ExcelUtils excelUtils;
    private final NotificationProducer notificationProducer;

    @Override
    public PageResult<DeviceInstanceVO> page(DeviceInstanceQueryDTO query) {
        IPage<DeviceInstanceVO> p = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<DeviceInstanceVO> result = deviceInstanceMapper.selectInstancePage(p, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public DeviceInstanceDetailVO getDetail(Long id) {
        DeviceInstanceVO device = deviceInstanceMapper.selectVoById(id);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        DeviceInstanceDetailVO detail = new DeviceInstanceDetailVO();
        detail.setDevice(device);
        detail.setStatusTrail(deviceInstanceMapper.selectStatusTrail(id));
        detail.setInventoryLogs(deviceInventoryLogMapper.selectLogListByDevice(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeviceInstanceDTO dto) {
        checkSnUnique(dto.getSerialNumber(), null);
        validateModelExist(dto.getModelId());
        DeviceInstanceEntity entity = new DeviceInstanceEntity();
        copyDtoToEntity(dto, entity);
        entity.setStatus(DeviceConstant.DEFAULT_DEVICE_STATUS);
        entity.setConfigVersion(0);
        deviceInstanceMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeviceInstanceDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "设备ID不能为空");
        }
        DeviceInstanceEntity exist = deviceInstanceMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        if (dto.getSerialNumber() != null && !dto.getSerialNumber().equals(exist.getSerialNumber())) {
            checkSnUnique(dto.getSerialNumber(), dto.getId());
        }
        if (dto.getModelId() != null && !dto.getModelId().equals(exist.getModelId())) {
            validateModelExist(dto.getModelId());
        }
        copyDtoToEntity(dto, exist);
        deviceInstanceMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (deviceInstanceMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        deviceInstanceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceImportResultVO importDevices(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "导入文件不能为空");
        }
        DeviceImportResultVO result = new DeviceImportResultVO();
        // 预加载型号编码 → 型号ID 与仓库编码 → 仓库ID，避免逐行查询
        Map<String, Long> modelCodeToId = loadModelCodeMap();
        Map<String, Long> warehouseCodeToId = loadWarehouseCodeMap();
        // 预加载已存在 SN 集合（用于导入前校验 SN 重复）
        Map<String, Long> existSnMap = loadExistSnMap();

        // 同步收集解析后的行（EasyExcel ReadListener 不可为 null，使用同步收集实现）
        List<DeviceImportRow> rows = readRowsSafely(file);

        // 文件内 SN 去重跟踪
        Map<String, Integer> fileSnRow = new HashMap<>();
        int rowIndex = 1; // 表头占第 1 行
        for (DeviceImportRow row : rows) {
            rowIndex++;
            result.setTotalRows(result.getTotalRows() + 1);
            String sn = trim(row.getSerialNumber());
            String reason = validateRow(row, sn, modelCodeToId, warehouseCodeToId);
            if (reason != null) {
                result.getErrors().add(new DeviceImportResultVO.ErrorItem(rowIndex, sn, reason));
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            // 文件内重复 SN
            if (fileSnRow.containsKey(sn)) {
                result.getErrors().add(new DeviceImportResultVO.ErrorItem(rowIndex, sn,
                        "文件内 SN 重复（首次出现于第 " + fileSnRow.get(sn) + " 行）"));
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            // 数据库已存在 SN
            if (existSnMap.containsKey(sn)) {
                result.getErrors().add(new DeviceImportResultVO.ErrorItem(rowIndex, sn, "SN 已存在，跳过导入"));
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            fileSnRow.put(sn, rowIndex);

            // 落库
            DeviceInstanceEntity entity = new DeviceInstanceEntity();
            entity.setSerialNumber(sn);
            entity.setMacAddress(trim(row.getMacAddress()));
            entity.setModelId(modelCodeToId.get(trim(row.getModelCode())));
            entity.setFirmwareVersion(trim(row.getFirmwareVersion()));
            Long whId = warehouseCodeToId.get(trim(row.getWarehouseCode()));
            entity.setWarehouseId(whId);
            entity.setRemark(trim(row.getRemark()));
            entity.setStatus(DeviceConstant.DEFAULT_DEVICE_STATUS);
            entity.setConfigVersion(0);
            deviceInstanceMapper.insert(entity);
            existSnMap.put(sn, entity.getId());
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long deviceId, DeviceStatusTransitionDTO dto) {
        DeviceInstanceEntity device = deviceInstanceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND, "设备不存在");
        }
        DeviceStatus from = DeviceStatus.parse(device.getStatus());
        DeviceStatus to = DeviceStatus.parse(dto.getToStatus());
        if (from == null) {
            throw new BusinessException(ResultCode.STATE_NOT_ALLOWED, "设备当前状态非法: " + device.getStatus());
        }
        if (to == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "目标状态非法: " + dto.getToStatus());
        }
        // 乐观锁版本号校验
        if (dto.getVersion() != null && !Objects.equals(dto.getVersion(), device.getVersion())) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "设备数据已被他人修改，请刷新后重试");
        }
        if (!DeviceStatus.canTransition(from, to)) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "设备状态流转非法：" + from.name() + " → " + to.name());
        }
        // 业务联动：出库发运需分配项目；安装完成记录安装人/日期；上线记录入网日期
        applyTransitionSideEffect(device, from, to, dto);

        device.setStatus(to.name());
        // 乐观锁：updateById 会自动带上 version 条件并 +1
        int affected = deviceInstanceMapper.updateById(device);
        if (affected == 0) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "设备状态更新失败，数据可能已被他人修改");
        }

        // 记录状态变更日志
        DeviceStatusLogEntity log = new DeviceStatusLogEntity();
        log.setDeviceId(deviceId);
        log.setFromStatus(from.name());
        log.setToStatus(to.name());
        log.setOperatorId(UserContextHolder.getUserId());
        log.setRemark(dto.getRemark());
        deviceStatusLogMapper.insert(log);

        // 通知事件投递：SHIPPED→RECEIVED 触发 DEVICE_ARRIVED；流转到异常状态触发 DEVICE_ABNORMAL
        sendDeviceTransitionNotification(device, from, to, dto);
    }

    /**
     * 根据设备状态流转投递通知事件。
     * - SHIPPED → RECEIVED：DEVICE_ARRIVED（设备到货通知）
     * - 流转到 DAMAGED/LOST/REPAIR：DEVICE_ABNORMAL（设备状态异常）
     */
    private void sendDeviceTransitionNotification(DeviceInstanceEntity device,
                                                  DeviceStatus from, DeviceStatus to,
                                                  DeviceStatusTransitionDTO dto) {
        String modelName = "";
        if (device.getModelId() != null) {
            DeviceModelEntity model = deviceModelMapper.selectById(device.getModelId());
            if (model != null && model.getModelName() != null) {
                modelName = model.getModelName();
            }
        }
        if (from == DeviceStatus.SHIPPED && to == DeviceStatus.RECEIVED) {
            // DEVICE_ARRIVED
            Map<String, String> variables = new HashMap<>(4);
            variables.put("modelName", modelName);
            variables.put("projectName", "");
            variables.put("quantity", "1");
            NotificationEvent event = NotificationEvent.of(
                    NotificationConstant.EVENT_DEVICE_ARRIVED,
                    NotificationConstant.RECIPIENT_INTERNAL,
                    dto.getInstallerId() == null ? java.util.Collections.emptyList()
                            : java.util.Collections.singletonList(dto.getInstallerId()),
                    variables, device.getId(), NotificationConstant.BIZ_DEVICE);
            notificationProducer.send(event);
        } else if (to == DeviceStatus.DAMAGED || to == DeviceStatus.LOST || to == DeviceStatus.REPAIR) {
            // DEVICE_ABNORMAL
            Map<String, String> variables = new HashMap<>(4);
            variables.put("serialNumber", device.getSerialNumber() == null ? "" : device.getSerialNumber());
            variables.put("modelName", modelName);
            variables.put("projectName", "");
            variables.put("abnormalDesc", to.getDisplayName() + (dto.getRemark() == null ? "" : ":" + dto.getRemark()));
            NotificationEvent event = NotificationEvent.of(
                    NotificationConstant.EVENT_DEVICE_ABNORMAL,
                    NotificationConstant.RECIPIENT_INTERNAL,
                    dto.getInstallerId() == null ? java.util.Collections.emptyList()
                            : java.util.Collections.singletonList(dto.getInstallerId()),
                    variables, device.getId(), NotificationConstant.BIZ_DEVICE);
            notificationProducer.send(event);
        }
    }

    @Override
    public Class<DeviceImportRow> importRowClass() {
        return DeviceImportRow.class;
    }

    // ============ 私有辅助方法 ============

    /**
     * 状态流转的业务联动副作用。
     */
    private void applyTransitionSideEffect(DeviceInstanceEntity device, DeviceStatus from, DeviceStatus to,
                                           DeviceStatusTransitionDTO dto) {
        switch (to) {
            case SHIPPED:
                // 出库发运：分配项目
                if (dto.getProjectId() != null) {
                    device.setProjectId(dto.getProjectId());
                }
                break;
            case RECEIVED:
                // 到货签收：可分配仓库
                if (dto.getWarehouseId() != null) {
                    device.setWarehouseId(dto.getWarehouseId());
                }
                break;
            case INSTALLED:
                if (dto.getInstallerId() != null) {
                    device.setInstallerId(dto.getInstallerId());
                }
                if (device.getInstallDate() == null) {
                    device.setInstallDate(LocalDate.now());
                }
                break;
            case ONLINE:
                if (device.getOnlineDate() == null) {
                    device.setOnlineDate(LocalDate.now());
                }
                break;
            case RETURNED:
                // 退货：清空项目/仓库归属
                device.setProjectId(null);
                break;
            default:
                break;
        }
    }

    private void checkSnUnique(String sn, Long excludeId) {
        LambdaQueryWrapper<DeviceInstanceEntity> wrapper = new LambdaQueryWrapper<DeviceInstanceEntity>()
                .eq(DeviceInstanceEntity::getSerialNumber, sn);
        if (excludeId != null) {
            wrapper.ne(DeviceInstanceEntity::getId, excludeId);
        }
        if (deviceInstanceMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.SN_DUPLICATED, "设备 SN 已存在: " + sn);
        }
    }

    private void validateModelExist(Long modelId) {
        if (modelId == null || deviceModelMapper.selectById(modelId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备型号不存在: " + modelId);
        }
    }

    private void copyDtoToEntity(DeviceInstanceDTO dto, DeviceInstanceEntity entity) {
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setMacAddress(dto.getMacAddress());
        entity.setModelId(dto.getModelId());
        entity.setFirmwareVersion(dto.getFirmwareVersion());
        entity.setProjectId(dto.getProjectId());
        entity.setPhaseId(dto.getPhaseId());
        entity.setSiteName(dto.getSiteName());
        entity.setInstallLocation(dto.getInstallLocation());
        entity.setWarehouseId(dto.getWarehouseId());
        entity.setAgentCompanyId(dto.getAgentCompanyId());
        entity.setInstallDate(dto.getInstallDate());
        entity.setInstallerId(dto.getInstallerId());
        entity.setRemark(dto.getRemark());
    }

    /**
     * 校验单行导入数据，返回错误原因（null 表示通过）。
     */
    private String validateRow(DeviceImportRow row, String sn,
                               Map<String, Long> modelCodeToId, Map<String, Long> warehouseCodeToId) {
        if (sn == null || sn.isEmpty()) {
            return "序列号 SN 不能为空";
        }
        String modelCode = trim(row.getModelCode());
        if (modelCode == null || modelCode.isEmpty()) {
            return "型号编码不能为空";
        }
        if (!modelCodeToId.containsKey(modelCode)) {
            return "型号编码不存在: " + modelCode;
        }
        String whCode = trim(row.getWarehouseCode());
        if (whCode != null && !whCode.isEmpty() && !warehouseCodeToId.containsKey(whCode)) {
            return "仓库编码不存在: " + whCode;
        }
        return null;
    }

    /**
     * 预加载型号编码 → ID 映射。
     */
    private Map<String, Long> loadModelCodeMap() {
        Map<String, Long> map = new HashMap<>();
        List<DeviceModelEntity> models = deviceModelMapper.selectList(null);
        for (DeviceModelEntity m : models) {
            if (m.getModelCode() != null) {
                map.put(m.getModelCode(), m.getId());
            }
        }
        return map;
    }

    /**
     * 预加载仓库编码 → ID 映射。
     */
    private Map<String, Long> loadWarehouseCodeMap() {
        Map<String, Long> map = new HashMap<>();
        List<WarehouseEntity> warehouses = warehouseMapper.selectList(null);
        for (WarehouseEntity w : warehouses) {
            if (w.getWarehouseCode() != null) {
                map.put(w.getWarehouseCode(), w.getId());
            }
        }
        return map;
    }

    /**
     * 预加载已存在 SN → ID 映射（用于导入前校验 SN 重复）。
     * 注：全表加载，适用于设备数量在百万级以内的场景。
     */
    private Map<String, Long> loadExistSnMap() {
        Map<String, Long> map = new HashMap<>();
        List<DeviceInstanceEntity> devices = deviceInstanceMapper.selectList(null);
        for (DeviceInstanceEntity d : devices) {
            if (d.getSerialNumber() != null) {
                map.put(d.getSerialNumber(), d.getId());
            }
        }
        return map;
    }

    /**
     * 使用同步收集 ReadListener 安全读取 Excel 全部行。
     */
    private List<DeviceImportRow> readRowsSafely(MultipartFile file) {
        List<DeviceImportRow> collected = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            excelUtils.read(is, DeviceImportRow.class, new ReadListener<DeviceImportRow>() {
                @Override
                public void invoke(DeviceImportRow row, com.alibaba.excel.context.AnalysisContext context) {
                    collected.add(row);
                }

                @Override
                public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                    // no-op
                }
            });
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_IO_ERROR, "读取 Excel 文件失败: " + e.getMessage());
        }
        return collected;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
