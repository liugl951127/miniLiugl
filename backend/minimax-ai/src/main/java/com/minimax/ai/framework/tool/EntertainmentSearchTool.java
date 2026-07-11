package com.minimax.ai.framework.tool;

import com.minimax.ai.framework.agent.Agent.AgentContext;
import com.minimax.ai.framework.location.PoiDatabase;
import com.minimax.ai.framework.location.PoiDatabase.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 娱乐搜索工具 (V2.8.6) - 位置感知
 *
 * <h3>支持的娱乐类型</h3>
 * CINEMA 影院 / KTV / RESTAURANT 餐厅 / PARK 公园
 *
 * <h3>输入</h3>
 * subType (CINEMA/KTV/RESTAURANT/PARK, 空=全部)
 * + city / maxDistanceKm / topK / minRating / maxPrice
 */
@Slf4j
@Component
public class EntertainmentSearchTool implements com.minimax.ai.framework.tool.Tool {

    private final PoiDatabase poiDatabase;

    public EntertainmentSearchTool(PoiDatabase poiDatabase) {
        this.poiDatabase = poiDatabase;
    }

    @Override
    public String getName() { return "entertainment.search"; }

    @Override
    public String getDescription() { return "搜索附近娱乐场所 (影院/KTV/餐厅/公园). 需 location:read 权限."; }

    @Override
    public boolean requiresPermission() { return true; }

    @Override
    public String requiredPermissionCode() { return "location:read"; }

    @Override
    public Map<String, ParameterDef> getParameters() {
        Map<String, ParameterDef> m = new LinkedHashMap<>();
        m.put("subType", new ParameterDef("subType", "string",
                "子类型 CINEMA/KTV/RESTAURANT/PARK (空=全部)", false, ""));
        m.put("city", new ParameterDef("city", "string", "城市", false, ""));
        m.put("maxDistanceKm", new ParameterDef("maxDistanceKm", "number", "最大距离 km", false, 10));
        m.put("topK", new ParameterDef("topK", "number", "返回数量", false, 5));
        m.put("minRating", new ParameterDef("minRating", "number", "最低评分", false, 0));
        m.put("maxPrice", new ParameterDef("maxPrice", "number", "最高人均消费", false, 0));
        return m;
    }

    @Override
    public Map<String, Object> execute(AgentContext context, Map<String, Object> input) throws Exception {
        String subType = (String) input.get("subType");
        String city = (String) input.get("city");
        double maxDist = input.get("maxDistanceKm") != null ? ((Number) input.get("maxDistanceKm")).doubleValue() : 10.0;
        int topK = input.get("topK") != null ? ((Number) input.get("topK")).intValue() : 5;
        double minRating = input.get("minRating") != null ? ((Number) input.get("minRating")).doubleValue() : 0;
        double maxPrice = input.get("maxPrice") != null ? ((Number) input.get("maxPrice")).doubleValue() : 0;

        double[] userLoc = resolveLoc(context, city);
        log.info("[entertainment.search] subType='{}', userLoc=({}, {}), maxDist={}km",
                subType, userLoc[0], userLoc[1], maxDist);

        PoiDatabase.Type type = parseSubType(subType);
        List<PoiDatabase.PoiResult> results = poiDatabase.findNearby(type, userLoc[0], userLoc[1], maxDist, topK * 3);

        List<Map<String, Object>> filtered = results.stream()
                .filter(r -> r.poi.rating >= minRating)
                .filter(r -> maxPrice <= 0 || r.poi.price <= maxPrice)
                .limit(topK)
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.poi.id);
                    m.put("name", r.poi.name);
                    m.put("subType", r.poi.type.name());
                    m.put("subTypeLabel", r.poi.type.label);
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
        result.put("toolName", getName());
        result.put("userLocation", Map.of("lat", userLoc[0], "lng", userLoc[1]));
        result.put("subType", subType != null ? subType : "ALL");
        result.put("totalFound", filtered.size());
        result.put("results", filtered);
        return result;
    }

    private PoiDatabase.Type parseSubType(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return PoiDatabase.Type.valueOf(s.toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private double[] resolveLoc(AgentContext context, String city) {
        if (context.userLat != null && context.userLng != null) {
            return new double[]{context.userLat, context.userLng};
        }
        if (city != null && !city.isEmpty()) {
            switch (city) {
                case "北京": return new double[]{39.9087, 116.3975};
                case "上海": return new double[]{31.2400, 121.4900};
                case "广州": return new double[]{23.1357, 113.3245};
                case "深圳": return new double[]{22.5454, 114.0865};
            }
        }
        if (context.userCity != null) {
            switch (context.userCity) {
                case "北京": return new double[]{39.9087, 116.3975};
                case "上海": return new double[]{31.2400, 121.4900};
                case "广州": return new double[]{23.1357, 113.3245};
                case "深圳": return new double[]{22.5454, 114.0865};
            }
        }
        return new double[]{39.9087, 116.3975};
    }
}
