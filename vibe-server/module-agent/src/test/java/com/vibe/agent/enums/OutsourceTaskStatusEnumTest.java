package com.vibe.agent.enums;

import com.vibe.agent.constant.AgentConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 转包任务状态机枚举单元测试
 *
 * <p>主流程：PENDING → ACCEPTED → IN_PROGRESS → SUBMITTED → CONFIRMED
 * 分支：REJECTED / RETURNED / OVERDUE</p>
 *
 * @author vibe
 */
@DisplayName("转包任务状态机 OutsourceTaskStatusEnum 测试")
class OutsourceTaskStatusEnumTest {

    @Nested
    @DisplayName("主流程合法流转")
    class MainFlowTest {

        @Test
        @DisplayName("PENDING → ACCEPTED 合法")
        void should_allow_pending_to_accepted() {
            assertTrue(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.ACCEPTED));
        }

        @Test
        @DisplayName("ACCEPTED → IN_PROGRESS 合法")
        void should_allow_accepted_to_in_progress() {
            assertTrue(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS));
        }

        @Test
        @DisplayName("IN_PROGRESS → SUBMITTED 合法")
        void should_allow_in_progress_to_submitted() {
            assertTrue(OutsourceTaskStatusEnum.IN_PROGRESS.canTransitionTo(OutsourceTaskStatusEnum.SUBMITTED));
        }

        @Test
        @DisplayName("SUBMITTED → CONFIRMED 合法")
        void should_allow_submitted_to_confirmed() {
            assertTrue(OutsourceTaskStatusEnum.SUBMITTED.canTransitionTo(OutsourceTaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("完整主流程 PENDING→ACCEPTED→IN_PROGRESS→SUBMITTED→CONFIRMED 全部合法")
        void should_allow_full_main_flow() {
            assertAll("转包任务主流程链路全部允许",
                    () -> assertTrue(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.ACCEPTED)),
                    () -> assertTrue(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS)),
                    () -> assertTrue(OutsourceTaskStatusEnum.IN_PROGRESS.canTransitionTo(OutsourceTaskStatusEnum.SUBMITTED)),
                    () -> assertTrue(OutsourceTaskStatusEnum.SUBMITTED.canTransitionTo(OutsourceTaskStatusEnum.CONFIRMED))
            );
        }
    }

    @Nested
    @DisplayName("REJECTED 拒绝分支")
    class RejectedTest {

        @Test
        @DisplayName("PENDING → REJECTED 合法")
        void should_allow_pending_to_rejected() {
            assertTrue(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.REJECTED));
        }

        @Test
        @DisplayName("REJECTED 是终态")
        void should_rejected_be_terminal() {
            assertTrue(OutsourceTaskStatusEnum.REJECTED.isTerminal());
        }

        @Test
        @DisplayName("REJECTED 终态不可再流转")
        void should_reject_rejected_to_any() {
            assertAll("REJECTED 终态不可流转",
                    () -> assertFalse(OutsourceTaskStatusEnum.REJECTED.canTransitionTo(OutsourceTaskStatusEnum.ACCEPTED)),
                    () -> assertFalse(OutsourceTaskStatusEnum.REJECTED.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS)),
                    () -> assertFalse(OutsourceTaskStatusEnum.REJECTED.canTransitionTo(OutsourceTaskStatusEnum.PENDING))
            );
        }
    }

    @Nested
    @DisplayName("RETURNED 退回分支")
    class ReturnedTest {

        @Test
        @DisplayName("SUBMITTED → RETURNED 审核退回合法")
        void should_allow_submitted_to_returned() {
            assertTrue(OutsourceTaskStatusEnum.SUBMITTED.canTransitionTo(OutsourceTaskStatusEnum.RETURNED));
        }

        @Test
        @DisplayName("RETURNED → IN_PROGRESS 回到执行合法")
        void should_allow_returned_to_in_progress() {
            assertTrue(OutsourceTaskStatusEnum.RETURNED.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS));
        }

        @Test
        @DisplayName("RETURNED 不是终态")
        void should_returned_not_be_terminal() {
            assertFalse(OutsourceTaskStatusEnum.RETURNED.isTerminal());
        }
    }

    @Nested
    @DisplayName("OVERDUE 超期分支")
    class OverdueTest {

        @Test
        @DisplayName("PENDING → OVERDUE 合法")
        void should_allow_pending_to_overdue() {
            assertTrue(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.OVERDUE));
        }

        @Test
        @DisplayName("ACCEPTED → OVERDUE 合法")
        void should_allow_accepted_to_overdue() {
            assertTrue(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(OutsourceTaskStatusEnum.OVERDUE));
        }

        @Test
        @DisplayName("IN_PROGRESS → OVERDUE 合法")
        void should_allow_in_progress_to_overdue() {
            assertTrue(OutsourceTaskStatusEnum.IN_PROGRESS.canTransitionTo(OutsourceTaskStatusEnum.OVERDUE));
        }

        @Test
        @DisplayName("SUBMITTED → OVERDUE 合法")
        void should_allow_submitted_to_overdue() {
            assertTrue(OutsourceTaskStatusEnum.SUBMITTED.canTransitionTo(OutsourceTaskStatusEnum.OVERDUE));
        }

        @Test
        @DisplayName("RETURNED → OVERDUE 合法")
        void should_allow_returned_to_overdue() {
            assertTrue(OutsourceTaskStatusEnum.RETURNED.canTransitionTo(OutsourceTaskStatusEnum.OVERDUE));
        }

        @Test
        @DisplayName("OVERDUE 是终态")
        void should_overdue_be_terminal() {
            assertTrue(OutsourceTaskStatusEnum.OVERDUE.isTerminal());
        }

        @Test
        @DisplayName("OVERDUE 终态不可再流转")
        void should_reject_overdue_to_any() {
            assertAll("OVERDUE 终态不可流转",
                    () -> assertFalse(OutsourceTaskStatusEnum.OVERDUE.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS)),
                    () -> assertFalse(OutsourceTaskStatusEnum.OVERDUE.canTransitionTo(OutsourceTaskStatusEnum.SUBMITTED)),
                    () -> assertFalse(OutsourceTaskStatusEnum.OVERDUE.canTransitionTo(OutsourceTaskStatusEnum.CONFIRMED))
            );
        }
    }

    @Nested
    @DisplayName("CONFIRMED 终态")
    class ConfirmedTest {

        @Test
        @DisplayName("CONFIRMED 是终态")
        void should_confirmed_be_terminal() {
            assertTrue(OutsourceTaskStatusEnum.CONFIRMED.isTerminal());
        }

        @Test
        @DisplayName("CONFIRMED 终态不可再流转")
        void should_reject_confirmed_to_any() {
            assertAll("CONFIRMED 终态不可流转",
                    () -> assertFalse(OutsourceTaskStatusEnum.CONFIRMED.canTransitionTo(OutsourceTaskStatusEnum.SUBMITTED)),
                    () -> assertFalse(OutsourceTaskStatusEnum.CONFIRMED.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS)),
                    () -> assertFalse(OutsourceTaskStatusEnum.CONFIRMED.canTransitionTo(OutsourceTaskStatusEnum.OVERDUE))
            );
        }
    }

    @Nested
    @DisplayName("非法流转被拒绝")
    class InvalidTransitionTest {

        @Test
        @DisplayName("PENDING → IN_PROGRESS 跨阶段非法")
        void should_reject_pending_to_in_progress() {
            assertFalse(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.IN_PROGRESS));
        }

        @Test
        @DisplayName("PENDING → SUBMITTED 跨阶段非法")
        void should_reject_pending_to_submitted() {
            assertFalse(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.SUBMITTED));
        }

        @Test
        @DisplayName("PENDING → CONFIRMED 跨阶段非法")
        void should_reject_pending_to_confirmed() {
            assertFalse(OutsourceTaskStatusEnum.PENDING.canTransitionTo(OutsourceTaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("ACCEPTED → SUBMITTED 跨阶段非法")
        void should_reject_accepted_to_submitted() {
            assertFalse(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(OutsourceTaskStatusEnum.SUBMITTED));
        }

        @Test
        @DisplayName("ACCEPTED → CONFIRMED 跨阶段非法")
        void should_reject_accepted_to_confirmed() {
            assertFalse(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(OutsourceTaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("IN_PROGRESS → CONFIRMED 跨阶段非法")
        void should_reject_in_progress_to_confirmed() {
            assertFalse(OutsourceTaskStatusEnum.IN_PROGRESS.canTransitionTo(OutsourceTaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("IN_PROGRESS → RETURNED 非法（必须先提交）")
        void should_reject_in_progress_to_returned() {
            assertFalse(OutsourceTaskStatusEnum.IN_PROGRESS.canTransitionTo(OutsourceTaskStatusEnum.RETURNED));
        }

        @Test
        @DisplayName("ACCEPTED → REJECTED 非法（仅 PENDING 可拒绝）")
        void should_reject_accepted_to_rejected() {
            assertFalse(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(OutsourceTaskStatusEnum.REJECTED));
        }

        @Test
        @DisplayName("SUBMITTED → ACCEPTED 逆向非法")
        void should_reject_submitted_to_accepted() {
            assertFalse(OutsourceTaskStatusEnum.SUBMITTED.canTransitionTo(OutsourceTaskStatusEnum.ACCEPTED));
        }

        @Test
        @DisplayName("null 目标状态非法")
        void should_reject_null_target() {
            assertFalse(OutsourceTaskStatusEnum.PENDING.canTransitionTo(null));
            assertFalse(OutsourceTaskStatusEnum.ACCEPTED.canTransitionTo(null));
        }
    }

    @Nested
    @DisplayName("of() code 解析")
    class OfTest {

        @Test
        @DisplayName("合法 code 解析为对应枚举")
        void should_parse_valid_code() {
            assertEquals(OutsourceTaskStatusEnum.PENDING, OutsourceTaskStatusEnum.of(AgentConstant.TASK_PENDING));
            assertEquals(OutsourceTaskStatusEnum.ACCEPTED, OutsourceTaskStatusEnum.of(AgentConstant.TASK_ACCEPTED));
            assertEquals(OutsourceTaskStatusEnum.REJECTED, OutsourceTaskStatusEnum.of(AgentConstant.TASK_REJECTED));
            assertEquals(OutsourceTaskStatusEnum.IN_PROGRESS, OutsourceTaskStatusEnum.of(AgentConstant.TASK_IN_PROGRESS));
            assertEquals(OutsourceTaskStatusEnum.SUBMITTED, OutsourceTaskStatusEnum.of(AgentConstant.TASK_SUBMITTED));
            assertEquals(OutsourceTaskStatusEnum.CONFIRMED, OutsourceTaskStatusEnum.of(AgentConstant.TASK_CONFIRMED));
            assertEquals(OutsourceTaskStatusEnum.RETURNED, OutsourceTaskStatusEnum.of(AgentConstant.TASK_RETURNED));
            assertEquals(OutsourceTaskStatusEnum.OVERDUE, OutsourceTaskStatusEnum.of(AgentConstant.TASK_OVERDUE));
        }

        @Test
        @DisplayName("非法 code 解析为 null")
        void should_return_null_for_invalid_code() {
            assertNull(OutsourceTaskStatusEnum.of("NOT_EXIST"));
            assertNull(OutsourceTaskStatusEnum.of(""));
        }

        @Test
        @DisplayName("null code 解析为 null")
        void should_return_null_for_null_code() {
            assertNull(OutsourceTaskStatusEnum.of(null));
        }

        @Test
        @DisplayName("每个枚举的 code 与 description 非空")
        void should_all_codes_and_descs_be_non_null() {
            for (OutsourceTaskStatusEnum e : OutsourceTaskStatusEnum.values()) {
                assertNotNull(e.getCode(), "code 不能为空: " + e);
                assertNotNull(e.getDescription(), "description 不能为空: " + e);
            }
        }
    }
}
