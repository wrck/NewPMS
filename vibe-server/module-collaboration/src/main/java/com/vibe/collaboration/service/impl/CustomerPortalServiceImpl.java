package com.vibe.collaboration.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.collaboration.bo.PhaseDeliverableRow;
import com.vibe.collaboration.mapper.CustomerPortalMapper;
import com.vibe.collaboration.service.CustomerPortalService;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.DocumentVO;
import com.vibe.collaboration.vo.PhaseTimelineVO;
import com.vibe.collaboration.vo.ProjectProgressVO;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 客户门户服务实现
 *
 * <p>核心职责：</p>
 * <ol>
 *   <li>数据隔离：通过 {@link UserContextHolder#getTenantId()} 获取客户ID，校验项目归属</li>
 *   <li>VO 脱敏：仅返回客户可见字段（不含 PM/工程师/成本等）</li>
 *   <li>文档预签名：对 MinIO 中的对象生成限时下载 URL</li>
 * </ol>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPortalServiceImpl implements CustomerPortalService {

    /** 文档类型默认值 */
    private static final String DOC_TYPE_OTHER = "OTHER";

    private final CustomerPortalMapper customerPortalMapper;
    private final MinioUtils minioUtils;
    private final ObjectMapper objectMapper;

    @Override
    public List<CustomerProjectVO> getMyProjects() {
        Long customerId = requireCurrentCustomerId();
        List<CustomerProjectVO> list = customerPortalMapper.selectCustomerProjects(customerId);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public ProjectProgressVO getProjectProgress(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "项目ID不能为空");
        }
        // 数据隔离校验
        checkProjectOwnership(projectId);

        ProjectProgressVO progress = customerPortalMapper.selectProjectProgress(projectId);
        if (progress == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        List<PhaseTimelineVO> phases = customerPortalMapper.selectPhaseTimeline(projectId);
        progress.setPhases(phases == null ? Collections.emptyList() : phases);
        return progress;
    }

    @Override
    public List<DocumentVO> getProjectDocuments(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "项目ID不能为空");
        }
        // 数据隔离校验
        checkProjectOwnership(projectId);

        List<PhaseDeliverableRow> rows = customerPortalMapper.selectProjectDocuments(projectId);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentVO> documents = new ArrayList<>();
        for (PhaseDeliverableRow row : rows) {
            List<DocumentVO> phaseDocs = buildDocumentsFromRow(row);
            documents.addAll(phaseDocs);
        }
        return documents;
    }

    /* ============ 私有方法 ============ */

    /**
     * 获取当前登录客户ID，未登录或非客户角色时抛出异常。
     */
    private Long requireCurrentCustomerId() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getTenantId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        return ctx.getTenantId();
    }

    /**
     * 校验项目归属当前客户。
     *
     * <p>项目不存在时抛 {@link ResultCode#PROJECT_NOT_FOUND}，
     * 项目不属于当前客户时抛 {@link ResultCode#DATA_PERMISSION_DENIED}。</p>
     */
    private void checkProjectOwnership(Long projectId) {
        Long customerId = requireCurrentCustomerId();
        Long projectCustomerId = customerPortalMapper.selectCustomerIdByProjectId(projectId);
        if (projectCustomerId == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!customerId.equals(projectCustomerId)) {
            throw BusinessException.of(ResultCode.DATA_PERMISSION_DENIED);
        }
    }

    /**
     * 解析阶段交付物 JSON，构建文档 VO 列表。
     *
     * <p>支持两种 JSON 结构：</p>
     * <ul>
     *   <li>字符串数组：{@code ["project/101/xxx.pdf", ...]} — 直接作为对象名</li>
     *   <li>对象数组：{@code [{"name":"方案.pdf","url":"project/101/xxx.pdf","type":"DESIGN"}, ...]}</li>
     * </ul>
     * <p>对每个对象名生成 MinIO 预签名下载 URL。</p>
     */
    private List<DocumentVO> buildDocumentsFromRow(PhaseDeliverableRow row) {
        if (!StringUtils.hasText(row.getDeliverables())) {
            return Collections.emptyList();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(row.getDeliverables());
        } catch (Exception e) {
            log.warn("[CustomerPortal] 阶段交付物 JSON 解析失败: phaseId={}, json={}",
                    row.getPhaseId(), row.getDeliverables(), e);
            return Collections.emptyList();
        }
        if (root == null || !root.isArray() || root.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime uploadTime = row.getUpdateTime();
        String defaultDocType = resolveDocTypeByPhase(row.getPhaseCode());
        List<DocumentVO> docs = new ArrayList<>(root.size());
        int index = 0;
        for (JsonNode node : root) {
            DocumentVO vo = buildDocumentVo(node, row, index, defaultDocType, uploadTime);
            if (vo != null) {
                docs.add(vo);
                index++;
            }
        }
        return docs;
    }

    /**
     * 根据单个 JSON 节点构建 DocumentVO。
     */
    private DocumentVO buildDocumentVo(JsonNode node, PhaseDeliverableRow row,
                                       int index, String defaultDocType,
                                       LocalDateTime uploadTime) {
        String objectName;
        String docName;
        String docType;

        if (node.isTextual()) {
            // 字符串数组：直接作为对象名
            objectName = node.asText();
            docName = extractFileName(objectName);
            docType = defaultDocType;
        } else if (node.isObject()) {
            // 对象数组：提取 name/url/type
            JsonNode urlNode = node.get("url");
            if (urlNode == null || urlNode.isNull()) {
                urlNode = node.get("path");
            }
            if (urlNode == null || urlNode.isNull() || !urlNode.isTextual()) {
                return null;
            }
            objectName = urlNode.asText();
            JsonNode nameNode = node.get("name");
            docName = (nameNode != null && nameNode.isTextual()) ? nameNode.asText() : extractFileName(objectName);
            JsonNode typeNode = node.get("type");
            docType = (typeNode != null && typeNode.isTextual()) ? typeNode.asText() : defaultDocType;
        } else {
            return null;
        }

        if (!StringUtils.hasText(objectName)) {
            return null;
        }

        DocumentVO vo = new DocumentVO();
        vo.setDocId(row.getPhaseId() + "_" + index);
        vo.setDocName(StringUtils.hasText(docName) ? docName : objectName);
        vo.setDocType(StringUtils.hasText(docType) ? docType : DOC_TYPE_OTHER);
        vo.setUploadTime(uploadTime);
        vo.setDownloadUrl(generatePresignedUrl(objectName));
        return vo;
    }

    /**
     * 生成 MinIO 预签名下载 URL，失败时返回 null。
     */
    private String generatePresignedUrl(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return null;
        }
        // 仅对相对路径（MinIO 对象名）生成预签名 URL；已经是完整 http(s) URL 的直接返回
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }
        try {
            return minioUtils.getPresignedDownloadUrl(objectName);
        } catch (Exception e) {
            log.warn("[CustomerPortal] 预签名 URL 生成失败: objectName={}", objectName, e);
            return null;
        }
    }

    /**
     * 从对象名中提取文件名（最后一段路径）。
     */
    private String extractFileName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return objectName;
        }
        int slashIdx = objectName.lastIndexOf('/');
        if (slashIdx >= 0 && slashIdx < objectName.length() - 1) {
            return objectName.substring(slashIdx + 1);
        }
        return objectName;
    }

    /**
     * 根据阶段编码推断文档类型。
     */
    private String resolveDocTypeByPhase(String phaseCode) {
        if (!StringUtils.hasText(phaseCode)) {
            return DOC_TYPE_OTHER;
        }
        return switch (phaseCode.toUpperCase(Locale.ROOT)) {
            case "DESIGN" -> "DESIGN";
            case "ACCEPT" -> "REPORT";
            case "DEBUG" -> "CONFIG";
            default -> DOC_TYPE_OTHER;
        };
    }
}
