package com.vibe.collaboration.service;

import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.DocumentVO;
import com.vibe.collaboration.vo.ProjectProgressVO;

import java.util.List;

/**
 * 客户门户服务
 *
 * <p>提供客户 H5 端的项目进度查看与文档下载能力。
 * 所有方法均强制数据隔离：CUSTOMER 角色仅能访问自己关联的项目。</p>
 *
 * @author vibe
 */
public interface CustomerPortalService {

    /**
     * 查询当前登录客户关联的项目列表。
     *
     * <p>客户ID通过 {@code UserContextHolder.get().getTenantId()} 获取。</p>
     *
     * @return 项目列表（脱敏）
     */
    List<CustomerProjectVO> getMyProjects();

    /**
     * 查询项目整体进度（含阶段时间线）。
     *
     * <p>校验项目归属当前客户，未归属时抛出 {@code BusinessException}。</p>
     *
     * @param projectId 项目ID
     * @return 项目进度详情
     */
    ProjectProgressVO getProjectProgress(Long projectId);

    /**
     * 查询项目可下载文档列表。
     *
     * <p>对 MinIO 中的对象生成预签名下载 URL。校验项目归属当前客户。</p>
     *
     * @param projectId 项目ID
     * @return 文档列表
     */
    List<DocumentVO> getProjectDocuments(Long projectId);
}
