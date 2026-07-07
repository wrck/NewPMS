package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.finance.constant.FinanceConstant;
import com.vibe.finance.dto.FinanceWorkloadQueryDTO;
import com.vibe.finance.dto.FinanceWorkloadSaveDTO;
import com.vibe.finance.entity.FinanceWorkloadConfirmationEntity;
import com.vibe.finance.mapper.FinanceWorkloadConfirmationMapper;
import com.vibe.finance.vo.FinanceWorkloadConfirmationVO;
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
 * 代理商结算服务实现单元测试（Task 3 SubTask 3.2）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、查询条件透传、空结果</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>save：总额计算（工作量×单价 + 差旅 + 其他）、初始状态 DRAFT/UNPAID</li>
 *   <li>update：不存在 / 非草稿状态禁止修改 / 正常更新</li>
 *   <li>delete：不存在 / 正常删除</li>
 *   <li>pmConfirm：DRAFT → PM_CONFIRMED，写入 PM 确认人与时间</li>
 *   <li>agentConfirm：PM_CONFIRMED → PENDING，写入代理商确认人与时间</li>
 *   <li>directorApprove：PENDING → DIRECTOR_APPROVED/REJECTED，remark 处理</li>
 *   <li>financeApprove：DIRECTOR_APPROVED → FINANCE_APPROVED/REJECTED</li>
 *   <li>updatePaymentStatus：仅 FINANCE_APPROVED 可更新付款状态</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("代理商结算服务 FinanceWorkloadServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class FinanceWorkloadServiceImplTest {

    @Mock
    private FinanceWorkloadConfirmationMapper workloadMapper;

    @InjectMocks
    private FinanceWorkloadServiceImpl workloadService;

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
            FinanceWorkloadQueryDTO query = new FinanceWorkloadQueryDTO();
            query.setProjectId(10L);
            query.setApprovalStatus(FinanceConstant.SETTLEMENT_STATUS_PENDING);

            Page<FinanceWorkloadConfirmationEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PENDING)));
            mockPage.setTotal(1L);
            when(workloadMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<FinanceWorkloadConfirmationVO> result = workloadService.page(query);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1L, result.getTotal());
            assertEquals(10L, result.getRecords().get(0).getProjectId());
        }

        @Test
        @DisplayName("空结果时返回空列表")
        void should_return_empty_when_no_data() {
            FinanceWorkloadQueryDTO query = new FinanceWorkloadQueryDTO();
            Page<FinanceWorkloadConfirmationEntity> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(workloadMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<FinanceWorkloadConfirmationVO> result = workloadService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.getDetail(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_vo() {
            FinanceWorkloadConfirmationEntity entity = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_DRAFT);
            when(workloadMapper.selectById(1L)).thenReturn(entity);

            FinanceWorkloadConfirmationVO vo = workloadService.getDetail(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals(10L, vo.getProjectId());
        }
    }

    @Nested
    @DisplayName("save 保存结算单")
    class SaveTest {

        @Test
        @DisplayName("正常保存：计算总额、状态 DRAFT、付款状态 UNPAID")
        void should_save_and_calc_total() {
            FinanceWorkloadSaveDTO dto = buildSaveDto(10L, 50L,
                    new BigDecimal("5"), new BigDecimal("200"),
                    new BigDecimal("100"), new BigDecimal("50"));

            when(workloadMapper.insert(any(FinanceWorkloadConfirmationEntity.class))).thenAnswer(invocation -> {
                FinanceWorkloadConfirmationEntity e = invocation.getArgument(0);
                e.setId(500L);
                return 1;
            });

            Long id = workloadService.save(dto);

            assertEquals(500L, id);
            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).insert(captor.capture());
            FinanceWorkloadConfirmationEntity saved = captor.getValue();
            assertAll("字段映射",
                    () -> assertEquals(10L, saved.getProjectId()),
                    () -> assertEquals(50L, saved.getAgentCompanyId()),
                    () -> assertEquals(new BigDecimal("1150"), saved.getTotalAmount()),
                    () -> assertEquals(FinanceConstant.SETTLEMENT_STATUS_DRAFT, saved.getApprovalStatus()),
                    () -> assertEquals(FinanceConstant.PAYMENT_STATUS_UNPAID, saved.getPaymentStatus())
            );
        }

        @Test
        @DisplayName("部分金额为 null 时按 0 计算")
        void should_treat_null_amount_as_zero() {
            FinanceWorkloadSaveDTO dto = buildSaveDto(10L, 50L,
                    new BigDecimal("3"), new BigDecimal("100"), null, null);

            workloadService.save(dto);

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).insert(captor.capture());
            // 3 * 100 + 0 + 0 = 300
            assertEquals(new BigDecimal("300"), captor.getValue().getTotalAmount());
        }
    }

    @Nested
    @DisplayName("update 更新结算单")
    class UpdateTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);
            FinanceWorkloadSaveDTO dto = buildSaveDto(10L, 50L,
                    BigDecimal.ONE, BigDecimal.ONE, null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.update(99L, dto));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("非草稿状态禁止修改抛 STATE_NOT_ALLOWED")
        void should_throw_state_not_allowed_when_not_draft() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PENDING);
            when(workloadMapper.selectById(1L)).thenReturn(exist);
            FinanceWorkloadSaveDTO dto = buildSaveDto(10L, 50L,
                    BigDecimal.ONE, BigDecimal.ONE, null, null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.update(1L, dto));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("草稿状态可正常更新并重算总额")
        void should_update_when_draft() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_DRAFT);
            when(workloadMapper.selectById(1L)).thenReturn(exist);
            FinanceWorkloadSaveDTO dto = buildSaveDto(11L, 51L,
                    new BigDecimal("2"), new BigDecimal("50"), new BigDecimal("20"), null);

            workloadService.update(1L, dto);

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            assertAll("更新字段",
                    () -> assertEquals(1L, captor.getValue().getId()),
                    () -> assertEquals(11L, captor.getValue().getProjectId()),
                    () -> assertEquals(51L, captor.getValue().getAgentCompanyId()),
                    () -> assertEquals(new BigDecimal("120"), captor.getValue().getTotalAmount())
            );
        }
    }

    @Nested
    @DisplayName("delete 删除结算单")
    class DeleteTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_delete_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.delete(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(workloadMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("正常删除")
        void should_delete_success() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_DRAFT);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            workloadService.delete(1L);

            verify(workloadMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("pmConfirm PM 确认")
    class PmConfirmTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_pm_confirm_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.pmConfirm(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非草稿状态禁止 PM 确认")
        void should_throw_state_not_allowed_when_not_draft() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PM_CONFIRMED);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.pmConfirm(1L));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("草稿状态 PM 确认后变 PM_CONFIRMED 并写入确认人")
        void should_transition_to_pm_confirmed() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_DRAFT);
            when(workloadMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(77L).build());

            workloadService.pmConfirm(1L);

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            FinanceWorkloadConfirmationEntity updated = captor.getValue();
            assertAll("PM 确认字段",
                    () -> assertEquals(FinanceConstant.SETTLEMENT_STATUS_PM_CONFIRMED, updated.getApprovalStatus()),
                    () -> assertEquals(77L, updated.getPmConfirmUserId()),
                    () -> assertNotNull(updated.getPmConfirmTime())
            );
        }
    }

    @Nested
    @DisplayName("agentConfirm 代理商确认")
    class AgentConfirmTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_agent_confirm_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.agentConfirm(99L));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 PM_CONFIRMED 状态禁止代理商确认")
        void should_throw_state_not_allowed_when_not_pm_confirmed() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_DRAFT);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.agentConfirm(1L));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("PM_CONFIRMED 状态代理商确认后变 PENDING（注意覆盖 AGENT_CONFIRMED）")
        void should_transition_to_pending() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PM_CONFIRMED);
            when(workloadMapper.selectById(1L)).thenReturn(exist);
            UserContextHolder.set(UserContext.builder().userId(66L).build());

            workloadService.agentConfirm(1L);

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            FinanceWorkloadConfirmationEntity updated = captor.getValue();
            assertAll("代理商确认字段",
                    () -> assertEquals(FinanceConstant.SETTLEMENT_STATUS_PENDING, updated.getApprovalStatus()),
                    () -> assertEquals(66L, updated.getAgentConfirmUserId()),
                    () -> assertNotNull(updated.getAgentConfirmTime())
            );
        }
    }

    @Nested
    @DisplayName("directorApprove 总监审批")
    class DirectorApproveTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_director_approve_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.directorApprove(99L, true, null));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 PENDING 状态禁止总监审批")
        void should_throw_state_not_allowed_when_not_pending() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_DRAFT);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.directorApprove(1L, true, null));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("通过审批：状态 DIRECTOR_APPROVED、remark 写入")
        void should_approve_pass() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PENDING);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            workloadService.directorApprove(1L, true, "总监通过");

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            assertAll("总监通过字段",
                    () -> assertEquals(FinanceConstant.SETTLEMENT_STATUS_DIRECTOR_APPROVED,
                            captor.getValue().getApprovalStatus()),
                    () -> assertEquals("总监通过", captor.getValue().getRemark())
            );
        }

        @Test
        @DisplayName("驳回审批：状态 REJECTED、remark 为 null 时不覆盖")
        void should_approve_reject_and_keep_remark_when_null() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PENDING);
            exist.setRemark("原备注");
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            workloadService.directorApprove(1L, false, null);

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            assertEquals(FinanceConstant.SETTLEMENT_STATUS_REJECTED, captor.getValue().getApprovalStatus());
            assertEquals("原备注", captor.getValue().getRemark());
        }
    }

    @Nested
    @DisplayName("financeApprove 财务审批")
    class FinanceApproveTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_finance_approve_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.financeApprove(99L, true, null));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 DIRECTOR_APPROVED 状态禁止财务审批")
        void should_throw_state_not_allowed_when_not_director_approved() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L, FinanceConstant.SETTLEMENT_STATUS_PENDING);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.financeApprove(1L, true, null));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("通过审批：状态 FINANCE_APPROVED")
        void should_approve_pass() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L,
                    FinanceConstant.SETTLEMENT_STATUS_DIRECTOR_APPROVED);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            workloadService.financeApprove(1L, true, "财务通过");

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            assertEquals(FinanceConstant.SETTLEMENT_STATUS_FINANCE_APPROVED,
                    captor.getValue().getApprovalStatus());
        }

        @Test
        @DisplayName("驳回审批：状态 REJECTED")
        void should_approve_reject() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L,
                    FinanceConstant.SETTLEMENT_STATUS_DIRECTOR_APPROVED);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            workloadService.financeApprove(1L, false, "材料不全");

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            assertEquals(FinanceConstant.SETTLEMENT_STATUS_REJECTED,
                    captor.getValue().getApprovalStatus());
            assertEquals("材料不全", captor.getValue().getRemark());
        }
    }

    @Nested
    @DisplayName("updatePaymentStatus 更新付款状态")
    class UpdatePaymentStatusTest {

        @Test
        @DisplayName("结算单不存在抛 NOT_FOUND")
        void should_throw_not_found_when_update_payment_missing() {
            when(workloadMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.updatePaymentStatus(99L, FinanceConstant.PAYMENT_STATUS_PAID));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 FINANCE_APPROVED 状态禁止更新付款状态")
        void should_throw_state_not_allowed_when_not_finance_approved() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L,
                    FinanceConstant.SETTLEMENT_STATUS_DIRECTOR_APPROVED);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workloadService.updatePaymentStatus(1L, FinanceConstant.PAYMENT_STATUS_PAID));
            assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
            verify(workloadMapper, never()).updateById(any(FinanceWorkloadConfirmationEntity.class));
        }

        @Test
        @DisplayName("FINANCE_APPROVED 状态可更新付款状态")
        void should_update_payment_status() {
            FinanceWorkloadConfirmationEntity exist = buildEntity(1L, 10L,
                    FinanceConstant.SETTLEMENT_STATUS_FINANCE_APPROVED);
            exist.setPaymentStatus(FinanceConstant.PAYMENT_STATUS_UNPAID);
            when(workloadMapper.selectById(1L)).thenReturn(exist);

            workloadService.updatePaymentStatus(1L, FinanceConstant.PAYMENT_STATUS_PAID);

            ArgumentCaptor<FinanceWorkloadConfirmationEntity> captor =
                    ArgumentCaptor.forClass(FinanceWorkloadConfirmationEntity.class);
            verify(workloadMapper).updateById(captor.capture());
            assertEquals(FinanceConstant.PAYMENT_STATUS_PAID, captor.getValue().getPaymentStatus());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private FinanceWorkloadConfirmationEntity buildEntity(Long id, Long projectId, String status) {
        FinanceWorkloadConfirmationEntity entity = new FinanceWorkloadConfirmationEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setAgentCompanyId(50L);
        entity.setPeriod("2026-07");
        entity.setApprovalStatus(status);
        entity.setPaymentStatus(FinanceConstant.PAYMENT_STATUS_UNPAID);
        return entity;
    }

    private FinanceWorkloadSaveDTO buildSaveDto(Long projectId, Long agentCompanyId,
                                                 BigDecimal workloadDays, BigDecimal unitPrice,
                                                 BigDecimal travelAmount, BigDecimal otherAmount) {
        FinanceWorkloadSaveDTO dto = new FinanceWorkloadSaveDTO();
        dto.setProjectId(projectId);
        dto.setAgentCompanyId(agentCompanyId);
        dto.setPeriod("2026-07");
        dto.setWorkloadDays(workloadDays);
        dto.setUnitPrice(unitPrice);
        dto.setTravelAmount(travelAmount);
        dto.setOtherAmount(otherAmount);
        return dto;
    }
}
