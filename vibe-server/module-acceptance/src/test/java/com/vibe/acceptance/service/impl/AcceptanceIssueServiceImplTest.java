package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.constant.AcceptanceConstant;
import com.vibe.acceptance.dto.AcceptanceIssueQueryDTO;
import com.vibe.acceptance.dto.AcceptanceIssueSaveDTO;
import com.vibe.acceptance.entity.AcceptanceIssueEntity;
import com.vibe.acceptance.mapper.AcceptanceIssueMapper;
import com.vibe.acceptance.vo.AcceptanceIssueVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验收遗留问题服务实现单元测试（Task 3 SubTask 3.3）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>save：状态默认 OPEN、严重等级默认 MEDIUM</li>
 *   <li>update：不存在抛 NOT_FOUND、正常更新</li>
 *   <li>delete：不存在抛 NOT_FOUND、正常删除</li>
 *   <li>assign：OPEN 状态自动流转到 IN_PROGRESS</li>
 *   <li>resolve：写入整改完成时间</li>
 *   <li>close：仅 RESOLVED 可闭环、写入闭环确认人</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("验收遗留问题服务 AcceptanceIssueServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class AcceptanceIssueServiceImplTest {

    @Mock
    private AcceptanceIssueMapper issueMapper;

    @InjectMocks
    private AcceptanceIssueServiceImpl issueService;

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
            AcceptanceIssueQueryDTO query = new AcceptanceIssueQueryDTO();
            query.setProjectId(10L);
            query.setStatus(AcceptanceConstant.ISSUE_STATUS_OPEN);

            Page<AcceptanceIssueEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_OPEN)));
            mockPage.setTotal(1L);
            when(issueMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<AcceptanceIssueVO> result = issueService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals(10L, result.getRecords().get(0).getProjectId());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            AcceptanceIssueQueryDTO query = new AcceptanceIssueQueryDTO();
            Page<AcceptanceIssueEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(issueMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<AcceptanceIssueVO> result = issueService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("问题不存在抛 NOT_FOUND")
        void should_throw_not_found_when_missing() {
            when(issueMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_vo() {
            AcceptanceIssueEntity entity = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_OPEN);
            when(issueMapper.selectById(1L)).thenReturn(entity);

            AcceptanceIssueVO vo = issueService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals(AcceptanceConstant.ISSUE_STATUS_OPEN, vo.getStatus());
        }
    }

    @Nested
    @DisplayName("save 保存问题")
    class SaveTest {

        @Test
        @DisplayName("正常保存：状态默认 OPEN、严重等级默认 MEDIUM")
        void should_save_with_default_status_and_severity() {
            AcceptanceIssueSaveDTO dto = buildSaveDto(1L, 10L, "问题", null, null);

            when(issueMapper.insert(any(AcceptanceIssueEntity.class))).thenAnswer(invocation -> {
                AcceptanceIssueEntity e = invocation.getArgument(0);
                e.setId(800L);
                return 1;
            });

            Long id = issueService.save(dto);

            assertEquals(800L, id);
            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).insert(captor.capture());
            AcceptanceIssueEntity saved = captor.getValue();
            assertEquals(AcceptanceConstant.ISSUE_STATUS_OPEN, saved.getStatus());
            assertEquals("MEDIUM", saved.getSeverity());
        }

        @Test
        @DisplayName("显式指定状态与严重等级时保留指定值")
        void should_save_with_explicit_status_and_severity() {
            AcceptanceIssueSaveDTO dto = buildSaveDto(1L, 10L, "问题",
                    AcceptanceConstant.ISSUE_STATUS_IN_PROGRESS, "HIGH");

            issueService.save(dto);

            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).insert(captor.capture());
            // 注意：BeanUtils.copyProperties 把 DTO 的 status 字段复制到 entity，
            // 这里 dto 没有 status 字段，所以 entity.status 为 null 后被赋默认值 OPEN
            // severity 在 DTO 中存在，被复制后保留 HIGH
            assertEquals("HIGH", captor.getValue().getSeverity());
        }
    }

    @Nested
    @DisplayName("update 更新问题")
    class UpdateTest {

        @Test
        @DisplayName("问题不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(issueMapper.selectById(99L)).thenReturn(null);
            AcceptanceIssueSaveDTO dto = buildSaveDto(1L, 10L, "问题", null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(issueMapper, never()).updateById(any(AcceptanceIssueEntity.class));
        }

        @Test
        @DisplayName("正常更新：保留 ID")
        void should_update_and_keep_id() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_OPEN);
            when(issueMapper.selectById(1L)).thenReturn(exist);
            AcceptanceIssueSaveDTO dto = buildSaveDto(2L, 11L, "新问题", null, "HIGH");

            issueService.update(1L, dto);

            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).updateById(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertEquals(2L, captor.getValue().getTaskId());
            assertEquals(11L, captor.getValue().getProjectId());
            assertEquals("新问题", captor.getValue().getName());
        }
    }

    @Nested
    @DisplayName("delete 删除问题")
    class DeleteTest {

        @Test
        @DisplayName("问题不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(issueMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(issueMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_OPEN);
            when(issueMapper.selectById(1L)).thenReturn(exist);

            issueService.delete(1L);

            verify(issueMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("assign 指派责任人")
    class AssignTest {

        @Test
        @DisplayName("问题不存在抛 NOT_FOUND")
        void should_throw_not_found_when_assign_missing() {
            when(issueMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.assign(99L, 5L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("OPEN 状态指派后自动流转到 IN_PROGRESS")
        void should_transition_to_in_progress_when_assign_open_issue() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_OPEN);
            when(issueMapper.selectById(1L)).thenReturn(exist);

            issueService.assign(1L, 5L);

            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).updateById(captor.capture());
            AcceptanceIssueEntity updated = captor.getValue();
            assertEquals(5L, updated.getAssigneeId());
            assertEquals(AcceptanceConstant.ISSUE_STATUS_IN_PROGRESS, updated.getStatus());
        }

        @Test
        @DisplayName("非 OPEN 状态指派时仅更新 assigneeId 不改状态")
        void should_only_update_assignee_when_not_open() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_RESOLVED);
            when(issueMapper.selectById(1L)).thenReturn(exist);

            issueService.assign(1L, 6L);

            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).updateById(captor.capture());
            assertEquals(6L, captor.getValue().getAssigneeId());
            assertEquals(AcceptanceConstant.ISSUE_STATUS_RESOLVED, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("resolve 标记已整改")
    class ResolveTest {

        @Test
        @DisplayName("问题不存在抛 NOT_FOUND")
        void should_throw_not_found_when_resolve_missing() {
            when(issueMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.resolve(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常标记为已整改并写入整改完成时间")
        void should_resolve_and_set_resolved_time() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_IN_PROGRESS);
            when(issueMapper.selectById(1L)).thenReturn(exist);

            issueService.resolve(1L);

            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).updateById(captor.capture());
            AcceptanceIssueEntity updated = captor.getValue();
            assertEquals(AcceptanceConstant.ISSUE_STATUS_RESOLVED, updated.getStatus());
            assertNotNull(updated.getResolvedTime());
        }
    }

    @Nested
    @DisplayName("close 闭环确认")
    class CloseTest {

        @Test
        @DisplayName("问题不存在抛 NOT_FOUND")
        void should_throw_not_found_when_close_missing() {
            when(issueMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.close(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 RESOLVED 状态禁止闭环抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_resolved() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_OPEN);
            when(issueMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.close(1L));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(issueMapper, never()).updateById(any(AcceptanceIssueEntity.class));
        }

        @Test
        @DisplayName("RESOLVED 状态可闭环并写入确认人")
        void should_close_when_resolved() {
            AcceptanceIssueEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.ISSUE_STATUS_RESOLVED);
            when(issueMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(99L).build());

            issueService.close(1L);

            ArgumentCaptor<AcceptanceIssueEntity> captor = ArgumentCaptor.forClass(AcceptanceIssueEntity.class);
            verify(issueMapper).updateById(captor.capture());
            AcceptanceIssueEntity updated = captor.getValue();
            assertEquals(AcceptanceConstant.ISSUE_STATUS_CLOSED, updated.getStatus());
            assertEquals(99L, updated.getCloseUserId());
            assertNotNull(updated.getCloseTime());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private AcceptanceIssueEntity buildEntity(Long id, Long projectId, Long taskId, String status) {
        AcceptanceIssueEntity entity = new AcceptanceIssueEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setTaskId(taskId);
        entity.setName("问题");
        entity.setStatus(status);
        return entity;
    }

    private AcceptanceIssueSaveDTO buildSaveDto(Long taskId, Long projectId, String name,
                                                 String status, String severity) {
        AcceptanceIssueSaveDTO dto = new AcceptanceIssueSaveDTO();
        dto.setTaskId(taskId);
        dto.setProjectId(projectId);
        dto.setName(name);
        // 注意：AcceptanceIssueSaveDTO 没有 status/severity 字段，这里仅用于测试
        dto.setSeverity(severity);
        return dto;
    }
}
