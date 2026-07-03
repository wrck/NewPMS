package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysLogQueryDTO;
import com.vibe.system.entity.SysLogEntity;
import com.vibe.system.entity.SysLoginLogEntity;
import com.vibe.system.mapper.SysLogMapper;
import com.vibe.system.mapper.SysLoginLogMapper;
import com.vibe.system.service.SysLogService;
import com.vibe.system.vo.SysLogVO;
import com.vibe.system.vo.SysLoginLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 系统日志服务实现（操作日志 + 登录日志）
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper sysLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DT_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}$");

    @Override
    public PageResult<SysLogVO> pageOperationLog(SysLogQueryDTO query) {
        IPage<SysLogVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysLogVO> result = sysLogMapper.selectLogPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Async
    @Override
    public void asyncSave(SysLogEntity entity) {
        try {
            sysLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("[操作日志] 异步保存失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<SysLoginLogVO> pageLoginLog(Integer page, Integer size, String username,
                                                  Integer status, String beginTime, String endTime) {
        IPage<SysLoginLogEntity> p = new Page<>(
                page == null ? 1 : page,
                size == null ? 20 : size);
        // 直接使用实体查询，简化复杂参数
        LambdaQueryWrapper<SysLoginLogEntity> wrapper = new LambdaQueryWrapper<SysLoginLogEntity>()
                .orderByDesc(SysLoginLogEntity::getLoginTime);
        if (username != null && !username.isBlank()) {
            wrapper.like(SysLoginLogEntity::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysLoginLogEntity::getStatus, status);
        }
        if (isValidDateTime(beginTime)) {
            wrapper.ge(SysLoginLogEntity::getLoginTime, LocalDateTime.parse(beginTime, DT_FMT));
        }
        if (isValidDateTime(endTime)) {
            wrapper.le(SysLoginLogEntity::getLoginTime, LocalDateTime.parse(endTime, DT_FMT));
        }
        IPage<SysLoginLogEntity> entityPage = sysLoginLogMapper.selectPage(p, wrapper);
        IPage<SysLoginLogVO> voPage = entityPage.convert(this::toLoginLogVo);
        return PageResult.of(voPage.getRecords(), voPage.getTotal(),
                voPage.getCurrent(), voPage.getSize());
    }

    @Override
    public void recordLoginLog(SysLoginLogEntity entity) {
        try {
            sysLoginLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("[登录日志] 保存失败: {}", e.getMessage());
        }
    }

    private SysLoginLogVO toLoginLogVo(SysLoginLogEntity e) {
        SysLoginLogVO vo = new SysLoginLogVO();
        vo.setId(e.getId());
        vo.setUsername(e.getUsername());
        vo.setLoginTime(e.getLoginTime());
        vo.setLoginIp(e.getLoginIp());
        vo.setLoginLocation(e.getLoginLocation());
        vo.setBrowser(e.getBrowser());
        vo.setOs(e.getOs());
        vo.setStatus(e.getStatus());
        vo.setMsg(e.getMsg());
        return vo;
    }

    private boolean isValidDateTime(String s) {
        return s != null && !s.isBlank() && DT_PATTERN.matcher(s).matches();
    }
}
