package com.vibe.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.SparePartActionDTO;
import com.vibe.device.dto.SparePartDTO;
import com.vibe.device.entity.SparePartEntity;
import com.vibe.device.entity.SparePartLogEntity;
import com.vibe.device.enums.SparePartActionType;
import com.vibe.device.mapper.SparePartLogMapper;
import com.vibe.device.mapper.SparePartMapper;
import com.vibe.device.service.SparePartService;
import com.vibe.device.vo.SparePartLogVO;
import com.vibe.device.vo.SparePartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 备件服务实现。
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SparePartServiceImpl implements SparePartService {

    private final SparePartMapper sparePartMapper;
    private final SparePartLogMapper sparePartLogMapper;

    @Override
    public PageResult<SparePartVO> page(Integer page, Integer size, String keyword, Long warehouseId, Long modelId) {
        List<SparePartVO> all = sparePartMapper.selectSparePartList(keyword, warehouseId, modelId);
        int total = all.size();
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);
        return PageResult.of(all.subList(from, to), total, p, s);
    }

    @Override
    public SparePartVO getDetail(Long id) {
        SparePartVO vo = sparePartMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备件不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SparePartDTO dto) {
        checkCodeUnique(dto.getPartCode(), null);
        SparePartEntity entity = new SparePartEntity();
        copyDtoToEntity(dto, entity);
        entity.setStatus(dto.getStatus() == null ? DeviceConstant.SPARE_PART_ENABLED : dto.getStatus());
        entity.setQuantity(dto.getQuantity() == null ? 0 : dto.getQuantity());
        sparePartMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SparePartDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "备件ID不能为空");
        }
        SparePartEntity exist = sparePartMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备件不存在");
        }
        if (dto.getPartCode() != null && !dto.getPartCode().equals(exist.getPartCode())) {
            checkCodeUnique(dto.getPartCode(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        if (dto.getStatus() != null) {
            exist.setStatus(dto.getStatus());
        }
        if (dto.getQuantity() != null) {
            exist.setQuantity(dto.getQuantity());
        }
        sparePartMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (sparePartMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备件不存在");
        }
        sparePartMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void action(SparePartActionDTO dto) {
        SparePartActionType actionType = SparePartActionType.parse(dto.getActionType());
        if (actionType == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "备件操作类型非法: " + dto.getActionType());
        }
        SparePartEntity part = sparePartMapper.selectById(dto.getSparePartId());
        if (part == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备件不存在");
        }
        int current = part.getQuantity() == null ? 0 : part.getQuantity();
        int qty = dto.getQuantity();
        int newQty;
        switch (actionType) {
            case IN:
                newQty = current + qty;
                break;
            case OUT:
            case REPAIR:
                if (current < qty) {
                    throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "备件库存不足");
                }
                newQty = current - qty;
                break;
            case RETURN:
                newQty = current + qty;
                break;
            default:
                throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的备件操作类型");
        }
        part.setQuantity(newQty);
        sparePartMapper.updateById(part);

        // 记录流水
        SparePartLogEntity log = new SparePartLogEntity();
        log.setSparePartId(part.getId());
        log.setActionType(actionType.name());
        log.setQuantity(qty);
        log.setProjectId(dto.getProjectId());
        log.setOperatorId(UserContextHolder.getUserId());
        log.setRemark(dto.getRemark());
        sparePartLogMapper.insert(log);
    }

    @Override
    public List<SparePartLogVO> logList(Long sparePartId, Long projectId, String actionType) {
        return sparePartLogMapper.selectLogList(sparePartId, projectId, actionType);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SparePartEntity> wrapper = new LambdaQueryWrapper<SparePartEntity>()
                .eq(SparePartEntity::getPartCode, code);
        if (excludeId != null) {
            wrapper.ne(SparePartEntity::getId, excludeId);
        }
        if (sparePartMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "备件编码已存在");
        }
    }

    private void copyDtoToEntity(SparePartDTO dto, SparePartEntity entity) {
        entity.setPartName(dto.getPartName());
        entity.setPartCode(dto.getPartCode());
        entity.setModelId(dto.getModelId());
        entity.setWarehouseId(dto.getWarehouseId());
    }
}
