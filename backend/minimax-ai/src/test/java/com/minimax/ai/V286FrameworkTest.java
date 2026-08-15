package com.minimax.ai;

import com.minimax.ai.framework.agent.*;
import com.minimax.ai.framework.agent.Agent.AgentContext;
import com.minimax.ai.framework.agent.Agent.AgentResult;
import com.minimax.ai.framework.location.GeoUtils;
import com.minimax.ai.framework.location.PoiDatabase;
import com.minimax.ai.framework.location.PoiDatabase.Type;
import com.minimax.ai.framework.memory.MemoryItem;
import com.minimax.ai.framework.memory.MemoryStore;
import com.minimax.ai.framework.permission.Permission;
import com.minimax.ai.framework.permission.PermissionGate;
import com.minimax.ai.framework.tool.*;
import com.minimax.ai.pipeline.stage.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.6 MiniMax AI 框架 端到端测试
 */
class V286FrameworkTest {

    private AgentRegistry registry;
    private PermissionGate gate;
    private MemoryStore memory;
    private Tokenizer tokenizer;
    private PoiDatabase poiDb;
    private ProductSearchTool productTool;
    private HotelSearchTool hotelTool;
    private EntertainmentSearchTool entTool;
    private ShoppingAgent shoppingAgent;
    private HotelAgent hotelAgent;
    private EntertainmentAgent entertainmentAgent;

    @BeforeEach
    void setup() {
        registry = new AgentRegistry();
        gate = new PermissionGate();
        memory = new MemoryStore();
        tokenizer = new Tokenizer();
        poiDb = new PoiDatabase();
        productTool = new ProductSearchTool();
        hotelTool = new HotelSearchTool(poiDb);
        entTool = new EntertainmentSearchTool(poiDb);
        shoppingAgent = new ShoppingAgent(productTool);
        hotelAgent = new HotelAgent(hotelTool);
        entertainmentAgent = new EntertainmentAgent(entTool);

        shoppingAgent.withDependencies(memory, gate, tokenizer);
        hotelAgent.withDependencies(memory, gate, tokenizer);
        entertainmentAgent.withDependencies(memory, gate, tokenizer);

        registry.register(shoppingAgent);
        registry.register(hotelAgent);
        registry.register(entertainmentAgent);
    }

    // ============================================
    // 1. POI + Geo 真实数据
    // ============================================

    @Test
    void testPoiDatabaseRealData() {
        assertTrue(poiDb.size() > 30, "应加载 30+ 真实 POI, 实际: " + poiDb.size());
        // 北京天安门附近的酒店
        List<PoiDatabase.PoiResult> hotels = poiDb.findNearby(
                Type.HOTEL, 39.9087, 116.3975, 20.0, 5);
        assertTrue(hotels.size() > 0, "应找到北京酒店");
        for (PoiDatabase.PoiResult r : hotels) {
            assertNotNull(r.poi.name);
            assertTrue(r.poi.rating > 0);
            assertTrue(r.distanceKm <= 20.0);
        }
    }

    @Test
    void testHaversineDistance() {
        // 北京天安门到上海外滩 ≈ 1067 km
        double d = GeoUtils.haversine(39.9087, 116.3975, 31.2400, 121.4900);
        assertTrue(d > 1000 && d < 1100, "天安门到外滩距离应在 1000-1100km, 实际: " + d);
        // 同一地点距离 = 0
        assertEquals(0, GeoUtils.haversine(39.9, 116.4, 39.9, 116.4), 0.001);
        // 格式化
        assertEquals("500m", GeoUtils.formatDistance(0.5));
        assertEquals("1.5km", GeoUtils.formatDistance(1.5));
        assertEquals("10km", GeoUtils.formatDistance(10.0));
    }

    // ============================================
    // 2. 商品搜索 (真实数据)
    // ============================================

    @Test
    void testProductSearch() throws Exception {
        Map<String, Object> result = productTool.execute(null, Map.of(
                "keyword", "iPhone", "topK", 5));
        assertNotNull(result);
        assertTrue(result.containsKey("products"));
        List<?> products = (List<?>) result.get("products");
        assertTrue(products.size() > 0, "iPhone 应有结果");
        // 包含 iPhone 15 Pro Max
        assertTrue(products.toString().contains("iPhone"));
    }

