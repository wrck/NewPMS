package com.vibe.agent.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.AgentEngineerDTO;
import com.vibe.agent.dto.AgentEngineerQueryDTO;
import com.vibe.agent.entity.AgentEngineerEntity;
import com.vibe.agent.mapper.AgentEngineerMapper;
import com.vibe.agent.service.AgentEngineerService;
import com.vibe.agent.vo.AgentEngineerVO;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 代理商工程师服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEngineerServiceImpl implements AgentEngineerService {

    private final AgentEngineerMapper agentEngineerMapper;

    @Override
    public PageResult<AgentEngineerVO> page(AgentEngineerQueryDTO query) {
        IPage<AgentEngineerVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        // @DataPermission 在 Mapper 方法上：AGENT_ADMIN 仅看本公司工程师
        IPage<AgentEngineerVO> result = agentEngineerMapper.selectEngineerPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<AgentEngineerVO> listByCompanyId(Long companyId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        return agentEngineerMapper.selectByCompanyId(companyId);
    }

    @Override
    public AgentEngineerVO getDetail(Long id) {
        if (id == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        AgentEngineerVO vo = agentEngineerMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工程师不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentEngineerDTO dto) {
        checkPhoneUnique(dto.getPhone(), null);

        AgentEngineerEntity entity = new AgentEngineerEntity();
        entity.setAgentCompanyId(dto.getAgentCompanyId());
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setSkills(toJson(dto.getSkills()));
        entity.setCertifications(toJson(dto.getCertifications()));
        entity.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : AgentConstant.ENGINEER_STATUS_ACTIVE);
        agentEngineerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AgentEngineerDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        AgentEngineerEntity exist = agentEngineerMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工程师不存在");
        }
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(exist.getPhone())) {
            checkPhoneUnique(dto.getPhone(), dto.getId());
            exist.setPhone(dto.getPhone());
        }
        exist.setAgentCompanyId(dto.getAgentCompanyId());
        exist.setName(dto.getName());
        if (dto.getSkills() != null) {
            exist.setSkills(toJson(dto.getSkills()));
        }
        if (dto.getCertifications() != null) {
            exist.setCertifications(toJson(dto.getCertifications()));
        }
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        agentEngineerMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        if (agentEngineerMapper.selectById(id) == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工程师不存在");
        }
        agentEngineerMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        if (!AgentConstant.ENGINEER_STATUS_ACTIVE.equals(status)
                && !AgentConstant.ENGINEER_STATUS_DISABLED.equals(status)) {
            throw BusinessException.of(ResultCode.PARAM_INVALID, "无效的工程师状态: " + status);
        }
        AgentEngineerEntity exist = agentEngineerMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工程师不存在");
        }
        AgentEngineerEntity update = new AgentEngineerEntity();
        update.setId(id);
        update.setStatus(status);
        agentEngineerMapper.updateById(update);
    }

    @Override
    public List<OutsourceTaskVO> getTaskHistory(Long engineerId) {
        if (engineerId == null) {
            return Collections.emptyList();
        }
        // @DataPermission 在 Mapper 方法上：AGENT_ADMIN/AGENT_ENGINEER 数据隔离
        return agentEngineerMapper.selectEngineerTaskHistory(engineerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQualityScore(Long engineerId, BigDecimal score) {
        if (engineerId == null) {
            return;
        }
        AgentEngineerEntity update = new AgentEngineerEntity();
        update.setId(engineerId);
        update.setQualityScore(score);
        agentEngineerMapper.updateById(update);
    }

    @Override
    public void validateActive(Long engineerId, Long expectedCompanyId) {
        if (engineerId == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "工程师ID不能为空");
        }
        AgentEngineerEntity entity = agentEngineerMapper.selectById(engineerId);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工程师不存在");
        }
        if (expectedCompanyId != null
                && !expectedCompanyId.equals(entity.getAgentCompanyId())) {
            throw BusinessException.of(ResultCode.DATA_PERMISSION_DENIED,
                    "工程师不属于指定代理商公司");
        }
        if (!entity.isActive()) {
            throw BusinessException.of(ResultCode.STATE_NOT_ALLOWED, "工程师已停用");
        }
    }

    /* ============ 私有方法 ============ */

    private void checkPhoneUnique(String phone, Long excludeId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        LambdaQueryWrapper<AgentEngineerEntity> wrapper = new LambdaQueryWrapper<AgentEngineerEntity>()
                .eq(AgentEngineerEntity::getPhone, phone);
        if (excludeId != null) {
            wrapper.ne(AgentEngineerEntity::getId, excludeId);
        }
        if (agentEngineerMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "手机号已被使用");
        }
    }

    private String toJson(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return JSONUtil.toJsonStr(list);
    }
}
