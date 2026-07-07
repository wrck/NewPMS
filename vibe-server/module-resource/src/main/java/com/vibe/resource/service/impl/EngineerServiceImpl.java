package com.vibe.resource.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.EngineerDTO;
import com.vibe.resource.dto.EngineerQueryDTO;
import com.vibe.resource.dto.EngineerSkillDTO;
import com.vibe.resource.entity.EngineerEntity;
import com.vibe.resource.entity.EngineerSkillEntity;
import com.vibe.resource.mapper.EngineerMapper;
import com.vibe.resource.mapper.EngineerSkillMapper;
import com.vibe.resource.service.EngineerService;
import com.vibe.resource.vo.EngineerSkillVO;
import com.vibe.resource.vo.EngineerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工程师资源池服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EngineerServiceImpl implements EngineerService {

    private final EngineerMapper engineerMapper;
    private final EngineerSkillMapper engineerSkillMapper;

    @Override
    public PageResult<EngineerVO> page(EngineerQueryDTO query) {
        IPage<EngineerVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<EngineerVO> result = engineerMapper.selectEngineerPage(page, query);
        // 填充技能列表
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            List<Long> engineerIds = result.getRecords().stream()
                    .map(EngineerVO::getId).collect(Collectors.toList());
            List<EngineerSkillEntity> skills = engineerSkillMapper.selectByEngineerIds(engineerIds);
            for (EngineerVO vo : result.getRecords()) {
                List<EngineerSkillVO> skillVos = skills.stream()
                        .filter(s -> s.getEngineerId().equals(vo.getId()))
                        .map(this::toSkillVO)
                        .collect(Collectors.toList());
                vo.setSkillList(skillVos);
            }
        }
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public EngineerVO getDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        EngineerVO vo = engineerMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.notFound("工程师");
        }
        vo.setSkillList(listSkills(id));
        return vo;
    }

    @Override
    public EngineerVO getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        EngineerEntity entity = engineerMapper.selectByUserId(userId);
        if (entity == null) {
            return null;
        }
        EngineerVO vo = engineerMapper.selectVoById(entity.getId());
        if (vo != null) {
            vo.setSkillList(listSkills(entity.getId()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EngineerDTO dto) {
        // 唯一性校验：同一 userId 只能关联一个工程师档案
        Long count = engineerMapper.countByUserId(dto.getUserId(), null);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "该用户已关联工程师档案");
        }
        EngineerEntity entity = new EngineerEntity();
        entity.setUserId(dto.getUserId());
        entity.setEmployeeNo(dto.getEngineerNo());
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setRegion(dto.getRegion());
        entity.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : ResourceConstant.ENGINEER_STATUS_ACTIVE);
        entity.setHireDate(dto.getHireDate());
        entity.setSkills(dto.getSkills());
        entity.setCertifications(dto.getCertifications());
        engineerMapper.insert(entity);

        // 保存技能列表
        if (!CollectionUtils.isEmpty(dto.getSkillList())) {
            saveSkills(entity.getId(), dto.getSkillList());
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(EngineerDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        EngineerEntity exist = engineerMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.notFound("工程师");
        }
        // userId 变更时校验唯一性
        if (dto.getUserId() != null && !dto.getUserId().equals(exist.getUserId())) {
            Long count = engineerMapper.countByUserId(dto.getUserId(), dto.getId());
            if (count != null && count > 0) {
                throw new BusinessException(ResultCode.DATA_DUPLICATED, "该用户已关联工程师档案");
            }
            exist.setUserId(dto.getUserId());
        }
        exist.setEmployeeNo(dto.getEngineerNo());
        exist.setName(dto.getName());
        exist.setPhone(dto.getPhone());
        exist.setRegion(dto.getRegion());
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        exist.setHireDate(dto.getHireDate());
        exist.setSkills(dto.getSkills());
        exist.setCertifications(dto.getCertifications());
        engineerMapper.updateById(exist);

        // 技能列表非空时刷新
        if (dto.getSkillList() != null) {
            saveSkills(dto.getId(), dto.getSkillList());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        EngineerEntity exist = engineerMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.notFound("工程师");
        }
        engineerMapper.deleteById(id);
        // 同步逻辑删除技能
        engineerSkillMapper.deleteByEngineerId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "状态不能为空");
        }
        EngineerEntity exist = engineerMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.notFound("工程师");
        }
        EngineerEntity update = new EngineerEntity();
        update.setId(id);
        update.setStatus(status);
        engineerMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkills(Long engineerId, List<EngineerSkillDTO> skills) {
        if (engineerId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        // 先删后插
        engineerSkillMapper.deleteByEngineerId(engineerId);
        if (CollectionUtils.isEmpty(skills)) {
            return;
        }
        for (EngineerSkillDTO dto : skills) {
            EngineerSkillEntity entity = new EngineerSkillEntity();
            entity.setEngineerId(engineerId);
            entity.setSkillTag(dto.getSkillTag());
            entity.setLevel(dto.getLevel());
            engineerSkillMapper.insert(entity);
        }
    }

    @Override
    public List<EngineerSkillVO> listSkills(Long engineerId) {
        if (engineerId == null) {
            return Collections.emptyList();
        }
        List<EngineerSkillEntity> entities = engineerSkillMapper.selectByEngineerId(engineerId);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        List<EngineerSkillVO> list = new ArrayList<>(entities.size());
        for (EngineerSkillEntity entity : entities) {
            list.add(toSkillVO(entity));
        }
        return list;
    }

    @Override
    public List<EngineerVO> listAvailable(List<String> skillTags, String region) {
        List<EngineerVO> engineers = engineerMapper.selectAvailableEngineers(
                skillTags, region, ResourceConstant.ENGINEER_STATUS_ACTIVE);
        if (CollectionUtils.isEmpty(engineers)) {
            return Collections.emptyList();
        }
        // 批量填充技能列表
        List<Long> engineerIds = engineers.stream()
                .map(EngineerVO::getId).collect(Collectors.toList());
        List<EngineerSkillEntity> skills = engineerSkillMapper.selectByEngineerIds(engineerIds);
        for (EngineerVO vo : engineers) {
            List<EngineerSkillVO> skillVos = skills.stream()
                    .filter(s -> s.getEngineerId().equals(vo.getId()))
                    .map(this::toSkillVO)
                    .collect(Collectors.toList());
            vo.setSkillList(skillVos);
            // ongoingTaskCount 由调度服务在推荐场景填充，这里默认 0
            vo.setOngoingTaskCount(0);
        }
        return engineers;
    }

    /* ============ 私有方法 ============ */

    private EngineerSkillVO toSkillVO(EngineerSkillEntity entity) {
        EngineerSkillVO vo = new EngineerSkillVO();
        vo.setId(entity.getId());
        vo.setEngineerId(entity.getEngineerId());
        vo.setSkillTag(entity.getSkillTag());
        vo.setLevel(entity.getLevel());
        return vo;
    }
}
