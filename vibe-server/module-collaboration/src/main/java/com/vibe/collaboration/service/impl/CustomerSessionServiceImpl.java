package com.vibe.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.collaboration.dto.CustomerSessionDTO;
import com.vibe.collaboration.entity.CustomerSessionEntity;
import com.vibe.collaboration.mapper.CustomerSessionMapper;
import com.vibe.collaboration.service.CustomerSessionService;
import com.vibe.collaboration.vo.CustomerSessionVO;
import com.vibe.common.base.PageQuery;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 客户会话服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSessionServiceImpl implements CustomerSessionService {

    /** 会话状态常量 */
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";

    private final CustomerSessionMapper customerSessionMapper;

    @Override
    public CustomerSessionVO getDetail(Long id) {
        CustomerSessionEntity entity = customerSessionMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerSessionDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户ID不能为空");
        }
        CustomerSessionEntity entity = new CustomerSessionEntity();
        copyDtoToEntity(dto, entity);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ACTIVE);
        }
        customerSessionMapper.insert(entity);
        log.info("[CustomerSession] 新增会话: customerId={}, token={}",
                entity.getCustomerId(), maskToken(entity.getLoginToken()));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomerSessionDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "会话ID不能为空");
        }
        CustomerSessionEntity exist = customerSessionMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        copyDtoToEntity(dto, exist);
        customerSessionMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CustomerSessionEntity exist = customerSessionMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        customerSessionMapper.deleteById(id);
    }

    @Override
    public List<CustomerSessionVO> listByCustomerId(Long customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        List<CustomerSessionEntity> list = customerSessionMapper.selectList(
                new LambdaQueryWrapper<CustomerSessionEntity>()
                        .eq(CustomerSessionEntity::getCustomerId, customerId)
                        .orderByDesc(CustomerSessionEntity::getLoginTime));
        return list == null ? Collections.emptyList() : list.stream().map(this::toVO).toList();
    }

    @Override
    public CustomerSessionVO getByToken(String loginToken) {
        if (!StringUtils.hasText(loginToken)) {
            return null;
        }
        CustomerSessionEntity entity = customerSessionMapper.selectOne(
                new LambdaQueryWrapper<CustomerSessionEntity>()
                        .eq(CustomerSessionEntity::getLoginToken, loginToken)
                        .last("LIMIT 1"));
        return entity == null ? null : toVO(entity);
    }

    @Override
    public PageResult<CustomerSessionVO> pageByCustomerId(Long customerId, PageQuery query) {
        if (customerId == null) {
            return PageResult.empty(query.getPage(), query.getSize());
        }
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();
        IPage<CustomerSessionEntity> p = new Page<>(page, size);
        LambdaQueryWrapper<CustomerSessionEntity> wrapper = new LambdaQueryWrapper<CustomerSessionEntity>()
                .eq(CustomerSessionEntity::getCustomerId, customerId)
                .orderByDesc(CustomerSessionEntity::getLoginTime);
        IPage<CustomerSessionEntity> result = customerSessionMapper.selectPage(p, wrapper);
        List<CustomerSessionVO> records = result.getRecords() == null
                ? Collections.emptyList()
                : result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceOffline(Long id) {
        CustomerSessionEntity exist = customerSessionMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        exist.setStatus(STATUS_REVOKED);
        customerSessionMapper.updateById(exist);
        // 逻辑删除会话，使 token 不再可查（getByToken 返回 null）即强制下线
        customerSessionMapper.deleteById(id);
        log.info("[CustomerSession] 强制下线: sessionId={}, customerId={}", id, exist.getCustomerId());
    }

    /* ============ 私有辅助方法 ============ */

    private void copyDtoToEntity(CustomerSessionDTO dto, CustomerSessionEntity entity) {
        if (dto.getCustomerId() != null) {
            entity.setCustomerId(dto.getCustomerId());
        }
        if (StringUtils.hasText(dto.getLoginToken())) {
            entity.setLoginToken(dto.getLoginToken());
        }
        entity.setLoginIp(dto.getLoginIp());
        entity.setLoginLocation(dto.getLoginLocation());
        entity.setLoginTime(dto.getLoginTime());
        entity.setExpireTime(dto.getExpireTime());
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
    }

    private CustomerSessionVO toVO(CustomerSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        CustomerSessionVO vo = new CustomerSessionVO();
        vo.setId(entity.getId());
        vo.setCustomerId(entity.getCustomerId());
        vo.setLoginToken(maskToken(entity.getLoginToken()));
        vo.setLoginIp(entity.getLoginIp());
        vo.setLoginLocation(entity.getLoginLocation());
        vo.setLoginTime(entity.getLoginTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 对 token 做脱敏处理（仅保留前 8 位与后 4 位）。
     */
    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return token;
        }
        if (token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
    }
}
