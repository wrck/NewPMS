package com.vibe.project.service.impl;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.dto.ProjectCommentDTO;
import com.vibe.project.entity.ProjectCommentEntity;
import com.vibe.project.mapper.ProjectCommentMapper;
import com.vibe.project.service.ProjectCommentService;
import com.vibe.project.vo.ProjectCommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目沟通记录服务实现
 *
 * <p>评论支持回复树结构：顶层评论 parentId 为 null，回复评论 parentId 指向被回复评论。
 * 查询返回时按顶层评论聚合 replies 列表。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCommentServiceImpl implements ProjectCommentService {

    private final ProjectCommentMapper projectCommentMapper;

    @Override
    public List<ProjectCommentVO> listByProjectId(Long projectId) {
        List<ProjectCommentVO> all = projectCommentMapper.selectByProjectId(projectId);
        return buildTree(all);
    }

    @Override
    public List<ProjectCommentVO> listByTaskId(Long taskId) {
        List<ProjectCommentVO> all = projectCommentMapper.selectByTaskId(taskId);
        return buildTree(all);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCommentDTO dto) {
        Long authorId = UserContextHolder.getUserId();
        ProjectCommentEntity entity = new ProjectCommentEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setContent(dto.getContent());
        entity.setAuthorId(authorId);
        entity.setParentId(dto.getParentId());
        projectCommentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectCommentEntity exist = projectCommentMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "评论不存在");
        }
        // 仅作者或超级管理员可删除（Phase 1 简化：作者即可删除）
        Long currentUserId = UserContextHolder.getUserId();
        if (currentUserId != null && !currentUserId.equals(exist.getAuthorId())) {
            throw BusinessException.of(ResultCode.NO_PERMISSION, "无权删除他人评论");
        }
        projectCommentMapper.deleteById(id);
    }

    /**
     * 将平铺评论列表构建为带 replies 的树结构。
     *
     * <p>实现：先按 id 索引，再把 parentId 非空的回复挂到对应顶层评论的 replies 中。</p>
     */
    private List<ProjectCommentVO> buildTree(List<ProjectCommentVO> all) {
        if (CollectionUtils.isEmpty(all)) {
            return new ArrayList<>();
        }
        Map<Long, ProjectCommentVO> idIndex = new LinkedHashMap<>();
        for (ProjectCommentVO vo : all) {
            vo.setReplies(new ArrayList<>());
            idIndex.put(vo.getId(), vo);
        }
        List<ProjectCommentVO> roots = new ArrayList<>();
        for (ProjectCommentVO vo : all) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                ProjectCommentVO parent = idIndex.get(vo.getParentId());
                if (parent != null) {
                    parent.getReplies().add(vo);
                } else {
                    // 父评论不在当前结果集（可能已删除），按顶层处理
                    roots.add(vo);
                }
            }
        }
        return roots;
    }
}
