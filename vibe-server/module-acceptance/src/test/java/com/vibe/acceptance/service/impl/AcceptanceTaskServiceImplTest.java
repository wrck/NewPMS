package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.constant.AcceptanceConstant;
import com.vibe.acceptance.dto.AcceptanceTaskActionDTO;
import com.vibe.acceptance.dto.AcceptanceTaskCreateDTO;
import com.vibe.acceptance.dto.AcceptanceTaskQueryDTO;
import com.vibe.acceptance.entity.AcceptanceTaskEntity;
import com.vibe.acceptance.entity.AcceptanceTestRecordEntity;
import com.vibe.acceptance.mapper.AcceptanceTaskMapper;
import com.vibe.acceptance.mapper.AcceptanceTestRecordMapper;
import com.vibe.acceptance.vo.AcceptanceTaskVO;
import com.vibe.acceptance.vo.AcceptanceTestRecordVO;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验收任务服务实现单元测试（Task 3 SubTask 3.3）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>create：写入 DRAFT 状态</li>
 *   <li>update：不存在 / 非草稿状态禁止修改 / 正常更新</li>
 *   <li>delete：不存在 / 非草稿状态禁止删除 / 正常删除</li>
 *   <li>apply：DRAFT → APPLIED，写入申请人与时间</li>
 *   <li>internalAudit：APPLIED → INTERNAL_AUDITED/REJECTED，result 非法抛 400</li>
 *   <li>startCustomerSign：INTERNAL_AUDITED → CUSTOMER_SIGNING，生成签核链接</li>
 *   <li>customerSign：CUSTOMER_SIGNING → COMPLETED/REJECTED，result 非法抛 400</li>
 *   <li>listTestRecords：按 taskId 查询测试记录</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("验收任务服务 AcceptanceTaskServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class AcceptanceTaskServiceImplTest {

    @Mock
    private AcceptanceTaskMapper taskMapper;
    @Mock
    private AcceptanceTestRecordMapper testRecordMapper;

    @InjectMocks
    private AcceptanceTaskServiceImpl taskService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Nested
    @DisplayName("page 分页查询")
    class PageTest {

        @Test
        @DisplayName("正常分页：返回 records 与 total")
        void should_return_paged_result() {
            AcceptanceTaskQueryDTO query = new AcceptanceTaskQueryDTO();
            query.setProjectId(10L);
            query.setStatus(AcceptanceConstant.TASK_STATUS_DRAFT);

            Page<AcceptanceTaskEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT)));
            mockPage.setTotal(1L);
            when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<AcceptanceTaskVO> result = taskService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals(10L, result.getRecords().get(0).getProjectId());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            AcceptanceTaskQueryDTO query = new AcceptanceTaskQueryDTO();
            Page<AcceptanceTaskEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(taskMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<AcceptanceTaskVO> result = taskService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_vo() {
            AcceptanceTaskEntity entity = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(entity);

            AcceptanceTaskVO vo = taskService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals(10L, vo.getProjectId());
        }
    }

    @Nested
    @DisplayName("create 创建任务")
    class CreateTest {

        @Test
        @DisplayName("正常创建：状态 DRAFT")
        void should_create_with_draft_status() {
            AcceptanceTaskCreateDTO dto = new AcceptanceTaskCreateDTO();
            dto.setProjectId(10L);
            dto.setName("初验任务");
            dto.setRemark("备注");

            when(taskMapper.insert(any(AcceptanceTaskEntity.class))).thenAnswer(invocation -> {
                AcceptanceTaskEntity e = invocation.getArgument(0);
                e.setId(500L);
                return 1;
            });

            Long id = taskService.create(dto);

            assertEquals(500L, id);
            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).insert(captor.capture());
            assertEquals(AcceptanceConstant.TASK_STATUS_DRAFT, captor.getValue().getStatus());
            assertEquals("初验任务", captor.getValue().getName());
        }
    }

    @Nested
    @DisplayName("update 更新任务")
    class UpdateTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);
            AcceptanceTaskCreateDTO dto = new AcceptanceTaskCreateDTO();
            dto.setProjectId(10L);
            dto.setName("xx");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("非草稿状态禁止修改抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_draft() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_APPLIED);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskCreateDTO dto = new AcceptanceTaskCreateDTO();
            dto.setProjectId(10L);
            dto.setName("xx");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.update(1L, dto));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("草稿状态可正常更新")
        void should_update_when_draft() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskCreateDTO dto = new AcceptanceTaskCreateDTO();
            dto.setProjectId(11L);
            dto.setName("新名称");

            taskService.update(1L, dto);

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertEquals(11L, captor.getValue().getProjectId());
            assertEquals("新名称", captor.getValue().getName());
        }
    }

    @Nested
    @DisplayName("delete 删除任务")
    class DeleteTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(taskMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("非草稿状态禁止删除抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_draft() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_APPLIED);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.delete(1L));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(taskMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("草稿状态可正常删除")
        void should_delete_when_draft() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            taskService.delete(1L);

            verify(taskMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("apply 申请验收")
    class ApplyTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_apply_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);
            AcceptanceTaskActionDTO dto = buildActionDto(99L, null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.apply(dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非草稿状态禁止申请抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_draft() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_APPLIED);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskActionDTO dto = buildActionDto(1L, null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.apply(dto));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("草稿状态申请后变 APPLIED 并写入申请人")
        void should_transition_to_applied() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(33L).build());

            taskService.apply(buildActionDto(1L, null, "申请备注"));

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            AcceptanceTaskEntity updated = captor.getValue();
            assertEquals(AcceptanceConstant.TASK_STATUS_APPLIED, updated.getStatus());
            assertEquals(33L, updated.getApplyUserId());
            assertNotNull(updated.getApplyTime());
            assertEquals("申请备注", updated.getRemark());
        }
    }

    @Nested
    @DisplayName("internalAudit 内部审核")
    class InternalAuditTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_audit_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);
            AcceptanceTaskActionDTO dto = buildActionDto(99L, "PASS", null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.internalAudit(dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 APPLIED 状态禁止审核抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_applied() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskActionDTO dto = buildActionDto(1L, "PASS", null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.internalAudit(dto));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("result 非法抛 400 错误")
        void should_throw_when_result_invalid() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_APPLIED);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskActionDTO dto = buildActionDto(1L, "INVALID", null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.internalAudit(dto));
            assertEquals(400, ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("PASS：状态变 INTERNAL_AUDITED")
        void should_transition_to_internal_audited_when_pass() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_APPLIED);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(44L).build());

            taskService.internalAudit(buildActionDto(1L, "PASS", null));

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            AcceptanceTaskEntity updated = captor.getValue();
            assertEquals(AcceptanceConstant.TASK_STATUS_INTERNAL_AUDITED, updated.getStatus());
            assertEquals("PASS", updated.getInternalAuditResult());
            assertEquals(44L, updated.getInternalAuditUserId());
        }

        @Test
        @DisplayName("REJECT：状态变 REJECTED")
        void should_transition_to_rejected_when_reject() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_APPLIED);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            taskService.internalAudit(buildActionDto(1L, "REJECT", null));

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            assertEquals(AcceptanceConstant.TASK_STATUS_REJECTED, captor.getValue().getStatus());
            assertEquals("REJECT", captor.getValue().getInternalAuditResult());
        }
    }

    @Nested
    @DisplayName("startCustomerSign 发起客户签核")
    class StartCustomerSignTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_start_sign_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.startCustomerSign(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 INTERNAL_AUDITED 状态禁止发起签核")
        void should_throw_state_not_allowed_when_not_audited() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.startCustomerSign(1L));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("INTERNAL_AUDITED 状态可发起签核并生成链接")
        void should_start_customer_sign_and_generate_link() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_INTERNAL_AUDITED);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            taskService.startCustomerSign(1L);

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            AcceptanceTaskEntity updated = captor.getValue();
            assertEquals(AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING, updated.getStatus());
            assertNotNull(updated.getCustomerSignLink());
            assertTrue(updated.getCustomerSignLink().startsWith("acceptance-sign-1-"));
        }
    }

    @Nested
    @DisplayName("customerSign 客户签核")
    class CustomerSignTest {

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void should_throw_not_found_when_sign_missing() {
            when(taskMapper.selectById(99L)).thenReturn(null);
            AcceptanceTaskActionDTO dto = buildActionDto(99L, "PASS", null);
            dto.setCustomerSignUser("张三");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.customerSign(dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 CUSTOMER_SIGNING 状态禁止签核")
        void should_throw_state_not_allowed_when_not_signing() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_DRAFT);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskActionDTO dto = buildActionDto(1L, "PASS", null);
            dto.setCustomerSignUser("张三");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.customerSign(dto));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(taskMapper, never()).updateById(any(AcceptanceTaskEntity.class));
        }

        @Test
        @DisplayName("result 非法抛 400 错误")
        void should_throw_when_result_invalid() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING);
            when(taskMapper.selectById(1L)).thenReturn(exist);
            AcceptanceTaskActionDTO dto = buildActionDto(1L, "UNKNOWN", null);
            dto.setCustomerSignUser("张三");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> taskService.customerSign(dto));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("PASS：状态变 COMPLETED")
        void should_transition_to_completed_when_pass() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            taskService.customerSign(buildActionDto(1L, "PASS", null, "张三"));

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            AcceptanceTaskEntity updated = captor.getValue();
            assertEquals(AcceptanceConstant.TASK_STATUS_COMPLETED, updated.getStatus());
            assertEquals("PASS", updated.getCustomerSignResult());
            assertEquals("张三", updated.getCustomerSignUser());
            assertNotNull(updated.getCustomerSignTime());
        }

        @Test
        @DisplayName("CONDITIONAL_PASS：状态变 COMPLETED")
        void should_transition_to_completed_when_conditional_pass() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            taskService.customerSign(buildActionDto(1L, "CONDITIONAL_PASS", null, "李四"));

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            assertEquals(AcceptanceConstant.TASK_STATUS_COMPLETED, captor.getValue().getStatus());
            assertEquals("CONDITIONAL_PASS", captor.getValue().getCustomerSignResult());
        }

        @Test
        @DisplayName("REJECT：状态变 REJECTED")
        void should_transition_to_rejected_when_reject() {
            AcceptanceTaskEntity exist = buildEntity(1L, 10L, AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING);
            when(taskMapper.selectById(1L)).thenReturn(exist);

            taskService.customerSign(buildActionDto(1L, "REJECT", null, "王五"));

            ArgumentCaptor<AcceptanceTaskEntity> captor = ArgumentCaptor.forClass(AcceptanceTaskEntity.class);
            verify(taskMapper).updateById(captor.capture());
            assertEquals(AcceptanceConstant.TASK_STATUS_REJECTED, captor.getValue().getStatus());
            assertEquals("REJECT", captor.getValue().getCustomerSignResult());
        }
    }

    @Nested
    @DisplayName("listTestRecords 查询测试记录")
    class ListTestRecordsTest {

        @Test
        @DisplayName("正常返回测试记录列表")
        void should_return_test_records() {
            AcceptanceTestRecordEntity record = new AcceptanceTestRecordEntity();
            record.setId(1L);
            record.setTaskId(10L);
            record.setTestName("连通性测试");
            record.setTestResult("PASS");
            when(testRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            List<AcceptanceTestRecordVO> result = taskService.listTestRecords(10L);

            assertEquals(1, result.size());
            assertEquals("连通性测试", result.get(0).getTestName());
            assertEquals("PASS", result.get(0).getTestResult());
        }

        @Test
        @DisplayName("无测试记录时返回空列表")
        void should_return_empty_when_no_records() {
            when(testRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<AcceptanceTestRecordVO> result = taskService.listTestRecords(99L);

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private AcceptanceTaskEntity buildEntity(Long id, Long projectId, String status) {
        AcceptanceTaskEntity entity = new AcceptanceTaskEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setName("验收任务");
        entity.setStatus(status);
        return entity;
    }

    private AcceptanceTaskActionDTO buildActionDto(Long taskId, String result, String remark) {
        AcceptanceTaskActionDTO dto = new AcceptanceTaskActionDTO();
        dto.setTaskId(taskId);
        dto.setResult(result);
        dto.setRemark(remark);
        return dto;
    }

    private AcceptanceTaskActionDTO buildActionDto(Long taskId, String result, String remark, String customerSignUser) {
        AcceptanceTaskActionDTO dto = buildActionDto(taskId, result, remark);
        dto.setCustomerSignUser(customerSignUser);
        return dto;
    }
}
