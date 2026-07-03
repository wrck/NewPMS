package com.vibe.delivery.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.delivery.bo.GpsLocation;
import com.vibe.delivery.constant.DeliveryConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 站点信息（site_info JSON）解析工具
 *
 * <p>约定 project_task.site_info JSON 中可包含：</p>
 * <ul>
 *   <li>expectedLatitude / expectedLongitude：客户现场期望 GPS 坐标</li>
 *   <li>allowedRadiusMeters：允许签到半径（米），未配置则用默认值</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
public final class SiteInfoUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SiteInfoUtils() {
    }

    /**
     * 解析 site_info JSON，返回期望坐标与允许半径。
     *
     * @param siteInfoJson site_info JSON 字符串
     * @return 数组 [expectedLat, expectedLon, allowedRadiusMeters]，缺失项为 null / 默认值
     */
    public static SiteExpectation parseExpectation(String siteInfoJson) {
        SiteExpectation expectation = new SiteExpectation();
        if (siteInfoJson == null || siteInfoJson.isBlank()) {
            expectation.allowedRadiusMeters = DeliveryConstant.DEFAULT_CHECKIN_RADIUS_METERS;
            return expectation;
        }
        try {
            JsonNode node = MAPPER.readTree(siteInfoJson);
            if (node.has("expectedLatitude")) {
                expectation.expectedLatitude = node.get("expectedLatitude").asDouble();
            }
            if (node.has("expectedLongitude")) {
                expectation.expectedLongitude = node.get("expectedLongitude").asDouble();
            }
            if (node.has("allowedRadiusMeters")) {
                expectation.allowedRadiusMeters = node.get("allowedRadiusMeters").asDouble();
            } else {
                expectation.allowedRadiusMeters = DeliveryConstant.DEFAULT_CHECKIN_RADIUS_METERS;
            }
        } catch (Exception e) {
            log.warn("[SiteInfoUtils] 解析 site_info 失败: {}", e.getMessage());
            expectation.allowedRadiusMeters = DeliveryConstant.DEFAULT_CHECKIN_RADIUS_METERS;
        }
        return expectation;
    }

    /**
     * 站点期望信息
     */
    public static class SiteExpectation {
        /** 期望纬度（客户现场） */
        public Double expectedLatitude;
        /** 期望经度（客户现场） */
        public Double expectedLongitude;
        /** 允许半径（米） */
        public double allowedRadiusMeters = DeliveryConstant.DEFAULT_CHECKIN_RADIUS_METERS;
    }

    /**
     * 校验 GPS 定位是否在客户现场允许范围内。
     *
     * @param location     当前定位
     * @param siteInfoJson site_info JSON
     * @return 校验结果
     */
    public static CheckinCheckResult checkLocation(GpsLocation location, String siteInfoJson) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return CheckinCheckResult.fail("GPS 定位信息缺失");
        }
        SiteExpectation expectation = parseExpectation(siteInfoJson);
        // 期望坐标缺失时，无法做距离校验，放行（兼容未配置站点坐标的场景）
        if (expectation.expectedLatitude == null || expectation.expectedLongitude == null) {
            return CheckinCheckResult.ok(null, "站点未配置坐标，跳过距离校验");
        }
        double distance = GpsUtils.distanceMeters(
                location.getLatitude(), location.getLongitude(),
                expectation.expectedLatitude, expectation.expectedLongitude);
        if (distance > expectation.allowedRadiusMeters) {
            return CheckinCheckResult.fail(distance,
                    String.format("当前位置距客户现场 %.0f 米，超出允许范围 %.0f 米",
                            distance, expectation.allowedRadiusMeters));
        }
        return CheckinCheckResult.ok(distance, "在允许范围内");
    }

    /**
     * 校验结果
     */
    public static class CheckinCheckResult {
        /** 是否通过 */
        public boolean passed;
        /** 与现场距离（米），可能为 null */
        public Double distanceMeters;
        /** 提示消息 */
        public String message;

        public static CheckinCheckResult ok(Double distance, String message) {
            CheckinCheckResult r = new CheckinCheckResult();
            r.passed = true;
            r.distanceMeters = distance;
            r.message = message;
            return r;
        }

        public static CheckinCheckResult fail(String message) {
            CheckinCheckResult r = new CheckinCheckResult();
            r.passed = false;
            r.message = message;
            return r;
        }

        public static CheckinCheckResult fail(Double distance, String message) {
            CheckinCheckResult r = new CheckinCheckResult();
            r.passed = false;
            r.distanceMeters = distance;
            r.message = message;
            return r;
        }
    }
}
