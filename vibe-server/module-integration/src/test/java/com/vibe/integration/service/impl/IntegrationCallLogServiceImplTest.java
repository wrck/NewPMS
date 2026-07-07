package com.vibe.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.integration.constant.IntegrationConstant;
import com.vibe.integration.dto.IntegrationCallLogQueryDTO;
import com.vibe.integration.entity.IntegrationCallLogEntity;
import com.vibe.integration.mapper.IntegrationCallLogMapper;
import com.vibe.integration.vo.IntegrationCallLogVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 集成调用日志服务实现单元测试（Task 3 SubTask 3.5）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>delete：不存在抛 NOT_FOUND、正常删除</li>
 *   <li>clearAll：清空所有日志</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("集成调用日志服务 IntegrationCallLogServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class IntegrationCallLogServiceImplTest {

    @Mock
    private IntegrationCallLogMapper callLogMapper;

    @InjectMocks
    private IntegrationCallLogServiceImpl callLogService;

    @Nested
    @DisplayName("page 分页查询")
    class PageTest {

        @Test
        @DisplayName("正常分页：返回 records 与 total")
        void should_return_paged_result() {
            IntegrationCallLogQueryDTO query = new IntegrationCallLogQueryDTO();
            query.setConfigId(10L);
            query.setSystemCode("ERP");
            query.setStatus(IntegrationConstant.CALL_STATUS_SUCCESS);

            Page<IntegrationCallLogEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, "ERP", IntegrationConstant.CALL_STATUS_SUCCESS)));
            mockPage.setTotal(1L);
            when(callLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<IntegrationCallLogVO> result = callLogService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals("ERP", result.getRecords().get(0).getSystemCode());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            IntegrationCallLogQueryDTO query = new IntegrationCallLogQueryDTO();
            Page<IntegrationCallLogEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(callLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<IntegrationCallLogVO> result = callLogService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("日志不存在抛 NOT_FOUND")
        void should_throw_not_found_when_missing() {
            when(callLogMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> callLogService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_vo() {
            IntegrationCallLogEntity entity = buildEntity(1L, 10L, "ERP", IntegrationConstant.CALL_STATUS_SUCCESS);
            when(callLogMapper.selectById(1L)).thenReturn(entity);

            IntegrationCallLogVO vo = callLogService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals("ERP", vo.getSystemCode());
        }
    }

    @Nested
    @DisplayName("delete 删除日志")
    class DeleteTest {

        @Test
        @DisplayName("日志不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(callLogMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> callLogService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(callLogMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            IntegrationCallLogEntity exist = buildEntity(1L, 10L, "ERP", IntegrationConstant.CALL_STATUS_SUCCESS);
            when(callLogMapper.selectById(1L)).thenReturn(exist);

            callLogService.delete(1L);

            verify(callLogMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("clearAll 清空所有日志")
    class ClearAllTest {

        @Test
        @DisplayName("正常清空所有日志")
        void should_clear_all_logs() {
            callLogService.clearAll();

            verify(callLogMapper).delete(any(LambdaQueryWrapper.class));
        }
    }

    /* ============ 测试辅助方法 ============ */

    private IntegrationCallLogEntity buildEntity(Long id, Long configId, String systemCode, String status) {
        IntegrationCallLogEntity entity = new IntegrationCallLogEntity();
        entity.setId(id);
        entity.setConfigId(configId);
        entity.setSystemCode(systemCode);
        entity.setStatus(status);
        return entity;
    }
}
