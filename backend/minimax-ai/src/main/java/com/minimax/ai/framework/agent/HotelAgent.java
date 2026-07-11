package com.minimax.ai.framework.agent;

import com.minimax.ai.framework.permission.Permission;
import com.minimax.ai.framework.tool.HotelSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 酒店推荐 Agent (V2.8.6) - 位置感知
 *
 * <h3>能力</h3>
 * <ul>
 *   <li>根据用户位置推荐附近酒店</li>
 *   <li>价格/评分筛选</li>
 *   <li>城市切换</li>
 *   <li>位置权限请求 (location:read)</li>
 * </ul>
 *
 * <h3>典型对话</h3>
 * <pre>
 *   用户: 北京附近有什么 4 星以上酒店?
 *   → 解析: 城市=北京, minRating=4.0
 *   → 调用 hotel.search
 *   → 返回: 5 个酒店
 * </pre>
 */
@Slf4j
@Component
public class HotelAgent extends Agent {

    public HotelAgent(HotelSearchTool hotelTool) {
        super("hotel-agent", "酒店推荐助手, 基于位置找附近酒店",
              "你是 MiniMax 酒店推荐助手. 根据用户位置推荐高质量酒店, 关注距离/评分/价格.",
              5);
        registerTool(hotelTool);
        addCapability("hotel");
        addCapability("location-aware");
        addCapability("recommendation");
        // 酒店推荐需要位置权限
        requirePermission(Permission.location());
    }

    @Override
    protected String think(AgentContext context) {
        String query = context.userQuery;
        StringBuilder thought = new StringBuilder();
        thought.append("用户查询: ").append(query).append("\n");

        // 解析城市
        Pattern pCity = Pattern.compile("(北京|上海|广州|深圳)");
        Matcher m = pCity.matcher(query);
        if (m.find()) {
            thought.append("检测到城市: ").append(m.group(1)).append("\n");
            context.variables.put("city", m.group(1));
        }
        // 解析评分
        Pattern pRating = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*星[级以上]?");
        m = pRating.matcher(query);
        if (m.find()) {
            thought.append("检测到评分: ").append(m.group(1)).append("\n");
            context.variables.put("minRating", Double.parseDouble(m.group(1)));
        }
        // 解析价格
        Pattern pPrice = Pattern.compile("(不超过|低于|小于|≤|<=|<)\\s*(\\d+)\\s*[元块]?");
        m = pPrice.matcher(query);
        if (m.find()) {
            thought.append("检测到价格上限: ").append(m.group(2)).append(" 元\n");
            context.variables.put("maxPrice", Integer.parseInt(m.group(2)));
        }
        // 解析距离
        Pattern pDist = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*km\\s*(以内|内)?");
        m = pDist.matcher(query);
        if (m.find()) {
            thought.append("检测到距离限制: ").append(m.group(1)).append(" km\n");
            context.variables.put("maxDistanceKm", Double.parseDouble(m.group(1)));
        }
        return thought.toString();
    }

    @Override
    protected ActionDecision decide(AgentContext context, String thought) {
        ActionDecision d = new ActionDecision();
        d.action = "hotel.search";
        d.input = new LinkedHashMap<>();
        if (context.variables.get("city") != null) d.input.put("city", context.variables.get("city"));
        if (context.variables.get("minRating") != null) d.input.put("minRating", context.variables.get("minRating"));
        if (context.variables.get("maxPrice") != null) d.input.put("maxPrice", context.variables.get("maxPrice"));
        if (context.variables.get("maxDistanceKm") != null) d.input.put("maxDistanceKm", context.variables.get("maxDistanceKm"));
        d.input.put("topK", 5);
        return d;
    }

    @Override
    protected String summarize(AgentContext context) {
        if (context.observations.isEmpty()) {
            return "抱歉, 没有找到符合条件的酒店. 请放宽条件 (如降低评分要求, 提高价格上限) 后重试.";
        }
        String obs = context.observations.get(context.observations.size() - 1);
        return "根据您的需求, 为您推荐以下酒店:\n\n" + obs + "\n\n点击酒店名称可查看详情.";
    }
}
