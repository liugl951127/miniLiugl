package com.minimax.ai.framework.agent;

import com.minimax.ai.framework.permission.Permission;
import com.minimax.ai.framework.tool.EntertainmentSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 娱乐推荐 Agent (V2.8.6) - 位置感知
 *
 * <h3>支持的娱乐类型</h3>
 * 影院 / KTV / 餐厅 / 公园
 *
 * <h3>典型对话</h3>
 * <pre>
 *   用户: 上海附近有什么电影院?
 *   → 解析: 城市=上海, subType=CINEMA
 *   → 调用 entertainment.search
 * </pre>
 */
@Slf4j
@Component
public class EntertainmentAgent extends Agent {

    public EntertainmentAgent(EntertainmentSearchTool entTool) {
        super("entertainment-agent", "娱乐推荐助手, 找附近的影院/KTV/餐厅/公园",
              "你是 MiniMax 娱乐推荐助手. 根据用户位置推荐娱乐场所.",
              5);
        registerTool(entTool);
        addCapability("entertainment");
        addCapability("cinema");
        addCapability("restaurant");
        addCapability("location-aware");
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
        // 解析子类型
        if (query.contains("影院") || query.contains("电影") || query.contains("电影院") || query.contains("cinema")) {
            thought.append("检测到子类型: CINEMA\n");
            context.variables.put("subType", "CINEMA");
        } else if (query.contains("KTV") || query.contains("唱歌") || query.contains("歌厅")) {
            thought.append("检测到子类型: KTV\n");
            context.variables.put("subType", "KTV");
        } else if (query.contains("餐厅") || query.contains("饭店") || query.contains("吃饭")) {
            thought.append("检测到子类型: RESTAURANT\n");
            context.variables.put("subType", "RESTAURANT");
        } else if (query.contains("公园") || query.contains("景区") || query.contains("游乐")) {
            thought.append("检测到子类型: PARK\n");
            context.variables.put("subType", "PARK");
        }
        // 距离
        Pattern pDist = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*km\\s*(以内|内)?");
        m = pDist.matcher(query);
        if (m.find()) {
            thought.append("检测到距离: ").append(m.group(1)).append(" km\n");
            context.variables.put("maxDistanceKm", Double.parseDouble(m.group(1)));
        }
        // 评分
        Pattern pRating = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*星[级以上]?");
        m = pRating.matcher(query);
        if (m.find()) {
            thought.append("检测到评分: ").append(m.group(1)).append("\n");
            context.variables.put("minRating", Double.parseDouble(m.group(1)));
        }
        return thought.toString();
    }

    @Override
    protected ActionDecision decide(AgentContext context, String thought) {
        ActionDecision d = new ActionDecision();
        d.action = "entertainment.search";
        d.input = new LinkedHashMap<>();
        if (context.variables.get("city") != null) d.input.put("city", context.variables.get("city"));
        if (context.variables.get("subType") != null) d.input.put("subType", context.variables.get("subType"));
        if (context.variables.get("maxDistanceKm") != null) d.input.put("maxDistanceKm", context.variables.get("maxDistanceKm"));
        if (context.variables.get("minRating") != null) d.input.put("minRating", context.variables.get("minRating"));
        d.input.put("topK", 5);
        return d;
    }

    @Override
    protected String summarize(AgentContext context) {
        if (context.observations.isEmpty()) {
            return "抱歉, 没有找到符合条件的娱乐场所.";
        }
        String obs = context.observations.get(context.observations.size() - 1);
        return "为您找到以下娱乐场所:\n\n" + obs;
    }
}
