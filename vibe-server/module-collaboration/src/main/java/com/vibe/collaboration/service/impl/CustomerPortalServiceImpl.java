package com.vibe.collaboration.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.collaboration.bo.PhaseDeliverableRow;
import com.vibe.collaboration.dto.CustomerAcceptanceSignDTO;
import com.vibe.collaboration.dto.CustomerCutoverApprovalDTO;
import com.vibe.collaboration.mapper.CustomerPortalMapper;
import com.vibe.collaboration.service.CustomerPortalService;
import com.vibe.collaboration.vo.CustomerAcceptanceTaskVO;
import com.vibe.collaboration.vo.CustomerCutoverPlanVO;
import com.vibe.collaboration.vo.CustomerMessageVO;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.CustomerTodoVO;
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
import org.springframework.transaction.annotation.Transactional;
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

    /* ============ 3.2 割接审批 ============ */

    @Override
    public CustomerCutoverPlanVO getCutoverPlanByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw BusinessException.of(ResultCode.PARAM_MISSING);
        }
        CustomerCutoverPlanVO plan = customerPortalMapper.selectCutoverPlanByToken(token);
        if (plan == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        // 拉取步骤列表（脱敏）
        List<CustomerCutoverPlanVO.CustomerCutoverStepVO> steps =
                customerPortalMapper.selectCutoverStepsByPlanId(plan.getId());
        plan.setSteps(steps == null ? Collections.emptyList() : steps);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitCutoverApproval(CustomerCutoverApprovalDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getToken()) || !StringUtils.hasText(dto.getResult())) {
            throw BusinessException.of(ResultCode.PARAM_MISSING);
        }
        // 校验 result 取值
        String result = dto.getResult().toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(result) && !"REJECTED".equals(result)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "审批结果仅支持 APPROVED/REJECTED");
        }
        // 通过 token 查方案
        CustomerCutoverPlanVO plan = customerPortalMapper.selectCutoverPlanByToken(dto.getToken());
        if (plan == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        // 数据隔离：校验方案所在项目归属当前客户（已登录态）
        Long customerId = requireCurrentCustomerId();
        Long projectCustomerId = customerPortalMapper.selectCustomerIdByProjectId(plan.getProjectId());
        if (projectCustomerId == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!customerId.equals(projectCustomerId)) {
            throw BusinessException.of(ResultCode.DATA_PERMISSION_DENIED);
        }
        // 计算新状态
        String newStatus = "APPROVED".equals(result) ? "CUSTOMER_APPROVED" : "CUSTOMER_REJECTED";
        LocalDateTime now = LocalDateTime.now();
        String signUser = StringUtils.hasText(dto.getSignUser()) ? dto.getSignUser() : resolveCurrentCustomerName();
        int affected = customerPortalMapper.updateCutoverPlanCustomerApproval(
                plan.getId(), result, signUser, dto.getRemark(), now, newStatus);
        if (affected == 0) {
            throw BusinessException.conflict("割接方案状态已变更，请刷新后重试");
        }
        log.info("[CustomerPortal] 客户 {} 提交割接方案 {} 审批结果: {}", customerId, plan.getId(), result);
    }

    /* ============ 3.3 验收签核 ============ */

    @Override
    public CustomerAcceptanceTaskVO getAcceptanceTaskByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw BusinessException.of(ResultCode.PARAM_MISSING);
        }
        CustomerAcceptanceTaskVO task = customerPortalMapper.selectAcceptanceTaskByToken(token);
        if (task == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        List<CustomerAcceptanceTaskVO.CustomerTestRecordVO> records =
                customerPortalMapper.selectAcceptanceTestRecords(task.getId());
        task.setTestRecords(records == null ? Collections.emptyList() : records);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAcceptanceSign(CustomerAcceptanceSignDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getToken()) || !StringUtils.hasText(dto.getResult())) {
            throw BusinessException.of(ResultCode.PARAM_MISSING);
        }
        String result = dto.getResult().toUpperCase(Locale.ROOT);
        if (!"PASS".equals(result) && !"CONDITIONAL_PASS".equals(result) && !"REJECT".equals(result)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "签核结果仅支持 PASS/CONDITIONAL_PASS/REJECT");
        }
        CustomerAcceptanceTaskVO task = customerPortalMapper.selectAcceptanceTaskByToken(dto.getToken());
        if (task == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        // 数据隔离
        Long customerId = requireCurrentCustomerId();
        Long projectCustomerId = customerPortalMapper.selectCustomerIdByProjectId(task.getProjectId());
        if (projectCustomerId == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        if (!customerId.equals(projectCustomerId)) {
            throw BusinessException.of(ResultCode.DATA_PERMISSION_DENIED);
        }
        // 计算新状态：PASS/CONDITIONAL_PASS → COMPLETED；REJECT → REJECTED
        String newStatus = "REJECT".equals(result) ? "REJECTED" : "COMPLETED";
        LocalDateTime now = LocalDateTime.now();
        String signUser = StringUtils.hasText(dto.getSignUser()) ? dto.getSignUser() : resolveCurrentCustomerName();
        int affected = customerPortalMapper.updateAcceptanceTaskCustomerSign(
                task.getId(), result, signUser, dto.getRemark(), now, newStatus);
        if (affected == 0) {
            throw BusinessException.conflict("验收任务状态已变更，请刷新后重试");
        }
        log.info("[CustomerPortal] 客户 {} 提交验收任务 {} 签核结果: {}", customerId, task.getId(), result);
    }

    /* ============ 3.5 消息通知 ============ */

    @Override
    public List<CustomerMessageVO> getMyMessages() {
        Long customerId = requireCurrentCustomerId();
        List<CustomerMessageVO> msgs = customerPortalMapper.selectCustomerMessages(customerId);
        return msgs == null ? Collections.emptyList() : msgs;
    }

    @Override
    public int countUnreadMessages() {
        Long customerId = requireCurrentCustomerId();
        return customerPortalMapper.countUnreadMessages(customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessageRead(Long messageId) {
        if (messageId == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING);
        }
        Long customerId = requireCurrentCustomerId();
        customerPortalMapper.markMessageRead(messageId, customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllMessagesRead() {
        Long customerId = requireCurrentCustomerId();
        customerPortalMapper.markAllMessagesRead(customerId);
    }

    /* ============ 待办列表（聚合） ============ */

    @Override
    public List<CustomerTodoVO> getMyTodos() {
        Long customerId = requireCurrentCustomerId();
        List<CustomerProjectVO> projects = customerPortalMapper.selectCustomerProjects(customerId);
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyList();
        }
        List<CustomerTodoVO> todos = new ArrayList<>();
        for (CustomerProjectVO project : projects) {
            // 待审批的割接方案
            List<CustomerCutoverPlanVO> cutoverPending =
                    customerPortalMapper.selectCutoverPlansPendingApproval(project.getProjectId());
            if (cutoverPending != null) {
                for (CustomerCutoverPlanVO plan : cutoverPending) {
                    CustomerTodoVO todo = new CustomerTodoVO();
                    todo.setType("CUTOVER_APPROVAL");
                    todo.setBusinessId(plan.getId());
                    todo.setProjectId(project.getProjectId());
                    todo.setProjectName(project.getProjectName());
                    todo.setTitle("割接方案待审批: " + plan.getPlanName());
                    // 注意：待办列表不带 token；客户需登录后点击详情查看，token 由单独的方案详情接口返回
                    todos.add(todo);
                }
            }
            // 待签核的验收任务
            List<CustomerAcceptanceTaskVO> acceptancePending =
                    customerPortalMapper.selectAcceptanceTasksPendingSign(project.getProjectId());
            if (acceptancePending != null) {
                for (CustomerAcceptanceTaskVO task : acceptancePending) {
                    CustomerTodoVO todo = new CustomerTodoVO();
                    todo.setType("ACCEPTANCE_SIGN");
                    todo.setBusinessId(task.getId());
                    todo.setProjectId(project.getProjectId());
                    todo.setProjectName(project.getProjectName());
                    todo.setTitle("验收任务待签核: " + task.getName());
                    todos.add(todo);
                }
            }
        }
        return todos;
    }

    /* ============ 私有辅助方法 ============ */

    /**
     * 从当前登录态获取客户姓名（用于自动填充 signUser）。
     */
    private String resolveCurrentCustomerName() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            return "客户";
        }
        return StringUtils.hasText(ctx.getRealName()) ? ctx.getRealName() : "客户";
    }

    /* ============ 既有方法（文档解析等） ============ */

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
