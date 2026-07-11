package com.minimax.ai.framework.tool;

import com.minimax.ai.framework.location.PoiDatabase;
import com.minimax.ai.framework.location.PoiDatabase.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 酒店搜索工具 (V2.8.6) - 位置感知
 *
 * <h3>输入</h3>
 * city / maxDistanceKm / topK / minRating / maxPrice
 *
 * <h3>输出</h3>
 * 附近酒店列表, 含距离 + 评分 + 价格
 */
@Component
public class HotelSearchTool extends LocationAwareTool {

    public HotelSearchTool(PoiDatabase poiDatabase) {
        super(poiDatabase, Type.HOTEL,
              "hotel.search",
              "搜索附近酒店. 需 location:read 权限. 输入: city, maxDistanceKm, topK, minRating, maxPrice");
    }
}
