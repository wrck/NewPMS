package com.vibe.project.enums;

import com.vibe.project.constant.ProjectConstant;
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
 * 项目状态机枚举单元测试
 *
 * <p>覆盖主流程合法流转、非法流转拒绝、ON_HOLD 恢复、终态保护、code 解析等场景。</p>
 *
 * @author vibe
 */
@DisplayName("项目状态机 ProjectStatusEnum 测试")
class ProjectStatusEnumTest {

    @Nested
    @DisplayName("主流程合法流转")
    class MainFlowTest {

        @Test
        @DisplayName("INIT → PLAN 合法")
        void should_allow_init_to_plan() {
            assertTrue(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.PLAN));
        }

        @Test
        @DisplayName("PLAN → EXECUTE 合法")
        void should_allow_plan_to_execute() {
            assertTrue(ProjectStatusEnum.PLAN.canTransitionTo(ProjectStatusEnum.EXECUTE));
        }

        @Test
        @DisplayName("EXECUTE → ACCEPT 合法")
        void should_allow_execute_to_accept() {
            assertTrue(ProjectStatusEnum.EXECUTE.canTransitionTo(ProjectStatusEnum.ACCEPT));
        }

        @Test
        @DisplayName("ACCEPT → CLOSE 合法")
        void should_allow_accept_to_close() {
            assertTrue(ProjectStatusEnum.ACCEPT.canTransitionTo(ProjectStatusEnum.CLOSE));
        }

        @Test
        @DisplayName("CLOSE → ARCHIVED 合法")
        void should_allow_close_to_archived() {
            assertTrue(ProjectStatusEnum.CLOSE.canTransitionTo(ProjectStatusEnum.ARCHIVED));
        }

