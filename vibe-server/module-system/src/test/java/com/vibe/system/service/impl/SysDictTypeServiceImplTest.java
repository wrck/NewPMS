package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysDictTypeDTO;
import com.vibe.system.dto.SysDictTypeQueryDTO;
import com.vibe.system.entity.SysDictTypeEntity;
import com.vibe.system.mapper.SysDictTypeMapper;
import com.vibe.system.service.SysDictDataService;
import com.vibe.system.vo.SysDictTypeVO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典类型服务实现单元测试（Task 3 SubTask 3.7）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、默认分页参数</li>
 *   <li>create：默认状态、唯一性校验</li>
 *   <li>update：id 为空、不存在、dictType 变更唯一性校验、缓存清除（旧/新）</li>
 *   <li>delete：不存在校验、删除并清缓存</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("字典类型服务 SysDictTypeServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;
    @Mock
    private SysDictDataService sysDictDataService;

    @InjectMocks
    private SysDictTypeServiceImpl dictTypeService;

    /* ============ page ============ */

    @Nested
    @DisplayName("page 分页查询")
    class PageTest {

        @Test
        @DisplayName("正常分页：返回 records 与 total")
        void should_return_paged_result() {
            SysDictTypeQueryDTO query = new SysDictTypeQueryDTO();
            query.setPage(1);
            query.setSize(10);

            SysDictTypeVO vo = new SysDictTypeVO();
            vo.setId(1L);
            vo.setDictType("user_status");
            Page<SysDictTypeVO> mockPage = new Page<>(1, 10);
            mockPage.setRecords(java.util.List.of(vo));
            mockPage.setTotal(1L);
            when(sysDictTypeMapper.selectDictTypePage(any(IPage.class), eq(query))).thenReturn(mockPage);

            PageResult<SysDictTypeVO> result = dictTypeService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals("user_status", result.getRecords().get(0).getDictType());
        }

        @Test
        @DisplayName("page/size 为 null 时默认 1/20")
        void should_use_default_page_size_when_null() {
            SysDictTypeQueryDTO query = new SysDictTypeQueryDTO();
            Page<SysDictTypeVO> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(sysDictTypeMapper.selectDictTypePage(any(IPage.class), eq(query))).thenReturn(mockPage);

            PageResult<SysDictTypeVO> result = dictTypeService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(1L, result.getPage());
            assertEquals(20L, result.getSize());
        }

        @Test
        @DisplayName("空结果返回空列表")
        void should_return_empty_when_no_data() {
            SysDictTypeQueryDTO query = new SysDictTypeQueryDTO();
            Page<SysDictTypeVO> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(sysDictTypeMapper.selectDictTypePage(any(IPage.class), eq(query))).thenReturn(mockPage);

            PageResult<SysDictTypeVO> result = dictTypeService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
        }
    }

    /* ============ create ============ */

    @Nested
    @DisplayName("create 创建字典类型")
    class CreateTest {

        @Test
        @DisplayName("正常创建：status 为 null 时默认 1（启用）")
        void should_create_with_default_status() {
            SysDictTypeDTO dto = buildDto("用户状态", "user_status", null);
            when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysDictTypeMapper.insert(any(SysDictTypeEntity.class))).thenAnswer(invocation -> {
                SysDictTypeEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            Long id = dictTypeService.create(dto);

            assertEquals(100L, id);
            ArgumentCaptor<SysDictTypeEntity> captor = ArgumentCaptor.forClass(SysDictTypeEntity.class);
            verify(sysDictTypeMapper).insert(captor.capture());
            assertAll("字段映射",
                    () -> assertEquals("用户状态", captor.getValue().getDictName()),
                    () -> assertEquals("user_status", captor.getValue().getDictType()),
                    () -> assertEquals(SystemConstant.STATUS_ENABLED, captor.getValue().getStatus())
            );
        }

        @Test
        @DisplayName("status 显式传 0 时保存为 0（禁用）")
        void should_save_disabled_status() {
            SysDictTypeDTO dto = buildDto("废弃字典", "deprecated_dict", 0);
            when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysDictTypeMapper.insert(any(SysDictTypeEntity.class))).thenAnswer(invocation -> {
                SysDictTypeEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            dictTypeService.create(dto);

            ArgumentCaptor<SysDictTypeEntity> captor = ArgumentCaptor.forClass(SysDictTypeEntity.class);
            verify(sysDictTypeMapper).insert(captor.capture());
            assertEquals(SystemConstant.STATUS_DISABLED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("dictType 重复抛 DATA_DUPLICATED")
        void should_throw_when_dict_type_duplicated() {
            SysDictTypeDTO dto = buildDto("用户状态", "user_status", null);
            when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dictTypeService.create(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
            verify(sysDictTypeMapper, never()).insert(any(SysDictTypeEntity.class));
        }

        @Test
        @DisplayName("正常保存 remark 字段")
        void should_save_remark() {
            SysDictTypeDTO dto = buildDto("用户状态", "user_status", 1);
            dto.setRemark("用户状态字典");
            when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysDictTypeMapper.insert(any(SysDictTypeEntity.class))).thenAnswer(invocation -> {
                SysDictTypeEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            dictTypeService.create(dto);

            ArgumentCaptor<SysDictTypeEntity> captor = ArgumentCaptor.forClass(SysDictTypeEntity.class);
            verify(sysDictTypeMapper).insert(captor.capture());
            assertEquals("用户状态字典", captor.getValue().getRemark());
        }
    }

    /* ============ update ============ */

    @Nested
    @DisplayName("update 更新字典类型")
    class UpdateTest {

        @Test
        @DisplayName("id 为空抛 PARAM_MISSING")
        void should_throw_when_id_null() {
            SysDictTypeDTO dto = new SysDictTypeDTO();
            dto.setDictType("x");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dictTypeService.update(dto));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
            verify(sysDictTypeMapper, never()).updateById(any(SysDictTypeEntity.class));
        }

        @Test
        @DisplayName("字典类型不存在抛 NOT_FOUND")
        void should_throw_when_not_exist() {
            SysDictTypeDTO dto = buildDto("x", "x", null);
            dto.setId(99L);
            when(sysDictTypeMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dictTypeService.update(dto));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("dictType 变更时执行唯一性校验")
        void should_check_unique_when_dict_type_changed() {
            SysDictTypeDTO dto = buildDto("x", "new_type", null);
            dto.setId(1L);
            SysDictTypeEntity exist = buildEntity(1L, "user_status", "old_type");
            when(sysDictTypeMapper.selectById(1L)).thenReturn(exist);
            when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dictTypeService.update(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
            verify(sysDictTypeMapper, never()).updateById(any(SysDictTypeEntity.class));
        }

        @Test
        @DisplayName("dictType 未变更时不执行唯一性校验，但清除旧缓存")
        void should_clear_old_cache_when_dict_type_unchanged() {
            SysDictTypeDTO dto = buildDto("新名称", "user_status", null);
            dto.setId(1L);
            SysDictTypeEntity exist = buildEntity(1L, "用户状态", "user_status");
            when(sysDictTypeMapper.selectById(1L)).thenReturn(exist);

            dictTypeService.update(dto);

            verify(sysDictTypeMapper).updateById(any(SysDictTypeEntity.class));
            // oldType 非空时清除旧缓存
            verify(sysDictDataService).clearCache("user_status");
            // dictType 未变更，不应再次 clearCache 新值
            verify(sysDictDataService, never()).clearCache("new_type");
        }

        @Test
        @DisplayName("dictType 变更后清除旧缓存与新缓存")
        void should_clear_both_old_and_new_cache_when_changed() {
            SysDictTypeDTO dto = buildDto("用户状态", "new_type", null);
            dto.setId(1L);
            SysDictTypeEntity exist = buildEntity(1L, "用户状态", "old_type");
            when(sysDictTypeMapper.selectById(1L)).thenReturn(exist);
            when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            dictTypeService.update(dto);

            verify(sysDictDataService).clearCache("old_type");
            verify(sysDictDataService).clearCache("new_type");
        }
    }

    /* ============ delete ============ */

    @Nested
    @DisplayName("delete 删除字典类型")
    class DeleteTest {

        @Test
        @DisplayName("字典类型不存在抛 NOT_FOUND")
        void should_throw_when_not_exist() {
            when(sysDictTypeMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dictTypeService.delete(99L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sysDictTypeMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("正常删除：删除实体并清除对应 dictType 缓存")
        void should_delete_and_clear_cache() {
            SysDictTypeEntity exist = buildEntity(1L, "用户状态", "user_status");
            when(sysDictTypeMapper.selectById(1L)).thenReturn(exist);

            dictTypeService.delete(1L);

            verify(sysDictTypeMapper).deleteById(1L);
            verify(sysDictDataService).clearCache("user_status");
        }
    }

    /* ============ getDetail ============ */

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("字典类型不存在抛 NOT_FOUND")
        void should_throw_when_not_found() {
            when(sysDictTypeMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dictTypeService.getDetail(99L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_detail() {
            SysDictTypeEntity e = buildEntity(1L, "用户状态", "user_status");
            when(sysDictTypeMapper.selectById(1L)).thenReturn(e);

            SysDictTypeVO vo = dictTypeService.getDetail(1L);

            assertNotNull(vo);
            assertEquals("用户状态", vo.getDictName());
            assertEquals("user_status", vo.getDictType());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private SysDictTypeDTO buildDto(String name, String type, Integer status) {
        SysDictTypeDTO dto = new SysDictTypeDTO();
        dto.setDictName(name);
        dto.setDictType(type);
        dto.setStatus(status);
        return dto;
    }

    private SysDictTypeEntity buildEntity(Long id, String name, String type) {
        SysDictTypeEntity e = new SysDictTypeEntity();
        e.setId(id);
        e.setDictName(name);
        e.setDictType(type);
        e.setStatus(SystemConstant.STATUS_ENABLED);
        return e;
    }
}
