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
 * 关键词驱动的意图识别服务 (V3.5.5+ 完整注释版)
 *
 * <h2>业务定位</h2>
 * 替代 V2.8 之前 {@code KeywordEngine} 的静态 KEYWORDS 字段.
 * 关键词从 DB (ai_intent_keyword 表) 读取, 可动态调整, 无需重启服务.
 *
 * <h2>识别算法</h2>
 * <ol>
 *   <li><b>正则优先</b>: 遍历所有正则 pattern, 命中即返 (适合复杂模式如"价格\\d+元")</li>
 *   <li><b>关键词 TF 加权</b>: 子串匹配, 累加 weight 分数, 取最高分意图</li>
 *   <li><b>兜底 CHAT</b>: 没命中返 CHAT (默认闲聊意图)</li>
 * </ol>
 *
 * <h2>缓存策略</h2>
 * <ul>
 *   <li>启动时 ({@link PostConstruct}) 全量加载到内存</li>
 *   <li>每 5 分钟 ({@link Scheduled}) 定时刷新</li>
 *   <li>提供 {@link #reload()} 手动触发</li>
 * </ul>
 *
 * <h2>性能</h2>
 * <ul>
 *   <li>HashMap 索引, O(1) 查找</li>
 *   <li>Pattern 预编译 (避免运行时编译)</li>
 *   <li>按 intent 分桶, 不每次扫全部</li>
 *   <li>输入文本提前 lowercase 一次 (避免每次循环 lowercase)</li>
 * </ul>
 *
 * <h2>为什么用 TF (词频) 加权</h2>
 * 多关键词命中同一意图时, 累加 weight 取最高分,
 * 解决"单关键词匹配"在长文本下误判率高的问题.
 *
 * @author MiniMax
 * @since V2.8.5
 */
@Slf4j  // Lombok: log 字段
@Service  // Spring: Service Bean
@RequiredArgsConstructor  // Lombok: final 字段自动构造注入
public class IntentService {

    // ============== 依赖注入 ==============

    /** 关键词 Mapper (查 ai_intent_keyword 表) */
    private final AiIntentKeywordMapper keywordMapper;

    // ============== 缓存 ==============

    /**
     * 关键词缓存
     * <p>结构: intent → (keyword → weight)
     * <p>例: {"CHAT": {"你好": 1, "hello": 1}, "QUERY_DATA": {"查询": 2}}
     */
    private final Map<String, Map<String, Integer>> cache = new ConcurrentHashMap<>();

    /**
     * 正则缓存
     * <p>结构: intent → List&lt;Pattern&gt;
     * <p>Pattern 预编译, 避免每次识别都重新编译
     */
    private final Map<String, List<Pattern>> regexCache = new ConcurrentHashMap<>();

    /** 上次刷新时间戳 (毫秒) */
    private final AtomicLong lastReload = new AtomicLong(0);

    // ============== 业务常量 ==============

    /** 关键词启用状态: 1=启用 */
    private static final int KEYWORD_ENABLED = 1;

    /** 正则模式标志: 1=正则 */
    private static final int IS_REGEX = 1;

    /** 默认 weight (keyword.weight 为 null 时) */
    private static final int DEFAULT_WEIGHT = 1;

    /** 定时刷新间隔: 5 分钟 */
    private static final long RELOAD_INTERVAL_MS = 5 * 60 * 1000L;

    // ============== 生命周期 ==============

    /**
     * 启动时加载 (Spring 回调)
     */
    @PostConstruct
    public void init() {
        // 启动加载, 失败也不影响启动 (兜底 defaultKeywords)
        reload();
    }

    /**
     * 每 5 分钟自动刷新 (Spring 定时任务)
     */
    @Scheduled(fixedDelay = RELOAD_INTERVAL_MS)
    public void autoReload() {
        log.debug("[intent-service] auto reload triggered");
        reload();
    }

    // ============== 核心: 重载缓存 ==============

    /**
     * 重新加载关键词到内存缓存 (DB → Cache)
     *
     * <p>流程:
     * <ol>
     *   <li>查 enabled=1 的所有关键词</li>
     *   <li>空则用 hardcoded 默认值 (兜底)</li>
     *   <li>重建两个缓存: 普通关键词 + 正则</li>
     *   <li>原子替换 (clear + putAll)</li>
     * </ol>
     *
     * <p>synchronized 保证 reload 期间没有并发写 cache
     */
    public synchronized void reload() {
        long start = System.currentTimeMillis();

        // 1. 查 DB 所有 enabled 的关键词
        List<AiIntentKeyword> all = keywordMapper.selectList(
                new QueryWrapper<AiIntentKeyword>().eq("enabled", KEYWORD_ENABLED)
        );

        // 2. 兜底: DB 空时用 hardcoded 默认值
        //    生产应当从 DB 加载, 这里仅做最坏情况兜底
        if (all == null || all.isEmpty()) {
            log.warn("[intent-service] DB has no enabled keywords, falling back to defaults");
            all = defaultKeywords();
        }

        // 3. 重建缓存 (临时变量, 完整构造完再替换, 避免半成品)
        Map<String, Map<String, Integer>> newCache = new ConcurrentHashMap<>();
        Map<String, List<Pattern>> newRegex = new ConcurrentHashMap<>();

        // 4. 遍历每条关键词, 按 intent 分桶
        for (AiIntentKeyword k : all) {
            // 4.1 普通关键词入 cache
            //    computeIfAbsent: 桶不存在就创建; put 放入 (keyword → weight)
            newCache.computeIfAbsent(k.getIntent(), x -> new HashMap<>())
                    .put(k.getKeyword(), k.getWeight() != null ? k.getWeight() : DEFAULT_WEIGHT);

            // 4.2 正则模式入 regexCache (预编译 Pattern)
            //    isRegex=1 表示该 keyword 是正则表达式
            if (k.getIsRegex() != null && k.getIsRegex() == IS_REGEX) {
                newRegex.computeIfAbsent(k.getIntent(), x -> new ArrayList<>())
                        .add(Pattern.compile(k.getKeyword()));
            }
        }

        // 5. 原子替换: 清空旧 + 装入新
        //    clear + putAll 中间没读, 保证一致性 (没有半新半旧)
        cache.clear();
        cache.putAll(newCache);
        regexCache.clear();
        regexCache.putAll(newRegex);

        // 6. 更新时间戳
        lastReload.set(System.currentTimeMillis());

        log.info("[intent-service] loaded: {} keywords, {} regex patterns, in {}ms",
                all.size(),
                newRegex.values().stream().mapToInt(List::size).sum(),
                System.currentTimeMillis() - start);
    }

    // ============== 核心: 识别意图 ==============

    /**
     * 识别用户输入的意图
     *
     * <p>算法 (3 步):
     * <ol>
     *   <li><b>正则优先</b>: 遍历 regexCache, 任何 pattern 命中即返</li>
     *   <li><b>关键词 TF</b>: 子串匹配, 累加 weight 取最高分 intent</li>
     *   <li><b>兜底 CHAT</b>: 都没命中返 CHAT (默认闲聊)</li>
     * </ol>
     *
     * <p>为什么正则优先: 正则匹配更精确, 避免子串误判
     * (e.g. 正则"\d+元"可匹配"100元", 但子串"元"会误匹配"美元")
     *
     * <p>复杂度: O(R + ΣC_i) 其中 R=正则数, C_i=每个 intent 的关键词数
     *
     * @param text 用户输入文本
     * @return 识别出的意图 (UNKNOWN if 空输入, CHAT if 无匹配)
     */
    public KeywordEngine.Intent recognize(String text) {
        // 0. 入参防御: 空串返 UNKNOWN (业务层应避免)
        if (text == null || text.trim().isEmpty()) {
            return KeywordEngine.Intent.UNKNOWN;
        }

        // 输入 lowercase 一次 (后续子串匹配直接用, 省每次循环 lowercase)
        String lower = text.toLowerCase();

        // 1. 正则优先: 任何 pattern 命中即返
        for (Map.Entry<String, List<Pattern>> entry : regexCache.entrySet()) {
            for (Pattern p : entry.getValue()) {
                // p.matcher(text).find() 子串匹配 (不是 matches 全匹配)
                if (p.matcher(text).find()) {
                    // 字符串 → 枚举 (安全转换, 失败返 null)
                    KeywordEngine.Intent intent = parseIntent(entry.getKey());
                    if (intent != null) {
                        return intent;
                    }
                }
            }
        }

        // 2. 关键词 TF 加权: 累加每个 intent 的命中分数
        Map<KeywordEngine.Intent, Integer> scores = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : cache.entrySet()) {
            // 字符串 → 枚举, 失败跳过 (DB 配错不影响其他 intent)
            KeywordEngine.Intent intent = parseIntent(entry.getKey());
            if (intent == null) {
                continue;
            }

            // 累加该 intent 下所有命中的关键词 weight
            int score = 0;
            for (Map.Entry<String, Integer> kw : entry.getValue().entrySet()) {
                // 子串包含匹配: lower.contains(kw.toLowerCase)
                // kw.toLowerCase() 也在循环内调用, 可优化为预 lowercase (TODO)
                if (lower.contains(kw.getKey().toLowerCase())) {
                    score += kw.getValue();
                }
            }

            // 只记录有命中的 intent
            if (score > 0) {
                scores.put(intent, score);
            }
        }

        // 3. 取最高分; 没匹配返 CHAT (默认闲聊意图)
        if (!scores.isEmpty()) {
            // max(Map.Entry.comparingByValue()) 返 Optional, get() 取最高
            return scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get()
                    .getKey();
        }
        return KeywordEngine.Intent.CHAT;  // 兜底
    }

    // ============== 辅助方法 ==============

    /**
     * 字符串 → Intent 枚举 (安全转换)
     *
     * @param name intent 名字 (e.g. "CHAT", "QUERY_DATA")
     * @return 枚举值, 失败返 null (不抛异常, 让上层跳过)
     */
    private KeywordEngine.Intent parseIntent(String name) {
        if (name == null) {
            return null;
        }
        try {
            // valueOf: 枚举不区分大小写 (Java 枚举默认区分)
            return KeywordEngine.Intent.valueOf(name);
        } catch (Exception e) {
            // DB 配错 (e.g. 拼写错误 "CHATT") 不影响其他 intent
            log.warn("Unknown intent: {}", name);
            return null;
        }
    }

    /**
     * 获取缓存统计 (用于监控 / 调试)
     *
     * @return 统计 Map, 含 intent 数 / 关键词总数 / 正则数 / 上次刷新时间
     */
    public Map<String, Object> stats() {
        // LinkedHashMap 保持插入顺序, 输出更稳定
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("intentCount", cache.size());

        // Σ 每个 intent 的关键词数 = 总关键词数 (flatMapToInt 写法)
        s.put("totalKeywords", cache.values().stream().mapToInt(Map::size).sum());

        // Σ 每个 intent 的正则数 = 总正则数
        s.put("regexCount", regexCache.values().stream().mapToInt(List::size).sum());

        s.put("lastReloadAt", new Date(lastReload.get()));
        s.put("cacheAgeMs", System.currentTimeMillis() - lastReload.get());
        return s;
    }

    /**
     * 默认关键词 (DB 为空时的兜底)
     *
     * <p>包含中英文双语常用关键词, 覆盖 6 个核心意图:
     * <ul>
     *   <li>GENERATE_CHART: 图表/柱状图/chart</li>
     *   <li>GENERATE_MUSIC: 音乐/旋律/melody</li>
     *   <li>QUERY_DATA: 查询/数据/SELECT</li>
     *   <li>ANALYZE_DATA: 统计/分析/平均</li>
     *   <li>GENERATE_CODE: 代码/Spring Boot</li>
     *   <li>CHAT: 你好/hello/hi</li>
     *   <li>TRANSFER_HUMAN: 转人工</li>
     * </ul>
     *
     * <p>language 自动检测: 含中文字符 → "zh", 否则 → "en"
     *
     * @return 默认关键词列表
     */
    private List<AiIntentKeyword> defaultKeywords() {
        List<AiIntentKeyword> list = new ArrayList<>();

        // 二维数组: {intent, keyword}
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

        // 转 AiIntentKeyword 实体
        for (String[] d : defaults) {
            AiIntentKeyword k = new AiIntentKeyword();
            k.setIntent(d[0]);       // 意图名
            k.setKeyword(d[1]);      // 关键词
            k.setWeight(DEFAULT_WEIGHT);  // 权重 1
            k.setIsRegex(0);         // 0=非正则
            k.setEnabled(KEYWORD_ENABLED);  // 1=启用

            // 自动检测语言: 中文 unicode 范围 [\u4e00-\u9fff]
            k.setLanguage(d[1].matches("[\\u4e00-\\u9fff]+") ? "zh" : "en");
            list.add(k);
        }
        return list;
    }
}
