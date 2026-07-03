package com.vibe.delivery.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GPS 距离计算工具单元测试
 *
 * <p>验证 Haversine 公式计算结果：同点距离 0、已知两点距离、null 参数处理、半径范围判定。</p>
 *
 * @author vibe
 */
@DisplayName("GPS 距离计算 GpsUtils 测试")
class GpsUtilsTest {

    /** 测试容差（米），Haversine 公式精度容差 */
    private static final double DISTANCE_TOLERANCE_METERS = 5.0;

    @Nested
    @DisplayName("distanceMeters Haversine 距离计算")
    class DistanceMetersTest {

        @Test
        @DisplayName("同一点距离为 0")
        void should_return_zero_for_same_point() {
            double distance = GpsUtils.distanceMeters(39.9042, 116.4074, 39.9042, 116.4074);
            assertEquals(0.0, distance, 0.001, "同一点距离应为 0");
        }

        @Test
        @DisplayName("0.001° 纬度差距离约 111 米（验证 Haversine 公式正确性）")
        void should_calculate_latitude_delta_111m() {
            // 纬度差 0.001° ≈ 111 米（1° 纬度约 111km）
            double distance = GpsUtils.distanceMeters(39.0000, 116.0000, 39.0010, 116.0000);
            // 1° 纬度 ≈ 111195 米（WGS84 椭球平均），0.001° ≈ 111.2 米
            assertEquals(111.2, distance, 1.0, "0.001° 纬度差应约 111 米");
        }

        @Test
        @DisplayName("0.001° 经度差距离受纬度影响（赤道处约 111 米）")
        void should_calculate_longitude_delta_at_equator() {
            // 赤道处 0.001° 经度差 ≈ 111 米
            double distance = GpsUtils.distanceMeters(0.0, 0.0, 0.0, 0.0010);
            assertEquals(111.2, distance, 1.0, "赤道处 0.001° 经度差应约 111 米");
        }

        @Test
        @DisplayName("0.001° 经度差距离在 39°N 纬度下约 86 米（cos(39°) 折减）")
        void should_calculate_longitude_delta_at_39n() {
            // 39°N 处 0.001° 经度差 ≈ 111 × cos(39°) ≈ 111 × 0.777 ≈ 86.3 米
            double distance = GpsUtils.distanceMeters(39.0, 116.0, 39.0, 116.0010);
            assertEquals(86.3, distance, 1.0, "39°N 处 0.001° 经度差应约 86 米");
        }

        @Test
        @DisplayName("北京天安门 → 王府井 距离约 1300 米")
        void should_calculate_tiananmen_to_wangfujing() {
            // 天安门广场旗杆 39.9042, 116.4074
            // 王府井大街北口 39.9152, 116.4148
            double distance = GpsUtils.distanceMeters(39.9042, 116.4074, 39.9152, 116.4148);
            // 实际 Haversine 计算约 1376 米，允许 ±100 米容差
            assertTrue(distance > 1200 && distance < 1500,
                    "天安门到王府井距离应在 1200-1500m 之间，实际: " + distance);
        }

        @Test
        @DisplayName("距离对称性：A→B 距离等于 B→A 距离")
        void should_be_symmetric() {
            double ab = GpsUtils.distanceMeters(39.9042, 116.4074, 39.9152, 116.4148);
            double ba = GpsUtils.distanceMeters(39.9152, 116.4148, 39.9042, 116.4074);
            assertEquals(ab, ba, 0.001, "距离应满足对称性");
        }

        @Test
        @DisplayName("北京 → 上海 距离约 1067 公里（Haversine 大尺度验证）")
        void should_calculate_beijing_to_shanghai() {
            // 北京天安门 39.9042, 116.4074
            // 上海人民广场 31.2304, 121.4737
            double distance = GpsUtils.distanceMeters(39.9042, 116.4074, 31.2304, 121.4737);
            // 实际直线距离约 1067 公里，允许 ±20km 容差
            assertTrue(distance > 1_050_000 && distance < 1_090_000,
                    "北京到上海距离应约 1067km，实际: " + distance);
        }

