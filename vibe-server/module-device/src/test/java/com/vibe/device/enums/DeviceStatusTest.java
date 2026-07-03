package com.vibe.device.enums;

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
 * 设备状态机枚举单元测试
 *
 * <p>正常主流程：IN_FACTORY → SHIPPED → RECEIVED → PRE_CONFIG → INSTALLED → DEBUGGED → ONLINE
 * 异常分支：→ DAMAGED / LOST / REPAIR / EOL / RETURNED / REPLACED</p>
 *
 * @author vibe
 */
@DisplayName("设备状态机 DeviceStatus 测试")
class DeviceStatusTest {

    @Nested
    @DisplayName("正常主流程流转")
    class MainFlowTest {

        @Test
        @DisplayName("IN_FACTORY → SHIPPED 合法")
        void should_allow_in_factory_to_shipped() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.SHIPPED));
        }

        @Test
        @DisplayName("SHIPPED → RECEIVED 合法")
        void should_allow_shipped_to_received() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.RECEIVED));
        }

        @Test
        @DisplayName("RECEIVED → PRE_CONFIG 合法")
        void should_allow_received_to_pre_config() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.PRE_CONFIG));
        }

        @Test
        @DisplayName("PRE_CONFIG → INSTALLED 合法")
        void should_allow_pre_config_to_installed() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.INSTALLED));
        }

        @Test
        @DisplayName("INSTALLED → DEBUGGED 合法")
        void should_allow_installed_to_debugged() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.INSTALLED, DeviceStatus.DEBUGGED));
        }

        @Test
        @DisplayName("DEBUGGED → ONLINE 合法")
        void should_allow_debugged_to_online() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.DEBUGGED, DeviceStatus.ONLINE));
        }

        @Test
        @DisplayName("完整主流程 IN_FACTORY→SHIPPED→RECEIVED→PRE_CONFIG→INSTALLED→DEBUGGED→ONLINE 全部合法")
        void should_allow_full_main_flow() {
            assertAll("设备主流程链路全部允许",
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.SHIPPED)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.RECEIVED)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.PRE_CONFIG)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.INSTALLED)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.INSTALLED, DeviceStatus.DEBUGGED)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.DEBUGGED, DeviceStatus.ONLINE))
            );
        }

        @Test
        @DisplayName("RECEIVED → INSTALLED 跳过预配合法（Phase 1 预配可选）")
        void should_allow_received_to_installed_skip_pre_config() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.INSTALLED));
        }
    }

    @Nested
    @DisplayName("异常分支流转")
    class ExceptionBranchTest {

        @Test
        @DisplayName("IN_FACTORY → DAMAGED 合法（异常登记）")
        void should_allow_in_factory_to_damaged() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.DAMAGED));
        }

        @Test
        @DisplayName("SHIPPED → LOST 合法（运输遗失）")
        void should_allow_shipped_to_lost() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.LOST));
        }

        @Test
        @DisplayName("INSTALLED → REPAIR 合法（返修）")
        void should_allow_installed_to_repair() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.INSTALLED, DeviceStatus.REPAIR));
        }

        @Test
        @DisplayName("ONLINE → REPAIR 合法（在线设备返修）")
        void should_allow_online_to_repair() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.ONLINE, DeviceStatus.REPAIR));
        }

        @Test
        @DisplayName("任意正常状态 → EOL 退网/报废合法")
        void should_allow_any_normal_to_eol() {
            assertAll("正常状态均可退网",
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.EOL)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.EOL)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.EOL)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.EOL)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.INSTALLED, DeviceStatus.EOL)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.DEBUGGED, DeviceStatus.EOL)),
                    () -> assertTrue(DeviceStatus.canTransition(DeviceStatus.ONLINE, DeviceStatus.EOL))
            );
        }

        @Test
        @DisplayName("任意正常状态 → DAMAGED 合法")
        void should_allow_any_normal_to_damaged() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.DAMAGED));
            assertTrue(DeviceStatus.canTransition(DeviceStatus.ONLINE, DeviceStatus.DAMAGED));
        }

        @Test
        @DisplayName("任意正常状态 → LOST 合法")
        void should_allow_any_normal_to_lost() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.LOST));
            assertTrue(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.LOST));
        }

        @Test
        @DisplayName("任意正常状态 → REPAIR 合法")
        void should_allow_any_normal_to_repair() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.REPAIR));
            assertTrue(DeviceStatus.canTransition(DeviceStatus.DEBUGGED, DeviceStatus.REPAIR));
        }
    }

    @Nested
    @DisplayName("特殊分支流转")
    class SpecialBranchTest {

        @Test
        @DisplayName("RECEIVED → RETURNED 退货合法")
        void should_allow_received_to_returned() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.RECEIVED, DeviceStatus.RETURNED));
        }

        @Test
        @DisplayName("RETURNED 是终态，不可再流转到 IN_FACTORY（终态保护优先于 switch 分支）")
        void should_reject_returned_to_in_factory() {
            // RETURNED 在 TERMINAL_STATES 集合中，canTransition 优先返回 false，
            // switch 中的 RETURNED → IN_FACTORY 分支为防御性死代码。
            assertFalse(DeviceStatus.canTransition(DeviceStatus.RETURNED, DeviceStatus.IN_FACTORY));
        }

        @Test
        @DisplayName("ONLINE → REPLACED 设备替换合法")
        void should_allow_online_to_replaced() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.ONLINE, DeviceStatus.REPLACED));
        }

        @Test
        @DisplayName("REPAIR → INSTALLED 返修恢复合法")
        void should_allow_repair_to_installed() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.REPAIR, DeviceStatus.INSTALLED));
        }

        @Test
        @DisplayName("REPAIR → DEBUGGED 返修恢复合法")
        void should_allow_repair_to_debugged() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.REPAIR, DeviceStatus.DEBUGGED));
        }

        @Test
        @DisplayName("REPAIR → ONLINE 返修恢复合法")
        void should_allow_repair_to_online() {
            assertTrue(DeviceStatus.canTransition(DeviceStatus.REPAIR, DeviceStatus.ONLINE));
        }
    }

    @Nested
    @DisplayName("非法流转被拒绝")
    class InvalidTransitionTest {

        @Test
        @DisplayName("IN_FACTORY → RECEIVED 跨阶段非法")
        void should_reject_in_factory_to_received() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.RECEIVED));
        }

        @Test
        @DisplayName("IN_FACTORY → ONLINE 跨阶段非法")
        void should_reject_in_factory_to_online() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.ONLINE));
        }

        @Test
        @DisplayName("SHIPPED → PRE_CONFIG 跨阶段非法")
        void should_reject_shipped_to_pre_config() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.PRE_CONFIG));
        }

        @Test
        @DisplayName("PRE_CONFIG → DEBUGGED 跨阶段非法")
        void should_reject_pre_config_to_debugged() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.DEBUGGED));
        }

        @Test
        @DisplayName("INSTALLED → ONLINE 跨阶段非法")
        void should_reject_installed_to_online() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.INSTALLED, DeviceStatus.ONLINE));
        }

        @Test
        @DisplayName("逆向流转非法：SHIPPED → IN_FACTORY")
        void should_reject_shipped_to_in_factory() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.IN_FACTORY));
        }

        @Test
        @DisplayName("ONLINE → IN_FACTORY 非法")
        void should_reject_online_to_in_factory() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.ONLINE, DeviceStatus.IN_FACTORY));
        }

        @Test
        @DisplayName("相同状态流转非法")
        void should_reject_same_status() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.IN_FACTORY));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.ONLINE, DeviceStatus.ONLINE));
        }

        @Test
        @DisplayName("null 参数非法")
        void should_reject_null_params() {
            assertFalse(DeviceStatus.canTransition(null, DeviceStatus.SHIPPED));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, null));
            assertFalse(DeviceStatus.canTransition(null, null));
        }

        @Test
        @DisplayName("正常状态 → RETURNED 仅 RECEIVED 允许")
        void should_reject_other_normal_to_returned() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.IN_FACTORY, DeviceStatus.RETURNED));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.SHIPPED, DeviceStatus.RETURNED));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.PRE_CONFIG, DeviceStatus.RETURNED));
        }

        @Test
        @DisplayName("非 ONLINE 状态 → REPLACED 非法")
        void should_reject_non_online_to_replaced() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.INSTALLED, DeviceStatus.REPLACED));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.DEBUGGED, DeviceStatus.REPLACED));
        }
    }

    @Nested
    @DisplayName("终态保护")
    class TerminalProtectionTest {

        @Test
        @DisplayName("DAMAGED 是终态")
        void should_damaged_be_terminal() {
            assertTrue(DeviceStatus.DAMAGED.isTerminal());
        }

        @Test
        @DisplayName("LOST 是终态")
        void should_lost_be_terminal() {
            assertTrue(DeviceStatus.LOST.isTerminal());
        }

        @Test
        @DisplayName("RETURNED 是终态")
        void should_returned_be_terminal() {
            assertTrue(DeviceStatus.RETURNED.isTerminal());
        }

        @Test
        @DisplayName("REPLACED 是终态")
        void should_replaced_be_terminal() {
            assertTrue(DeviceStatus.REPLACED.isTerminal());
        }

        @Test
        @DisplayName("EOL 是终态")
        void should_eol_be_terminal() {
            assertTrue(DeviceStatus.EOL.isTerminal());
        }

        @Test
        @DisplayName("DAMAGED 终态不可再流转")
        void should_reject_damaged_to_any() {
            assertAll("DAMAGED 终态不可流转",
                    () -> assertFalse(DeviceStatus.canTransition(DeviceStatus.DAMAGED, DeviceStatus.REPAIR)),
                    () -> assertFalse(DeviceStatus.canTransition(DeviceStatus.DAMAGED, DeviceStatus.IN_FACTORY)),
                    () -> assertFalse(DeviceStatus.canTransition(DeviceStatus.DAMAGED, DeviceStatus.ONLINE))
            );
        }

        @Test
        @DisplayName("LOST 终态不可再流转")
        void should_reject_lost_to_any() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.LOST, DeviceStatus.REPAIR));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.LOST, DeviceStatus.IN_FACTORY));
        }

        @Test
        @DisplayName("EOL 终态不可再流转")
        void should_reject_eol_to_any() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.EOL, DeviceStatus.REPAIR));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.EOL, DeviceStatus.IN_FACTORY));
        }

        @Test
        @DisplayName("REPLACED 终态不可再流转")
        void should_reject_replaced_to_any() {
            assertFalse(DeviceStatus.canTransition(DeviceStatus.REPLACED, DeviceStatus.IN_FACTORY));
            assertFalse(DeviceStatus.canTransition(DeviceStatus.REPLACED, DeviceStatus.ONLINE));
        }
    }

    @Nested
    @DisplayName("正常状态判定")
    class NormalTest {

        @Test
        @DisplayName("主流程状态均为正常状态")
        void should_main_flow_states_be_normal() {
            assertAll("主流程状态均正常",
                    () -> assertTrue(DeviceStatus.IN_FACTORY.isNormal()),
                    () -> assertTrue(DeviceStatus.SHIPPED.isNormal()),
                    () -> assertTrue(DeviceStatus.RECEIVED.isNormal()),
                    () -> assertTrue(DeviceStatus.PRE_CONFIG.isNormal()),
                    () -> assertTrue(DeviceStatus.INSTALLED.isNormal()),
                    () -> assertTrue(DeviceStatus.DEBUGGED.isNormal()),
                    () -> assertTrue(DeviceStatus.ONLINE.isNormal())
            );
        }

        @Test
        @DisplayName("异常/终态状态非正常状态")
        void should_exception_states_not_be_normal() {
            assertAll("异常状态非正常",
                    () -> assertFalse(DeviceStatus.DAMAGED.isNormal()),
                    () -> assertFalse(DeviceStatus.LOST.isNormal()),
                    () -> assertFalse(DeviceStatus.RETURNED.isNormal()),
                    () -> assertFalse(DeviceStatus.REPAIR.isNormal()),
                    () -> assertFalse(DeviceStatus.REPLACED.isNormal()),
                    () -> assertFalse(DeviceStatus.EOL.isNormal())
            );
        }
    }

    @Nested
    @DisplayName("parse() 字符串解析")
    class ParseTest {

        @Test
        @DisplayName("合法字符串解析为对应枚举")
        void should_parse_valid_string() {
            assertEquals(DeviceStatus.IN_FACTORY, DeviceStatus.parse("IN_FACTORY"));
            assertEquals(DeviceStatus.SHIPPED, DeviceStatus.parse("SHIPPED"));
            assertEquals(DeviceStatus.ONLINE, DeviceStatus.parse("ONLINE"));
            assertEquals(DeviceStatus.REPAIR, DeviceStatus.parse("REPAIR"));
        }

        @Test
        @DisplayName("忽略大小写解析")
        void should_parse_ignore_case() {
            assertEquals(DeviceStatus.IN_FACTORY, DeviceStatus.parse("in_factory"));
            assertEquals(DeviceStatus.ONLINE, DeviceStatus.parse("online"));
            assertEquals(DeviceStatus.PRE_CONFIG, DeviceStatus.parse("  PRE_CONFIG  "));
        }

        @Test
        @DisplayName("非法字符串返回 null")
        void should_return_null_for_invalid() {
            assertNull(DeviceStatus.parse("NOT_EXIST"));
            assertNull(DeviceStatus.parse("xyz"));
        }

        @Test
        @DisplayName("null/空字符串返回 null")
        void should_return_null_for_blank() {
            assertNull(DeviceStatus.parse(null));
            assertNull(DeviceStatus.parse(""));
            assertNull(DeviceStatus.parse("   "));
        }

        @Test
        @DisplayName("所有枚举 displayName 非空")
        void should_all_display_names_be_non_null() {
            for (DeviceStatus s : DeviceStatus.values()) {
                assertNotNull(s.getDisplayName(), "displayName 不能为空: " + s);
            }
        }
    }
}