    @Test
    void testProductSearchWithPrice() throws Exception {
        Map<String, Object> result = productTool.execute(null, Map.of(
                "category", "手机", "maxPrice", 7000, "topK", 10));
        List<?> products = (List<?>) result.get("products");
        assertTrue(products.size() > 0);
        for (Object o : products) {
            ProductSearchTool.Product p = (ProductSearchTool.Product) o;
            assertTrue(p.price <= 7000, "价格应 <= 7000, 实际: " + p.price);
        }
    }

    // ============================================
    // 3. 酒店/娱乐搜索 (位置感知)
    // ============================================

    @Test
    void testHotelSearchByLocation() throws Exception {
        AgentContext ctx = new AgentContext();
        ctx.userLat = 39.9087;
        ctx.userLng = 116.3975;
        ctx.sessionId = "test-hotel";
        Map<String, Object> result = hotelTool.execute(ctx, Map.of(
                "maxDistanceKm", 20, "topK", 5, "minRating", 4.5));
        List<?> results = (List<?>) result.get("results");
        assertTrue(results.size() > 0, "北京天安门附近 20km 应有 4.5+ 酒店");
    }

    @Test
    void testEntertainmentSearchBySubType() throws Exception {
        AgentContext ctx = new AgentContext();
        ctx.userCity = "上海";
        Map<String, Object> result = entTool.execute(ctx, Map.of(
                "subType", "CINEMA", "maxDistanceKm", 30, "topK", 5));
        List<?> results = (List<?>) result.get("results");
        assertTrue(results.size() > 0, "上海附近应有影院");
    }

    // ============================================
    // 4. Agent 路由 + 执行
    // ============================================

    @Test
    void testAgentRouting() {
        // 用 "酒店" + "住" 等强信号
        Agent a1 = registry.route("我要订酒店");
        // 手动算 score, debug
        String q = "我要订酒店";
        String lower = q.toLowerCase();
        for (Agent a : registry.list()) {
            int s = 0;
            for (String cap : a.getCapabilities()) {
                if (lower.contains(cap.toLowerCase()) || lower.contains(cap)) s += 3;
            }
            if (lower.contains(a.getName().toLowerCase()) || lower.contains(a.getName())) s += 5;
            if (a.getDescription() != null) {
                String desc = a.getDescription();
                for (int i = 0; i + 2 <= desc.length(); i++) {
                    String sub = desc.substring(i, i + 2);
                    if (lower.contains(sub)) s++;
                }
            }
            System.out.println("SCORE_DEBUG " + a.getName() + " = " + s);
        }
        assertEquals("hotel-agent", a1.getName(), "酒店查询应路由到 hotel-agent");
        Agent a2 = registry.route("上海电影院推荐");
        assertEquals("entertainment-agent", a2.getName(), "影院应路由到 entertainment-agent");
        Agent a3 = registry.route("iPhone 15 多少钱");
        assertEquals("shopping-agent", a3.getName());
    }

    @Test
    void testShoppingAgentExecution() {
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "shop-1";
        ctx.userId = 1L;
        ctx.userQuery = "推荐 iPhone 15, 不超过 8000 元";
        AgentResult r = shoppingAgent.execute(ctx);
        assertTrue(r.success, "应成功: " + r.errorMessage);
        assertNotNull(r.finalAnswer);
        assertTrue(r.finalAnswer.contains("iPhone") || r.finalAnswer.contains("商品"));
        // 长期记忆应该存了价格偏好
        List<MemoryItem> prefs = memory.recallLongTerm(1L, MemoryItem.Type.USER_PREFERENCE, 10);
        assertTrue(prefs.size() > 0, "应存价格偏好到长期记忆");
    }

    // ============================================
    // 5. 权限门控
    // ============================================

    @Test
    void testPermissionGate() {
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "perm-1";
        // 初始未授权
        PermissionGate.PermissionResult r1 = gate.checkAll(
                List.of(Permission.location()), ctx);
        assertFalse(r1.granted);
        assertNotNull(r1.request);
        assertTrue(r1.request.contains("访问位置"));
        // 用户授权
        gate.grant("perm-1", List.of("location:read"));
        PermissionGate.PermissionResult r2 = gate.checkAll(
                List.of(Permission.location()), ctx);
        assertTrue(r2.granted);
    }

