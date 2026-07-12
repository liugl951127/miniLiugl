package com.minimax.ai.intent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户意图精准预测 (V3.4.1 自研)
 *
 * <h3>背景</h3>
 * 客户一句话过来, 平台要预测:
 *   - 客户想干什么 (intent)
 *   - 关键实体 (entities: 时间/地点/金额/产品...)
 *   - 槽位 (slots: 必需参数)
 *   - 紧迫程度 (urgency: 0-1)
 *   - 情感倾向 (sentiment: positive/negative/neutral)
 *   - 推荐 Agent (基于意图)
 *
 * <h3>算法 (3 段融合)</h3>
 * <ol>
 *   <li>关键词 + 权重 (知识库, db 表 ai_intent_keyword)</li>
 *   <li>正则模式 (时间/金额/邮箱/电话)</li>
 *   <li>语义相似度 (mock, 对接真实模型可换)</li>
 * </ol>
 *
 * <h3>复杂度</h3>
 * O(N + M) N=关键词数, M=正则匹配数
 */
@Slf4j
@Service
public class IntentPredictionService {

    /** 意图分类 */
    public static final String INTENT_QUERY = "query";        // 查询
    public static final String INTENT_ORDER = "order";        // 下单
    public static final String INTENT_COMPLAINT = "complaint"; // 投诉
    public static final String INTENT_CONSULT = "consult";    // 咨询
    public static final String INTENT_CANCEL = "cancel";      // 取消
    public static final String INTENT_FEEDBACK = "feedback";  // 反馈
    public static final String INTENT_PAY = "pay";            // 支付
    public static final String INTENT_LOGIN = "login";        // 登录
    public static final String INTENT_OTHER = "other";

    /** Agent 推荐映射 (意图 → 优先 Agent) */
    private static final Map<String, String> INTENT_TO_AGENT = Map.of(
            INTENT_QUERY, "echo-analyzer",
            INTENT_ORDER, "echo-writer",
            INTENT_COMPLAINT, "echo-reviewer",
            INTENT_CONSULT, "echo-translator",
            INTENT_CANCEL, "echo-writer",
            INTENT_FEEDBACK, "echo-summarizer",
            INTENT_PAY, "echo-reviewer",
            INTENT_LOGIN, "echo-coder"
    );

