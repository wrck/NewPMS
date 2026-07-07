package com.vibe.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.integration.constant.IntegrationConstant;
import com.vibe.integration.dto.IntegrationConfigQueryDTO;
import com.vibe.integration.dto.IntegrationConfigSaveDTO;
import com.vibe.integration.entity.IntegrationConfigEntity;
import com.vibe.integration.mapper.IntegrationConfigMapper;
import com.vibe.integration.vo.IntegrationConfigVO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 集成配置服务实现单元测试（Task 3 SubTask 3.5）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>getBySystemCode：存在 / 不存在</li>
 *   <li>listEnabled：仅返回 enabled=1 的配置</li>
 *   <li>save：systemCode 唯一性校验、默认值填充（adapterType/authType/timeoutMs/retryCount/enabled）</li>
 *   <li>update：不存在 / systemCode 修改时校验唯一性 / 正常更新</li>
 *   <li>delete：不存在 / 正常删除</li>
 *   <li>toggleEnabled：切换启用状态</li>
 *   <li>testConnection：endpointUrl 非空返回 true，空返回 false</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("集成配置服务 IntegrationConfigServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class IntegrationConfigServiceImplTest {

    @Mock
    private IntegrationConfigMapper configMapper;

    @InjectMocks
    private IntegrationConfigServiceImpl configService;

    @Nested
    @DisplayName("page 分页查询")
    class PageTest {

        @Test
        @DisplayName("正常分页：返回 records 与 total")
        void should_return_paged_result() {
            IntegrationConfigQueryDTO query = new IntegrationConfigQueryDTO();
            query.setKeyword("ERP");
            query.setAdapterType(IntegrationConstant.ADAPTER_REST_API);
            query.setEnabled(1);

            Page<IntegrationConfigEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, "ERP", "ERP 系统", 1)));
            mockPage.setTotal(1L);
            when(configMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<IntegrationConfigVO> result = configService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals("ERP", result.getRecords().get(0).getSystemCode());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            IntegrationConfigQueryDTO query = new IntegrationConfigQueryDTO();
            Page<IntegrationConfigEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(configMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<IntegrationConfigVO> result = configService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("配置不存在抛 NOT_FOUND")
        void should_throw_not_found_when_missing() {
            when(configMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_vo() {
            IntegrationConfigEntity entity = buildEntity(1L, "ERP", "ERP 系统", 1);
            when(configMapper.selectById(1L)).thenReturn(entity);

            IntegrationConfigVO vo = configService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals("ERP", vo.getSystemCode());
        }
    }

    @Nested
    @DisplayName("getBySystemCode 按系统编码查询")
    class GetBySystemCodeTest {

        @Test
        @DisplayName("存在时返回 VO")
        void should_return_vo_when_exists() {
            IntegrationConfigEntity entity = buildEntity(1L, "ERP", "ERP 系统", 1);
            when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

            IntegrationConfigVO vo = configService.getBySystemCode("ERP");

            assertNotNull(vo);
            assertEquals("ERP", vo.getSystemCode());
        }

        @Test
        @DisplayName("不存在时返回 null")
        void should_return_null_when_not_exists() {
            when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            IntegrationConfigVO vo = configService.getBySystemCode("NOT_EXIST");

            assertNull(vo);
        }
    }

    @Nested
    @DisplayName("listEnabled 查询已启用配置")
    class ListEnabledTest {

        @Test
        @DisplayName("返回 enabled=1 的配置列表")
        void should_return_enabled_configs() {
            IntegrationConfigEntity entity = buildEntity(1L, "ERP", "ERP 系统", 1);
            when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(entity));

            List<IntegrationConfigVO> result = configService.listEnabled();

            assertEquals(1, result.size());
            assertEquals("ERP", result.get(0).getSystemCode());
        }

        @Test
        @DisplayName("无启用配置时返回空列表")
        void should_return_empty_when_no_enabled() {
            when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<IntegrationConfigVO> result = configService.listEnabled();

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("save 保存配置")
    class SaveTest {

        @Test
        @DisplayName("systemCode 已存在抛 400 错误")
        void should_throw_when_system_code_duplicated() {
            IntegrationConfigSaveDTO dto = buildSaveDto("ERP", "ERP 系统", "http://erp.api");
            when(configMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.save(dto));
            assertEquals(400, ex.getCode());
            verify(configMapper, never()).insert(any(IntegrationConfigEntity.class));
        }

        @Test
        @DisplayName("正常保存：填充默认值（adapterType/authType/timeoutMs/retryCount/enabled）")
        void should_save_with_defaults() {
            IntegrationConfigSaveDTO dto = buildSaveDto("ERP", "ERP 系统", "http://erp.api");
            // dto 中所有可选字段为 null
            when(configMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(configMapper.insert(any(IntegrationConfigEntity.class))).thenAnswer(invocation -> {
                IntegrationConfigEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            Long id = configService.save(dto);

            assertEquals(100L, id);
            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).insert(captor.capture());
            IntegrationConfigEntity saved = captor.getValue();
            assertEquals(IntegrationConstant.ADAPTER_REST_API, saved.getAdapterType());
            assertEquals(IntegrationConstant.AUTH_NONE, saved.getAuthType());
            assertEquals(10000, saved.getTimeoutMs());
            assertEquals(0, saved.getRetryCount());
            assertEquals(1, saved.getEnabled());
        }

        @Test
        @DisplayName("显式指定字段时保留指定值")
        void should_save_with_explicit_values() {
            IntegrationConfigSaveDTO dto = buildSaveDto("ERP", "ERP 系统", "http://erp.api");
            dto.setAdapterType(IntegrationConstant.ADAPTER_WEBHOOK);
            dto.setAuthType(IntegrationConstant.AUTH_BASIC);
            dto.setTimeoutMs(5000);
            dto.setRetryCount(3);
            dto.setEnabled(0);
            when(configMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            configService.save(dto);

            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).insert(captor.capture());
            IntegrationConfigEntity saved = captor.getValue();
            assertEquals(IntegrationConstant.ADAPTER_WEBHOOK, saved.getAdapterType());
            assertEquals(IntegrationConstant.AUTH_BASIC, saved.getAuthType());
            assertEquals(5000, saved.getTimeoutMs());
            assertEquals(3, saved.getRetryCount());
            assertEquals(0, saved.getEnabled());
        }
    }

    @Nested
    @DisplayName("update 更新配置")
    class UpdateTest {

        @Test
        @DisplayName("配置不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(configMapper.selectById(99L)).thenReturn(null);
            IntegrationConfigSaveDTO dto = buildSaveDto("ERP", "ERP", "http://x");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(configMapper, never()).updateById(any(IntegrationConfigEntity.class));
        }

        @Test
        @DisplayName("修改 systemCode 且新编码已存在抛 400 错误")
        void should_throw_when_system_code_changed_and_duplicated() {
            IntegrationConfigEntity exist = buildEntity(1L, "OLD_CODE", "旧系统", 1);
            when(configMapper.selectById(1L)).thenReturn(exist);
            IntegrationConfigSaveDTO dto = buildSaveDto("NEW_CODE", "新系统", "http://x");
            when(configMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.update(1L, dto));
            assertEquals(400, ex.getCode());
            verify(configMapper, never()).updateById(any(IntegrationConfigEntity.class));
        }

        @Test
        @DisplayName("systemCode 不变时不校验唯一性，正常更新")
        void should_update_when_system_code_unchanged() {
            IntegrationConfigEntity exist = buildEntity(1L, "ERP", "ERP 系统", 1);
            when(configMapper.selectById(1L)).thenReturn(exist);
            IntegrationConfigSaveDTO dto = buildSaveDto("ERP", "新名称", "http://new");

            configService.update(1L, dto);

            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).updateById(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertEquals("ERP", captor.getValue().getSystemCode());
            assertEquals("新名称", captor.getValue().getSystemName());
        }

        @Test
        @DisplayName("修改 systemCode 且新编码不存在时正常更新")
        void should_update_when_system_code_changed_and_not_duplicated() {
            IntegrationConfigEntity exist = buildEntity(1L, "OLD", "旧", 1);
            when(configMapper.selectById(1L)).thenReturn(exist);
            IntegrationConfigSaveDTO dto = buildSaveDto("NEW", "新", "http://x");
            when(configMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            configService.update(1L, dto);

            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).updateById(captor.capture());
            assertEquals("NEW", captor.getValue().getSystemCode());
        }
    }

    @Nested
    @DisplayName("delete 删除配置")
    class DeleteTest {

        @Test
        @DisplayName("配置不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(configMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(configMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            IntegrationConfigEntity exist = buildEntity(1L, "ERP", "ERP 系统", 1);
            when(configMapper.selectById(1L)).thenReturn(exist);

            configService.delete(1L);

            verify(configMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("toggleEnabled 切换启用状态")
    class ToggleEnabledTest {

        @Test
        @DisplayName("配置不存在抛 NOT_FOUND")
        void should_throw_not_found_when_toggle_missing() {
            when(configMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.toggleEnabled(99L, 0));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(configMapper, never()).updateById(any(IntegrationConfigEntity.class));
        }

        @Test
        @DisplayName("正常切换为禁用")
        void should_toggle_to_disabled() {
            IntegrationConfigEntity exist = buildEntity(1L, "ERP", "ERP 系统", 1);
            when(configMapper.selectById(1L)).thenReturn(exist);

            configService.toggleEnabled(1L, 0);

            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).updateById(captor.capture());
            assertEquals(0, captor.getValue().getEnabled());
        }
    }

    @Nested
    @DisplayName("testConnection 测试连接")
    class TestConnectionTest {

        @Test
        @DisplayName("配置不存在抛 NOT_FOUND")
        void should_throw_not_found_when_test_missing() {
            when(configMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> configService.testConnection(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("endpointUrl 非空时返回 true，状态 SUCCESS")
        void should_return_true_when_endpoint_url_present() {
            IntegrationConfigEntity exist = buildEntity(1L, "ERP", "ERP 系统", 1);
            exist.setEndpointUrl("http://erp.api");
            when(configMapper.selectById(1L)).thenReturn(exist);

            boolean result = configService.testConnection(1L);

            assertTrue(result);
            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).updateById(captor.capture());
            assertEquals(IntegrationConstant.CALL_STATUS_SUCCESS, captor.getValue().getLastCallStatus());
            assertNotNull(captor.getValue().getLastCallTime());
        }

        @Test
        @DisplayName("endpointUrl 为空时返回 false，状态 FAIL")
        void should_return_false_when_endpoint_url_blank() {
            IntegrationConfigEntity exist = buildEntity(1L, "ERP", "ERP 系统", 1);
            exist.setEndpointUrl("");
            when(configMapper.selectById(1L)).thenReturn(exist);

            boolean result = configService.testConnection(1L);

            assertEquals(false, result);
            ArgumentCaptor<IntegrationConfigEntity> captor =
                    ArgumentCaptor.forClass(IntegrationConfigEntity.class);
            verify(configMapper).updateById(captor.capture());
            assertEquals(IntegrationConstant.CALL_STATUS_FAIL, captor.getValue().getLastCallStatus());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private IntegrationConfigEntity buildEntity(Long id, String systemCode, String systemName, Integer enabled) {
        IntegrationConfigEntity entity = new IntegrationConfigEntity();
        entity.setId(id);
        entity.setSystemCode(systemCode);
        entity.setSystemName(systemName);
        entity.setEnabled(enabled);
        return entity;
    }

    private IntegrationConfigSaveDTO buildSaveDto(String systemCode, String systemName, String endpointUrl) {
        IntegrationConfigSaveDTO dto = new IntegrationConfigSaveDTO();
        dto.setSystemCode(systemCode);
        dto.setSystemName(systemName);
        dto.setEndpointUrl(endpointUrl);
        return dto;
    }
}
