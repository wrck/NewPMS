package com.vibe.delivery.util;

/**
 * GPS 距离计算工具
 *
 * <p>使用 Haversine 公式计算两个经纬度坐标之间的球面距离（米）。</p>
 *
 * <p>公式：</p>
 * <pre>
 *   a = sin²(Δφ/2) + cos(φ1) · cos(φ2) · sin²(Δλ/2)
 *   c = 2 · atan2(√a, √(1−a))
 *   d = R · c   （R 为地球半径，取 6371000 米）
 * </pre>
 *
 * @author vibe
 */
public final class GpsUtils {

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0d;

    private GpsUtils() {
    }

    /**
     * 计算两个 GPS 坐标之间的距离（米）。
     *
     * @param lat1 起点纬度
     * @param lon1 起点经度
     * @param lat2 终点纬度
     * @param lon2 终点经度
     * @return 球面距离（米），任意坐标为 null 时返回 Double.MAX_VALUE 表示无法校验通过
     */
    public static double distanceMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * 判断当前坐标是否在允许半径范围内。
     *
     * @param currentLat      当前纬度
     * @param currentLon      当前经度
     * @param expectedLat     期望纬度（客户现场）
     * @param expectedLon     期望经度（客户现场）
     * @param allowedRadiusMeters 允许半径（米）
     * @return true 在范围内；false 不在范围内或参数缺失
     */
    public static boolean isWithinRange(Double currentLat, Double currentLon,
                                        Double expectedLat, Double expectedLon,
                                        double allowedRadiusMeters) {
        double distance = distanceMeters(currentLat, currentLon, expectedLat, expectedLon);
        return distance <= allowedRadiusMeters;
    }
}
