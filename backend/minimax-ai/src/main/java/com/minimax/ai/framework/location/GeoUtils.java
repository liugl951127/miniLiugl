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
     * 计算两点球面距离 (Haversine 公式, km)
     *
     * <p><b>公式推导</b>:
     *   球面两点间最短路径是大圆弧 (Great Circle), 长度由球面三角余弦定理推导:
     *   <pre>
     *     a = sin²(Δφ/2) + cos(φ1)·cos(φ2)·sin²(Δλ/2)
     *     c = 2·atan2(√a, √(1−a))
     *     d = R·c
     *   </pre>
     *   其中 φ 为纬度, λ 为经度 (弧度), R = 6371 km (地球平均半径).
     *
     * <p><b>为什么用 Haversine 而非平面勾股?</b>
     *   平面勾股公式在低纬度短距离下精度尚可 (e.g. 1km 误差 0.5%),
     *   但在跨城市/跨国 (距离>100km) 时误差 5%+.
     *   Haversine 考虑地球曲面, 全距离精度都很好.
     *
     * <p><b>精度</b>: 中距离 (1-1000km) 误差 0.1% 以内.
     *   极地附近 (高纬度) 误差增大, 可用 Vincenty 公式代苒.
     *
     * <p><b>复杂度</b>: O(1)
     *
     * @param lat1 起点纬度 (度)
     * @param lng1 起点经度 (度)
     * @param lat2 终点纬度
     * @param lng2 终点经度
     * @return 球面距离 (km)
     */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        // 1. 差值转弧度 (公式中需要弧度, 而 WGS-84 存储是度)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        // 2. Haversine 核心: a = sin²(Δφ/2) + cos(φ1)·cos(φ2)·sin²(Δλ/2)
        //    几何意义: 球面两点的半角公式化
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        // 3. 角距离: c = 2·atan2(√a, √(1−a))
        //    使用 atan2 形式比 acos 数值更稳定 (避免边界误差)
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 4. 弧长 = 半径 × 角距离
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
