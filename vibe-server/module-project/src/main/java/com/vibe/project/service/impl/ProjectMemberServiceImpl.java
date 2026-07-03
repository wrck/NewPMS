package com.vibe.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.entity.ProjectMemberEntity;
import com.vibe.project.mapper.ProjectMemberMapper;
import com.vibe.project.service.ProjectMemberService;
import com.vibe.project.vo.ProjectMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目成员服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberMapper projectMemberMapper;

    @Override
    public List<ProjectMemberVO> listByProjectId(Long projectId) {
        List<ProjectMemberEntity> list = projectMemberMapper.selectByProjectId(projectId);
        return list.stream().map(ProjectConverters::toMemberVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addMember(Long projectId, Long userId, String role) {
        if (projectId == null || userId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "项目ID与用户ID不能为空");
        }
        // 幂等：已存在则不重复插入
        ProjectMemberEntity exist = projectMemberMapper.selectByProjectAndUser(projectId, userId);
        if (exist != null) {
            return exist.getId();
        }
        ProjectMemberEntity entity = new ProjectMemberEntity();
        entity.setProjectId(projectId);
        entity.setUserId(userId);
        entity.setRole(role);
        entity.setJoinTime(LocalDateTime.now());
        projectMemberMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, String role) {
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "成员角色不能为空");
        }
        ProjectMemberEntity exist = projectMemberMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "项目成员不存在");
        }
        exist.setRole(role);
        projectMemberMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        ProjectMemberEntity exist = projectMemberMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "项目成员不存在");
        }
        projectMemberMapper.deleteById(id);
    }

    @Override
    public boolean isMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        return projectMemberMapper.selectByProjectAndUser(projectId, userId) != null;
    }

    @Override
    public List<Long> listProjectIdsByUserId(Long userId) {
        return projectMemberMapper.selectProjectIdsByUserId(userId);
    }
}
