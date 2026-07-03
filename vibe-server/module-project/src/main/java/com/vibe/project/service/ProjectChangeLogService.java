package com.vibe.project.service;

import com.vibe.project.dto.ChangeApproveDTO;
import com.vibe.project.dto.ProjectChangeDTO;
import com.vibe.project.vo.ProjectChangeLogVO;

import java.util.List;

/**
 * 项目变更记录服务
 *
 * <p>核心业务：变更申请、影响评估、审批、记录留痕。</p>
 *
 * @author vibe
 */
public interface ProjectChangeLogService {

    /**
     * 查询项目下的变更记录（按时间倒序）
     */
    List<ProjectChangeLogVO> listByProjectId(Long projectId);

    /**
     * 提交变更申请（含影响评估，状态置为 PENDING）
     */
    Long applyChange(ProjectChangeDTO dto);

    /**
     * 审批变更（APPROVED/REJECTED，记录审批人与审批时间）
     */
    void approve(Long id, ChangeApproveDTO dto);

    /**
     * 执行变更（APPROVED → EXECUTED）
     */
    void execute(Long id);

    /**
     * 变更详情
     */
    ProjectChangeLogVO getDetail(Long id);

    /**
     * 删除变更记录
     */
    void delete(Long id);
}