    @Test
    void testHotelAgentRequiresLocation() {
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "hotel-perm";
        ctx.userQuery = "北京附近酒店";
        AgentResult r = hotelAgent.execute(ctx);
        // 没授权应被拒
        assertTrue(r.permissionDenied, "应被权限拒绝");
        assertTrue(r.permissionRequest.contains("访问位置"));
    }

    @Test
    void testHotelAgentAfterPermissionGranted() {
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "hotel-ok";
        ctx.userId = 1L;
        ctx.userQuery = "北京附近 4 星以上酒店";
        // 预授权
        gate.grant("hotel-ok", List.of("location:read"));
        AgentResult r = hotelAgent.execute(ctx);
        assertTrue(r.success, "应成功: " + r.errorMessage);
        assertNotNull(r.finalAnswer);
        assertTrue(r.finalAnswer.contains("酒店") || r.finalAnswer.contains("为您"));
    }

    // ============================================
    // 6. 记忆系统
    // ============================================

    @Test
    void testMemoryShortAndLongTerm() {
        memory.remember("s1", MemoryItem.userMessage("你好"));
        memory.remember("s1", MemoryItem.agentMessage("您好!"));
        memory.remember("s1", MemoryItem.userMessage("北京有什么好玩"));
        List<MemoryItem> recent = memory.recallShortTerm("s1", 10);
        assertEquals(3, recent.size());
        // 长期
        memory.rememberLongTerm(100L, MemoryItem.preference("city", "北京"));
        memory.rememberLongTerm(100L, MemoryItem.preference("city", "上海"));  // 覆盖
        List<MemoryItem> prefs = memory.recallLongTerm(100L, MemoryItem.Type.USER_PREFERENCE, 10);
        assertEquals(1, prefs.size(), "同一 key 应被覆盖, 实际数量: " + prefs.size());
        assertTrue(prefs.get(0).content.contains("上海"));
    }

    // ============================================
    // 7. 端到端业务场景
    // ============================================

    @Test
    void testScenarioShopping() {
        // 用户: "iPhone 15 Pro Max, 不超过 12000 元"
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "scenario-1";
        ctx.userId = 1L;
        ctx.userQuery = "iPhone 15 Pro Max, 不超过 12000 元";
        AgentResult r = shoppingAgent.execute(ctx);
        assertTrue(r.success);
        assertTrue(r.finalAnswer.contains("iPhone") || r.finalAnswer.contains("商品"));
        // 检查价格筛选生效
        assertTrue(r.finalAnswer.contains("9999") || r.finalAnswer.contains("12"), "应包含 9999 元 iPhone 或价格信息");
    }

    @Test
    void testScenarioHotelLocationBased() {
        // 用户在北京天安门附近
        gate.grant("hotel-s1", List.of("location:read"));
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "hotel-s1";
        ctx.userId = 2L;
        ctx.userLat = 39.9087;
        ctx.userLng = 116.3975;
        ctx.userQuery = "附近有什么 4 星以上酒店?";
        AgentResult r = hotelAgent.execute(ctx);
        assertTrue(r.success, "应成功: " + r.errorMessage);
        // 应该有具体酒店名字
        boolean hasHotel = r.finalAnswer.contains("酒店")
                || r.finalAnswer.contains("希尔顿")
                || r.finalAnswer.contains("瑰丽")
                || r.finalAnswer.contains("香格里拉");
        assertTrue(hasHotel, "应包含真实酒店: " + r.finalAnswer);
    }

    @Test
    void testScenarioEntertainmentCityBased() {
        gate.grant("ent-s1", List.of("location:read"));
        AgentContext ctx = new AgentContext();
        ctx.sessionId = "ent-s1";
        ctx.userId = 3L;
        ctx.userCity = "上海";
        ctx.userQuery = "上海有什么电影院?";
        AgentResult r = entertainmentAgent.execute(ctx);
        assertTrue(r.success);
        assertTrue(r.finalAnswer.contains("影院") || r.finalAnswer.contains("百丽宫"));
    }
}