        @Test
        @DisplayName("完整主流程 INIT→PLAN→EXECUTE→ACCEPT→CLOSE→ARCHIVED 全部合法")
        void should_allow_full_main_flow() {
            assertAll("主流程链路全部允许",
                    () -> assertTrue(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.PLAN)),
                    () -> assertTrue(ProjectStatusEnum.PLAN.canTransitionTo(ProjectStatusEnum.EXECUTE)),
                    () -> assertTrue(ProjectStatusEnum.EXECUTE.canTransitionTo(ProjectStatusEnum.ACCEPT)),
                    () -> assertTrue(ProjectStatusEnum.ACCEPT.canTransitionTo(ProjectStatusEnum.CLOSE)),
                    () -> assertTrue(ProjectStatusEnum.CLOSE.canTransitionTo(ProjectStatusEnum.ARCHIVED))
            );
        }
    }

    @Nested
    @DisplayName("非法流转被拒绝")
    class InvalidTransitionTest {

        @Test
        @DisplayName("INIT → EXECUTE 跨阶段非法")
        void should_reject_init_to_execute() {
            assertFalse(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.EXECUTE));
        }

        @Test
        @DisplayName("INIT → CLOSE 跨阶段非法")
        void should_reject_init_to_close() {
            assertFalse(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.CLOSE));
        }

        @Test
        @DisplayName("PLAN → ACCEPT 跨阶段非法")
        void should_reject_plan_to_accept() {
            assertFalse(ProjectStatusEnum.PLAN.canTransitionTo(ProjectStatusEnum.ACCEPT));
        }

        @Test
        @DisplayName("EXECUTE → CLOSE 跨阶段非法")
        void should_reject_execute_to_close() {
            assertFalse(ProjectStatusEnum.EXECUTE.canTransitionTo(ProjectStatusEnum.CLOSE));
        }

        @Test
        @DisplayName("CLOSE → EXECUTE 逆向非法")
        void should_reject_close_to_execute() {
            assertFalse(ProjectStatusEnum.CLOSE.canTransitionTo(ProjectStatusEnum.EXECUTE));
        }

        @Test
        @DisplayName("ARCHIVED → 任意状态 非法（终态保护）")
        void should_reject_archived_to_any() {
            assertAll("ARCHIVED 终态不可流转",
                    () -> assertFalse(ProjectStatusEnum.ARCHIVED.canTransitionTo(ProjectStatusEnum.INIT)),
                    () -> assertFalse(ProjectStatusEnum.ARCHIVED.canTransitionTo(ProjectStatusEnum.PLAN)),
                    () -> assertFalse(ProjectStatusEnum.ARCHIVED.canTransitionTo(ProjectStatusEnum.EXECUTE)),
                    () -> assertFalse(ProjectStatusEnum.ARCHIVED.canTransitionTo(ProjectStatusEnum.ACCEPT)),
                    () -> assertFalse(ProjectStatusEnum.ARCHIVED.canTransitionTo(ProjectStatusEnum.CLOSE)),
                    () -> assertFalse(ProjectStatusEnum.ARCHIVED.canTransitionTo(ProjectStatusEnum.ON_HOLD))
            );
        }

        @Test
        @DisplayName("CANCELLED → 任意状态 非法（终态保护）")
        void should_reject_cancelled_to_any() {
            assertAll("CANCELLED 终态不可流转",
                    () -> assertFalse(ProjectStatusEnum.CANCELLED.canTransitionTo(ProjectStatusEnum.INIT)),
                    () -> assertFalse(ProjectStatusEnum.CANCELLED.canTransitionTo(ProjectStatusEnum.PLAN)),
                    () -> assertFalse(ProjectStatusEnum.CANCELLED.canTransitionTo(ProjectStatusEnum.EXECUTE)),
                    () -> assertFalse(ProjectStatusEnum.CANCELLED.canTransitionTo(ProjectStatusEnum.CLOSE))
            );
        }

        @Test
        @DisplayName("相同状态流转非法（无意义）")
        void should_reject_same_status() {
            assertFalse(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.INIT));
            assertFalse(ProjectStatusEnum.EXECUTE.canTransitionTo(ProjectStatusEnum.EXECUTE));
        }

        @Test
        @DisplayName("null 目标状态非法")
        void should_reject_null_target() {
            assertFalse(ProjectStatusEnum.INIT.canTransitionTo(null));
        }
    }

    @Nested
    @DisplayName("ON_HOLD 暂停与恢复")
    class OnHoldTest {

        @Test
        @DisplayName("任意主流程阶段 → ON_HOLD 合法")
        void should_allow_any_main_stage_to_on_hold() {
            assertAll("主流程任一阶段均可暂停",
                    () -> assertTrue(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.ON_HOLD)),
                    () -> assertTrue(ProjectStatusEnum.PLAN.canTransitionTo(ProjectStatusEnum.ON_HOLD)),
                    () -> assertTrue(ProjectStatusEnum.EXECUTE.canTransitionTo(ProjectStatusEnum.ON_HOLD)),
                    () -> assertTrue(ProjectStatusEnum.ACCEPT.canTransitionTo(ProjectStatusEnum.ON_HOLD)),
                    () -> assertTrue(ProjectStatusEnum.CLOSE.canTransitionTo(ProjectStatusEnum.ON_HOLD))
            );
        }

        @Test
        @DisplayName("ON_HOLD → 回到 INIT 合法")
        void should_allow_on_hold_to_init() {
            assertTrue(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.INIT));
        }

        @Test
        @DisplayName("ON_HOLD → 回到 PLAN 合法")
        void should_allow_on_hold_to_plan() {
            assertTrue(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.PLAN));
        }

        @Test
        @DisplayName("ON_HOLD → 回到 EXECUTE 合法")
        void should_allow_on_hold_to_execute() {
            assertTrue(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.EXECUTE));
        }

        @Test
        @DisplayName("ON_HOLD → 回到 ACCEPT 合法")
        void should_allow_on_hold_to_accept() {
            assertTrue(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.ACCEPT));
        }

        @Test
        @DisplayName("ON_HOLD → 回到 CLOSE 合法")
        void should_allow_on_hold_to_close() {
            assertTrue(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.CLOSE));
        }

        @Test
        @DisplayName("ON_HOLD → CANCELLED 合法")
        void should_allow_on_hold_to_cancelled() {
            assertTrue(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.CANCELLED));
        }

        @Test
        @DisplayName("ON_HOLD → ARCHIVED 非法（不可直接归档）")
        void should_reject_on_hold_to_archived() {
            assertFalse(ProjectStatusEnum.ON_HOLD.canTransitionTo(ProjectStatusEnum.ARCHIVED));
        }

        @Test
        @DisplayName("ON_HOLD 不是终态")
        void should_on_hold_not_be_terminal() {
            assertFalse(ProjectStatusEnum.ON_HOLD.isTerminal());
        }
    }

    @Nested
    @DisplayName("CANCELLED 取消分支")
    class CancelledTest {

        @Test
        @DisplayName("任意主流程阶段 → CANCELLED 合法")
        void should_allow_any_main_stage_to_cancelled() {
            assertAll("主流程任一阶段均可取消",
                    () -> assertTrue(ProjectStatusEnum.INIT.canTransitionTo(ProjectStatusEnum.CANCELLED)),
                    () -> assertTrue(ProjectStatusEnum.PLAN.canTransitionTo(ProjectStatusEnum.CANCELLED)),
                    () -> assertTrue(ProjectStatusEnum.EXECUTE.canTransitionTo(ProjectStatusEnum.CANCELLED)),
                    () -> assertTrue(ProjectStatusEnum.ACCEPT.canTransitionTo(ProjectStatusEnum.CANCELLED)),
                    () -> assertTrue(ProjectStatusEnum.CLOSE.canTransitionTo(ProjectStatusEnum.CANCELLED))
            );
        }
    }

    @Nested
    @DisplayName("ACCEPT 驳回返工")
    class AcceptReworkTest {

        @Test
        @DisplayName("ACCEPT → EXECUTE 驳回返工合法")
        void should_allow_accept_to_execute_rework() {
            assertTrue(ProjectStatusEnum.ACCEPT.canTransitionTo(ProjectStatusEnum.EXECUTE));
        }
    }

    @Nested
    @DisplayName("终态判定")
    class TerminalTest {

        @Test
        @DisplayName("ARCHIVED 是终态")
        void should_archived_be_terminal() {
            assertTrue(ProjectStatusEnum.ARCHIVED.isTerminal());
        }

        @Test
        @DisplayName("CANCELLED 是终态")
        void should_cancelled_be_terminal() {
            assertTrue(ProjectStatusEnum.CANCELLED.isTerminal());
        }

        @Test
        @DisplayName("主流程阶段均非终态")
        void should_main_stages_not_be_terminal() {
            assertAll("主流程阶段非终态",
                    () -> assertFalse(ProjectStatusEnum.INIT.isTerminal()),
                    () -> assertFalse(ProjectStatusEnum.PLAN.isTerminal()),
                    () -> assertFalse(ProjectStatusEnum.EXECUTE.isTerminal()),
                    () -> assertFalse(ProjectStatusEnum.ACCEPT.isTerminal()),
                    () -> assertFalse(ProjectStatusEnum.CLOSE.isTerminal())
            );
        }
    }

    @Nested
    @DisplayName("of() code 解析")
    class OfTest {

        @Test
        @DisplayName("合法 code 解析为对应枚举")
        void should_parse_valid_code() {
            assertEquals(ProjectStatusEnum.INIT, ProjectStatusEnum.of(ProjectConstant.STATUS_INIT));
            assertEquals(ProjectStatusEnum.PLAN, ProjectStatusEnum.of(ProjectConstant.STATUS_PLAN));
            assertEquals(ProjectStatusEnum.EXECUTE, ProjectStatusEnum.of(ProjectConstant.STATUS_EXECUTE));
            assertEquals(ProjectStatusEnum.ACCEPT, ProjectStatusEnum.of(ProjectConstant.STATUS_ACCEPT));
            assertEquals(ProjectStatusEnum.CLOSE, ProjectStatusEnum.of(ProjectConstant.STATUS_CLOSE));
            assertEquals(ProjectStatusEnum.ARCHIVED, ProjectStatusEnum.of(ProjectConstant.STATUS_ARCHIVED));
            assertEquals(ProjectStatusEnum.ON_HOLD, ProjectStatusEnum.of(ProjectConstant.STATUS_ON_HOLD));
            assertEquals(ProjectStatusEnum.CANCELLED, ProjectStatusEnum.of(ProjectConstant.STATUS_CANCELLED));
        }

        @Test
        @DisplayName("非法 code 解析为 null")
        void should_return_null_for_invalid_code() {
            assertNull(ProjectStatusEnum.of("NOT_EXIST"));
            assertNull(ProjectStatusEnum.of(""));
        }

        @Test
        @DisplayName("null code 解析为 null")
        void should_return_null_for_null_code() {
            assertNull(ProjectStatusEnum.of(null));
        }

        @Test
        @DisplayName("每个枚举的 code 与 desc 非空")
        void should_all_codes_and_descs_be_non_null() {
            for (ProjectStatusEnum e : ProjectStatusEnum.values()) {
                assertNotNull(e.getCode(), "code 不能为空: " + e);
                assertNotNull(e.getDesc(), "desc 不能为空: " + e);
            }
        }
    }
}
