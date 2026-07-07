package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.dto.SysNoticeQueryDTO;
import com.vibe.system.entity.SysNoticeEntity;
import com.vibe.system.mapper.SysNoticeMapper;
import com.vibe.system.vo.SysNoticeVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站内信服务实现单元测试（Task 3 SubTask 3.7）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>pageMyNotices：分页查询、默认分页参数</li>
 *   <li>send：recipientId 为空校验、默认通知类型、已读状态默认 0、发送时间填充</li>
 *   <li>markRead：站内信不存在、归属校验防止越权、状态更新</li>
 *   <li>markAllRead：批量更新当前用户未读为已读</li>
 *   <li>countUnread：委托 Mapper</li>
 *   <li>delete：不存在校验、recipientId 匹配校验、recipientId 为 null 时仅校验存在性</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("站内信服务 SysNoticeServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SysNoticeServiceImplTest {

    @Mock
    private SysNoticeMapper sysNoticeMapper;

    @InjectMocks
    private SysNoticeServiceImpl noticeService;

    /**
     * 初始化 MyBatis-Plus 实体元数据缓存，避免 LambdaUpdateWrapper.set()/eq()
     * 在解析 SFunction 列名时抛出 "can not find lambda cache for this entity"。
     *
     * <p>SysNoticeServiceImpl 在 markRead / markAllRead 中使用 LambdaUpdateWrapper，
     * 而 LambdaUpdateWrapper.set() 是立即解析列名（不同于 LambdaQueryWrapper.eq() 的延迟解析），
     * 因此必须在测试启动前注册 TableInfo。</p>
     */
    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SysNoticeEntity.class);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /* ============ pageMyNotices ============ */

    @Nested
    @DisplayName("pageMyNotices 分页查询")
    class PageMyNoticesTest {

        @Test
        @DisplayName("正常分页：返回当前用户的站内信列表")
        void should_return_paged_notices_for_recipient() {
            SysNoticeQueryDTO query = new SysNoticeQueryDTO();
            query.setPage(1);
            query.setSize(10);

            SysNoticeVO vo = new SysNoticeVO();
            vo.setId(1L);
            vo.setNoticeTitle("测试通知");
            Page<SysNoticeVO> mockPage = new Page<>(1, 10);
            mockPage.setRecords(java.util.List.of(vo));
            mockPage.setTotal(1L);
            when(sysNoticeMapper.selectNoticePage(any(IPage.class), eq(query), eq(99L))).thenReturn(mockPage);

            PageResult<SysNoticeVO> result = noticeService.pageMyNotices(query, 99L);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals("测试通知", result.getRecords().get(0).getNoticeTitle());
        }

        @Test
        @DisplayName("page/size 为 null 时默认 1/20")
        void should_use_default_page_size_when_null() {
            SysNoticeQueryDTO query = new SysNoticeQueryDTO();
            Page<SysNoticeVO> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(sysNoticeMapper.selectNoticePage(any(IPage.class), eq(query), eq(1L))).thenReturn(mockPage);

            PageResult<SysNoticeVO> result = noticeService.pageMyNotices(query, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getPage());
            assertEquals(20L, result.getSize());
        }

        @Test
        @DisplayName("空结果返回空列表")
        void should_return_empty_when_no_data() {
            SysNoticeQueryDTO query = new SysNoticeQueryDTO();
            Page<SysNoticeVO> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(sysNoticeMapper.selectNoticePage(any(IPage.class), eq(query), eq(1L))).thenReturn(mockPage);

            PageResult<SysNoticeVO> result = noticeService.pageMyNotices(query, 1L);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
        }
    }

    /* ============ send ============ */

    @Nested
    @DisplayName("send 发送站内信")
    class SendTest {

        @Test
        @DisplayName("recipientId 为空抛 PARAM_MISSING")
        void should_throw_when_recipient_id_null() {
            SysNoticeDTO dto = buildDto("标题", null, 1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> noticeService.send(dto));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
            verify(sysNoticeMapper, never()).insert(any(SysNoticeEntity.class));
        }

        @Test
        @DisplayName("noticeType 为 null 时默认 1（通知）")
        void should_use_default_notice_type_when_null() {
            SysNoticeDTO dto = buildDto("标题", 10L, null);
            when(sysNoticeMapper.insert(any(SysNoticeEntity.class))).thenAnswer(invocation -> {
                SysNoticeEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            noticeService.send(dto);

            ArgumentCaptor<SysNoticeEntity> captor = ArgumentCaptor.forClass(SysNoticeEntity.class);
            verify(sysNoticeMapper).insert(captor.capture());
            assertEquals(SystemConstant.NOTICE_TYPE_NOTICE, captor.getValue().getNoticeType());
        }

        @Test
        @DisplayName("正常发送：已读状态默认 0、发送时间被设置、返回 ID")
        void should_send_and_return_id() {
            SysNoticeDTO dto = buildDto("测试通知", 10L, SystemConstant.NOTICE_TYPE_MSG);
            dto.setNoticeContent("通知内容");
            when(sysNoticeMapper.insert(any(SysNoticeEntity.class))).thenAnswer(invocation -> {
                SysNoticeEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            Long id = noticeService.send(dto);

            assertEquals(100L, id);
            ArgumentCaptor<SysNoticeEntity> captor = ArgumentCaptor.forClass(SysNoticeEntity.class);
            verify(sysNoticeMapper).insert(captor.capture());
            assertAll("字段映射",
                    () -> assertEquals("测试通知", captor.getValue().getNoticeTitle()),
                    () -> assertEquals("通知内容", captor.getValue().getNoticeContent()),
                    () -> assertEquals(10L, captor.getValue().getRecipientId()),
                    () -> assertEquals(SystemConstant.NOTICE_TYPE_MSG, captor.getValue().getNoticeType()),
                    () -> assertEquals(SystemConstant.READ_UNREAD, captor.getValue().getReadStatus()),
                    () -> assertNotNull(captor.getValue().getSendTime(), "发送时间应被设置")
            );
        }

        @Test
        @DisplayName("通知类型 2（消息）正常保存")
        void should_save_msg_type() {
            SysNoticeDTO dto = buildDto("消息", 5L, SystemConstant.NOTICE_TYPE_MSG);
            when(sysNoticeMapper.insert(any(SysNoticeEntity.class))).thenAnswer(invocation -> {
                SysNoticeEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            noticeService.send(dto);

            ArgumentCaptor<SysNoticeEntity> captor = ArgumentCaptor.forClass(SysNoticeEntity.class);
            verify(sysNoticeMapper).insert(captor.capture());
            assertEquals(SystemConstant.NOTICE_TYPE_MSG, captor.getValue().getNoticeType());
        }
    }

    /* ============ markRead ============ */

    @Nested
    @DisplayName("markRead 标记已读")
    class MarkReadTest {

        @Test
        @DisplayName("站内信不存在抛 NOT_FOUND")
        void should_throw_when_notice_not_exist() {
            when(sysNoticeMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> noticeService.markRead(99L, 1L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sysNoticeMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("recipientId 与站内信接收人不匹配抛 NO_PERMISSION")
        void should_throw_when_recipient_mismatch() {
            SysNoticeEntity exist = buildEntity(1L, 10L);
            when(sysNoticeMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> noticeService.markRead(1L, 99L));

            assertEquals(ResultCode.NO_PERMISSION.getCode(), ex.getCode());
            verify(sysNoticeMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("recipientId 为 null 时跳过归属校验（管理员代操作）")
        void should_skip_owner_check_when_recipient_id_null() {
            SysNoticeEntity exist = buildEntity(1L, 10L);
            when(sysNoticeMapper.selectById(1L)).thenReturn(exist);

            noticeService.markRead(1L, null);

            verify(sysNoticeMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("正常标记已读：调用 update 将 UNREAD 改为 READ")
        void should_mark_read_success() {
            SysNoticeEntity exist = buildEntity(1L, 10L);
            when(sysNoticeMapper.selectById(1L)).thenReturn(exist);

            noticeService.markRead(1L, 10L);

            verify(sysNoticeMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        }
    }

    /* ============ markAllRead ============ */

    @Nested
    @DisplayName("markAllRead 全部标记已读")
    class MarkAllReadTest {

        @Test
        @DisplayName("正常批量更新当前用户所有未读为已读")
        void should_mark_all_read_for_recipient() {
            noticeService.markAllRead(10L);

            verify(sysNoticeMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        }
    }

    /* ============ countUnread ============ */

    @Nested
    @DisplayName("countUnread 统计未读")
    class CountUnreadTest {

        @Test
        @DisplayName("正常返回未读数量")
        void should_return_unread_count() {
            when(sysNoticeMapper.countUnread(10L)).thenReturn(5L);

            long count = noticeService.countUnread(10L);

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("无未读返回 0")
        void should_return_zero_when_no_unread() {
            when(sysNoticeMapper.countUnread(10L)).thenReturn(0L);

            long count = noticeService.countUnread(10L);

            assertEquals(0L, count);
        }
    }

    /* ============ delete ============ */

    @Nested
    @DisplayName("delete 删除站内信")
    class DeleteTest {

        @Test
        @DisplayName("站内信不存在或无权操作抛 NOT_FOUND")
        void should_throw_when_not_exist_or_no_permission() {
            when(sysNoticeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> noticeService.delete(99L, 1L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sysNoticeMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("recipientId 匹配且站内信存在时正常删除")
        void should_delete_when_recipient_matches() {
            when(sysNoticeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            noticeService.delete(1L, 10L);

            verify(sysNoticeMapper).deleteById(1L);
        }

        @Test
        @DisplayName("recipientId 为 null 时仅校验站内信存在性（管理员代删）")
        void should_delete_without_recipient_check_when_null() {
            when(sysNoticeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            noticeService.delete(1L, null);

            verify(sysNoticeMapper).deleteById(1L);
        }
    }

    /* ============ 测试辅助方法 ============ */

    private SysNoticeDTO buildDto(String title, Long recipientId, Integer noticeType) {
        SysNoticeDTO dto = new SysNoticeDTO();
        dto.setNoticeTitle(title);
        dto.setRecipientId(recipientId);
        dto.setNoticeType(noticeType);
        return dto;
    }

    private SysNoticeEntity buildEntity(Long id, Long recipientId) {
        SysNoticeEntity e = new SysNoticeEntity();
        e.setId(id);
        e.setRecipientId(recipientId);
        e.setReadStatus(SystemConstant.READ_UNREAD);
        e.setSendTime(LocalDateTime.now());
        return e;
    }
}
