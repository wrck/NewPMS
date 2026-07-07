package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.finance.constant.FinanceConstant;
import com.vibe.finance.dto.FinanceBudgetQueryDTO;
import com.vibe.finance.dto.FinanceBudgetSaveDTO;
import com.vibe.finance.entity.FinanceBudgetEntity;
import com.vibe.finance.entity.FinanceCostEntity;
import com.vibe.finance.mapper.FinanceBudgetMapper;
import com.vibe.finance.mapper.FinanceCostMapper;
import com.vibe.finance.vo.FinanceBudgetVO;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预算服务实现单元测试（Task 3 SubTask 3.2）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND、实际成本聚合计算</li>
 *   <li>save：总额计算、初始状态 DRAFT</li>
 *   <li>update：不存在 / 非草稿状态禁止修改 / 正常更新</li>
 *   <li>delete：不存在 / 正常删除</li>
 *   <li>submit：仅草稿可提交</li>
 *   <li>approve：仅待审批可审批、通过/驳回分支、写入 approverId</li>
 *   <li>sumActualCost：按年度/类型筛选汇总</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("预算服务 FinanceBudgetServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class FinanceBudgetServiceImplTest {

    @Mock
    private FinanceBudgetMapper budgetMapper;
    @Mock
    private FinanceCostMapper costMapper;

    @InjectMocks
    private FinanceBudgetServiceImpl budgetService;

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
            FinanceBudgetQueryDTO query = new FinanceBudgetQueryDTO();
            query.setProjectId(10L);
            query.setYear(2026);
            query.setApprovalStatus(FinanceConstant.BUDGET_STATUS_PENDING);

            Page<FinanceBudgetEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_PENDING)));
            mockPage.setTotal(1L);
            when(budgetMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<FinanceBudgetVO> result = budgetService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals(10L, result.getRecords().get(0).getProjectId());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            FinanceBudgetQueryDTO query = new FinanceBudgetQueryDTO();
            Page<FinanceBudgetEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(budgetMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<FinanceBudgetVO> result = budgetService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("预算不存在抛 NOT_FOUND")
        void should_throw_not_found_when_budget_missing() {
            when(budgetMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回：聚合四类实际成本并求和")
        void should_aggregate_actual_cost() {
            FinanceBudgetEntity entity = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_APPROVED);
            entity.setLaborAmount(new BigDecimal("1000"));
            when(budgetMapper.selectById(1L)).thenReturn(entity);
            // 4 次 selectList：分别对应 LABOR/TRAVEL/AGENT/OTHER
            when(costMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(buildCost(new BigDecimal("100"))))
                    .thenReturn(List.of(buildCost(new BigDecimal("50"))))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(List.of(buildCost(new BigDecimal("25"))));

            FinanceBudgetVO vo = budgetService.getDetail(1L);

            assertNotNull(vo);
            assertAll("实际成本聚合",
                    () -> assertEquals(new BigDecimal("100"), vo.getActualLabor()),
                    () -> assertEquals(new BigDecimal("50"), vo.getActualTravel()),
                    () -> assertEquals(BigDecimal.ZERO, vo.getActualAgent()),
                    () -> assertEquals(new BigDecimal("25"), vo.getActualOther()),
                    () -> assertEquals(new BigDecimal("175"), vo.getActualTotal())
            );
        }

        @Test
        @DisplayName("实际成本全部为空时 actualTotal = 0")
        void should_return_zero_when_all_cost_empty() {
            FinanceBudgetEntity entity = buildEntity(2L, 11L, 2026, FinanceConstant.BUDGET_STATUS_APPROVED);
            when(budgetMapper.selectById(2L)).thenReturn(entity);
            when(costMapper.selectList(any())).thenReturn(Collections.emptyList());

            FinanceBudgetVO vo = budgetService.getDetail(2L);

            assertEquals(BigDecimal.ZERO, vo.getActualTotal());
        }
    }

    @Nested
    @DisplayName("save 保存预算")
    class SaveTest {

        @Test
        @DisplayName("正常保存：计算总额、状态 DRAFT")
        void should_save_and_calc_total() {
            FinanceBudgetSaveDTO dto = buildSaveDto(10L, 2026,
                    new BigDecimal("100"), new BigDecimal("200"),
                    new BigDecimal("300"), new BigDecimal("50"));

            when(budgetMapper.insert(any(FinanceBudgetEntity.class))).thenAnswer(invocation -> {
                FinanceBudgetEntity e = invocation.getArgument(0);
                e.setId(500L);
                return 1;
            });

            Long id = budgetService.save(dto);

            assertEquals(500L, id);
            ArgumentCaptor<FinanceBudgetEntity> captor = ArgumentCaptor.forClass(FinanceBudgetEntity.class);
            verify(budgetMapper).insert(captor.capture());
            FinanceBudgetEntity saved = captor.getValue();
            assertAll("字段映射",
                    () -> assertEquals(10L, saved.getProjectId()),
                    () -> assertEquals(2026, saved.getYear()),
                    () -> assertEquals(new BigDecimal("650"), saved.getTotalAmount()),
                    () -> assertEquals(FinanceConstant.BUDGET_STATUS_DRAFT, saved.getApprovalStatus())
            );
        }

        @Test
        @DisplayName("部分金额为 null 时不计入总额")
        void should_treat_null_amount_as_zero() {
            FinanceBudgetSaveDTO dto = buildSaveDto(10L, 2026,
                    new BigDecimal("100"), null, null, null);

            budgetService.save(dto);

            ArgumentCaptor<FinanceBudgetEntity> captor = ArgumentCaptor.forClass(FinanceBudgetEntity.class);
            verify(budgetMapper).insert(captor.capture());
            assertEquals(new BigDecimal("100"), captor.getValue().getTotalAmount());
        }
    }

    @Nested
    @DisplayName("update 更新预算")
    class UpdateTest {

        @Test
        @DisplayName("预算不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(budgetMapper.selectById(99L)).thenReturn(null);
            FinanceBudgetSaveDTO dto = buildSaveDto(10L, 2026, BigDecimal.ONE, null, null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(budgetMapper, never()).updateById(any(FinanceBudgetEntity.class));
        }

        @Test
        @DisplayName("非草稿状态禁止修改抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_draft() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_PENDING);
            when(budgetMapper.selectById(1L)).thenReturn(exist);
            FinanceBudgetSaveDTO dto = buildSaveDto(10L, 2026, BigDecimal.ONE, null, null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.update(1L, dto));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(budgetMapper, never()).updateById(any(FinanceBudgetEntity.class));
        }

        @Test
        @DisplayName("草稿状态可正常更新并重算总额")
        void should_update_when_draft() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_DRAFT);
            when(budgetMapper.selectById(1L)).thenReturn(exist);
            FinanceBudgetSaveDTO dto = buildSaveDto(11L, 2027,
                    new BigDecimal("10"), new BigDecimal("20"), null, null);

            budgetService.update(1L, dto);

            ArgumentCaptor<FinanceBudgetEntity> captor = ArgumentCaptor.forClass(FinanceBudgetEntity.class);
            verify(budgetMapper).updateById(captor.capture());
            assertAll("更新字段",
                    () -> assertEquals(1L, captor.getValue().getId()),
                    () -> assertEquals(11L, captor.getValue().getProjectId()),
                    () -> assertEquals(2027, captor.getValue().getYear()),
                    () -> assertEquals(new BigDecimal("30"), captor.getValue().getTotalAmount())
            );
        }
    }

    @Nested
    @DisplayName("delete 删除预算")
    class DeleteTest {

        @Test
        @DisplayName("预算不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(budgetMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(budgetMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_DRAFT);
            when(budgetMapper.selectById(1L)).thenReturn(exist);

            budgetService.delete(1L);

            verify(budgetMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("submit 提交审批")
    class SubmitTest {

        @Test
        @DisplayName("预算不存在抛 NOT_FOUND")
        void should_throw_not_found_when_submit_missing() {
            when(budgetMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.submit(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非草稿状态禁止提交抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_submit_non_draft() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_APPROVED);
            when(budgetMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.submit(1L));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(budgetMapper, never()).updateById(any(FinanceBudgetEntity.class));
        }

        @Test
        @DisplayName("草稿状态提交后变 PENDING")
        void should_transition_to_pending_when_submit_draft() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_DRAFT);
            when(budgetMapper.selectById(1L)).thenReturn(exist);

            budgetService.submit(1L);

            ArgumentCaptor<FinanceBudgetEntity> captor = ArgumentCaptor.forClass(FinanceBudgetEntity.class);
            verify(budgetMapper).updateById(captor.capture());
            assertEquals(FinanceConstant.BUDGET_STATUS_PENDING, captor.getValue().getApprovalStatus());
        }
    }

    @Nested
    @DisplayName("approve 审批")
    class ApproveTest {

        @Test
        @DisplayName("预算不存在抛 NOT_FOUND")
        void should_throw_not_found_when_approve_missing() {
            when(budgetMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.approve(99L, true, "ok"));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非待审批状态禁止审批抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_pending() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_DRAFT);
            when(budgetMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> budgetService.approve(1L, true, null));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("通过审批：状态 APPROVED、写入 approverId 与 approveTime、备注写入")
        void should_approve_pass_and_set_approver() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_PENDING);
            when(budgetMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(88L).build());

            budgetService.approve(1L, true, "通过");

            ArgumentCaptor<FinanceBudgetEntity> captor = ArgumentCaptor.forClass(FinanceBudgetEntity.class);
            verify(budgetMapper).updateById(captor.capture());
            FinanceBudgetEntity updated = captor.getValue();
            assertAll("审批通过字段",
                    () -> assertEquals(FinanceConstant.BUDGET_STATUS_APPROVED, updated.getApprovalStatus()),
                    () -> assertEquals(88L, updated.getApproverId()),
                    () -> assertNotNull(updated.getApproveTime()),
                    () -> assertEquals("通过", updated.getRemark())
            );
        }

        @Test
        @DisplayName("驳回审批：状态 REJECTED、remark 为 null 时不覆盖原 remark")
        void should_approve_reject_and_keep_remark_when_null() {
            FinanceBudgetEntity exist = buildEntity(1L, 10L, 2026, FinanceConstant.BUDGET_STATUS_PENDING);
            exist.setRemark("原备注");
            when(budgetMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(2L).build());

            budgetService.approve(1L, false, null);

            ArgumentCaptor<FinanceBudgetEntity> captor = ArgumentCaptor.forClass(FinanceBudgetEntity.class);
            verify(budgetMapper).updateById(captor.capture());
            assertEquals(FinanceConstant.BUDGET_STATUS_REJECTED, captor.getValue().getApprovalStatus());
            assertEquals("原备注", captor.getValue().getRemark());
        }
    }

    @Nested
    @DisplayName("sumActualCost 实际成本汇总")
    class SumActualCostTest {

        @Test
        @DisplayName("按年度与类型汇总：累加 amount")
        void should_sum_amounts_by_year_and_type() {
            when(costMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(
                            buildCost(new BigDecimal("100")),
                            buildCost(new BigDecimal("250.50")),
                            buildCost(new BigDecimal("50"))
                    ));

            BigDecimal total = budgetService.sumActualCost(10L, 2026, FinanceConstant.COST_TYPE_LABOR);

            assertEquals(new BigDecimal("400.50"), total);
        }

        @Test
        @DisplayName("空列表返回 0")
        void should_return_zero_when_empty() {
            when(costMapper.selectList(any())).thenReturn(Collections.emptyList());

            BigDecimal total = budgetService.sumActualCost(10L, 2026, null);

            assertEquals(BigDecimal.ZERO, total);
        }
    }

    /* ============ 测试辅助方法 ============ */

    private FinanceBudgetEntity buildEntity(Long id, Long projectId, Integer year, String status) {
        FinanceBudgetEntity entity = new FinanceBudgetEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setYear(year);
        entity.setApprovalStatus(status);
        entity.setLaborAmount(BigDecimal.ZERO);
        entity.setTravelAmount(BigDecimal.ZERO);
        entity.setAgentAmount(BigDecimal.ZERO);
        entity.setOtherAmount(BigDecimal.ZERO);
        return entity;
    }

    private FinanceCostEntity buildCost(BigDecimal amount) {
        FinanceCostEntity cost = new FinanceCostEntity();
        cost.setAmount(amount);
        return cost;
    }

    private FinanceBudgetSaveDTO buildSaveDto(Long projectId, Integer year,
                                                BigDecimal labor, BigDecimal travel,
                                                BigDecimal agent, BigDecimal other) {
        FinanceBudgetSaveDTO dto = new FinanceBudgetSaveDTO();
        dto.setProjectId(projectId);
        dto.setYear(year);
        dto.setLaborAmount(labor);
        dto.setTravelAmount(travel);
        dto.setAgentAmount(agent);
        dto.setOtherAmount(other);
        return dto;
    }
}
