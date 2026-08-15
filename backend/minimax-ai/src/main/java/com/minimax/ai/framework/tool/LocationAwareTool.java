package com.minimax.ai.framework.tool;

import com.minimax.ai.framework.agent.Agent.AgentContext;
import com.minimax.ai.framework.location.GeoUtils;
import com.minimax.ai.framework.location.Location;
import com.minimax.ai.framework.location.PoiDatabase;
import com.minimax.ai.framework.location.PoiDatabase.Poi;
import com.minimax.ai.framework.location.PoiDatabase.PoiResult;
import com.minimax.ai.framework.location.PoiDatabase.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 位置感知 POI 搜索工具 (V2.8.6) - 统一基类
 *
 * <h3>流程</h3>
 * <ol>
 *   <li>从 AgentContext.userLat / userLng 拿用户位置</li>
 *   <li>无位置: 用默认 (北京天安门) 或提示用户授权 location:read</li>
 *   <li>调用 PoiDatabase.findNearby (基于 Haversine)</li>
 *   <li>返回 topK 个 POI, 含距离</li>
 * </ol>
 */
@Slf4j
public abstract class LocationAwareTool implements Tool {

    protected final PoiDatabase poiDatabase;
    protected final Type poiType;
    protected final String toolName;
    protected final String toolDescription;

    public LocationAwareTool(PoiDatabase poiDatabase, Type poiType,
                               String toolName, String toolDescription) {
        this.poiDatabase = poiDatabase;
        this.poiType = poiType;
        this.toolName = toolName;
        this.toolDescription = toolDescription;
    }

    @Override
    public String getName() { return toolName; }

    @Override
    public String getDescription() { return toolDescription; }

    @Override
    public boolean requiresPermission() { return true; }

    @Override
    public String requiredPermissionCode() { return "location:read"; }

    @Override
    public Map<String, ParameterDef> getParameters() {
        Map<String, ParameterDef> m = new LinkedHashMap<>();
        m.put("city", new ParameterDef("city", "string", "城市名 (如 北京)", false, ""));
        m.put("maxDistanceKm", new ParameterDef("maxDistanceKm", "number", "最大距离 (km)", false, 10));
        m.put("topK", new ParameterDef("topK", "number", "返回数量", false, 5));
        m.put("minRating", new ParameterDef("minRating", "number", "最低评分", false, 0));
        m.put("maxPrice", new ParameterDef("maxPrice", "number", "最高价格 (元)", false, 0));
        return m;
    }

    /**
     * 解析用户位置, 若 AgentContext 无位置, 使用默认
     */
    protected double[] resolveLocation(AgentContext context, String city) {
        if (context.userLat != null && context.userLng != null) {
            return new double[]{context.userLat, context.userLng};
        }
        if (context.userCity != null) {
            // 根据 userCity 选默认中心
            return defaultCityCenter(context.userCity);
        }
        if (city != null && !city.isEmpty()) {
            return defaultCityCenter(city);
        }
        return new double[]{39.9087, 116.3975};  // 北京天安门
    }

    /** 城市 → 默认中心坐标 */
    private double[] defaultCityCenter(String city) {
        switch (city) {
            case "北京": return new double[]{39.9087, 116.3975};
            case "上海": return new double[]{31.2400, 121.4900};
            case "广州": return new double[]{23.1357, 113.3245};
            case "深圳": return new double[]{22.5454, 114.0865};
            default: return new double[]{39.9087, 116.3975};
        }
    }

    @Override
    public Map<String, Object> execute(AgentContext context, Map<String, Object> input) throws Exception {
        String city = (String) input.get("city");
        double maxDist = input.get("maxDistanceKm") != null ? ((Number) input.get("maxDistanceKm")).doubleValue() : 10.0;
        int topK = input.get("topK") != null ? ((Number) input.get("topK")).intValue() : 5;
        double minRating = input.get("minRating") != null ? ((Number) input.get("minRating")).doubleValue() : 0;
        double maxPrice = input.get("maxPrice") != null ? ((Number) input.get("maxPrice")).doubleValue() : 0;

        double[] userLoc = resolveLocation(context, city);
        log.info("[{}] userLoc=({}, {}), maxDist={}km, topK={}, minRating={}, maxPrice={}",
                toolName, userLoc[0], userLoc[1], maxDist, topK, minRating, maxPrice);

        // 查 POI
        List<PoiResult> results = poiDatabase.findNearby(poiType, userLoc[0], userLoc[1], maxDist, topK * 3);
        // 过滤评分/价格
        List<Map<String, Object>> filtered = results.stream()
                .filter(r -> r.poi.rating >= minRating)
                .filter(r -> maxPrice <= 0 || r.poi.price <= maxPrice)
                .limit(topK)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.poi.id);
                    m.put("name", r.poi.name);
                    m.put("type", r.poi.type.name());
                    m.put("typeLabel", r.poi.type.label);
                    m.put("city", r.poi.city);
                    m.put("address", r.poi.address);
                    m.put("lat", r.poi.lat);
                    m.put("lng", r.poi.lng);
                    m.put("rating", r.poi.rating);
                    m.put("price", r.poi.price);
                    m.put("tags", r.poi.tags);
                    m.put("description", r.poi.description);
                    m.put("distanceKm", r.distanceKm);
                    m.put("distanceStr", r.getDistanceStr());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", toolName);
        result.put("userLocation", Map.of("lat", userLoc[0], "lng", userLoc[1]));
        result.put("totalFound", filtered.size());
        result.put("results", filtered);
        return result;
    }
}