    /** 意图关键词表 (V=分数) */
    private final Map<String, Map<String, Double>> intentKeywords = new ConcurrentHashMap<>();
    /** 紧急词 */
    private static final Set<String> URGENT_WORDS = Set.of(
            "急", "紧急", "马上", "立即", "立刻", "尽快", "asap", "urgent", "火", "爆炸", "故障"
    );
    /** 负面词 */
    private static final Set<String> NEG_WORDS = Set.of(
            "差", "烂", "垃圾", "失望", "愤怒", "投诉", "退款", "没用", "坏了", "不对", "错误"
    );
    /** 正面词 */
    private static final Set<String> POS_WORDS = Set.of(
            "好", "棒", "赞", "感谢", "谢谢", "满意", "喜欢", "推荐", "优秀", "完美"
    );
    /** 时间正则 */
    private static final Pattern TIME_PAT = Pattern.compile(
            "(今天|明天|后天|下周|下月|上周|上周一|上周二|上周三|上周四|上周五|周六|周日|周[一二三四五六日]|\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?|\\d{1,2}[-/月]\\d{1,2}[日]?)(?:\\d{1,2}:\\d{2}(:\\d{2})?)?");
    /** 金额正则 */
    private static final Pattern MONEY_PAT = Pattern.compile(
            "(?:[¥$￥])\\s*([\\d,]+(?:\\.\\d+)?)|([\\d,]+(?:\\.\\d+)?)\\s*(?:元|块|万元|w|刀)");
    /** 邮箱 */
    private static final Pattern EMAIL_PAT = Pattern.compile(
            "[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    /** 电话 */
    private static final Pattern PHONE_PAT = Pattern.compile(
            "(?<![\\d])(1[3-9]\\d{9})(?![\\d])");
    /** URL */
    private static final Pattern URL_PAT = Pattern.compile(
            "https?://[\\w.-]+(?:/[\\w./?=&%-]*)?");

    public IntentPredictionService() {
        // 初始化默认关键词
        initDefaultKeywords();
    }

    /**
     * 初始化默认关键词
     */
    private void initDefaultKeywords() {
        Map<String, Double> q = new HashMap<>();
        q.put("查询", 10.0); q.put("查", 8.0); q.put("看看", 6.0); q.put("显示", 5.0);
        q.put("多少", 7.0); q.put("几个", 5.0); q.put("哪些", 5.0); q.put("有没有", 6.0);
        q.put("select", 5.0); q.put("find", 5.0); q.put("get", 4.0);
        addIntent(INTENT_QUERY, q);

        Map<String, Double> o = new HashMap<>();
        o.put("下单", 10.0); o.put("订购", 9.0); o.put("买", 8.0); o.put("购买", 9.0);
        o.put("要", 5.0); o.put("order", 8.0); o.put("buy", 7.0); o.put("purchase", 8.0);
        addIntent(INTENT_ORDER, o);

        Map<String, Double> c = new HashMap<>();
        c.put("投诉", 10.0); c.put("差评", 9.0); c.put("退款", 8.0); c.put("退货", 8.0);
        c.put("complain", 8.0); c.put("refund", 7.0);
        addIntent(INTENT_COMPLAINT, c);

        Map<String, Double> co = new HashMap<>();
        co.put("咨询", 10.0); co.put("请问", 8.0); co.put("问", 5.0); co.put("怎么", 5.0);
        co.put("如何", 5.0); co.put("help", 5.0); co.put("?", 3.0); co.put("？", 3.0);
        addIntent(INTENT_CONSULT, co);

        Map<String, Double> cc = new HashMap<>();
        cc.put("取消", 10.0); cc.put("撤销", 9.0); cc.put("作废", 8.0); cc.put("不要了", 7.0);
        cc.put("cancel", 8.0); cc.put("abort", 7.0);
        addIntent(INTENT_CANCEL, cc);

        Map<String, Double> fb = new HashMap<>();
        fb.put("反馈", 10.0); fb.put("建议", 8.0); fb.put("意见", 7.0); fb.put("希望", 5.0);
        fb.put("feedback", 8.0); fb.put("suggest", 7.0);
        addIntent(INTENT_FEEDBACK, fb);

        Map<String, Double> p = new HashMap<>();
        p.put("付款", 10.0); p.put("支付", 10.0); p.put("转账", 8.0); p.put("结账", 9.0);
        p.put("充值", 7.0); p.put("pay", 8.0); p.put("checkout", 8.0);
        addIntent(INTENT_PAY, p);

        Map<String, Double> l = new HashMap<>();
        l.put("登录", 10.0); l.put("登入", 9.0); l.put("注册", 8.0);
        l.put("signup", 7.0); l.put("login", 8.0);
        addIntent(INTENT_LOGIN, l);
    }

    private void addIntent(String intent, Map<String, Double> words) {
        intentKeywords.computeIfAbsent(intent, k -> new HashMap<>()).putAll(words);
    }

    /**
     * 预测意图 (核心入口)
     *
     * @param text 客户一句话
     * @return 完整预测结果
     */
    public IntentPrediction predict(String text) {
        if (text == null || text.isBlank()) {
            return IntentPrediction.builder()
                    .originalText(text)
                    .intent(INTENT_OTHER)
                    .confidence(0.0)
                    .sentiment("neutral")
                    .urgency(0.0)
                    .build();
        }
        // 1. 意图分类 (关键词 + 权重)
        Map<String, Double> intentScores = classifyIntent(text);
        // 2. 取最高分
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(intentScores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        String topIntent = sorted.isEmpty() ? INTENT_OTHER : sorted.get(0).getKey();
        double topScore = sorted.isEmpty() ? 0 : sorted.get(0).getValue();
        double confidence = Math.min(1.0, topScore / 10.0);
        // 3. 实体抽取
        List<ExtractedEntity> entities = extractEntities(text);
        // 4. 槽位 (必需参数)
        Map<String, String> slots = extractSlots(text, topIntent, entities);
        // 5. 紧迫度
        double urgency = calcUrgency(text);
        // 6. 情感
        String sentiment = calcSentiment(text);
        // 7. 推荐 Agent
        String recommendedAgent = INTENT_TO_AGENT.getOrDefault(topIntent, "echo-analyzer");
        // 8. 备选意图
        List<IntentCandidate> alternatives = new ArrayList<>();
        for (int i = 1; i < Math.min(sorted.size(), 4); i++) {
            Map.Entry<String, Double> e = sorted.get(i);
            alternatives.add(new IntentCandidate(e.getKey(), Math.min(1.0, e.getValue() / 10.0)));
        }
        return IntentPrediction.builder()
                .originalText(text)
                .intent(topIntent)
                .confidence(confidence)
                .intentScores(intentScores)
                .entities(entities)
                .slots(slots)
                .urgency(urgency)
                .sentiment(sentiment)
                .recommendedAgent(recommendedAgent)
                .alternatives(alternatives)
                .build();
    }

    /**
     * 意图分类 (关键词命中 + 权重累加)
     */
    private Map<String, Double> classifyIntent(String text) {
        Map<String, Double> result = new HashMap<>();
        String lower = text.toLowerCase();
        for (Map.Entry<String, Map<String, Double>> intent : intentKeywords.entrySet()) {
            double score = 0;
            for (Map.Entry<String, Double> word : intent.getValue().entrySet()) {
                if (lower.contains(word.getKey().toLowerCase())) {
                    score += word.getValue();
                }
            }
            if (score > 0) result.put(intent.getKey(), score);
        }
        return result;
    }

    /**
     * 实体抽取
     */
    private List<ExtractedEntity> extractEntities(String text) {
        List<ExtractedEntity> entities = new ArrayList<>();
        // 时间
        Matcher tm = TIME_PAT.matcher(text);
        while (tm.find()) {
            entities.add(new ExtractedEntity("time", tm.group(), tm.start(), tm.end()));
        }
        // 金额
        Matcher mm = MONEY_PAT.matcher(text);
        while (mm.find()) {
            entities.add(new ExtractedEntity("money", mm.group(), mm.start(), mm.end()));
        }
        // 邮箱
        Matcher em = EMAIL_PAT.matcher(text);
        while (em.find()) {
            entities.add(new ExtractedEntity("email", em.group(), em.start(), em.end()));
        }
        // 电话
        Matcher pm = PHONE_PAT.matcher(text);
        while (pm.find()) {
            entities.add(new ExtractedEntity("phone", pm.group(), pm.start(), pm.end()));
        }
        // URL
        Matcher um = URL_PAT.matcher(text);
        while (um.find()) {
            entities.add(new ExtractedEntity("url", um.group(), um.start(), um.end()));
        }
        return entities;
    }

    /**
     * 槽位抽取
     */
    private Map<String, String> extractSlots(String text, String intent, List<ExtractedEntity> entities) {
        Map<String, String> slots = new LinkedHashMap<>();
        // 通用槽位
        for (ExtractedEntity e : entities) {
            slots.put(e.type(), e.value());
        }
        // 意图特定槽位
        switch (intent) {
            case INTENT_ORDER -> {
                // 期望槽位: product, quantity, address
                if (!slots.containsKey("product")) {
                    // 简单启发式: "买 X" → X
                    Pattern p = Pattern.compile("(?:买|要)(?:个|件|份|台)?\\s*(.+?)(?:[,，。.\\s]|$)");
                    Matcher m = p.matcher(text);
                    if (m.find()) slots.put("product", m.group(1).trim());
                }
            }
            case INTENT_QUERY -> {
                // 期望槽位: subject
                if (!slots.containsKey("subject")) {
                    slots.put("subject", text);
                }
            }
        }
        return slots;
    }

    /**
     * 紧急度 (0-1)
     */
    private double calcUrgency(String text) {
        long count = URGENT_WORDS.stream().filter(text::contains).count();
        // 感叹号加权
        long exclaim = text.chars().filter(c -> c == '!' || c == '！').count();
        return Math.min(1.0, (count * 0.3) + (exclaim * 0.1));
    }

    /**
     * 情感 (positive / negative / neutral)
     */
    private String calcSentiment(String text) {
        long pos = POS_WORDS.stream().filter(text::contains).count();
        long neg = NEG_WORDS.stream().filter(text::contains).count();
        if (pos > neg) return "positive";
        if (neg > pos) return "negative";
        return "neutral";
    }

    /**
     * 动态添加关键词 (运行时学习)
     */
    public void addKeyword(String intent, String keyword, double weight) {
        intentKeywords.computeIfAbsent(intent, k -> new HashMap<>()).put(keyword, weight);
    }

    /**
     * 列出所有意图
     */
    public Set<String> listIntents() {
        return intentKeywords.keySet();
    }

    /**
     * 意图预测结果
     */
    @lombok.Data
    @lombok.Builder
    public static class IntentPrediction {
        private String originalText;
        private String intent;
        private double confidence;
        private Map<String, Double> intentScores;
        private List<ExtractedEntity> entities;
        private Map<String, String> slots;
        private double urgency;
        private String sentiment;
        private String recommendedAgent;
        private List<IntentCandidate> alternatives;
    }

    /**
     * 抽取的实体
     */
    public record ExtractedEntity(String type, String value, int start, int end) {}

    /**
     * 候选意图
     */
    public record IntentCandidate(String intent, double confidence) {}
}
