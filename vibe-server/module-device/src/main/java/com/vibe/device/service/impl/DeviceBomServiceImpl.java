package com.vibe.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.device.dto.DeviceBomDTO;
import com.vibe.device.entity.DeviceBomEntity;
import com.vibe.device.entity.DeviceInstanceEntity;
import com.vibe.device.enums.DeviceStatus;
import com.vibe.device.mapper.DeviceBomMapper;
import com.vibe.device.mapper.DeviceInstanceMapper;
import com.vibe.device.service.DeviceBomService;
import com.vibe.device.vo.DeviceBomVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目设备清单（BOM）服务实现。
 *
 * <p>BOM 进度数量（received_qty/installed_qty/accepted_qty）由设备实例状态聚合得出：
 * received_qty = 项目+型号下 RECEIVED 及之后状态的设备数；
 * installed_qty = INSTALLED 及之后状态的设备数；
 * accepted_qty = DEBUGGED 及之后状态的设备数。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceBomServiceImpl implements DeviceBomService {

    private final DeviceBomMapper deviceBomMapper;
    private final DeviceInstanceMapper deviceInstanceMapper;

    @Override
    public List<DeviceBomVO> listByProject(Long projectId) {
        List<DeviceBomVO> list = deviceBomMapper.selectBomListByProject(projectId);
        // 填充进度数量
        list.forEach(this::fillProgress);
        return list;
    }

    @Override
    public DeviceBomVO getDetail(Long id) {
        DeviceBomVO vo = deviceBomMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "BOM 记录不存在");
        }
        fillProgress(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DeviceBomDTO dto) {
        DeviceBomEntity exist = deviceBomMapper.selectByProjectAndModel(dto.getProjectId(), dto.getModelId());
        if (exist != null) {
            // 已存在则累加计划数量并更新实际数量字段
            exist.setPlannedQty((exist.getPlannedQty() == null ? 0 : exist.getPlannedQty())
                    + (dto.getPlannedQty() == null ? 0 : dto.getPlannedQty()));
            if (dto.getRemark() != null) {
                exist.setRemark(dto.getRemark());
            }
            deviceBomMapper.updateById(exist);
            return exist.getId();
        }
        DeviceBomEntity entity = new DeviceBomEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setModelId(dto.getModelId());
        entity.setPlannedQty(dto.getPlannedQty() == null ? 0 : dto.getPlannedQty());
        entity.setReceivedQty(0);
        entity.setInstalledQty(0);
        entity.setAcceptedQty(0);
        entity.setRemark(dto.getRemark());
        deviceBomMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeviceBomDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "BOM ID不能为空");
        }
        DeviceBomEntity exist = deviceBomMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "BOM 记录不存在");
        }
        if (dto.getPlannedQty() != null) {
            exist.setPlannedQty(dto.getPlannedQty());
        }
        if (dto.getRemark() != null) {
            exist.setRemark(dto.getRemark());
        }
        deviceBomMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (deviceBomMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "BOM 记录不存在");
        }
        deviceBomMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeBom(Long projectId, Long fromModelId, Long toModelId, int deltaQty, String remark) {
        if (projectId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "项目ID不能为空");
        }
        // 减少原型号
        if (fromModelId != null) {
            DeviceBomEntity from = deviceBomMapper.selectByProjectAndModel(projectId, fromModelId);
            if (from == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "原型号 BOM 不存在");
            }
            int newPlanned = (from.getPlannedQty() == null ? 0 : from.getPlannedQty()) + deltaQty;
            if (newPlanned < 0) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "减少后计划数量不能为负");
            }
            from.setPlannedQty(newPlanned);
            if (remark != null) {
                from.setRemark(remark);
            }
            deviceBomMapper.updateById(from);
        }
        // 增加新型号
        if (toModelId != null && (fromModelId == null || !toModelId.equals(fromModelId))) {
            DeviceBomEntity to = deviceBomMapper.selectByProjectAndModel(projectId, toModelId);
            int addQty = Math.abs(deltaQty);
            if (to == null) {
                to = new DeviceBomEntity();
                to.setProjectId(projectId);
                to.setModelId(toModelId);
                to.setPlannedQty(addQty);
                to.setReceivedQty(0);
                to.setInstalledQty(0);
                to.setAcceptedQty(0);
                to.setRemark(remark);
                deviceBomMapper.insert(to);
            } else {
                to.setPlannedQty((to.getPlannedQty() == null ? 0 : to.getPlannedQty()) + addQty);
                if (remark != null) {
                    to.setRemark(remark);
                }
                deviceBomMapper.updateById(to);
            }
        }
    }

    @Override
    public List<DeviceBomVO> statProgress(Long projectId) {
        return listByProject(projectId);
    }

    /**
     * 根据设备实例状态聚合填充 BOM 进度数量。
     */
    private void fillProgress(DeviceBomVO vo) {
        if (vo.getProjectId() == null || vo.getModelId() == null) {
            return;
        }
        // received: RECEIVED 及之后（已到货）
        long received = count(vo.getProjectId(), vo.getModelId(),
                DeviceStatus.RECEIVED, DeviceStatus.PRE_CONFIG,
                DeviceStatus.INSTALLED, DeviceStatus.DEBUGGED, DeviceStatus.ONLINE);
        // installed: INSTALLED 及之后
        long installed = count(vo.getProjectId(), vo.getModelId(),
                DeviceStatus.INSTALLED, DeviceStatus.DEBUGGED, DeviceStatus.ONLINE);
        // accepted: DEBUGGED 及之后（验收=调试通过）
        long accepted = count(vo.getProjectId(), vo.getModelId(),
                DeviceStatus.DEBUGGED, DeviceStatus.ONLINE);
        vo.setReceivedQty((int) received);
        vo.setInstalledQty((int) installed);
        vo.setAcceptedQty((int) accepted);
    }

    /**
     * 统计项目+型号下处于任一指定状态的设备数。
     */
    private long count(Long projectId, Long modelId, DeviceStatus... statuses) {
        java.util.List<String> statusList = new java.util.ArrayList<>();
        for (DeviceStatus s : statuses) {
            statusList.add(s.name());
        }
        LambdaQueryWrapper<DeviceInstanceEntity> wrapper = new LambdaQueryWrapper<DeviceInstanceEntity>()
                .eq(DeviceInstanceEntity::getProjectId, projectId)
                .eq(DeviceInstanceEntity::getModelId, modelId)
                .in(DeviceInstanceEntity::getStatus, statusList);
        return deviceInstanceMapper.selectCount(wrapper);
    }
}
