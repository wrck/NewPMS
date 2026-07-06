package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysFeedbackDTO;
import com.vibe.system.dto.SysFeedbackHandleDTO;
import com.vibe.system.dto.SysFeedbackQueryDTO;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.entity.SysFeedbackEntity;
import com.vibe.system.mapper.SysFeedbackMapper;
import com.vibe.system.service.SysNoticeService;
import com.vibe.system.vo.SysFeedbackVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 反馈服务实现单元测试（Task E3.1）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>submit：校验 submitterId、字段映射、状态默认 PENDING、不发送站内信</li>
 *   <li>pageAll / pageMy：分页查询、submitterId 透传、默认分页参数</li>
 *   <li>handle：状态变更、handlerId/handleNote/handleTime 写入、站内信通知（含失败兜底）</li>
 * </ul>
 *
 * <p>所有依赖均通过 Mockito 注入 Mock，不连接真实数据库。</p>
 *
 * @author vibe
 */
@DisplayName("反馈服务 SysFeedbackServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SysFeedbackServiceImplTest {

    @Mock
    private SysFeedbackMapper sysFeedbackMapper;

    @Mock
    private SysNoticeService sysNoticeService;

    @InjectMocks
    private SysFeedbackServiceImpl feedbackService;

    @Nested
    @DisplayName("submit 提交反馈")
    class SubmitTest {

        @Test
        @DisplayName("submitterId 为空抛 UNAUTHORIZED")
        void should_throw_unauthorized_when_submitter_id_null() {
            SysFeedbackDTO dto = buildDto("BUG", "标题", "内容");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> feedbackService.submit(dto, null),
                    "未登录用户提交反馈应抛 BusinessException");

            assertEquals(40100, ex.getCode(), "应为未认证错误码");
            verify(sysFeedbackMapper, never()).insert(any(SysFeedbackEntity.class));
        }

        @Test
        @DisplayName("正常提交：字段映射、状态 PENDING、返回 ID")
        void should_submit_and_return_id() {
            SysFeedbackDTO dto = buildDto("BUG", "页面打不开", "点击登录后白屏");
            dto.setScreenshotUrl("http://x/a.png");
            dto.setContact("user@vibe.com");
            when(sysFeedbackMapper.insert(any(SysFeedbackEntity.class))).thenAnswer(invocation -> {
                SysFeedbackEntity entity = invocation.getArgument(0);
                entity.setId(100L);
                return 1;
            });

            Long id = feedbackService.submit(dto, 9L);

            assertEquals(100L, id);

            ArgumentCaptor<SysFeedbackEntity> captor = ArgumentCaptor.forClass(SysFeedbackEntity.class);
            verify(sysFeedbackMapper).insert(captor.capture());
            SysFeedbackEntity saved = captor.getValue();
            assertAll("字段映射",
                    () -> assertEquals("BUG", saved.getType()),
                    () -> assertEquals("页面打不开", saved.getTitle()),
                    () -> assertEquals("点击登录后白屏", saved.getContent()),
                    () -> assertEquals("http://x/a.png", saved.getScreenshotUrl()),
                    () -> assertEquals("user@vibe.com", saved.getContact()),
                    () -> assertEquals(9L, saved.getSubmitterId()),
                    () -> assertEquals(SystemConstant.FEEDBACK_STATUS_PENDING, saved.getStatus())
            );

            // 提交时不发送站内信
            verify(sysNoticeService, never()).send(any(SysNoticeDTO.class));
        }

        @Test
        @DisplayName("SUGGESTION 类型反馈可正常提交")
        void should_submit_suggestion_type() {
            SysFeedbackDTO dto = buildDto("SUGGESTION", "建议增加暗黑模式", "护眼");

            feedbackService.submit(dto, 5L);

            ArgumentCaptor<SysFeedbackEntity> captor = ArgumentCaptor.forClass(SysFeedbackEntity.class);
            verify(sysFeedbackMapper).insert(captor.capture());
            assertEquals("SUGGESTION", captor.getValue().getType());
            assertEquals(SystemConstant.FEEDBACK_STATUS_PENDING, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("QUESTION 类型反馈可正常提交")
        void should_submit_question_type() {
            SysFeedbackDTO dto = buildDto("QUESTION", "如何重置密码", "");

            feedbackService.submit(dto, 5L);

            ArgumentCaptor<SysFeedbackEntity> captor = ArgumentCaptor.forClass(SysFeedbackEntity.class);
            verify(sysFeedbackMapper).insert(captor.capture());
            assertEquals("QUESTION", captor.getValue().getType());
        }
    }

    @Nested
    @DisplayName("pageAll / pageMy 分页查询")
    class PageQueryTest {

        @Test
        @DisplayName("pageAll 调用 selectFeedbackPage（submitterId=null）")
        void should_page_all_with_null_submitter_id() {
            SysFeedbackQueryDTO query = new SysFeedbackQueryDTO();
            query.setPage(2);
            query.setSize(15);
            query.setType("BUG");

            IPage<SysFeedbackVO> mockPage = new Page<>(2, 15);
            mockPage.setRecords(Collections.singletonList(buildVo(1L)));
            mockPage.setTotal(1L);
            when(sysFeedbackMapper.selectFeedbackPage(any(IPage.class), eq(query), eq(null)))
                    .thenReturn(mockPage);

            PageResult<SysFeedbackVO> result = feedbackService.pageAll(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            // 验证传入 mapper 的 submitterId 为 null
            verify(sysFeedbackMapper).selectFeedbackPage(any(IPage.class), eq(query), eq(null));
        }

        @Test
        @DisplayName("pageMy 调用 selectFeedbackPage（submitterId=当前用户）")
        void should_page_my_with_user_id() {
            SysFeedbackQueryDTO query = new SysFeedbackQueryDTO();
            query.setPage(1);
            query.setSize(10);

            IPage<SysFeedbackVO> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Collections.singletonList(buildVo(7L)));
            mockPage.setTotal(1L);
            when(sysFeedbackMapper.selectFeedbackPage(any(IPage.class), eq(query), eq(99L)))
                    .thenReturn(mockPage);

            PageResult<SysFeedbackVO> result = feedbackService.pageMy(query, 99L);

            assertNotNull(result);
            assertEquals(7L, result.getRecords().get(0).getId());
            verify(sysFeedbackMapper).selectFeedbackPage(any(IPage.class), eq(query), eq(99L));
        }

        @Test
        @DisplayName("page 参数为空时默认 1/20")
        void should_use_default_page_size_when_null() {
            SysFeedbackQueryDTO query = new SysFeedbackQueryDTO();
            query.setPage(null);
            query.setSize(null);

            IPage<SysFeedbackVO> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(sysFeedbackMapper.selectFeedbackPage(any(IPage.class), eq(query), anyLong()))
                    .thenReturn(mockPage);

            PageResult<SysFeedbackVO> result = feedbackService.pageMy(query, 1L);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
        }
    }

    @Nested
    @DisplayName("handle 处理反馈")
    class HandleTest {

        @Test
        @DisplayName("反馈不存在抛 NOT_FOUND")
        void should_throw_not_found_when_feedback_missing() {
            when(sysFeedbackMapper.selectById(99L)).thenReturn(null);
            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("RESOLVED");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> feedbackService.handle(99L, dto, 1L),
                    "反馈不存在应抛 BusinessException");

            assertEquals(40400, ex.getCode());
            verify(sysFeedbackMapper, never()).updateById(any(SysFeedbackEntity.class));
        }

        @Test
        @DisplayName("处理为 RESOLVED：写入状态/handlerId/handleNote/handleTime 并通知提交人")
        void should_handle_resolved_and_notify_submitter() {
            SysFeedbackEntity exist = buildEntity(5L, "BUG", "标题", 8L);
            when(sysFeedbackMapper.selectById(5L)).thenReturn(exist);
            when(sysFeedbackMapper.updateById(any(SysFeedbackEntity.class))).thenReturn(1);

            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("RESOLVED");
            dto.setHandleNote("已修复，请升级到 v1.2");

            feedbackService.handle(5L, dto, 1L);

            ArgumentCaptor<SysFeedbackEntity> captor = ArgumentCaptor.forClass(SysFeedbackEntity.class);
            verify(sysFeedbackMapper).updateById(captor.capture());
            SysFeedbackEntity updated = captor.getValue();
            assertAll("状态变更字段",
                    () -> assertEquals("RESOLVED", updated.getStatus()),
                    () -> assertEquals(1L, updated.getHandlerId()),
                    () -> assertEquals("已修复，请升级到 v1.2", updated.getHandleNote()),
                    () -> assertNotNull(updated.getHandleTime(), "处理时间应被设置")
            );

            // 验证站内信发送
            ArgumentCaptor<SysNoticeDTO> noticeCaptor = ArgumentCaptor.forClass(SysNoticeDTO.class);
            verify(sysNoticeService).send(noticeCaptor.capture());
            SysNoticeDTO notice = noticeCaptor.getValue();
            assertEquals("反馈处理进度更新：「标题」", notice.getNoticeTitle());
            assertEquals(SystemConstant.NOTICE_TYPE_MSG, notice.getNoticeType());
            assertEquals(8L, notice.getRecipientId());
            assertNotNull(notice.getNoticeContent());
        }

        @Test
        @DisplayName("PROCESSING 状态站内信内容包含「处理中」与处理备注")
        void should_notice_content_contain_processing_label() {
            SysFeedbackEntity exist = buildEntity(6L, "BUG", "登录白屏", 8L);
            when(sysFeedbackMapper.selectById(6L)).thenReturn(exist);
            when(sysFeedbackMapper.updateById(any(SysFeedbackEntity.class))).thenReturn(1);

            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("PROCESSING");
            dto.setHandleNote("排查中");

            feedbackService.handle(6L, dto, 2L);

            ArgumentCaptor<SysNoticeDTO> captor = ArgumentCaptor.forClass(SysNoticeDTO.class);
            verify(sysNoticeService).send(captor.capture());
            String content = captor.getValue().getNoticeContent();
            assertTrue(content.contains("处理中"),
                    "PROCESSING 状态通知内容应包含「处理中」：" + content);
            assertTrue(content.contains("排查中"),
                    "通知内容应包含处理备注：" + content);
        }

        @Test
        @DisplayName("CLOSED 状态站内信内容包含「已关闭」")
        void should_notice_content_contain_closed_label() {
            SysFeedbackEntity exist = buildEntity(7L, "BUG", "登录白屏", 8L);
            when(sysFeedbackMapper.selectById(7L)).thenReturn(exist);
            when(sysFeedbackMapper.updateById(any(SysFeedbackEntity.class))).thenReturn(1);

            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("CLOSED");
            dto.setHandleNote("");

            feedbackService.handle(7L, dto, 3L);

            ArgumentCaptor<SysNoticeDTO> captor = ArgumentCaptor.forClass(SysNoticeDTO.class);
            verify(sysNoticeService).send(captor.capture());
            String content = captor.getValue().getNoticeContent();
            assertTrue(content.contains("已关闭"),
                    "CLOSED 状态通知内容应包含「已关闭」：" + content);
        }

        @Test
        @DisplayName("未知状态站内信内容回退到原始状态码")
        void should_notice_content_fallback_to_raw_status_for_unknown() {
            SysFeedbackEntity exist = buildEntity(8L, "BUG", "X", 8L);
            when(sysFeedbackMapper.selectById(8L)).thenReturn(exist);
            when(sysFeedbackMapper.updateById(any(SysFeedbackEntity.class))).thenReturn(1);

            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("UNKNOWN_STATUS");

            feedbackService.handle(8L, dto, 4L);

            ArgumentCaptor<SysNoticeDTO> captor = ArgumentCaptor.forClass(SysNoticeDTO.class);
            verify(sysNoticeService).send(captor.capture());
            String content = captor.getValue().getNoticeContent();
            assertTrue(content.contains("UNKNOWN_STATUS"),
                    "未知状态应回退到原始状态码：" + content);
        }

        @Test
        @DisplayName("站内信发送失败不影响主流程")
        void should_not_fail_when_notice_send_throws() {
            SysFeedbackEntity exist = buildEntity(9L, "BUG", "标题", 8L);
            when(sysFeedbackMapper.selectById(9L)).thenReturn(exist);
            when(sysFeedbackMapper.updateById(any(SysFeedbackEntity.class))).thenReturn(1);
            doThrow(new RuntimeException("MQ 不可用")).when(sysNoticeService).send(any(SysNoticeDTO.class));

            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("RESOLVED");
            dto.setHandleNote("已修复");

            // 不应抛错
            feedbackService.handle(9L, dto, 1L);

            // 主流程的 updateById 应已执行
            verify(sysFeedbackMapper).updateById(any(SysFeedbackEntity.class));
        }

        @Test
        @DisplayName("handleNote 为空时站内信内容不含「处理备注」")
        void should_notice_not_contain_handle_note_when_empty() {
            SysFeedbackEntity exist = buildEntity(10L, "BUG", "标题", 8L);
            when(sysFeedbackMapper.selectById(10L)).thenReturn(exist);
            when(sysFeedbackMapper.updateById(any(SysFeedbackEntity.class))).thenReturn(1);

            SysFeedbackHandleDTO dto = new SysFeedbackHandleDTO();
            dto.setStatus("RESOLVED");
            dto.setHandleNote(null);

            feedbackService.handle(10L, dto, 1L);

            ArgumentCaptor<SysNoticeDTO> captor = ArgumentCaptor.forClass(SysNoticeDTO.class);
            verify(sysNoticeService).send(captor.capture());
            String content = captor.getValue().getNoticeContent();
            assertFalse(content.contains("处理备注"),
                    "handleNote 为空时通知内容不应包含「处理备注」：" + content);
        }
    }

    /* ============ 测试辅助方法 ============ */

    private SysFeedbackDTO buildDto(String type, String title, String content) {
        SysFeedbackDTO dto = new SysFeedbackDTO();
        dto.setType(type);
        dto.setTitle(title);
        dto.setContent(content);
        return dto;
    }

    private SysFeedbackEntity buildEntity(Long id, String type, String title, Long submitterId) {
        SysFeedbackEntity entity = new SysFeedbackEntity();
        entity.setId(id);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent("内容");
        entity.setSubmitterId(submitterId);
        entity.setStatus(SystemConstant.FEEDBACK_STATUS_PENDING);
        return entity;
    }

    private SysFeedbackVO buildVo(Long id) {
        SysFeedbackVO vo = new SysFeedbackVO();
        vo.setId(id);
        vo.setTitle("反馈" + id);
        vo.setStatus(SystemConstant.FEEDBACK_STATUS_PENDING);
        return vo;
    }
}
