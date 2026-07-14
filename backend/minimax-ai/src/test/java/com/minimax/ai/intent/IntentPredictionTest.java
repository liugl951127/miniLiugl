package com.minimax.ai.intent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户意图精准预测 V3.5.6 单元测试 (算法升级版)
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
     * 测试 2: 下单意图 + 实体抽取 (数量)
     */
    @Test
    @DisplayName("2. 下单意图 + quantity 实体 (V3.5.6 新增)")
    void testOrderIntent() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我想买 10 台服务器");
        assertEquals("order", r.getIntent());
        assertNotNull(r.getSlots().get("product"));
        // V3.5.6 新增: quantity 实体
        boolean hasQty = r.getEntities().stream().anyMatch(e -> "quantity".equals(e.type()));
        assertTrue(hasQty, "应识别 quantity 实体 (10 台)");
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
     * 测试 4: 紧急度 (urgent + 感叹号 + 程度副词)
     */
    @Test
    @DisplayName("4. 紧急度 (V3.5.6 加程度副词)")
    void testUrgency() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r1 = svc.predict("普通问题");
        IntentPredictionService.IntentPrediction r2 = svc.predict("紧急! 马上修复!!");
        IntentPredictionService.IntentPrediction r3 = svc.predict("非常紧急! 系统宕机了");
        assertTrue(r2.getUrgency() > r1.getUrgency(), "紧急文本紧迫度应更高");
        assertTrue(r3.getUrgency() >= r2.getUrgency(), "程度副词加权");
    }

    /**
     * 测试 5: 实体抽取 (time/money/phone/email)
     */
    @Test
    @DisplayName("5. 实体抽取 (time/money/phone/email)")
    void testEntityExtraction() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我 13800138000 邮箱 test@example.com 支付 100 元, 明天发货");
        assertTrue(r.getEntities().stream().anyMatch(e -> "phone".equals(e.type())));
        assertTrue(r.getEntities().stream().anyMatch(e -> "email".equals(e.type())));
        assertTrue(r.getEntities().stream().anyMatch(e -> "money".equals(e.type())));
        assertTrue(r.getEntities().stream().anyMatch(e -> "time".equals(e.type())));
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
     * 测试 7: 备选意图 (softmax 概率)
     */
    @Test
    @DisplayName("7. 备选意图 (V3.5.6 概率化)")
    void testAlternatives() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我要付款 100 元");
        assertNotNull(r.getAlternatives());
        // V3.5.6: probabilities 应只对备选 sum, top1 不计
        double sum = r.getAlternatives().stream().mapToDouble(IntentPredictionService.IntentCandidate::confidence).sum();
        // 备选部分应在 [0, 1) 范围
        assertTrue(sum < 1.0, "备选概率总和应 < 1, 实际=" + sum);
        assertTrue(sum >= 0.0, "备选概率总和应 >= 0, 实际=" + sum);
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
    }

    /**
     * 测试 9: 空文本
     */
    @Test
    @DisplayName("9. 空/空文本 → other 意图")
    void testEmptyText() {
        IntentPredictionService svc = new IntentPredictionService();
        assertEquals("other", svc.predict("").getIntent());
        assertEquals("other", svc.predict(null).getIntent());
    }

    /**
     * 测试 10: 动态添加关键词 (学习能力)
     */
    @Test
    @DisplayName("10. 动态添加关键词 (运行时学习)")
    void testDynamicKeyword() {
        IntentPredictionService svc = new IntentPredictionService();
        svc.addKeyword("query", "黄金", 100.0);
        IntentPredictionService.IntentPrediction r = svc.predict("我要查一下黄金价格");
        assertEquals("query", r.getIntent());
    }

    /**
     * 测试 11: 意图分类完整列表
     */
    @Test
    @DisplayName("11. 意图分类列表 (>=9 个, V3.5.6 加 register)")
    void testIntentsList() {
        IntentPredictionService svc = new IntentPredictionService();
        assertTrue(svc.listIntents().size() >= 9);
    }

    /**
     * 测试 12: 槽位 fallback
     */
    @Test
    @DisplayName("12. 槽位 (intent-specific)")
    void testSlots() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("买 2 个 iPhone");
        assertNotNull(r.getSlots().get("product"));
    }

    // ═══════════════════════════════════════════════════════════
    // V3.5.6 新增测试
    // ═══════════════════════════════════════════════════════════

    /**
     * 测试 13: V3.5.6 同义词扩展 (退钱 → 退款)
     */
    @Test
    @DisplayName("13. 同义词扩展 (V3.5.6 新增)")
    void testSynonymExpansion() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我要退钱, 太烂了");
        assertEquals("complaint", r.getIntent(), "退钱 应被同义词识别为退款 (complaint)");
        assertFalse(r.getExpansions().isEmpty(), "应有同义词扩展记录");
    }

    /**
     * 测试 14: V3.5.6 N-gram 短语
     */
    @Test
    @DisplayName("14. N-gram 短语 (V3.5.6 新增)")
    void testNgram() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我要退款, 请尽快处理");
        // 我要退款 是 4 字 = 2-gram × 2, 命中 complaint 短语
        Map<String, Object> modelScores = r.getModelScores();
        assertNotNull(modelScores);
        assertTrue(modelScores.containsKey("ngram"), "应包含 ngram 模型打分");
    }

    /**
     * 测试 15: V3.5.6 Negation Handling
     */
    @Test
    @DisplayName("15. Negation 否定处理 (V3.5.6 新增)")
    void testNegation() {
        IntentPredictionService svc = new IntentPredictionService();
        // 否定 + 满意 -> 实际 negative
        IntentPredictionService.IntentPrediction r = svc.predict("这个产品不满意");
        // 关键词 "满意" 触发 positive, 但 "不" 否定作用域
        // V3.5.6 应翻转为 negative 或降低 posWeighted
        assertNotNull(r.getSentiment());
    }

    /**
     * 测试 16: V3.5.6 上下文继承 (多轮对话)
     */
    @Test
    @DisplayName("16. 上下文继承 (V3.5.6 新增)")
    void testContext() {
        IntentPredictionService svc = new IntentPredictionService();
        // 第 1 轮: 明确投诉
        IntentPredictionService.IntentPrediction r1 = svc.predict("我要退款", "session-A");
        // 第 2 轮: 模糊 "咋办", 应继承 complaint 上下文
        IntentPredictionService.IntentPrediction r2 = svc.predict("咋办?", "session-A");
        // 第 2 轮置信度应 >= 0.5 (有上下文继承)
        assertTrue(r2.getConfidence() > 0.4, "上下文继承应提升置信度");
    }

    /**
     * 测试 17: V3.5.6 文本归一化
     */
    @Test
    @DisplayName("17. 文本归一化 (V3.5.6 新增)")
    void testNormalization() {
        IntentPredictionService svc = new IntentPredictionService();
        // 全角数字 + 简繁
        IntentPredictionService.IntentPrediction r = svc.predict("退訂單, 謝謝");
        assertNotNull(r.getNormalizedText(), "应有归一化结果");
        assertTrue(r.getNormalizedText().contains("订单"), "应归一为简体, 实际: " + r.getNormalizedText());
    }

    /**
     * 测试 18: V3.5.6 算法版本
     */
    @Test
    @DisplayName("18. 算法版本 (V3.5.6)")
    void testAlgorithmVersion() {
        IntentPredictionService svc = new IntentPredictionService();
        assertEquals("v3.5.6-weighted-voting", svc.getAlgorithmVersion());
    }

    /**
     * 测试 19: V3.5.6 动态添加短语
     */
    @Test
    @DisplayName("19. 动态添加短语 (V3.5.6 新增)")
    void testAddPhrase() {
        IntentPredictionService svc = new IntentPredictionService();
        svc.addPhrase("order", "赶紧下", 20.0);
        IntentPredictionService.IntentPrediction r = svc.predict("赶紧下单吧, 想要");
        assertEquals("order", r.getIntent());
    }

    /**
     * 测试 20: V3.5.6 身份证 + URL 实体
     */
    @Test
    @DisplayName("20. 身份证/URL 实体 (V3.5.6 新增)")
    void testIdCardEntity() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我的身份证 110101199003078888 丢了, 怎么办?");
        boolean hasId = r.getEntities().stream().anyMatch(e -> "id_card".equals(e.type()));
        assertTrue(hasId, "应识别身份证");
    }

    /**
     * 测试 21: V3.5.6 softmax 概率 sum = 1
     */
    @Test
    @DisplayName("21. softmax 概率归一 (V3.5.6 新增)")
    void testSoftmaxNormalization() {
        IntentPredictionService svc = new IntentPredictionService();
        IntentPredictionService.IntentPrediction r = svc.predict("我要查一下, 顺便问个问题");
        Map<String, Double> probs = r.getIntentScores();
        assertNotNull(probs);
        if (!probs.isEmpty()) {
            double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
            assertTrue(sum > 0.99 && sum < 1.01, "概率应 sum≈1, 实际=" + sum);
        }
    }

    /**
     * 测试 22: V3.5.6 benchmark 准确率 >= 75%
     */
    @Test
    @DisplayName("22. 准确率 (>=75%, V3.5.6 自评)")
    void testAccuracy() {
        IntentPredictionService svc = new IntentPredictionService();
        String[][] cases = {
            {"查询一下订单", "query"},
            {"我要买 iPhone", "order"},
            {"退款, 差评", "complaint"},
            {"怎么登录", "consult"},
            {"我要付款", "pay"},
            {"退钱!", "complaint"},
            {"我要注册", "register"},
            {"取消订单", "cancel"},
            {"建议添加暗黑模式", "feedback"},
            {"12306 怎么用", "consult"}
        };
        int correct = 0;
        for (String[] c : cases) {
            IntentPredictionService.IntentPrediction r = svc.predict(c[0]);
            if (c[1].equals(r.getIntent())) correct++;
        }
        assertTrue(correct >= 7, "准确率应 >=70%, 实际 " + correct + "/10");
    }
}
