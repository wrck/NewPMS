package com.vibe.project.converter;

import com.vibe.project.entity.CustomerEntity;
import com.vibe.project.entity.ProjectChangeLogEntity;
import com.vibe.project.entity.ProjectCommentEntity;
import com.vibe.project.entity.ProjectEntity;
import com.vibe.project.entity.ProjectIssueEntity;
import com.vibe.project.entity.ProjectMemberEntity;
import com.vibe.project.entity.ProjectMilestoneEntity;
import com.vibe.project.entity.ProjectPhaseEntity;
import com.vibe.project.entity.ProjectRiskEntity;
import com.vibe.project.entity.ProjectTaskEntity;
import com.vibe.project.entity.ProjectTemplateEntity;
import com.vibe.project.entity.ProjectTemplatePhaseEntity;
import com.vibe.project.entity.ProjectTemplateTaskEntity;
import com.vibe.project.vo.CustomerVO;
import com.vibe.project.vo.ProjectChangeLogVO;
import com.vibe.project.vo.ProjectCommentVO;
import com.vibe.project.vo.ProjectDetailVO;
import com.vibe.project.vo.ProjectIssueVO;
import com.vibe.project.vo.ProjectMemberVO;
import com.vibe.project.vo.ProjectMilestoneVO;
import com.vibe.project.vo.ProjectPhaseVO;
import com.vibe.project.vo.ProjectRiskVO;
import com.vibe.project.vo.ProjectTaskVO;
import com.vibe.project.vo.ProjectTemplatePhaseVO;
import com.vibe.project.vo.ProjectTemplateTaskVO;
import com.vibe.project.vo.ProjectTemplateVO;
import com.vibe.project.vo.ProjectVO;

/**
 * 项目模块 Entity &lt;-&gt; VO 手工转换工具
 *
 * @author vibe
 */
public final class ProjectConverters {

    private ProjectConverters() {
    }

