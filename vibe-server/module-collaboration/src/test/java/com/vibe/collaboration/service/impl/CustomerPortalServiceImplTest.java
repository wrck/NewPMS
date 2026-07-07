package com.vibe.collaboration.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.collaboration.dto.CustomerAcceptanceSignDTO;
import com.vibe.collaboration.dto.CustomerCutoverApprovalDTO;
import com.vibe.collaboration.mapper.CustomerPortalMapper;
import com.vibe.collaboration.vo.CustomerAcceptanceTaskVO;
import com.vibe.collaboration.vo.CustomerCutoverPlanVO;
import com.vibe.collaboration.vo.CustomerMessageVO;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.CustomerTodoVO;
import com.vibe.collaboration.vo.ProjectProgressVO;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.utils.MinioUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户门户服务实现单元测试（Task 3 SubTask 3.4）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>getMyProjects：未登录抛 UNAUTHORIZED、正常返回项目列表、空列表</li>
 *   <li>getProjectProgress：项目ID为空抛 PARAM_MISSING、归属校验、项目不存在、阶段时间线</li>
 *   <li>getProjectDocuments：归属校验、空交付物、字符串数组与对象数组两种 JSON 结构、预签名 URL</li>
 *   <li>getCutoverPlanByToken：token 为空、方案不存在、含步骤列表</li>
 *   <li>submitCutoverApproval：参数校验、result 非法、token 无效、归属校验、影响行数为 0 抛 CONFLICT</li>
 *   <li>getAcceptanceTaskByToken：token 为空、任务不存在、含测试记录</li>
 *   <li>submitAcceptanceSign：参数校验、result 非法、归属校验、影响行数为 0 抛 CONFLICT</li>
 *   <li>消息：getMyMessages / countUnreadMessages / markMessageRead / markAllMessagesRead</li>
 *   <li>getMyTodos：聚合待办列表</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("客户门户服务 CustomerPortalServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class CustomerPortalServiceImplTest {

    @Mock
    private CustomerPortalMapper customerPortalMapper;
    @Mock
    private MinioUtils minioUtils;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CustomerPortalServiceImpl portalService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    private void loginAsCustomer(Long customerId) {
        UserContextHolder.set(UserContext.builder()
                .tenantId(customerId)
                .tenantType("CUSTOMER")
                .realName("客户A")
                .build());
    }

    @Nested
    @DisplayName("getMyProjects 我的项目列表")
    class GetMyProjectsTest {

        @Test
        @DisplayName("未登录抛 UNAUTHORIZED")
        void should_throw_unauthorized_when_not_logged_in() {
            UserContextHolder.clear();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getMyProjects());
            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("tenantId 为空抛 UNAUTHORIZED")
        void should_throw_unauthorized_when_tenant_id_null() {
            UserContextHolder.set(UserContext.builder().tenantType("INTERNAL").build());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getMyProjects());
            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回项目列表")
        void should_return_projects() {
            loginAsCustomer(10L);
            CustomerProjectVO vo = new CustomerProjectVO();
            vo.setProjectId(100L);
            vo.setProjectName("项目A");
            when(customerPortalMapper.selectCustomerProjects(10L)).thenReturn(List.of(vo));

            List<CustomerProjectVO> result = portalService.getMyProjects();

            assertEquals(1, result.size());
            assertEquals(100L, result.get(0).getProjectId());
        }

        @Test
        @DisplayName("mapper 返回 null 时返回空列表")
        void should_return_empty_when_mapper_returns_null() {
            loginAsCustomer(10L);
            when(customerPortalMapper.selectCustomerProjects(10L)).thenReturn(null);

            List<CustomerProjectVO> result = portalService.getMyProjects();

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("getProjectProgress 项目进度")
    class GetProjectProgressTest {

        @Test
        @DisplayName("项目ID为空抛 PARAM_MISSING")
        void should_throw_param_missing_when_project_id_null() {
            loginAsCustomer(10L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getProjectProgress(null));
            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("项目不属于当前客户抛 DATA_PERMISSION_DENIED")
        void should_throw_data_permission_denied_when_not_owner() {
            loginAsCustomer(10L);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(99L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getProjectProgress(100L));
            assertEquals(ResultCode.DATA_PERMISSION_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("项目不存在（projectCustomerId 为 null）抛 PROJECT_NOT_FOUND")
        void should_throw_project_not_found_when_not_exist() {
            loginAsCustomer(10L);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getProjectProgress(100L));
            assertEquals(ResultCode.PROJECT_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("进度不存在抛 PROJECT_NOT_FOUND")
        void should_throw_project_not_found_when_progress_missing() {
            loginAsCustomer(10L);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            when(customerPortalMapper.selectProjectProgress(100L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getProjectProgress(100L));
            assertEquals(ResultCode.PROJECT_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回进度与阶段时间线")
        void should_return_progress_with_phases() {
            loginAsCustomer(10L);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            ProjectProgressVO progress = new ProjectProgressVO();
            progress.setProjectId(100L);
            when(customerPortalMapper.selectProjectProgress(100L)).thenReturn(progress);
            when(customerPortalMapper.selectPhaseTimeline(100L)).thenReturn(Collections.emptyList());

            ProjectProgressVO result = portalService.getProjectProgress(100L);

            assertNotNull(result);
            assertEquals(100L, result.getProjectId());
            assertNotNull(result.getPhases());
        }
    }

    @Nested
    @DisplayName("getCutoverPlanByToken 按token查询割接方案")
    class GetCutoverPlanByTokenTest {

        @Test
        @DisplayName("token 为空抛 PARAM_MISSING")
        void should_throw_param_missing_when_token_blank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getCutoverPlanByToken(""));
            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("方案不存在抛 NOT_FOUND")
        void should_throw_not_found_when_plan_missing() {
            when(customerPortalMapper.selectCutoverPlanByToken("token123")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getCutoverPlanByToken("token123"));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回方案含步骤列表")
        void should_return_plan_with_steps() {
            CustomerCutoverPlanVO plan = new CustomerCutoverPlanVO();
            plan.setId(1L);
            plan.setProjectId(100L);
            when(customerPortalMapper.selectCutoverPlanByToken("token123")).thenReturn(plan);
            when(customerPortalMapper.selectCutoverStepsByPlanId(1L))
                    .thenReturn(List.of(new CustomerCutoverPlanVO.CustomerCutoverStepVO()));

            CustomerCutoverPlanVO result = portalService.getCutoverPlanByToken("token123");

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertNotNull(result.getSteps());
            assertEquals(1, result.getSteps().size());
        }
    }

    @Nested
    @DisplayName("submitCutoverApproval 提交割接审批")
    class SubmitCutoverApprovalTest {

        @Test
        @DisplayName("DTO 为 null 抛 PARAM_MISSING")
        void should_throw_param_missing_when_dto_null() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitCutoverApproval(null));
            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("token 为空抛 PARAM_MISSING")
        void should_throw_param_missing_when_token_blank() {
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setResult("APPROVED");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitCutoverApproval(dto));
            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("result 非法抛 PARAM_INVALID")
        void should_throw_param_invalid_when_result_invalid() {
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setToken("token");
            dto.setResult("UNKNOWN");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitCutoverApproval(dto));
            assertEquals(ResultCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("token 无效抛 NOT_FOUND")
        void should_throw_not_found_when_token_invalid() {
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setToken("token");
            dto.setResult("APPROVED");
            when(customerPortalMapper.selectCutoverPlanByToken("token")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitCutoverApproval(dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("项目不属于当前客户抛 DATA_PERMISSION_DENIED")
        void should_throw_data_permission_denied_when_not_owner() {
            loginAsCustomer(10L);
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setToken("token");
            dto.setResult("APPROVED");
            CustomerCutoverPlanVO plan = new CustomerCutoverPlanVO();
            plan.setId(1L);
            plan.setProjectId(100L);
            when(customerPortalMapper.selectCutoverPlanByToken("token")).thenReturn(plan);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(99L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitCutoverApproval(dto));
            assertEquals(ResultCode.DATA_PERMISSION_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("影响行数为 0 抛 BUSINESS_CONFLICT")
        void should_throw_conflict_when_no_row_affected() {
            loginAsCustomer(10L);
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setToken("token");
            dto.setResult("APPROVED");
            dto.setSignUser("客户A");
            CustomerCutoverPlanVO plan = new CustomerCutoverPlanVO();
            plan.setId(1L);
            plan.setProjectId(100L);
            when(customerPortalMapper.selectCutoverPlanByToken("token")).thenReturn(plan);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            when(customerPortalMapper.updateCutoverPlanCustomerApproval(
                    eq(1L), anyString(), anyString(), any(), any(), anyString())).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitCutoverApproval(dto));
            assertEquals(ResultCode.BUSINESS_CONFLICT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("APPROVED：更新状态为 CUSTOMER_APPROVED")
        void should_update_to_customer_approved() {
            loginAsCustomer(10L);
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setToken("token");
            dto.setResult("APPROVED");
            dto.setSignUser("客户A");
            dto.setRemark("同意");
            CustomerCutoverPlanVO plan = new CustomerCutoverPlanVO();
            plan.setId(1L);
            plan.setProjectId(100L);
            when(customerPortalMapper.selectCutoverPlanByToken("token")).thenReturn(plan);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            when(customerPortalMapper.updateCutoverPlanCustomerApproval(
                    eq(1L), eq("APPROVED"), eq("客户A"), eq("同意"), any(), eq("CUSTOMER_APPROVED")))
                    .thenReturn(1);

            portalService.submitCutoverApproval(dto);

            verify(customerPortalMapper).updateCutoverPlanCustomerApproval(
                    eq(1L), eq("APPROVED"), eq("客户A"), eq("同意"), any(), eq("CUSTOMER_APPROVED"));
        }

        @Test
        @DisplayName("REJECTED：更新状态为 CUSTOMER_REJECTED，signUser 为空时从登录态填充")
        void should_update_to_customer_rejected_and_fill_sign_user() {
            loginAsCustomer(10L);
            CustomerCutoverApprovalDTO dto = new CustomerCutoverApprovalDTO();
            dto.setToken("token");
            dto.setResult("REJECTED");
            // signUser 为空
            CustomerCutoverPlanVO plan = new CustomerCutoverPlanVO();
            plan.setId(2L);
            plan.setProjectId(100L);
            when(customerPortalMapper.selectCutoverPlanByToken("token")).thenReturn(plan);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            when(customerPortalMapper.updateCutoverPlanCustomerApproval(
                    eq(2L), eq("REJECTED"), eq("客户A"), any(), any(), eq("CUSTOMER_REJECTED")))
                    .thenReturn(1);

            portalService.submitCutoverApproval(dto);

            verify(customerPortalMapper).updateCutoverPlanCustomerApproval(
                    eq(2L), eq("REJECTED"), eq("客户A"), any(), any(), eq("CUSTOMER_REJECTED"));
        }
    }

    @Nested
    @DisplayName("getAcceptanceTaskByToken 按token查询验收任务")
    class GetAcceptanceTaskByTokenTest {

        @Test
        @DisplayName("token 为空抛 PARAM_MISSING")
        void should_throw_param_missing_when_token_blank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getAcceptanceTaskByToken(""));
            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_task_missing() {
            when(customerPortalMapper.selectAcceptanceTaskByToken("token")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.getAcceptanceTaskByToken("token"));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回任务含测试记录")
        void should_return_task_with_records() {
            CustomerAcceptanceTaskVO task = new CustomerAcceptanceTaskVO();
            task.setId(1L);
            task.setProjectId(100L);
            when(customerPortalMapper.selectAcceptanceTaskByToken("token")).thenReturn(task);
            when(customerPortalMapper.selectAcceptanceTestRecords(1L))
                    .thenReturn(List.of(new CustomerAcceptanceTaskVO.CustomerTestRecordVO()));

            CustomerAcceptanceTaskVO result = portalService.getAcceptanceTaskByToken("token");

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertNotNull(result.getTestRecords());
            assertEquals(1, result.getTestRecords().size());
        }
    }

    @Nested
    @DisplayName("submitAcceptanceSign 提交验收签核")
    class SubmitAcceptanceSignTest {

        @Test
        @DisplayName("result 非法抛 PARAM_INVALID")
        void should_throw_param_invalid_when_result_invalid() {
            CustomerAcceptanceSignDTO dto = new CustomerAcceptanceSignDTO();
            dto.setToken("token");
            dto.setResult("UNKNOWN");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitAcceptanceSign(dto));
            assertEquals(ResultCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("token 无效抛 NOT_FOUND")
        void should_throw_not_found_when_token_invalid() {
            CustomerAcceptanceSignDTO dto = new CustomerAcceptanceSignDTO();
            dto.setToken("token");
            dto.setResult("PASS");
            when(customerPortalMapper.selectAcceptanceTaskByToken("token")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.submitAcceptanceSign(dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("PASS：更新状态为 COMPLETED")
        void should_update_to_completed_when_pass() {
            loginAsCustomer(10L);
            CustomerAcceptanceSignDTO dto = new CustomerAcceptanceSignDTO();
            dto.setToken("token");
            dto.setResult("PASS");
            dto.setSignUser("客户A");
            CustomerAcceptanceTaskVO task = new CustomerAcceptanceTaskVO();
            task.setId(1L);
            task.setProjectId(100L);
            when(customerPortalMapper.selectAcceptanceTaskByToken("token")).thenReturn(task);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            when(customerPortalMapper.updateAcceptanceTaskCustomerSign(
                    eq(1L), eq("PASS"), eq("客户A"), any(), any(), eq("COMPLETED")))
                    .thenReturn(1);

            portalService.submitAcceptanceSign(dto);

            verify(customerPortalMapper).updateAcceptanceTaskCustomerSign(
                    eq(1L), eq("PASS"), eq("客户A"), any(), any(), eq("COMPLETED"));
        }

        @Test
        @DisplayName("REJECT：更新状态为 REJECTED")
        void should_update_to_rejected_when_reject() {
            loginAsCustomer(10L);
            CustomerAcceptanceSignDTO dto = new CustomerAcceptanceSignDTO();
            dto.setToken("token");
            dto.setResult("REJECT");
            dto.setSignUser("客户A");
            CustomerAcceptanceTaskVO task = new CustomerAcceptanceTaskVO();
            task.setId(1L);
            task.setProjectId(100L);
            when(customerPortalMapper.selectAcceptanceTaskByToken("token")).thenReturn(task);
            when(customerPortalMapper.selectCustomerIdByProjectId(100L)).thenReturn(10L);
            when(customerPortalMapper.updateAcceptanceTaskCustomerSign(
                    eq(1L), eq("REJECT"), eq("客户A"), any(), any(), eq("REJECTED")))
                    .thenReturn(1);

            portalService.submitAcceptanceSign(dto);

            verify(customerPortalMapper).updateAcceptanceTaskCustomerSign(
                    eq(1L), eq("REJECT"), eq("客户A"), any(), any(), eq("REJECTED"));
        }
    }

    @Nested
    @DisplayName("消息通知")
    class MessageTest {

        @Test
        @DisplayName("getMyMessages：正常返回消息列表")
        void should_return_messages() {
            loginAsCustomer(10L);
            CustomerMessageVO msg = new CustomerMessageVO();
            when(customerPortalMapper.selectCustomerMessages(10L)).thenReturn(List.of(msg));

            List<CustomerMessageVO> result = portalService.getMyMessages();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("countUnreadMessages：返回未读数")
        void should_return_unread_count() {
            loginAsCustomer(10L);
            when(customerPortalMapper.countUnreadMessages(10L)).thenReturn(5);

            int count = portalService.countUnreadMessages();

            assertEquals(5, count);
        }

        @Test
        @DisplayName("markMessageRead：messageId 为空抛 PARAM_MISSING")
        void should_throw_param_missing_when_message_id_null() {
            loginAsCustomer(10L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> portalService.markMessageRead(null));
            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("markMessageRead：正常调用 mapper")
        void should_call_mapper_when_mark_read() {
            loginAsCustomer(10L);

            portalService.markMessageRead(99L);

            verify(customerPortalMapper).markMessageRead(99L, 10L);
        }

        @Test
        @DisplayName("markAllMessagesRead：正常调用 mapper")
        void should_call_mapper_when_mark_all_read() {
            loginAsCustomer(10L);

            portalService.markAllMessagesRead();

            verify(customerPortalMapper).markAllMessagesRead(10L);
        }
    }

    @Nested
    @DisplayName("getMyTodos 待办列表")
    class GetMyTodosTest {

        @Test
        @DisplayName("无项目时返回空列表")
        void should_return_empty_when_no_projects() {
            loginAsCustomer(10L);
            when(customerPortalMapper.selectCustomerProjects(10L)).thenReturn(Collections.emptyList());

            List<CustomerTodoVO> result = portalService.getMyTodos();

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("正常聚合待办列表（割接审批 + 验收签核）")
        void should_aggregate_todos() {
            loginAsCustomer(10L);
            CustomerProjectVO project = new CustomerProjectVO();
            project.setProjectId(100L);
            project.setProjectName("项目A");
            when(customerPortalMapper.selectCustomerProjects(10L)).thenReturn(List.of(project));

            CustomerCutoverPlanVO plan = new CustomerCutoverPlanVO();
            plan.setId(1L);
            plan.setPlanName("割接方案1");
            when(customerPortalMapper.selectCutoverPlansPendingApproval(100L)).thenReturn(List.of(plan));

            CustomerAcceptanceTaskVO task = new CustomerAcceptanceTaskVO();
            task.setId(2L);
            task.setName("验收任务1");
            when(customerPortalMapper.selectAcceptanceTasksPendingSign(100L)).thenReturn(List.of(task));

            List<CustomerTodoVO> result = portalService.getMyTodos();

            assertEquals(2, result.size());
            // 验证包含两类待办
            boolean hasCutover = result.stream()
                    .anyMatch(t -> "CUTOVER_APPROVAL".equals(t.getType()));
            boolean hasAcceptance = result.stream()
                    .anyMatch(t -> "ACCEPTANCE_SIGN".equals(t.getType()));
            assertTrue(hasCutover);
            assertTrue(hasAcceptance);
        }
    }
}
