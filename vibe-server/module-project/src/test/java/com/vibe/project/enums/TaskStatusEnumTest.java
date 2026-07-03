package com.vibe.project.enums;

import com.vibe.project.constant.ProjectConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目任务状态机枚举单元测试
 *
 * <p>主流程：PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → CONFIRMED，
 * 含退回/驳回逆向流转。</p>
 *
 * @author vibe
 */
@DisplayName("项目任务状态机 TaskStatusEnum 测试")
class TaskStatusEnumTest {

    @Nested
    @DisplayName("主流程合法流转")
    class MainFlowTest {

        @Test
        @DisplayName("PENDING → ASSIGNED 合法")
        void should_allow_pending_to_assigned() {
            assertTrue(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.ASSIGNED));
        }

        @Test
        @DisplayName("ASSIGNED → IN_PROGRESS 合法")
        void should_allow_assigned_to_in_progress() {
            assertTrue(TaskStatusEnum.ASSIGNED.canTransitionTo(TaskStatusEnum.IN_PROGRESS));
        }

        @Test
        @DisplayName("IN_PROGRESS → COMPLETED 合法")
        void should_allow_in_progress_to_completed() {
            assertTrue(TaskStatusEnum.IN_PROGRESS.canTransitionTo(TaskStatusEnum.COMPLETED));
        }

        @Test
        @DisplayName("COMPLETED → CONFIRMED 合法")
        void should_allow_completed_to_confirmed() {
            assertTrue(TaskStatusEnum.COMPLETED.canTransitionTo(TaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("完整主流程 PENDING→ASSIGNED→IN_PROGRESS→COMPLETED→CONFIRMED 全部合法")
        void should_allow_full_main_flow() {
            assertAll("任务主流程链路全部允许",
                    () -> assertTrue(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.ASSIGNED)),
                    () -> assertTrue(TaskStatusEnum.ASSIGNED.canTransitionTo(TaskStatusEnum.IN_PROGRESS)),
                    () -> assertTrue(TaskStatusEnum.IN_PROGRESS.canTransitionTo(TaskStatusEnum.COMPLETED)),
                    () -> assertTrue(TaskStatusEnum.COMPLETED.canTransitionTo(TaskStatusEnum.CONFIRMED))
            );
        }
    }

    @Nested
    @DisplayName("退回/驳回流转")
    class RollbackTest {

        @Test
        @DisplayName("ASSIGNED → PENDING 退回合法")
        void should_allow_assigned_to_pending_rollback() {
            assertTrue(TaskStatusEnum.ASSIGNED.canTransitionTo(TaskStatusEnum.PENDING));
        }

        @Test
        @DisplayName("IN_PROGRESS → ASSIGNED 退回合法")
        void should_allow_in_progress_to_assigned_rollback() {
            assertTrue(TaskStatusEnum.IN_PROGRESS.canTransitionTo(TaskStatusEnum.ASSIGNED));
        }

        @Test
        @DisplayName("COMPLETED → IN_PROGRESS 驳回返工合法")
        void should_allow_completed_to_in_progress_reject() {
            assertTrue(TaskStatusEnum.COMPLETED.canTransitionTo(TaskStatusEnum.IN_PROGRESS));
        }
    }

    @Nested
    @DisplayName("非法流转被拒绝")
    class InvalidTransitionTest {

        @Test
        @DisplayName("PENDING → IN_PROGRESS 跨阶段非法")
        void should_reject_pending_to_in_progress() {
            assertFalse(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.IN_PROGRESS));
        }

        @Test
        @DisplayName("PENDING → COMPLETED 跨阶段非法")
        void should_reject_pending_to_completed() {
            assertFalse(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.COMPLETED));
        }

