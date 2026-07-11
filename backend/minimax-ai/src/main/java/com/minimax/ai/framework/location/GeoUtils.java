package com.minimax.ai.framework.location;

/**
 * 地理工具类 (V2.8.6)
 *
 * <h3>算法: Haversine 公式 (球面距离)</h3>
 * <pre>
 *   a = sin²(Δφ/2) + cos(φ1)·cos(φ2)·sin²(Δλ/2)
 *   c = 2·atan2(√a, √(1-a))
 *   d = R·c   (R = 6371 km)
 * </pre>
 */
public final class GeoUtils {

    /** 地球平均半径 (km) */
    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {}

    /**
     * 计算两点球面距离 (km)
     * @param lat1 起点纬度 (度)
     * @param lng1 起点经度 (度)
     * @param lat2 终点纬度
     * @param lng2 终点经度
     */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 判断点是否在矩形范围内 (粗筛)
     */
    public static boolean inBoundingBox(double lat, double lng,
                                          double minLat, double maxLat,
                                          double minLng, double maxLng) {
        return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
    }

    /**
     * 格式化距离
     */
    public static String formatDistance(double km) {
        if (km < 1) return Math.round(km * 1000) + "m";
        if (km < 10) return String.format("%.1fkm", km);
        return Math.round(km) + "km";
    }
}