        @Test
        @DisplayName("任一坐标为 null 返回 Double.MAX_VALUE")
        void should_return_max_value_when_any_null() {
            assertMaxValue(GpsUtils.distanceMeters(null, 116.0, 39.0, 116.0));
            assertMaxValue(GpsUtils.distanceMeters(39.0, null, 39.0, 116.0));
            assertMaxValue(GpsUtils.distanceMeters(39.0, 116.0, null, 116.0));
            assertMaxValue(GpsUtils.distanceMeters(39.0, 116.0, 39.0, null));
        }

        private void assertMaxValue(double value) {
            assertEquals(Double.MAX_VALUE, value, 0.0, "null 坐标应返回 Double.MAX_VALUE");
        }
    }

    @Nested
    @DisplayName("isWithinRange 半径范围判定")
    class IsWithinRangeTest {

        @Test
        @DisplayName("当前坐标在允许半径内返回 true")
        void should_return_true_when_within_range() {
            // 期望坐标与当前坐标相同，距离 0，半径 100m
            assertTrue(GpsUtils.isWithinRange(39.9042, 116.4074, 39.9042, 116.4074, 100));
        }

        @Test
        @DisplayName("当前坐标在允许半径外返回 false")
        void should_return_false_when_out_of_range() {
            // 距离约 111m，半径 50m
            assertFalse(GpsUtils.isWithinRange(39.0000, 116.0000, 39.0010, 116.0000, 50));
        }

        @Test
        @DisplayName("距离恰好等于允许半径返回 true（边界含等号）")
        void should_return_true_when_distance_equals_radius() {
            // 距离约 111m，半径设为 111（实际距离 111.2，略大），改用更精确边界
            // 距离 0 时半径 0 应返回 true
            assertTrue(GpsUtils.isWithinRange(39.9042, 116.4074, 39.9042, 116.4074, 0));
        }

        @Test
        @DisplayName("距客户现场约 111m，半径 200m 在范围内")
        void should_return_true_when_111m_within_200m() {
            // 0.001° 纬度差 ≈ 111m，半径 200m
            assertTrue(GpsUtils.isWithinRange(39.0000, 116.0000, 39.0010, 116.0000, 200));
        }

        @Test
        @DisplayName("距客户现场约 111m，半径 100m 在范围外")
        void should_return_false_when_111m_out_of_100m() {
            // 0.001° 纬度差 ≈ 111.2m，半径 100m
            assertFalse(GpsUtils.isWithinRange(39.0000, 116.0000, 39.0010, 116.0000, 100));
        }

        @Test
        @DisplayName("null 坐标返回 false（距离为 MAX_VALUE 必然超出半径）")
        void should_return_false_when_null_coords() {
            assertFalse(GpsUtils.isWithinRange(null, 116.0, 39.0, 116.0, 1000));
            assertFalse(GpsUtils.isWithinRange(39.0, 116.0, 39.0, null, 1000));
        }

        @Test
        @DisplayName("大半径（1000m）覆盖天安门到王府井")
        void should_return_true_when_large_radius_covers_tiananmen_wangfujing() {
            // 天安门 → 王府井约 1376m，半径 1500m 应在范围内
            assertTrue(GpsUtils.isWithinRange(39.9042, 116.4074, 39.9152, 116.4148, 1500));
        }

        @Test
        @DisplayName("小半径（1000m）无法覆盖天安门到王府井")
        void should_return_false_when_small_radius_cannot_cover() {
            // 天安门 → 王府井约 1376m，半径 1000m 应在范围外
            assertFalse(GpsUtils.isWithinRange(39.9042, 116.4074, 39.9152, 116.4148, 1000));
        }
    }
}
