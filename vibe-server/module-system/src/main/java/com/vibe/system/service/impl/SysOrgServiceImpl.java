package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysOrgDTO;
import com.vibe.system.entity.SysOrgEntity;
import com.vibe.system.mapper.SysOrgMapper;
import com.vibe.system.service.SysOrgService;
import com.vibe.system.vo.SysOrgVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 组织架构服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl implements SysOrgService {

    private final SysOrgMapper sysOrgMapper;

    @Override
    public List<SysOrgVO> listTree() {
        List<SysOrgVO> all = sysOrgMapper.selectAllOrgVo();
        return SysConverters.buildOrgTree(all);
    }

    @Override
    public List<SysOrgVO> listAll() {
        return sysOrgMapper.selectAllOrgVo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysOrgDTO dto) {
        SysOrgEntity entity = new SysOrgEntity();
        copyDtoToEntity(dto, entity);
        sysOrgMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysOrgDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "组织ID不能为空");
        }
        SysOrgEntity exist = sysOrgMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组织不存在");
        }
        if (dto.getParentId() != null && dto.getParentId().equals(dto.getId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "父组织不能为自身");
        }
        copyDtoToEntity(dto, exist);
        sysOrgMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 存在子组织则禁止删除
        LambdaQueryWrapper<SysOrgEntity> childWrapper = new LambdaQueryWrapper<SysOrgEntity>()
                .eq(SysOrgEntity::getParentId, id);
        if (sysOrgMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT, "存在子组织，无法删除");
        }
        sysOrgMapper.deleteById(id);
    }

    @Override
    public SysOrgVO getDetail(Long id) {
        SysOrgEntity entity = sysOrgMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组织不存在");
        }
        return SysConverters.toOrgVo(entity);
    }

    private void copyDtoToEntity(SysOrgDTO dto, SysOrgEntity entity) {
        entity.setParentId(dto.getParentId() == null ? SystemConstant.ROOT_PARENT_ID : dto.getParentId());
        entity.setOrgName(dto.getOrgName());
        entity.setOrgCode(dto.getOrgCode());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? SystemConstant.STATUS_ENABLED : dto.getStatus());
    }
}