        @Test
        @DisplayName("PENDING → CONFIRMED 跨阶段非法")
        void should_reject_pending_to_confirmed() {
            assertFalse(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("ASSIGNED → COMPLETED 跨阶段非法")
        void should_reject_assigned_to_completed() {
            assertFalse(TaskStatusEnum.ASSIGNED.canTransitionTo(TaskStatusEnum.COMPLETED));
        }

        @Test
        @DisplayName("CONFIRMED → 任意状态 非法（终态保护）")
        void should_reject_confirmed_to_any() {
            assertAll("CONFIRMED 终态不可流转",
                    () -> assertFalse(TaskStatusEnum.CONFIRMED.canTransitionTo(TaskStatusEnum.PENDING)),
                    () -> assertFalse(TaskStatusEnum.CONFIRMED.canTransitionTo(TaskStatusEnum.ASSIGNED)),
                    () -> assertFalse(TaskStatusEnum.CONFIRMED.canTransitionTo(TaskStatusEnum.IN_PROGRESS)),
                    () -> assertFalse(TaskStatusEnum.CONFIRMED.canTransitionTo(TaskStatusEnum.COMPLETED))
            );
        }

        @Test
        @DisplayName("相同状态流转非法")
        void should_reject_same_status() {
            assertFalse(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.PENDING));
            assertFalse(TaskStatusEnum.IN_PROGRESS.canTransitionTo(TaskStatusEnum.IN_PROGRESS));
        }

        @Test
        @DisplayName("逆向非法：PENDING 不能直接到 CONFIRMED/COMPLETED")
        void should_reject_pending_forward_skip() {
            assertFalse(TaskStatusEnum.PENDING.canTransitionTo(TaskStatusEnum.CONFIRMED));
        }

        @Test
        @DisplayName("null 目标状态非法")
        void should_reject_null_target() {
            assertFalse(TaskStatusEnum.PENDING.canTransitionTo(null));
        }
    }

    @Nested
    @DisplayName("终态判定")
    class TerminalTest {

        @Test
        @DisplayName("CONFIRMED 是终态")
        void should_confirmed_be_terminal() {
            assertTrue(TaskStatusEnum.CONFIRMED.isTerminal());
        }

        @Test
        @DisplayName("非终态阶段判定")
        void should_non_terminal_stages() {
            assertAll("非终态阶段",
                    () -> assertFalse(TaskStatusEnum.PENDING.isTerminal()),
                    () -> assertFalse(TaskStatusEnum.ASSIGNED.isTerminal()),
                    () -> assertFalse(TaskStatusEnum.IN_PROGRESS.isTerminal()),
                    () -> assertFalse(TaskStatusEnum.COMPLETED.isTerminal())
            );
        }
    }

    @Nested
    @DisplayName("of() code 解析")
    class OfTest {

        @Test
        @DisplayName("合法 code 解析为对应枚举")
        void should_parse_valid_code() {
            assertEquals(TaskStatusEnum.PENDING, TaskStatusEnum.of(ProjectConstant.TASK_PENDING));
            assertEquals(TaskStatusEnum.ASSIGNED, TaskStatusEnum.of(ProjectConstant.TASK_ASSIGNED));
            assertEquals(TaskStatusEnum.IN_PROGRESS, TaskStatusEnum.of(ProjectConstant.TASK_IN_PROGRESS));
            assertEquals(TaskStatusEnum.COMPLETED, TaskStatusEnum.of(ProjectConstant.TASK_COMPLETED));
            assertEquals(TaskStatusEnum.CONFIRMED, TaskStatusEnum.of(ProjectConstant.TASK_CONFIRMED));
        }

        @Test
        @DisplayName("非法 code 解析为 null")
        void should_return_null_for_invalid_code() {
            assertNull(TaskStatusEnum.of("NOT_EXIST"));
            assertNull(TaskStatusEnum.of(""));
        }

        @Test
        @DisplayName("null code 解析为 null")
        void should_return_null_for_null_code() {
            assertNull(TaskStatusEnum.of(null));
        }
    }
}
