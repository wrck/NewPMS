package com.vibe.collaboration.mapper;

import com.vibe.collaboration.bo.PhaseDeliverableRow;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.PhaseTimelineVO;
import com.vibe.collaboration.vo.ProjectProgressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户门户 Mapper
 *
 * <p>提供客户 H5 端项目进度/文档查询所需的只读查询。</p>
 *
 * @author vibe
 */
@Mapper
public interface CustomerPortalMapper {

    /**
     * 查询客户关联的项目列表（脱敏字段）。
     *
     * <p>JOIN project + customer，按客户ID过滤，仅返回客户可见字段。</p>
     *
     * @param customerId 客户ID（取自 UserContextHolder.getTenantId()）
     * @return 项目列表
     */
    List<CustomerProjectVO> selectCustomerProjects(@Param("customerId") Long customerId);

    /**
     * 查询项目整体进度信息（不含阶段列表，阶段由 {@link #selectPhaseTimeline} 单独查询）。
     *
     * <p>同时关联 project_phase 以解析当前阶段名称。</p>
     *
     * @param projectId 项目ID
     * @return 项目进度 VO（无 phases 列表）；项目不存在时返回 null
     */
    ProjectProgressVO selectProjectProgress(@Param("projectId") Long projectId);

    /**
     * 查询项目阶段时间线。
     *
     * @param projectId 项目ID
     * @return 阶段时间线列表（按 sort_order 升序）
     */
    List<PhaseTimelineVO> selectPhaseTimeline(@Param("projectId") Long projectId);

    /**
     * 查询项目可下载文档。
     *
     * <p>当前 schema 未独立 project_document 表，从 project_phase.deliverables JSON 字段提取。
     * 返回中间 BO 列表，由 Service 层解析 JSON 并生成预签名 URL。</p>
     *
     * @param projectId 项目ID
     * @return 阶段交付物中间 BO 列表
     */
    List<PhaseDeliverableRow> selectProjectDocuments(@Param("projectId") Long projectId);

    /**
     * 查询项目的客户ID（用于数据隔离校验）。
     *
     * @param projectId 项目ID
     * @return 项目所属客户ID；项目不存在时返回 null
     */
    Long selectCustomerIdByProjectId(@Param("projectId") Long projectId);
}
