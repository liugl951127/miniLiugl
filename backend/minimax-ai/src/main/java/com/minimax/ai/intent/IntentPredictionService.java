package com.minimax.ai.intent;

import com.minimax.ai.intent.TextNormalizer.NormalizedResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户意图精准预测 V2 (V3.5.7 外部化配置版)
 *
 * <h2>V3.5.6 → V3.5.7 升级</h2>
 * <table border="1" cellpadding="4">
 *   <tr><th>维度</th><th>V3.5.6 (硬编码)</th><th>V3.5.7 (外部化)</th></tr>
 *   <tr><td>关键词</td><td>9 个 map 写死在 .java</td><td>yml 配 + 运行时改</td></tr>
 *   <tr><td>短语</td><td>硬编码</td><td>yml 配置</td></tr>
 *   <tr><td>同义词</td><td>硬编码</td><td>yml 配置</td></tr>
 *   <tr><td>紧急词</td><td>静态 Set</td><td>yml 配置</td></tr>
 *   <tr><td>模型权重</td><td>DEFAULT_WEIGHTS 常量</td><td>yml + 运行时调</td></tr>
 *   <tr><td>测试用例</td><td>Controller 硬编码 20 条</td><td>yml 可扩到 100+</td></tr>
 * </table>
 *
 * <h2>3 层数据源 (优先级递减)</h2>
 * <ol>
 *   <li>运行时 API addKeyword/addPhrase 动态加</li>
 *   <li>外部 yml (IntentConfig)</li>
 *   <li>{@link IntentConfig#defaults()} 兜底</li>
 * </ol>
 *
 * <h2>运行时更新</h2>
 * <ul>
 *   <li>PUT /api/v1/ai/intent/config   - 全量替换 (rebuild)</li>
 *   <li>POST /api/v1/ai/intent/keyword  - 加单个词 (向后兼容)</li>
 *   <li>POST /api/v1/ai/intent/phrase   - 加单个短语</li>
 *   <li>GET  /api/v1/ai/intent/config   - 查看当前配置</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.7
 */
@Slf4j
@Service
public class IntentPredictionService {

    /** Spring 默认无参构造 (从 yml 加载配置) */
    public IntentPredictionService() {
        this.config = IntentConfig.defaults();
        reloadConfig();
    }

    /** 测试 / 热更新场景用, 显式传入配置 */
    public IntentPredictionService(IntentConfig config) {
        this.config = config != null ? mergeWithDefaults(config) : IntentConfig.defaults();
        reloadConfig();
    }

    // ═══════════════════════════════════════════════════════════
    // 意图分类常量 (这些是 schema, 不放 yml)
    // ═══════════════════════════════════════════════════════════

    public static final String INTENT_QUERY = "query";
    public static final String INTENT_ORDER = "order";
    public static final String INTENT_COMPLAINT = "complaint";
    public static final String INTENT_CONSULT = "consult";
    public static final String INTENT_CANCEL = "cancel";
    public static final String INTENT_FEEDBACK = "feedback";
    public static final String INTENT_PAY = "pay";
    public static final String INTENT_LOGIN = "login";
    public static final String INTENT_REGISTER = "register";
    public static final String INTENT_OTHER = "other";

    // ═══════════════════════════════════════════════════════════
    // 知识库 (从 IntentConfig 加载)
    // ═══════════════════════════════════════════════════════════

    /** 意图关键词表 (intent -> { word -> weight }) */
    private final Map<String, Map<String, Double>> intentKeywords = new ConcurrentHashMap<>();
    /** 关键 N-gram 短语 (intent -> { phrase -> weight }) */
    private final Map<String, Map<String, Double>> intentPhrases = new ConcurrentHashMap<>();
    /** Agent 推荐映射 (intent -> agentId) */
    private final Map<String, String> intentToAgent = new ConcurrentHashMap<>();
    /** 紧急词 (Set 加速 contains 查询) */
    private Set<String> urgentWords = Set.of();
    /** 紧急程度副词 */
    private Set<String> urgentDegreeWords = Set.of();
    /** 正面词 */
    private Set<String> positiveWords = Set.of();
    /** 负面词 */
    private Set<String> negativeWords = Set.of();
    /** 程度副词 (word -> 系数) */
    private Map<String, Double> degreeAdverbs = Map.of();
    /** 4 模型权重 */
    private double[] modelWeights = WeightedVotingEnsemble.DEFAULT_WEIGHTS;
    /** sigmoid 置信度缩放 */
    private double confidenceScale = 5.0;

    /** Benchmark 测试用例 (intent -> benchmark) */
    private List<IntentConfig.BenchmarkCase> benchmarkCases = List.of();

    // ═══════════════════════════════════════════════════════════
    // 实体正则 (V3.5.7 保留, 这些是 schema 不是业务规则)
    // ═══════════════════════════════════════════════════════════

    private static final Pattern TIME_PAT = Pattern.compile(
            "(今天|明天|后天|大后天|下周|下月|上周|上周一|上周二|上周三|上周四|上周五|周六|周日|周[一二三四五六日末初]|\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?|\\d{1,2}[-/月]\\d{1,2}[日]?)(?:\\s?\\d{1,2}:\\d{2}(?::\\d{2})?)?");
    private static final Pattern MONEY_PAT = Pattern.compile(
            "(?:[¥$￥])\\s*([\\d,]+(?:\\.\\d+)?)|([\\d,]+(?:\\.\\d+)?)\\s*(?:元|块|万元|w|刀|rmb|RMB)");
    private static final Pattern EMAIL_PAT = Pattern.compile(
            "[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE_PAT = Pattern.compile(
            "(?<![\\d])(1[3-9]\\d{9})(?![\\d])");
    private static final Pattern URL_PAT = Pattern.compile(
            "https?://[\\w.-]+(?:/[\\w./?=&%-]*)?");
    private static final Pattern QUANTITY_PAT = Pattern.compile(
            "(\\d+)\\s*(个|件|份|台|辆|条|只|张|本|瓶|块|箱)");
    private static final Pattern ID_CARD_PAT = Pattern.compile(
            "(?<![\\d])([1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx])(?![\\d])");

    // ═══════════════════════════════════════════════════════════
    // 上下文 (短时记忆, LRU + TTL)
    // ═══════════════════════════════════════════════════════════

    private final Map<String, ContextEntry> sessionContext = new ConcurrentHashMap<>();
    private static final long CONTEXT_TTL_MS = 5 * 60 * 1000L;

    private record ContextEntry(String intent, long timestamp) {}

    // ═══════════════════════════════════════════════════════════
    // 外部配置 (Spring 自动注入, 兜底用 defaults)
    // ═══════════════════════════════════════════════════════════

    private IntentConfig config;

    @Autowired
    public void setConfig(IntentConfig config) {
        // yml 没配 intent 段时, Spring 仍会创建空 IntentConfig
        // 用 defaults() 兜底
        this.config = config != null && config.getKeywords() != null
                ? mergeWithDefaults(config)
                : IntentConfig.defaults();
        reloadConfig();
    }

    /** 合并 yml + defaults, yml 缺的字段用 defaults 补 */
    private static IntentConfig mergeWithDefaults(IntentConfig yml) {
        IntentConfig d = IntentConfig.defaults();
        if (yml.getAlgorithm() != null) d.setAlgorithm(yml.getAlgorithm());
        if (yml.getWeights() != null && yml.getWeights().length == 4) d.setWeights(yml.getWeights());
        if (yml.getConfidenceScale() > 0) d.setConfidenceScale(yml.getConfidenceScale());
        if (yml.getNegation() != null) d.setNegation(yml.getNegation());
        if (yml.getUrgent() != null) d.setUrgent(yml.getUrgent());
        if (yml.getSentiment() != null) d.setSentiment(yml.getSentiment());
        if (yml.getAgents() != null && !yml.getAgents().isEmpty()) d.setAgents(yml.getAgents());
        if (yml.getKeywords() != null && !yml.getKeywords().isEmpty()) d.setKeywords(yml.getKeywords());
        if (yml.getPhrases() != null && !yml.getPhrases().isEmpty()) d.setPhrases(yml.getPhrases());
        if (yml.getSynonyms() != null && !yml.getSynonyms().isEmpty()) d.setSynonyms(yml.getSynonyms());
        if (yml.getTraditional() != null && !yml.getTraditional().isEmpty()) d.setTraditional(yml.getTraditional());
        if (yml.getBenchmark() != null && !yml.getBenchmark().isEmpty()) d.setBenchmark(yml.getBenchmark());
        return d;
    }

    @PostConstruct
    public void init() {
        if (config == null) {
            config = IntentConfig.defaults();
            reloadConfig();
        }
        log.info("[Intent] 加载配置: {} 个意图, {} 个关键词, {} 个短语, {} 个 benchmark",
                intentKeywords.size(),
                intentKeywords.values().stream().mapToInt(Map::size).sum(),
                intentPhrases.values().stream().mapToInt(Map::size).sum(),
                benchmarkCases.size());
    }

    /** 重载配置 (启动时 + 运行时热更新) */
    public synchronized void reloadConfig() {
        intentKeywords.clear();
        if (config.getKeywords() != null) {
            for (var e : config.getKeywords().entrySet()) {
                intentKeywords.put(e.getKey(), new HashMap<>(e.getValue()));
            }
        }
        intentPhrases.clear();
        if (config.getPhrases() != null) {
            for (var e : config.getPhrases().entrySet()) {
                intentPhrases.put(e.getKey(), new HashMap<>(e.getValue()));
            }
        }
        intentToAgent.clear();
        intentToAgent.putAll(config.getAgents());

        urgentWords = new HashSet<>(config.getUrgent().getWords());
        urgentDegreeWords = new HashSet<>(config.getUrgent().getDegree());
        positiveWords = new HashSet<>(config.getSentiment().getPositive());
        negativeWords = new HashSet<>(config.getSentiment().getNegative());
        degreeAdverbs = new HashMap<>(config.getSentiment().getDegreeWords());
        modelWeights = config.getWeights();
        confidenceScale = config.getConfidenceScale();
        benchmarkCases = new ArrayList<>(config.getBenchmark());

        // 同步给 TextNormalizer + NegationHandler
        TextNormalizer.setSynonyms(config.getSynonyms());
        TextNormalizer.setTraditional(config.getTraditional());
        NegationHandler.setPrefixes(config.getNegation().getPrefixes());
        NegationHandler.setScope(config.getNegation().getScope());
    }

    // ═══════════════════════════════════════════════════════════
    // 主入口
    // ═══════════════════════════════════════════════════════════

    public IntentPrediction predict(String text) {
        return predict(text, null);
    }

    public IntentPrediction predict(String text, String sessionId) {
        if (text == null || text.isBlank()) {
            return IntentPrediction.builder()
                    .originalText(text)
                    .normalizedText("")
                    .intent(INTENT_OTHER)
                    .confidence(0.0)
                    .sentiment("neutral")
                    .urgency(0.0)
                    .algorithm(config.getAlgorithm())
                    .build();
        }
        NormalizedResult norm = TextNormalizer.normalize(text);
        String work = norm.normalized();
        log.debug("[intent] in='{}' normalized='{}' expansions={}", text, work, norm.expansions());
        Map<Integer, Integer> negScopes = NegationHandler.detectNegationScopes(work);

        Map<String, Double> tfScores = scoreKeywords(work, negScopes);
        Map<String, Double> ngramScores = scoreNgrams(work);
        Map<String, Double> expandScores = scoreExpansions(norm.expansions());
        String ctxIntent = getContextIntent(sessionId);

        Map<String, Double> fused = WeightedVotingEnsemble.fuse(
                tfScores, ngramScores, expandScores, ctxIntent, modelWeights);

        if (fused.isEmpty()) {
            return IntentPrediction.builder()
                    .originalText(text)
                    .normalizedText(work)
                    .intent(INTENT_OTHER)
                    .confidence(0.0)
                    .sentiment(calcSentiment(text, work, negScopes))
                    .urgency(calcUrgency(text, work))
                    .algorithm(config.getAlgorithm())
                    .modelScores(Map.of("tf", tfScores, "ngram", ngramScores, "expand", expandScores))
                    .build();
        }
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(fused.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        String topIntent = sorted.get(0).getKey();
        double confidence = confidenceSigmoid(sorted);

        List<ExtractedEntity> entities = extractEntities(text);
        Map<String, String> slots = extractSlots(text, topIntent, entities);

        double urgency = calcUrgency(text, work);
        String sentiment = calcSentiment(text, work, negScopes);

        String recommendedAgent = intentToAgent.getOrDefault(topIntent, "echo-analyzer");

        List<IntentCandidate> alternatives = new ArrayList<>();
        Map<String, Double> probs = WeightedVotingEnsemble.softmax(fused);
        for (int i = 1; i < Math.min(sorted.size(), 4); i++) {
            Map.Entry<String, Double> e = sorted.get(i);
            alternatives.add(new IntentCandidate(e.getKey(), probs.getOrDefault(e.getKey(), 0.0)));
        }

        if (sessionId != null) {
            sessionContext.put(sessionId, new ContextEntry(topIntent, System.currentTimeMillis()));
            cleanupExpiredContext();
        }

        return IntentPrediction.builder()
                .originalText(text)
                .normalizedText(work)
                .intent(topIntent)
                .confidence(confidence)
                .intentScores(probs)
                .entities(entities)
                .slots(slots)
                .urgency(urgency)
                .sentiment(sentiment)
                .recommendedAgent(recommendedAgent)
                .alternatives(alternatives)
                .algorithm(config.getAlgorithm())
                .modelScores(Map.of(
                        "tf", tfScores,
                        "ngram", ngramScores,
                        "expand", expandScores,
                        "context", ctxIntent != null ? Map.of(ctxIntent, 1.0) : Map.of()
                ))
                .expansions(norm.expansions())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 4 模型打分
    // ═══════════════════════════════════════════════════════════

    private Map<String, Double> scoreKeywords(String text, Map<Integer, Integer> negScopes) {
        Map<String, Double> result = new HashMap<>();
        String lower = text.toLowerCase(Locale.ROOT);
        int totalTokens = Math.max(1, lower.length());
        for (Map.Entry<String, Map<String, Double>> intent : intentKeywords.entrySet()) {
            double score = 0;
            for (Map.Entry<String, Double> word : intent.getValue().entrySet()) {
                String w = word.getKey().toLowerCase(Locale.ROOT);
                int idx = lower.indexOf(w);
                if (idx >= 0) {
                    double s = word.getValue();
                    if (NegationHandler.isNegated(idx, negScopes)) {
                        s *= NegationHandler.negationPenalty(true);
                    }
                    int occurrences = countOccurrences(lower, w);
                    double tf = 1.0 + Math.log(1 + occurrences - 1) * 0.3;
                    score += s * tf;
                }
            }
            score = score / Math.sqrt(totalTokens);
            if (score > 0) result.put(intent.getKey(), score);
        }
        return result;
    }

    private Map<String, Double> scoreNgrams(String text) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> intent : intentPhrases.entrySet()) {
            List<NgramExtractor.NgramMatch> matches =
                    NgramExtractor.extract(text, intent.getValue());
            double sum = matches.stream().mapToDouble(NgramExtractor.NgramMatch::score).sum();
            if (sum > 0) result.put(intent.getKey(), sum);
        }
        return result;
    }

    private Map<String, Double> scoreExpansions(List<String> expansions) {
        Map<String, Double> result = new HashMap<>();
        if (expansions == null || expansions.isEmpty()) return result;
        for (String exp : expansions) {
            for (Map.Entry<String, Map<String, Double>> intent : intentKeywords.entrySet()) {
                if (intent.getValue().containsKey(exp)) {
                    double s = intent.getValue().get(exp);
                    result.merge(intent.getKey(), s * 0.5, Double::sum);
                }
            }
        }
        return result;
    }

    private String getContextIntent(String sessionId) {
        if (sessionId == null) return null;
        ContextEntry entry = sessionContext.get(sessionId);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > CONTEXT_TTL_MS) {
            sessionContext.remove(sessionId);
            return null;
        }
        return entry.intent;
    }

    private void cleanupExpiredContext() {
        long now = System.currentTimeMillis();
        sessionContext.entrySet().removeIf(e -> now - e.getValue().timestamp > CONTEXT_TTL_MS);
    }



    private static int countOccurrences(String text, String sub) {
        int count = 0;
        int from = 0;
        while (true) {
            int idx = text.indexOf(sub, from);
            if (idx < 0) break;
            count++;
            from = idx + sub.length();
        }
        return count;
    }

    /** sigmoid 置信度 (用配置里的 scale 因子) */
    private double confidenceSigmoid(List<Map.Entry<String, Double>> sortedScores) {
        if (sortedScores.isEmpty()) return 0.0;
        if (sortedScores.size() == 1) return Math.min(1.0, sortedScores.get(0).getValue());
        double s1 = sortedScores.get(0).getValue();
        double s2 = sortedScores.get(1).getValue();
        double diff = s1 - s2;
        return 0.5 + 0.5 * Math.tanh(diff / confidenceScale);
    }

    // ═══════════════════════════════════════════════════════════
    // 实体 / 槽位 / 紧迫度 / 情感
    // ═══════════════════════════════════════════════════════════

    private List<ExtractedEntity> extractEntities(String text) {
        List<ExtractedEntity> entities = new ArrayList<>();
        addMatches(entities, text, "time", TIME_PAT);
        addMatches(entities, text, "money", MONEY_PAT);
        addMatches(entities, text, "email", EMAIL_PAT);
        addMatches(entities, text, "phone", PHONE_PAT);
        addMatches(entities, text, "url", URL_PAT);
        addMatches(entities, text, "quantity", QUANTITY_PAT);
        addMatches(entities, text, "id_card", ID_CARD_PAT);
        return entities;
    }

    private void addMatches(List<ExtractedEntity> out, String text, String type, Pattern p) {
        Matcher m = p.matcher(text);
        while (m.find()) out.add(new ExtractedEntity(type, m.group(), m.start(), m.end()));
    }

    private Map<String, String> extractSlots(String text, String intent, List<ExtractedEntity> entities) {
        Map<String, String> slots = new LinkedHashMap<>();
        for (ExtractedEntity e : entities) slots.put(e.type(), e.value());
        switch (intent) {
            case INTENT_ORDER -> {
                if (!slots.containsKey("product")) {
                    Pattern p = Pattern.compile("(?:买|要)(?:个|件|份|台)?\\s*(.+?)(?:[,，。.\\s]|$)");
                    Matcher m = p.matcher(text);
                    if (m.find()) slots.put("product", m.group(1).trim());
                }
            }
            case INTENT_QUERY -> {
                if (!slots.containsKey("subject")) slots.put("subject", text);
            }
            case INTENT_COMPLAINT -> {
                if (!slots.containsKey("reason")) {
                    Pattern p = Pattern.compile("(?:退款|退货|投诉)\\s*(?:因为|因|，|,)?\\s*(.+?)(?:[,。.\\s]|$)");
                    Matcher m = p.matcher(text);
                    if (m.find()) slots.put("reason", m.group(1).trim());
                }
            }
        }
        return slots;
    }

    private double calcUrgency(String text, String work) {
        long baseCount = urgentWords.stream().filter(work::contains).count();
        long degreeCount = urgentDegreeWords.stream().filter(work::contains).count();
        long exclaim = text.chars().filter(c -> c == '!' || c == '！').count();
        double score = baseCount * 0.3 + degreeCount * 0.15 + exclaim * 0.1;
        return Math.min(1.0, score);
    }

    private String calcSentiment(String text, String work, Map<Integer, Integer> negScopes) {
        long pos = positiveWords.stream().filter(work::contains).count();
        long neg = negativeWords.stream().filter(work::contains).count();
        double posWeighted = pos;
        double negWeighted = neg;
        for (var e : degreeAdverbs.entrySet()) {
            if (work.contains(e.getKey())) {
                posWeighted *= e.getValue();
                negWeighted *= e.getValue();
            }
        }
        if (NegationHandler.isNegated(work.indexOf("好") >= 0 ? work.indexOf("好") : 0, negScopes)) {
            posWeighted *= 0.3;
        }
        if (NegationHandler.isNegated(work.indexOf("差") >= 0 ? work.indexOf("差") : 0, negScopes)) {
            negWeighted *= 0.3;
        }
        if (posWeighted > negWeighted) return "positive";
        if (negWeighted > posWeighted) return "negative";
        return "neutral";
    }

    // ═══════════════════════════════════════════════════════════
    // 运行时学习 + 热更新 API
    // ═══════════════════════════════════════════════════════════

    public void addKeyword(String intent, String keyword, double weight) {
        intentKeywords.computeIfAbsent(intent, k -> new HashMap<>()).put(keyword, weight);
    }

    public void addPhrase(String intent, String phrase, double weight) {
        intentPhrases.computeIfAbsent(intent, k -> new HashMap<>()).put(phrase, weight);
    }

    public void clearContext(String sessionId) {
        if (sessionId != null) sessionContext.remove(sessionId);
    }

    public Set<String> listIntents() {
        return intentKeywords.keySet();
    }

    public String getAlgorithmVersion() {
        return config != null ? config.getAlgorithm() : "v3.5.6";
    }

    /** 获取当前配置 (只读) */
    public IntentConfig getConfig() {
        return config;
    }

    /** 运行时热更新配置 (部分字段) */
    public synchronized void updateConfig(IntentConfig newConfig) {
        this.config = mergeWithDefaults(newConfig);
        reloadConfig();
    }

    /** 获取 benchmark 测试用例 */
    public List<IntentConfig.BenchmarkCase> getBenchmarkCases() {
        return benchmarkCases;
    }

    // ═══════════════════════════════════════════════════════════
    // 数据类
    // ═══════════════════════════════════════════════════════════

    @lombok.Data
    @lombok.Builder
    public static class IntentPrediction {
        private String originalText;
        private String normalizedText;
        private String intent;
        private double confidence;
        private Map<String, Double> intentScores;
        private List<ExtractedEntity> entities;
        private Map<String, String> slots;
        private double urgency;
        private String sentiment;
        private String recommendedAgent;
        private List<IntentCandidate> alternatives;
        private String algorithm;
        private Map<String, Object> modelScores;
        private List<String> expansions;
    }

    public record ExtractedEntity(String type, String value, int start, int end) {}

    public record IntentCandidate(String intent, double confidence) {}
}
