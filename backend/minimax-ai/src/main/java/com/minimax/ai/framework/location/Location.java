package com.minimax.ai.framework.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置坐标 (V2.8.6)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    /** 纬度 (度) */
    private double lat;
    /** 经度 (度) */
    private double lng;
    /** 城市/区域 (可选) */
    private String city;
    /** 详细地址 */
    private String address;
    /** POI 名称 (如 "天安门") */
    private String poiName;

    /** 北京天安门 */
    public static Location tianAnMen() {
        return new Location(39.9087, 116.3975, "北京", "北京市东城区", "天安门");
    }
    /** 上海外滩 */
    public static Location shanghaiBund() {
        return new Location(31.2400, 121.4900, "上海", "上海市黄浦区", "外滩");
    }
    /** 广州天河城 */
    public static Location guangzhouTianhe() {
        return new Location(23.1357, 113.3245, "广州", "广州市天河区", "天河城");
    }
    /** 深圳华强北 */
    public static Location shenzhenHuaqiang() {
        return new Location(22.5454, 114.0865, "深圳", "深圳市福田区", "华强北");
    }
}
