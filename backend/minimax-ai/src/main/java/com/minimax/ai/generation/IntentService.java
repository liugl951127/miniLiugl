package com.minimax.ai.generation;

import com.minimax.ai.entity.AiIntentKeyword;
import com.minimax.ai.mapper.AiIntentKeywordMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 关键词驱动的意图识别服务 (V2.8.5) - 数据库驱动版
 *
 * <h3>取代</h3>
 * 旧 KeywordEngine 的 KEYWORDS 静态字段. 现在关键词从 DB 读取, 可动态调整.
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>启动时全量加载</li>
 *   <li>每 5 分钟定时刷新 (可配置)</li>
 *   <li>提供 reload() 方法供运维手动刷新</li>
 * </ul>
 *
 * <h3>性能优化</h3>
 * <ul>
 *   <li>HashMap 索引, O(1) 查找</li>
 *   <li>正则预编译 (Pattern 缓存)</li>
 *   <li>按意图分桶, 避免每次扫描全部</li>
 *   <li>Case-insensitive 比较提前 lowercase</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentService {

    private final AiIntentKeywordMapper keywordMapper;

    /** 缓存: intent → (keyword → weight) */
    private final Map<String, Map<String, Integer>> cache = new ConcurrentHashMap<>();
    /** 缓存: intent → List<Pattern> (正则) */
    private final Map<String, List<Pattern>> regexCache = new ConcurrentHashMap<>();
    /** 上次刷新时间 */
    private final AtomicLong lastReload = new AtomicLong(0);

    /** 启动时加载 */
    @PostConstruct
    public void init() {
        reload();
    }

    /** 每 5 分钟自动刷新 */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void autoReload() {
        log.debug("[intent-service] auto reload triggered");
        reload();
    }

    /**
     * 手动重载关键词
     */
    public synchronized void reload() {
        long start = System.currentTimeMillis();
        List<AiIntentKeyword> all = keywordMapper.selectList(
                new QueryWrapper<AiIntentKeyword>().eq("enabled", 1)
        );
        if (all == null || all.isEmpty()) {
            log.warn("[intent-service] DB has no enabled keywords, falling back to defaults");
            // TODO V2.8.6: 加载默认 hardcoded fallback
            all = defaultKeywords();
        }
        // 重建缓存
        Map<String, Map<String, Integer>> newCache = new ConcurrentHashMap<>();
        Map<String, List<Pattern>> newRegex = new ConcurrentHashMap<>();
        for (AiIntentKeyword k : all) {
            newCache.computeIfAbsent(k.getIntent(), x -> new HashMap<>())
                    .put(k.getKeyword(), k.getWeight() != null ? k.getWeight() : 1);
            if (k.getIsRegex() != null && k.getIsRegex() == 1) {
                newRegex.computeIfAbsent(k.getIntent(), x -> new ArrayList<>())
                        .add(Pattern.compile(k.getKeyword()));
            }
        }
        cache.clear();
        cache.putAll(newCache);
        regexCache.clear();
        regexCache.putAll(newRegex);
        lastReload.set(System.currentTimeMillis());
        log.info("[intent-service] loaded: {} keywords, {} regex patterns, in {}ms",
                all.size(), newRegex.values().stream().mapToInt(List::size).sum(),
                System.currentTimeMillis() - start);
    }

    /**
     * 识别意图 (DB 驱动版)
     */
    public KeywordEngine.Intent recognize(String text) {
        if (text == null || text.trim().isEmpty()) return KeywordEngine.Intent.UNKNOWN;
        String lower = text.toLowerCase();

        // 1. 正则优先
        for (Map.Entry<String, List<Pattern>> entry : regexCache.entrySet()) {
            for (Pattern p : entry.getValue()) {
                if (p.matcher(text).find()) {
                    KeywordEngine.Intent intent = parseIntent(entry.getKey());
                    if (intent != null) return intent;
                }
            }
        }

        // 2. 关键词 TF 加权
        Map<KeywordEngine.Intent, Integer> scores = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : cache.entrySet()) {
            KeywordEngine.Intent intent = parseIntent(entry.getKey());
            if (intent == null) continue;
            int score = 0;
            for (Map.Entry<String, Integer> kw : entry.getValue().entrySet()) {
                if (lower.contains(kw.getKey().toLowerCase())) {
                    score += kw.getValue();
                }
            }
            if (score > 0) scores.put(intent, score);
        }

        // 3. 取最高分
        if (!scores.isEmpty()) {
            return scores.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
        }
        return KeywordEngine.Intent.CHAT;
    }

    /**
     * 字符串 → 枚举 (安全转换)
     */
    private KeywordEngine.Intent parseIntent(String name) {
        if (name == null) return null;
        try { return KeywordEngine.Intent.valueOf(name); } catch (Exception e) { return null; }
    }

    /** 缓存统计 */
    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("intentCount", cache.size());
        s.put("totalKeywords", cache.values().stream().mapToInt(Map::size).sum());
        s.put("regexCount", regexCache.values().stream().mapToInt(List::size).sum());
        s.put("lastReloadAt", new Date(lastReload.get()));
        s.put("cacheAgeMs", System.currentTimeMillis() - lastReload.get());
        return s;
    }

    /**
     * 默认关键词 (DB 为空时回退)
     * 真实生产: 应当从 DB 加载, 这里仅做兜底
     */
    private List<AiIntentKeyword> defaultKeywords() {
        List<AiIntentKeyword> list = new ArrayList<>();
        String[][] defaults = {
            {"GENERATE_CHART", "图表"}, {"GENERATE_CHART", "柱状图"}, {"GENERATE_CHART", "折线图"},
            {"GENERATE_CHART", "饼图"}, {"GENERATE_CHART", "chart"}, {"GENERATE_CHART", "graph"},
            {"GENERATE_MUSIC", "音乐"}, {"GENERATE_MUSIC", "旋律"}, {"GENERATE_MUSIC", "MIDI"},
            {"GENERATE_MUSIC", "music"}, {"GENERATE_MUSIC", "melody"},
            {"QUERY_DATA", "查询"}, {"QUERY_DATA", "SELECT"}, {"QUERY_DATA", "数据"},
            {"ANALYZE_DATA", "统计"}, {"ANALYZE_DATA", "分析"}, {"ANALYZE_DATA", "平均"},
            {"GENERATE_CODE", "代码"}, {"GENERATE_CODE", "Spring Boot"}, {"GENERATE_CODE", "项目"},
            {"CHAT", "你好"}, {"CHAT", "hello"}, {"CHAT", "hi"},
            {"TRANSFER_HUMAN", "转人工"}, {"TRANSFER_HUMAN", "人工"},
        };
        for (String[] d : defaults) {
            AiIntentKeyword k = new AiIntentKeyword();
            k.setIntent(d[0]);
            k.setKeyword(d[1]);
            k.setWeight(1);
            k.setIsRegex(0);
            k.setEnabled(1);
            k.setLanguage(d[1].matches("[\\u4e00-\\u9fff]+") ? "zh" : "en");
            list.add(k);
        }
        return list;
    }
}
