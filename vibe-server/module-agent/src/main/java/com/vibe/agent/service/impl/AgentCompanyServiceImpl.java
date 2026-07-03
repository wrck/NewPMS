package com.vibe.agent.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.AgentCompanyDTO;
import com.vibe.agent.dto.AgentCompanyQueryDTO;
import com.vibe.agent.entity.AgentCompanyEntity;
import com.vibe.agent.mapper.AgentCompanyMapper;
import com.vibe.agent.service.AgentCompanyService;
import com.vibe.agent.vo.AgentCompanyVO;
import com.vibe.agent.vo.AgentRankingVO;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 代理商公司服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCompanyServiceImpl implements AgentCompanyService {

    private final AgentCompanyMapper agentCompanyMapper;

    @Override
    public PageResult<AgentCompanyVO> page(AgentCompanyQueryDTO query) {
        IPage<AgentCompanyVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        // @DataPermission 在 Mapper 方法上：AGENT_ADMIN 仅看本公司（id = tenantId）
        IPage<AgentCompanyVO> result = agentCompanyMapper.selectCompanyPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AgentCompanyVO getDetail(Long id) {
        if (id == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "公司ID不能为空");
        }
        AgentCompanyVO vo = agentCompanyMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.AGENT_NOT_FOUND);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentCompanyDTO dto) {
        // 唯一性校验
        checkNameUnique(dto.getCompanyName(), null);
        checkCodeUnique(dto.getCompanyCode(), null);

        AgentCompanyEntity entity = new AgentCompanyEntity();
        entity.setCompanyName(dto.getCompanyName());
        entity.setCompanyCode(dto.getCompanyCode());
        entity.setQualification(dto.getQualification());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setServiceRegions(toJson(dto.getServiceRegions()));
        entity.setProductLines(toJson(dto.getProductLines()));
        entity.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : AgentConstant.COMPANY_STATUS_ACTIVE);
        entity.setCooperationStart(dto.getCooperationStart());
        agentCompanyMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AgentCompanyDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "公司ID不能为空");
        }
        AgentCompanyEntity exist = agentCompanyMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.AGENT_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getCompanyName())
                && !dto.getCompanyName().equals(exist.getCompanyName())) {
            checkNameUnique(dto.getCompanyName(), dto.getId());
            exist.setCompanyName(dto.getCompanyName());
        }
        if (StringUtils.hasText(dto.getCompanyCode())
                && !dto.getCompanyCode().equals(exist.getCompanyCode())) {
            checkCodeUnique(dto.getCompanyCode(), dto.getId());
            exist.setCompanyCode(dto.getCompanyCode());
        }
        exist.setQualification(dto.getQualification());
        exist.setContactName(dto.getContactName());
        exist.setContactPhone(dto.getContactPhone());
        if (dto.getServiceRegions() != null) {
            exist.setServiceRegions(toJson(dto.getServiceRegions()));
        }
        if (dto.getProductLines() != null) {
            exist.setProductLines(toJson(dto.getProductLines()));
        }
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        exist.setCooperationStart(dto.getCooperationStart());
        agentCompanyMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "公司ID不能为空");
        }
        if (agentCompanyMapper.selectById(id) == null) {
            throw BusinessException.of(ResultCode.AGENT_NOT_FOUND);
        }
        agentCompanyMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        if (!isValidCompanyStatus(status)) {
            throw BusinessException.of(ResultCode.PARAM_INVALID, "无效的合作状态: " + status);
        }
        AgentCompanyEntity exist = agentCompanyMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.AGENT_NOT_FOUND);
        }
        AgentCompanyEntity update = new AgentCompanyEntity();
        update.setId(id);
        update.setStatus(status);
        agentCompanyMapper.updateById(update);
    }

    @Override
    public List<AgentRankingVO> ranking(int limit) {
        int n = limit <= 0 ? 10 : Math.min(limit, 100);
        List<AgentCompanyVO> companies = agentCompanyMapper.selectRanking(n);
        if (CollectionUtils.isEmpty(companies)) {
            return Collections.emptyList();
        }
        List<AgentRankingVO> result = new ArrayList<>(companies.size());
        int rank = 1;
        for (AgentCompanyVO c : companies) {
            AgentRankingVO r = new AgentRankingVO();
            r.setRank(rank++);
            r.setId(c.getId());
            r.setCompanyName(c.getCompanyName());
            r.setCompanyCode(c.getCompanyCode());
            r.setQualification(c.getQualification());
            r.setOverallScore(c.getOverallScore());
            r.setStatus(c.getStatus());
            result.add(r);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshOverallScore(Long companyId) {
        if (companyId == null) {
            return;
        }
        BigDecimal avg = agentCompanyMapper.selectAvgScoreByCompanyId(companyId);
        AgentCompanyEntity update = new AgentCompanyEntity();
        update.setId(companyId);
        update.setOverallScore(avg != null ? avg.setScale(2, RoundingMode.HALF_UP) : null);
        agentCompanyMapper.updateById(update);
        log.info("刷新代理商综合评分: companyId={}, score={}", companyId, avg);
    }

    @Override
    public List<AgentCompanyVO> listByRegion(String region) {
        if (!StringUtils.hasText(region)) {
            return Collections.emptyList();
        }
        return agentCompanyMapper.selectByRegion(region);
    }

    @Override
    public void validateActive(Long companyId) {
        if (companyId == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "代理商公司ID不能为空");
        }
        AgentCompanyEntity entity = agentCompanyMapper.selectById(companyId);
        if (entity == null) {
            throw BusinessException.of(ResultCode.AGENT_NOT_FOUND);
        }
        if (!entity.isActive()) {
            throw BusinessException.of(ResultCode.STATE_NOT_ALLOWED,
                    "代理商公司当前状态(" + entity.getStatus() + ")不可接单");
        }
    }

    /* ============ 私有方法 ============ */

    private void checkNameUnique(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        LambdaQueryWrapper<AgentCompanyEntity> wrapper = new LambdaQueryWrapper<AgentCompanyEntity>()
                .eq(AgentCompanyEntity::getCompanyName, name);
        if (excludeId != null) {
            wrapper.ne(AgentCompanyEntity::getId, excludeId);
        }
        if (agentCompanyMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "公司名称已存在");
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        LambdaQueryWrapper<AgentCompanyEntity> wrapper = new LambdaQueryWrapper<AgentCompanyEntity>()
                .eq(AgentCompanyEntity::getCompanyCode, code);
        if (excludeId != null) {
            wrapper.ne(AgentCompanyEntity::getId, excludeId);
        }
        if (agentCompanyMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "公司编码已存在");
        }
    }

    private boolean isValidCompanyStatus(String status) {
        return AgentConstant.COMPANY_STATUS_ACTIVE.equals(status)
                || AgentConstant.COMPANY_STATUS_SUSPENDED.equals(status)
                || AgentConstant.COMPANY_STATUS_TERMINATED.equals(status);
    }

    /**
     * 将 List 序列化为 JSON 字符串（存储到 JSON 列）。
     */
    private String toJson(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return JSONUtil.toJsonStr(list);
    }
}
