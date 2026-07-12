package com.minimax.ai.intent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户意图精准预测 (V3.4.1) 单元测试
 */
class IntentPredictionTest {

    /**
     * 测试 1: 简单查询意图
     */
    @Test
    @DisplayName("1. 查询意图 (高置信度)")
    void testQueryIntent() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("查询一下今天的订单");
        assertEquals("query", r.getIntent());
        assertTrue(r.getConfidence() > 0.3);
    }

    /**
     * 测试 2: 下单意图
     */
    @Test
    @DisplayName("2. 下单意图")
    void testOrderIntent() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我想买 10 台服务器");
        assertEquals("order", r.getIntent());
        assertNotNull(r.getSlots().get("product"));
    }

    /**
     * 测试 3: 投诉意图 + 负面情感
     */
    @Test
    @DisplayName("3. 投诉意图 + 负面情感")
    void testComplaintIntent() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我要退款, 差评, 紧急!");
        assertEquals("complaint", r.getIntent());
        assertEquals("negative", r.getSentiment());
        assertTrue(r.getUrgency() > 0.1);
    }

    /**
     * 测试 4: 紧急词检测
     */
    @Test
    @DisplayName("4. 紧急度 (urgent + 感叹号)")
    void testUrgency() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r1 = svc.predict("普通问题");
        IntentPredictionService.IntentPrediction r2 = svc.predict("紧急! 马上修复!!");
        assertTrue(r2.getUrgency() > r1.getUrgency(), "紧急文本紧迫度应更高");
    }

    /**
     * 测试 5: 实体抽取 (时间/金额/电话/邮箱)
     */
    @Test
    @DisplayName("5. 实体抽取 (time/money/phone/email)")
    void testEntityExtraction() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我 13800138000 邮箱 test@example.com 支付 100 元, 明天发货");
        // 应有 phone/email/money/time
        boolean hasPhone = r.getEntities().stream().anyMatch(e -> "phone".equals(e.type()));
        boolean hasEmail = r.getEntities().stream().anyMatch(e -> "email".equals(e.type()));
        boolean hasMoney = r.getEntities().stream().anyMatch(e -> "money".equals(e.type()));
        boolean hasTime = r.getEntities().stream().anyMatch(e -> "time".equals(e.type()));
        assertTrue(hasPhone, "应识别 phone");
        assertTrue(hasEmail, "应识别 email");
        assertTrue(hasMoney, "应识别 money");
        assertTrue(hasTime, "应识别 time");
    }

    /**
     * 测试 6: 情感 (positive)
     */
    @Test
    @DisplayName("6. 情感 (positive)")
    void testPositiveSentiment() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("非常棒, 谢谢!");
        assertEquals("positive", r.getSentiment());
    }

    /**
     * 测试 7: 备选意图
     */
    @Test
    @DisplayName("7. 备选意图 (alternatives)")
    void testAlternatives() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我要付款 100 元");
        assertNotNull(r.getAlternatives());
        assertTrue(r.getAlternatives().size() >= 0);
    }

    /**
     * 测试 8: 推荐 Agent
     */
    @Test
    @DisplayName("8. 推荐 Agent (非空)")
    void testRecommendedAgent() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("查询订单状态");
        assertNotNull(r.getRecommendedAgent());
        assertFalse(r.getRecommendedAgent().isEmpty());
    }

    /**
     * 测试 9: 空文本
     */
    @Test
    @DisplayName("9. 空/空文本 → other 意图")
    void testEmptyText() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r1 = svc.predict("");
        assertEquals("other", r1.getIntent());
        IntentPredictionService.IntentPrediction r2 = svc.predict(null);
        assertEquals("other", r2.getIntent());
    }

    /**
     * 测试 10: 动态添加关键词
     */
    @Test
    @DisplayName("10. 动态添加关键词 (运行时学习)")
    void testDynamicKeyword() {
        IntentPredictionService svc = new IntentPredictionService();
        svc.addKeyword("query", "黄金", 100.0);  // 高权重
        IntentPredictionService.IntentPrediction r = svc.predict("我要查一下黄金价格");
        // 黄金 应主导 query 意图
        assertEquals("query", r.getIntent());
    }

    /**
     * 测试 11: 意图分类完整列表
     */
    @Test
    @DisplayName("11. 意图分类列表 (>=8 个)")
    void testIntentsList() {
        IntentPredictionService svc = new IntentPredictionService();
        assertTrue(svc.listIntents().size() >= 8);
    }

    /**
     * 测试 12: 槽位 fallback
     */
    @Test
    @DisplayName("12. 槽位 (intent-specific)")
    void testSlots() {
        IntentPredictionService svc = new IntentPredictionService();
        // order 意图应填 product
        IntentPredictionService.IntentPrediction r = svc.predict("买 2 个 iPhone");
        assertNotNull(r.getSlots().get("product"));
    }
}
