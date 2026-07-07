package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.finance.dto.FinanceCostQueryDTO;
import com.vibe.finance.dto.FinanceCostSaveDTO;
import com.vibe.finance.entity.FinanceCostEntity;
import com.vibe.finance.mapper.FinanceCostMapper;
import com.vibe.finance.vo.FinanceCostVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * 成本归集服务实现单元测试（Task 3 SubTask 3.2）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>save：refType 默认 MANUAL、字段映射</li>
 *   <li>update：不存在抛 NOT_FOUND / 正常更新</li>
 *   <li>delete：不存在抛 NOT_FOUND / 正常删除</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("成本服务 FinanceCostServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class FinanceCostServiceImplTest {

    @Mock
    private FinanceCostMapper costMapper;

    @InjectMocks
    private FinanceCostServiceImpl costService;

    @Nested
    @DisplayName("page 分页查询")
    class PageTest {

        @Test
        @DisplayName("正常分页：返回 records 与 total")
        void should_return_paged_result() {
            FinanceCostQueryDTO query = new FinanceCostQueryDTO();
            query.setProjectId(10L);
            query.setCostType("LABOR");
            query.setStartDate(LocalDate.of(2026, 1, 1));
            query.setEndDate(LocalDate.of(2026, 12, 31));

            Page<FinanceCostEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, "LABOR", new BigDecimal("100"))));
            mockPage.setTotal(1L);
            when(costMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<FinanceCostVO> result = costService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals("LABOR", result.getRecords().get(0).getCostType());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            FinanceCostQueryDTO query = new FinanceCostQueryDTO();
            Page<FinanceCostEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(costMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<FinanceCostVO> result = costService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
        }

        @Test
        @DisplayName("costType 为空白时不加入查询条件（不抛错）")
        void should_handle_blank_cost_type() {
            FinanceCostQueryDTO query = new FinanceCostQueryDTO();
            query.setCostType("   ");
            Page<FinanceCostEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(costMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<FinanceCostVO> result = costService.page(query);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("成本记录不存在抛 NOT_FOUND")
        void should_throw_not_found_when_cost_missing() {
            when(costMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> costService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回详情")
        void should_return_detail() {
            FinanceCostEntity entity = buildEntity(1L, 10L, "LABOR", new BigDecimal("100"));
            when(costMapper.selectById(1L)).thenReturn(entity);

            FinanceCostVO vo = costService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals("LABOR", vo.getCostType());
            assertEquals(new BigDecimal("100"), vo.getAmount());
        }
    }

    @Nested
    @DisplayName("save 保存成本")
    class SaveTest {

        @Test
        @DisplayName("refType 为空时默认 MANUAL")
        void should_default_ref_type_when_blank() {
            FinanceCostSaveDTO dto = buildSaveDto(10L, "LABOR", new BigDecimal("100"));
            dto.setRefType("  ");

            when(costMapper.insert(any(FinanceCostEntity.class))).thenAnswer(invocation -> {
                FinanceCostEntity e = invocation.getArgument(0);
                e.setId(700L);
                return 1;
            });

            Long id = costService.save(dto);

            assertEquals(700L, id);
            ArgumentCaptor<FinanceCostEntity> captor = ArgumentCaptor.forClass(FinanceCostEntity.class);
            verify(costMapper).insert(captor.capture());
            assertEquals("MANUAL", captor.getValue().getRefType());
        }

        @Test
        @DisplayName("refType 已指定时保留原值")
        void should_keep_ref_type_when_provided() {
            FinanceCostSaveDTO dto = buildSaveDto(10L, "TRAVEL", new BigDecimal("50"));
            dto.setRefType("BUSINESS_TRIP");

            costService.save(dto);

            ArgumentCaptor<FinanceCostEntity> captor = ArgumentCaptor.forClass(FinanceCostEntity.class);
            verify(costMapper).insert(captor.capture());
            assertEquals("BUSINESS_TRIP", captor.getValue().getRefType());
            assertEquals("TRAVEL", captor.getValue().getCostType());
        }

        @Test
        @DisplayName("字段映射：amount、costDate、projectId 写入")
        void should_map_fields_on_save() {
            FinanceCostSaveDTO dto = buildSaveDto(11L, "OTHER", new BigDecimal("33"));
            dto.setCostDate(LocalDate.of(2026, 7, 1));
            dto.setDescription("测试费用");

            costService.save(dto);

            ArgumentCaptor<FinanceCostEntity> captor = ArgumentCaptor.forClass(FinanceCostEntity.class);
            verify(costMapper).insert(captor.capture());
            assertEquals(11L, captor.getValue().getProjectId());
            assertEquals(new BigDecimal("33"), captor.getValue().getAmount());
            assertEquals(LocalDate.of(2026, 7, 1), captor.getValue().getCostDate());
            assertEquals("测试费用", captor.getValue().getDescription());
        }
    }

    @Nested
    @DisplayName("update 更新成本")
    class UpdateTest {

        @Test
        @DisplayName("成本记录不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(costMapper.selectById(99L)).thenReturn(null);
            FinanceCostSaveDTO dto = buildSaveDto(10L, "LABOR", BigDecimal.ONE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> costService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(costMapper, never()).updateById(any(FinanceCostEntity.class));
        }

        @Test
        @DisplayName("正常更新：保留原 ID")
        void should_update_success() {
            FinanceCostEntity exist = buildEntity(1L, 10L, "LABOR", BigDecimal.ONE);
            when(costMapper.selectById(1L)).thenReturn(exist);
            FinanceCostSaveDTO dto = buildSaveDto(11L, "TRAVEL", new BigDecimal("200"));

            costService.update(1L, dto);

            ArgumentCaptor<FinanceCostEntity> captor = ArgumentCaptor.forClass(FinanceCostEntity.class);
            verify(costMapper).updateById(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertEquals("TRAVEL", captor.getValue().getCostType());
            assertEquals(new BigDecimal("200"), captor.getValue().getAmount());
        }
    }

    @Nested
    @DisplayName("delete 删除成本")
    class DeleteTest {

        @Test
        @DisplayName("成本记录不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(costMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> costService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(costMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            FinanceCostEntity exist = buildEntity(1L, 10L, "LABOR", BigDecimal.ONE);
            when(costMapper.selectById(1L)).thenReturn(exist);

            costService.delete(1L);

            verify(costMapper).deleteById(1L);
        }
    }

    /* ============ 测试辅助方法 ============ */

    private FinanceCostEntity buildEntity(Long id, Long projectId, String costType, BigDecimal amount) {
        FinanceCostEntity entity = new FinanceCostEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setCostType(costType);
        entity.setAmount(amount);
        entity.setCostDate(LocalDate.of(2026, 1, 1));
        return entity;
    }

    private FinanceCostSaveDTO buildSaveDto(Long projectId, String costType, BigDecimal amount) {
        FinanceCostSaveDTO dto = new FinanceCostSaveDTO();
        dto.setProjectId(projectId);
        dto.setCostType(costType);
        dto.setAmount(amount);
        return dto;
    }
}