    public static CustomerVO toCustomerVo(CustomerEntity e) {
        if (e == null) {
            return null;
        }
        CustomerVO vo = new CustomerVO();
        vo.setId(e.getId());
        vo.setCustomerName(e.getCustomerName());
        vo.setCustomerCode(e.getCustomerCode());
        vo.setContactName(e.getContactName());
        vo.setContactPhone(e.getContactPhone());
        vo.setContactEmail(e.getContactEmail());
        vo.setAddress(e.getAddress());
        vo.setRegion(e.getRegion());
        vo.setIndustry(e.getIndustry());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectVO toProjectVo(ProjectEntity e) {
        if (e == null) {
            return null;
        }
        ProjectVO vo = new ProjectVO();
        vo.setId(e.getId());
        vo.setProjectCode(e.getProjectCode());
        vo.setProjectName(e.getProjectName());
        vo.setCustomerId(e.getCustomerId());
        vo.setProjectType(e.getProjectType());
        vo.setProductLine(e.getProductLine());
        vo.setExecuteMode(e.getExecuteMode());
        vo.setPriority(e.getPriority());
        vo.setStatus(e.getStatus());
        vo.setCurrentPhase(e.getCurrentPhase());
        vo.setPmId(e.getPmId());
        vo.setRegion(e.getRegion());
        vo.setContractNo(e.getContractNo());
        vo.setPlannedStart(e.getPlannedStart());
        vo.setPlannedEnd(e.getPlannedEnd());
        vo.setActualStart(e.getActualStart());
        vo.setActualEnd(e.getActualEnd());
        vo.setProgressPct(e.getProgressPct());
        vo.setDescription(e.getDescription());
        vo.setRemark(e.getRemark());
        vo.setVersion(e.getVersion());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    public static ProjectDetailVO toProjectDetailVo(ProjectEntity e) {
        if (e == null) {
            return null;
        }
        ProjectDetailVO vo = new ProjectDetailVO();
        vo.setId(e.getId());
        vo.setProjectCode(e.getProjectCode());
        vo.setProjectName(e.getProjectName());
        vo.setCustomerId(e.getCustomerId());
        vo.setProjectType(e.getProjectType());
        vo.setProductLine(e.getProductLine());
        vo.setExecuteMode(e.getExecuteMode());
        vo.setPriority(e.getPriority());
        vo.setStatus(e.getStatus());
        vo.setCurrentPhase(e.getCurrentPhase());
        vo.setPmId(e.getPmId());
        vo.setRegion(e.getRegion());
        vo.setContractNo(e.getContractNo());
        vo.setPlannedStart(e.getPlannedStart());
        vo.setPlannedEnd(e.getPlannedEnd());
        vo.setActualStart(e.getActualStart());
        vo.setActualEnd(e.getActualEnd());
        vo.setProgressPct(e.getProgressPct());
        vo.setDescription(e.getDescription());
        vo.setRemark(e.getRemark());
        vo.setVersion(e.getVersion());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    public static ProjectPhaseVO toPhaseVo(ProjectPhaseEntity e) {
        if (e == null) {
            return null;
        }
        ProjectPhaseVO vo = new ProjectPhaseVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setPhaseCode(e.getPhaseCode());
        vo.setPhaseName(e.getPhaseName());
        vo.setSortOrder(e.getSortOrder());
        vo.setStatus(e.getStatus());
        vo.setPlannedStart(e.getPlannedStart());
        vo.setPlannedEnd(e.getPlannedEnd());
        vo.setActualStart(e.getActualStart());
        vo.setActualEnd(e.getActualEnd());
        vo.setDeliverables(e.getDeliverables());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectTaskVO toTaskVo(ProjectTaskEntity e) {
        if (e == null) {
            return null;
        }
        ProjectTaskVO vo = new ProjectTaskVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setPhaseId(e.getPhaseId());
        vo.setParentTaskId(e.getParentTaskId());
        vo.setTaskName(e.getTaskName());
        vo.setTaskType(e.getTaskType());
        vo.setStatus(e.getStatus());
        vo.setExecuteMode(e.getExecuteMode());
        vo.setAssigneeId(e.getAssigneeId());
        vo.setAgentCompanyId(e.getAgentCompanyId());
        vo.setAgentEngineerId(e.getAgentEngineerId());
        vo.setSiteInfo(e.getSiteInfo());
        vo.setDeviceIds(e.getDeviceIds());
        vo.setPlannedStart(e.getPlannedStart());
        vo.setPlannedEnd(e.getPlannedEnd());
        vo.setActualStart(e.getActualStart());
        vo.setActualEnd(e.getActualEnd());
        vo.setPriority(e.getPriority());
        vo.setDescription(e.getDescription());
        vo.setAttachments(e.getAttachments());
        vo.setVersion(e.getVersion());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    public static ProjectMilestoneVO toMilestoneVo(ProjectMilestoneEntity e) {
        if (e == null) {
            return null;
        }
        ProjectMilestoneVO vo = new ProjectMilestoneVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setMilestoneName(e.getMilestoneName());
        vo.setPlannedDate(e.getPlannedDate());
        vo.setActualDate(e.getActualDate());
        vo.setDeliverables(e.getDeliverables());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectRiskVO toRiskVo(ProjectRiskEntity e) {
        if (e == null) {
            return null;
        }
        ProjectRiskVO vo = new ProjectRiskVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setRiskDesc(e.getRiskDesc());
        vo.setImpact(e.getImpact());
        vo.setProbability(e.getProbability());
        vo.setMeasure(e.getMeasure());
        vo.setOwnerId(e.getOwnerId());
        vo.setStatus(e.getStatus());
        vo.setDueDate(e.getDueDate());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectIssueVO toIssueVo(ProjectIssueEntity e) {
        if (e == null) {
            return null;
        }
        ProjectIssueVO vo = new ProjectIssueVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setTaskId(e.getTaskId());
        vo.setIssueDesc(e.getIssueDesc());
        vo.setImpact(e.getImpact());
        vo.setOwnerId(e.getOwnerId());
        vo.setStatus(e.getStatus());
        vo.setDueDate(e.getDueDate());
        vo.setResolvedTime(e.getResolvedTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectChangeLogVO toChangeLogVo(ProjectChangeLogEntity e) {
        if (e == null) {
            return null;
        }
        ProjectChangeLogVO vo = new ProjectChangeLogVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setChangeType(e.getChangeType());
        vo.setChangeContent(e.getChangeContent());
        vo.setReason(e.getReason());
        vo.setImpactAnalysis(e.getImpactAnalysis());
        vo.setStatus(e.getStatus());
        vo.setApplicantId(e.getApplicantId());
        vo.setApproverId(e.getApproverId());
        vo.setApproveTime(e.getApproveTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectCommentVO toCommentVo(ProjectCommentEntity e) {
        if (e == null) {
            return null;
        }
        ProjectCommentVO vo = new ProjectCommentVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setTaskId(e.getTaskId());
        vo.setContent(e.getContent());
        vo.setAuthorId(e.getAuthorId());
        vo.setParentId(e.getParentId());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectMemberVO toMemberVo(ProjectMemberEntity e) {
        if (e == null) {
            return null;
        }
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setUserId(e.getUserId());
        vo.setRole(e.getRole());
        vo.setJoinTime(e.getJoinTime());
        return vo;
    }

    public static ProjectTemplateVO toTemplateVo(ProjectTemplateEntity e) {
        if (e == null) {
            return null;
        }
        ProjectTemplateVO vo = new ProjectTemplateVO();
        vo.setId(e.getId());
        vo.setTemplateName(e.getTemplateName());
        vo.setProjectType(e.getProjectType());
        vo.setProductLine(e.getProductLine());
        vo.setDescription(e.getDescription());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    public static ProjectTemplatePhaseVO toTemplatePhaseVo(ProjectTemplatePhaseEntity e) {
        if (e == null) {
            return null;
        }
        ProjectTemplatePhaseVO vo = new ProjectTemplatePhaseVO();
        vo.setId(e.getId());
        vo.setTemplateId(e.getTemplateId());
        vo.setPhaseCode(e.getPhaseCode());
        vo.setPhaseName(e.getPhaseName());
        vo.setSortOrder(e.getSortOrder());
        vo.setDeliverables(e.getDeliverables());
        return vo;
    }

    public static ProjectTemplateTaskVO toTemplateTaskVo(ProjectTemplateTaskEntity e) {
        if (e == null) {
            return null;
        }
        ProjectTemplateTaskVO vo = new ProjectTemplateTaskVO();
        vo.setId(e.getId());
        vo.setTemplateId(e.getTemplateId());
        vo.setPhaseCode(e.getPhaseCode());
        vo.setTaskName(e.getTaskName());
        vo.setTaskType(e.getTaskType());
        vo.setDescription(e.getDescription());
        vo.setDefaultDays(e.getDefaultDays());
        return vo;
    }
}
