package com.minimax.ai.framework.agent;

import com.minimax.ai.framework.memory.MemoryItem;
import com.minimax.ai.framework.memory.MemoryStore;
import com.minimax.ai.framework.tool.ProductSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 购物 Agent (V2.8.6)
 *
 * <h3>能力</h3>
 * <ul>
 *   <li>商品搜索 (product.search)</li>
 *   <li>价格筛选 (低于 X 元)</li>
 *   <li>类别推荐 (手机/电脑/...)</li>
 *   <li>购买引导 (需 order:create 权限)</li>
 *   <li>长期记忆用户偏好 (品牌/价格区间)</li>
 * </ul>
 *
 * <h3>思考 (think)</h3>
 * <ol>
 *   <li>解析用户查询: 关键词 + 价格上限 + 类别</li>
 *   <li>检查长期记忆: 用户偏好品牌?</li>
 *   <li>决定: 是 FINAL 还是调 product.search</li>
 * </ol>
 */
@Slf4j
@Component
public class ShoppingAgent extends Agent {

    public ShoppingAgent(ProductSearchTool productTool) {
        super("shopping-agent", "购物助手, 帮助用户搜索商品, 推荐, 比价",
              "你是 MiniMax 购物助手, 用专业简洁的语言推荐商品, 关注价格/品牌/用户偏好.",
              5);
        registerTool(productTool);
        addCapability("shopping");
        addCapability("product-search");
        addCapability("price-compare");
        // 购物前可不需权限 (仅搜索), 创建订单才需 order:create
    }

    @Override
    protected String think(AgentContext context) {
        String query = context.userQuery;
        log.debug("[shopping-agent] thinking: '{}'", query);
        // 简单思考: 解析用户意图
        StringBuilder thought = new StringBuilder();
        thought.append("用户查询: ").append(query).append("\n");
        thought.append("思考: 解析关键词, 准备搜索商品\n");
        // 解析价格上限
        Pattern pPrice = Pattern.compile("(不超过|低于|小于|最多|≤|<=|<)\\s*(\\d+)\\s*[元块]?");
        Matcher m = pPrice.matcher(query);
        if (m.find()) {
            thought.append("检测到价格限制: ").append(m.group(2)).append(" 元\n");
            context.variables.put("maxPrice", Integer.parseInt(m.group(2)));
        }
        // 解析类别
        for (String cat : new String[]{"手机", "电脑", "平板", "耳机", "手表", "服装", "食品", "家电"}) {
            if (query.contains(cat)) {
                thought.append("检测到类别: ").append(cat).append("\n");
                context.variables.put("category", cat);
            }
        }
        // 解析品牌
        for (String brand : new String[]{"Apple", "iPhone", "MacBook", "华为", "小米", "OPPO", "Nike", "Adidas", "Sony", "茅台"}) {
            if (query.contains(brand)) {
                thought.append("检测到品牌: ").append(brand).append("\n");
                context.variables.put("keyword", brand);
            }
        }
        return thought.toString();
    }

    @Override
    protected ActionDecision decide(AgentContext context, String thought) {
        // 调用 product.search
        ActionDecision d = new ActionDecision();
        d.action = "product.search";
        d.input = new LinkedHashMap<>();
        if (context.variables.get("keyword") != null) d.input.put("keyword", context.variables.get("keyword"));
        if (context.variables.get("category") != null) d.input.put("category", context.variables.get("category"));
        if (context.variables.get("maxPrice") != null) d.input.put("maxPrice", context.variables.get("maxPrice"));
        d.input.put("topK", 5);
        return d;
    }

    @Override
    protected String summarize(AgentContext context) {
        // 把搜索结果格式化为可读文本
        if (context.observations.isEmpty()) {
            return "抱歉, 没有找到匹配的商品.";
        }
        String last = context.observations.get(context.observations.size() - 1);
        return "为您找到以下商品:\n\n" + last + "\n\n如需下单请告诉我.";
    }

    @Override
    protected void extractAndStorePreferences(AgentContext context, AgentResult result) {
        // 提取品牌偏好 → 长期记忆
        String keyword = (String) context.variables.get("keyword");
        if (keyword != null && context.userId != null && memoryStore != null) {
            memoryStore.rememberLongTerm(context.userId,
                    MemoryItem.preference("preferred_brand", keyword));
        }
        Integer maxPrice = (Integer) context.variables.get("maxPrice");
        if (maxPrice != null && context.userId != null && memoryStore != null) {
            memoryStore.rememberLongTerm(context.userId,
                    MemoryItem.preference("max_price", maxPrice));
        }
    }
}
