package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.constant.AcceptanceConstant;
import com.vibe.acceptance.dto.AcceptanceDocQueryDTO;
import com.vibe.acceptance.dto.AcceptanceDocSaveDTO;
import com.vibe.acceptance.entity.AcceptanceDocEntity;
import com.vibe.acceptance.mapper.AcceptanceDocMapper;
import com.vibe.acceptance.vo.AcceptanceDocVO;
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
 * 竣工文档服务实现单元测试（Task 3 SubTask 3.3）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>save：版本默认 1.0.0、写入上传人</li>
 *   <li>update：不存在抛 NOT_FOUND、正常更新保留 ID</li>
 *   <li>delete：不存在抛 NOT_FOUND、正常删除</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("竣工文档服务 AcceptanceDocServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class AcceptanceDocServiceImplTest {

    @Mock
    private AcceptanceDocMapper docMapper;

    @InjectMocks
    private AcceptanceDocServiceImpl docService;

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
            AcceptanceDocQueryDTO query = new AcceptanceDocQueryDTO();
            query.setProjectId(10L);
            query.setDocType(AcceptanceConstant.DOC_TYPE_TOPOLOGY);

            Page<AcceptanceDocEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, 1L, AcceptanceConstant.DOC_TYPE_TOPOLOGY)));
            mockPage.setTotal(1L);
            when(docMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<AcceptanceDocVO> result = docService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals(10L, result.getRecords().get(0).getProjectId());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            AcceptanceDocQueryDTO query = new AcceptanceDocQueryDTO();
            Page<AcceptanceDocEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(docMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<AcceptanceDocVO> result = docService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("文档不存在抛 NOT_FOUND")
        void should_throw_not_found_when_missing() {
            when(docMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> docService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_vo() {
            AcceptanceDocEntity entity = buildEntity(1L, 10L, 1L, AcceptanceConstant.DOC_TYPE_TOPOLOGY);
            when(docMapper.selectById(1L)).thenReturn(entity);

            AcceptanceDocVO vo = docService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals(AcceptanceConstant.DOC_TYPE_TOPOLOGY, vo.getDocType());
        }
    }

    @Nested
    @DisplayName("save 保存文档")
    class SaveTest {

        @Test
        @DisplayName("正常保存：版本默认 1.0.0、写入上传人")
        void should_save_with_default_version_and_uploader() {
            AcceptanceDocSaveDTO dto = buildSaveDto(1L, 10L, AcceptanceConstant.DOC_TYPE_TOPOLOGY, null);
            UserContextHolder.set(UserContext.builder().userId(55L).build());

            when(docMapper.insert(any(AcceptanceDocEntity.class))).thenAnswer(invocation -> {
                AcceptanceDocEntity e = invocation.getArgument(0);
                e.setId(700L);
                return 1;
            });

            Long id = docService.save(dto);

            assertEquals(700L, id);
            ArgumentCaptor<AcceptanceDocEntity> captor = ArgumentCaptor.forClass(AcceptanceDocEntity.class);
            verify(docMapper).insert(captor.capture());
            AcceptanceDocEntity saved = captor.getValue();
            assertEquals("1.0.0", saved.getDocVersion());
            assertEquals(55L, saved.getUploaderId());
        }

        @Test
        @DisplayName("显式指定版本时保留指定版本")
        void should_save_with_explicit_version() {
            AcceptanceDocSaveDTO dto = buildSaveDto(1L, 10L, AcceptanceConstant.DOC_TYPE_TOPOLOGY, "2.1.3");

            docService.save(dto);

            ArgumentCaptor<AcceptanceDocEntity> captor = ArgumentCaptor.forClass(AcceptanceDocEntity.class);
            verify(docMapper).insert(captor.capture());
            assertEquals("2.1.3", captor.getValue().getDocVersion());
        }

        @Test
        @DisplayName("版本为空白字符串时使用默认 1.0.0")
        void should_use_default_version_when_blank() {
            AcceptanceDocSaveDTO dto = buildSaveDto(1L, 10L, AcceptanceConstant.DOC_TYPE_TOPOLOGY, "   ");

            docService.save(dto);

            ArgumentCaptor<AcceptanceDocEntity> captor = ArgumentCaptor.forClass(AcceptanceDocEntity.class);
            verify(docMapper).insert(captor.capture());
            assertEquals("1.0.0", captor.getValue().getDocVersion());
        }
    }

    @Nested
    @DisplayName("update 更新文档")
    class UpdateTest {

        @Test
        @DisplayName("文档不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(docMapper.selectById(99L)).thenReturn(null);
            AcceptanceDocSaveDTO dto = buildSaveDto(1L, 10L, AcceptanceConstant.DOC_TYPE_TOPOLOGY, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> docService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(docMapper, never()).updateById(any(AcceptanceDocEntity.class));
        }

        @Test
        @DisplayName("正常更新：保留 ID")
        void should_update_and_keep_id() {
            AcceptanceDocEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.DOC_TYPE_TOPOLOGY);
            when(docMapper.selectById(1L)).thenReturn(exist);
            AcceptanceDocSaveDTO dto = buildSaveDto(2L, 11L, AcceptanceConstant.DOC_TYPE_DEVICE_LIST, null);

            docService.update(1L, dto);

            ArgumentCaptor<AcceptanceDocEntity> captor = ArgumentCaptor.forClass(AcceptanceDocEntity.class);
            verify(docMapper).updateById(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertEquals(2L, captor.getValue().getTaskId());
            assertEquals(11L, captor.getValue().getProjectId());
            assertEquals(AcceptanceConstant.DOC_TYPE_DEVICE_LIST, captor.getValue().getDocType());
        }
    }

    @Nested
    @DisplayName("delete 删除文档")
    class DeleteTest {

        @Test
        @DisplayName("文档不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(docMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> docService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(docMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            AcceptanceDocEntity exist = buildEntity(1L, 10L, 1L, AcceptanceConstant.DOC_TYPE_TOPOLOGY);
            when(docMapper.selectById(1L)).thenReturn(exist);

            docService.delete(1L);

            verify(docMapper).deleteById(1L);
        }
    }

    /* ============ 测试辅助方法 ============ */

    private AcceptanceDocEntity buildEntity(Long id, Long projectId, Long taskId, String docType) {
        AcceptanceDocEntity entity = new AcceptanceDocEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setTaskId(taskId);
        entity.setDocType(docType);
        entity.setName("文档");
        entity.setFileUrl("/test/url");
        return entity;
    }

    private AcceptanceDocSaveDTO buildSaveDto(Long taskId, Long projectId, String docType, String version) {
        AcceptanceDocSaveDTO dto = new AcceptanceDocSaveDTO();
        dto.setTaskId(taskId);
        dto.setProjectId(projectId);
        dto.setDocType(docType);
        dto.setName("文档");
        dto.setFileUrl("/test/url");
        dto.setDocVersion(version);
        return dto;
    }
}
