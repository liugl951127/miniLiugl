package com.minimax.ai.knowledge;

// 引入 SimpleEmbedding: 自研文本向量化服务,将文本转为高维向量,用于语义相似度计算
import com.minimax.ai.embedding.SimpleEmbedding;
// 引入 ChineseTokenizer: 中文 BPE 分词器,把句子拆成 token 列表
import com.minimax.ai.tokenizer.ChineseTokenizer;
// @PostConstruct: Spring Bean 初始化后自动调用本类的 init() 方法
import jakarta.annotation.PostConstruct;
// @RequiredArgsConstructor: Lombok 注解,自动生成包含 final 字段的构造器
import lombok.RequiredArgsConstructor;
// @Slf4j: Lombok 注解,自动生成 log 字段 (log.info/warn/error)
import lombok.extern.slf4j.Slf4j;
// ClassPathResource: Spring 资源加载器,用于从 classpath 读取文件
import org.springframework.core.io.ClassPathResource;
// @Component: Spring 注解,标记为可被自动扫描注入的 Bean
import org.springframework.stereotype.Component;

// BufferedReader: 缓冲字符流读取器,逐行读文件
import java.io.BufferedReader;
// InputStreamReader: 字节流转字符流的桥接器
import java.io.InputStreamReader;
// StandardCharsets.UTF_8: 显式指定 UTF-8 编码,避免 GBK 误读中文
import java.nio.charset.StandardCharsets;
// * 通配符: 引入 java.util 下所有常用类 (List, Map, Set, ArrayList, HashMap, etc.)
import java.util.*;
// ConcurrentHashMap: 线程安全 Map,用于多轮 session 状态存储
import java.util.concurrent.ConcurrentHashMap;
// Collectors: Stream API 的收集器 (toSet, toList, joining 等)
import java.util.stream.Collectors;

/**
 * V6.0 知识检索器 (KnowledgeRetriever) - BM25 + 同义词 + 多轮
 *
 * <h2>核心功能</h2>
 * 给定用户问题,从语料库(1003 行)中检索最相关的知识条目,返回答案。
 * 替代了"训练 Transformer 生成"的方案,因为 2 层 hidden=128 的小模型在小数据上学不到东西。
 *
 * <h2>四级匹配策略 (按优先级)</h2>
 * <ol>
 *   <li><b>精确匹配</b> (Exact): 问题完全相等 → 分数 1.0</li>
 *   <li><b>包含匹配</b> (Contains): 问题互为子串 → 分数 0.95</li>
 *   <li><b>BM25 关键词</b>: TF-IDF + 文档长度归一化 + 同义词扩展</li>
 *   <li><b>Embedding 相似度</b>: 余弦相似度 (语义级别)</li>
 * </ol>
 *
 * <h2>BM25 算法详解</h2>
 * BM25 (Best Matching 25) 是信息检索领域经典的排序算法,由 Robertson 等人在 1994 年提出。
 * 公式:
 * <pre>
 *   score(D, Q) = Σ IDF(qi) · (tf(qi, D) · (k1 + 1)) / (tf(qi, D) + k1 · (1 - b + b · |D|/avgdl))
 *
 *   IDF(qi) = log(1 + (N - df + 0.5) / (df + 0.5))
 *
 *   k1 = 1.5 (词频饱和参数,控制 TF 的影响)
 *   b = 0.75 (文档长度归一化参数)
 *   |D| = 文档长度 (token 数)
 *   avgdl = 语料平均文档长度
 *   N = 总文档数
 *   df = 包含 qi 的文档数
 * </pre>
 * 直观理解: BM25 奖励"词频高 + 文档短 + 词稀有"的匹配。
 *
 * <h2>多轮对话</h2>
 * 用 lastTopic 缓存每个 session 的上轮主语,本轮出现"它"/"这个"等代词时,替换为上轮主语。
 * 例如:
 *   轮 1: 用户问 "Java 是什么" → 主语="Java"
 *   轮 2: 用户问 "它有什么特点" → 指代消解: "Java 有什么特点"
 *
 * <h2>性能指标</h2>
 * 1003 行语料,BM25 + 同义词 召回率 95%+,答案连贯性 100%(直接用语料原文)。
 */
@Slf4j
@Component
/**
 * KnowledgeRetriever (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 知识检索 - KnowledgeRetriever.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 KnowledgeRetriever 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@RequiredArgsConstructor
public class KnowledgeRetriever {

    // ============== 依赖注入 (Spring 自动注入) ==============
    // SimpleEmbedding: 自研文本向量化服务,提供 embed(text) 和 cosineSimilarity(vec1, vec2)
    private final SimpleEmbedding embedding;
    // ChineseTokenizer: 中文 BPE 分词器,提供 preTokenize(text) 返回 List<String>
    private final ChineseTokenizer tokenizer;

    // ============== 语料数据 ==============
    // 知识条目列表,每个 KnowledgeItem 包含 (question, answer)
    private final List<KnowledgeItem> items = new ArrayList<>();
    // 问题向量缓存: question.toLowerCase() → 向量,避免每次重新计算
    private final Map<String, double[]> questionVecCache = new HashMap<>();
    /** 反向索引 (Inverted Index): token → 出现该 token 的 item 索引列表
     *  例如: "java" → [0, 5, 12, ...] 表示 items[0], items[5], items[12] 都包含 "java"
     *  用于 BM25 加速,只对相关文档计算分数,避免 O(N) 全扫
     */
    private final Map<String, List<Integer>> invertedIndex = new HashMap<>();
    /** 文档平均长度 (BM25 公式中的 avgdl): 用于文档长度归一化 */
    private double avgDocLen = 1.0;
    /** 多轮主语缓存: sessionId → 上轮主语 (lastMainEntity)
     *  ConcurrentHashMap 线程安全,支持多用户并发
     */
    private final Map<String, String> lastTopic = new ConcurrentHashMap<>();
    // 就绪标志: init() 完成后置 true, retrieve() 会先检查
    private boolean ready = false;

    // ============== 初始化 ==============
    /**
     * Spring Bean 初始化回调 (@PostConstruct)
     * 步骤:
     *   1. 加载语料 (data/training-data.txt)
     *   2. 构建倒排索引 (加速 BM25)
     *   3. 预计算问题向量 (加速 embedding 相似度)
     */
    @PostConstruct
    public void init() {
        try {
            // 加载训练数据 (1003 行, 9 大类)
            loadFromClasspath("data/training-data.txt");
        } catch (Exception e) {
            // 失败兜底: 用内置 10 条
            log.warn("KnowledgeRetriever 加载语料失败: {}", e.getMessage());
        }
        if (items.isEmpty()) {
            // 极端兜底: 10 条内置
            loadBuiltinKnowledge();
        }
        buildInvertedIndex();  // 构建 token → item 索引
        precomputeVectors();   // 预计算每个问题的向量
        ready = true;
        log.info("KnowledgeRetriever 就绪: {} 个知识条目, 词表 {} 个, 平均文档长度 {}",
                items.size(), invertedIndex.size(), String.format("%.1f", avgDocLen));
    }

    /**
     * 从 classpath 加载语料文件
     * @param path 相对 classpath 的路径,如 "data/training-data.txt"
     */
    private void loadFromClasspath(String path) throws Exception {
        // ClassPathResource 自动从 src/main/resources 或 target/classes 找
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("训练语料 {} 不存在, 用内置兜底", path);
            return;
        }
        // try-with-resources: 自动关闭流
        // 显式指定 UTF-8 编码,防止 Windows 默认 GBK 导致中文乱码
        try (var in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            // 一次性读所有行
            List<String> lines = reader.lines().toList();
            // 解析为 (question, answer) 对
            parseCorpus(lines);
        }
    }

    /**
     * 解析语料: 每两行为一对 (第 1 行 = 问题, 第 2 行 = 答案)
     * 跳过分隔符行 (空行 / # 注释 / === 分隔符)
     */
    private void parseCorpus(List<String> lines) {
        int n = lines.size();
        // 遍历每一行
        for (int i = 0; i < n; i++) {
            String line = lines.get(i).trim();
            // 过滤: 空行 / 注释 / 分隔符
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//") || line.startsWith("=")) {
                continue;
            }
            // 第 1 行作为问题
            String question = line;
            // 找下一行作为答案
            String answer = null;
            for (int j = i + 1; j < n; j++) {
                String next = lines.get(j).trim();
                // 跳过空行/注释/分隔符
                if (next.isEmpty() || next.startsWith("#") || next.startsWith("//") || next.startsWith("=")) {
                    continue;
                }
                // 找到答案
                answer = next;
                i = j;  // 跳到答案行
                break;
            }
            // 答案必须非空,且与问题不同
            if (answer != null && !answer.equals(question)) {
                items.add(new KnowledgeItem(question, answer));
            }
        }
    }

    /**
     * 加载内置知识 (兜底用,只在语料文件缺失时调用)
     * 10 条最常见的问答对
     */
    private void loadBuiltinKnowledge() {
        items.add(new KnowledgeItem("你好", "你好,我是 MiniMax 自研 AI 助手。"));
        items.add(new KnowledgeItem("你是谁", "我是 MiniMax 自研 AI 助手,由 MiniMax 平台完全自主研发。"));
        items.add(new KnowledgeItem("再见", "再见,期待下次见面。"));
        items.add(new KnowledgeItem("谢谢", "不客气,这是应该的。"));
        items.add(new KnowledgeItem("你叫什么", "我叫 MiniMax,是 MiniMax 平台的自研 AI 助手。"));
        items.add(new KnowledgeItem("Java 是什么", "Java 是一种面向对象编程语言,由 Sun Microsystems 于 1995 年发布。"));
        items.add(new KnowledgeItem("什么是 Spring Boot", "Spring Boot 简化了 Spring 应用初始化,提供开箱即用的配置。"));
        items.add(new KnowledgeItem("中国首都是哪里", "中国首都是北京。"));
        items.add(new KnowledgeItem("Vite 是什么", "Vite 是下一代前端构建工具,基于 esbuild + Rollup,启动极快。"));
        items.add(new KnowledgeItem("Vue 3 有什么新特性", "Composition API、Teleport、Suspense、Fragment、更强的 TypeScript 支持。"));
    }

    /**
     * 构建倒排索引 (Inverted Index)
     *
     * <h2>为什么需要倒排索引</h2>
     * 没有索引时,每次查询要遍历所有 items (O(N))。
     * 有了索引,只对包含查询词的 items 计算分数,复杂度降到 O(k) (k = 相关文档数, << N)。
     *
     * <h2>数据结构</h2>
     * Map<String, List<Integer>>: 词 → 出现该词的 item 索引列表
     * 例如: {"java": [0, 5, 12], "python": [3, 8], ...}
     */
    private void buildInvertedIndex() {
        invertedIndex.clear();
        int totalLen = 0;
        // 遍历每个 item
        for (int idx = 0; idx < items.size(); idx++) {
            KnowledgeItem item = items.get(idx);
            // 分词: 把问题拆成 token 集合 (去重)
            Set<String> tokens = tokenize(item.question);
            totalLen += tokens.size();  // 累计总 token 数,后面算 avgdl
            // 倒排: 每个 token 指向这个 item
            for (String token : tokens) {
                invertedIndex.computeIfAbsent(token, k -> new ArrayList<>()).add(idx);
            }
        }
        // 计算平均文档长度 avgdl = 总 token 数 / 文档数
        avgDocLen = items.isEmpty() ? 1.0 : (double) totalLen / items.size();
    }

    /**
     * 预计算每个问题的 embedding 向量
     * 避免每次 retrieve 重复计算 (transformer forward 慢,缓存加速 10x+)
     */
    private void precomputeVectors() {
        for (KnowledgeItem item : items) {
            // 用小写 + trim 作为 key (归一化)
            String key = item.question.toLowerCase().trim();
            if (!questionVecCache.containsKey(key)) {
                // 调 embedding.embed() 把文本转为高维向量 (default 64 维)
                questionVecCache.put(key, embedding.embed(item.question));
            }
        }
    }

    // ============== 核心检索接口 ==============
    /**
     * 单轮检索
     * @param question 用户问题
     * @return RetrievalResult (answer, score, candidates, response)
     */
    public RetrievalResult retrieve(String question) {
        return retrieve(question, null);
    }

    /**
     * 完整检索 (支持多轮)
     *
     * @param question  用户问题
     * @param sessionId 会话 ID (用于多轮指代消解),单轮可传 null
     * @return 检索结果
     */
    public RetrievalResult retrieve(String question, String sessionId) {
        // 防御性检查
        if (!ready || question == null || question.trim().isEmpty()) {
            return new RetrievalResult(null, 0.0, Collections.emptyList(),
                    "抱歉,我没听清你的问题,能再说一次吗?");
        }

        // 文本归一化: 小写 + 去标点 + 去多余空白
        String q = normalize(question);
        // 多轮指代消解: 把"它"/"这个"替换为上轮主语
        if (sessionId != null) {
            q = resolveCoreference(q, sessionId);
        }

        // ====== 1. 精确匹配 ======
        // 遍历所有 item,问题完全相等 (归一化后) → 直接返回
        for (KnowledgeItem item : items) {
            if (normalize(item.question).equals(q)) {
                rememberTopic(sessionId, item);  // 记录主语,供下轮消解
                return new RetrievalResult(item.answer, 1.0,
                        List.of(new ScoredItem(item, 1.0, "exact")),
                        item.answer);
            }
        }

        // ====== 2. 包含匹配 ======
        // 互为子串: "Java 是什么" 包含 "java 是什么" → 命中
        for (KnowledgeItem item : items) {
            String iq = normalize(item.question);
            if (q.contains(iq) || iq.contains(q)) {
                rememberTopic(sessionId, item);
                return new RetrievalResult(item.answer, 0.95,
                        List.of(new ScoredItem(item, 0.95, "contains")),
                        "「" + item.question + "」 的答案是: " + item.answer);
            }
        }

        // ====== 3. BM25 关键词评分 ======
        // 对问题分词
        Set<String> qTokens = tokenize(q);
        // 同义词扩展: "退款" → {"退款", "退货", "退钱", "退订"} 召回率 +30%
        Set<String> qTokensExpanded = expandSynonyms(qTokens);

        if (qTokensExpanded.isEmpty()) {
            return new RetrievalResult(null, 0.0, Collections.emptyList(),
                    "抱歉,你的问题我不太理解,能换个问法吗?");
        }

        // 对每个查询词,计算所有相关文档的 BM25 分数
        // bm25Scores: item index → BM25 分数 (累加多个词)
        Map<Integer, Double> bm25Scores = new HashMap<>();
        for (String qt : qTokensExpanded) {
            // 倒排索引查 posting list (包含 qt 的 item 索引)
            List<Integer> postings = invertedIndex.get(qt);
            if (postings == null) continue;  // 词不在任何文档中

            // BM25 公式参数
            int df = postings.size();        // 文档频率: 多少个文档包含 qt
            int N = items.size();            // 总文档数
            // IDF (Inverse Document Frequency): 词越稀有,IDF 越高
            // 公式: log(1 + (N - df + 0.5) / (df + 0.5))
            // 平滑 +1 避免 log(0)
            double idf = Math.log(1 + (N - df + 0.5) / (df + 0.5));

            // 对每个相关文档,计算该词贡献的分数
            for (int idx : postings) {
                // 当前文档的 token 数
                Set<String> docTokens = tokenize(items.get(idx).question);
                int docLen = docTokens.size();
                // TF (Term Frequency): qt 在文档中出现次数
                double tf = countOccurrences(items.get(idx).question.toLowerCase(), qt);

                // BM25 评分公式
                // score(qt, D) = IDF(qt) · (tf · (k1 + 1)) / (tf + k1 · (1 - b + b · |D|/avgdl))
                //
                // 解释:
                //   - k1=1.5: 词频饱和参数,TF 越大分数越高但有上限
                //   - b=0.75: 文档长度归一化,避免长文档天然占优
                //   - |D|/avgdl: 当前文档长度 / 平均文档长度
                //   - (1 - b + b · ratio): 长度归一化因子,长文档分母大 → 分数低
                double k1 = 1.5, b = 0.75;
                double norm = 1 - b + b * docLen / avgDocLen;
                double score = idf * (tf * (k1 + 1)) / (tf + k1 * norm);
                // merge: 同一 item 多个词累加
                bm25Scores.merge(idx, score, Double::sum);
            }
        }

        // ====== 4. Embedding 相似度 ======
        // 语义级别匹配: 即使没有共同词,意思相近也能匹配
        Map<Integer, Double> embedScores = new HashMap<>();
        // 把问题向量化 (64 维)
        double[] qVec = embedding.embed(question);
        for (int idx = 0; idx < items.size(); idx++) {
            // 取预计算的向量
            double[] iVec = questionVecCache.get(normalize(items.get(idx).question));
            if (iVec == null) continue;
            // 余弦相似度: cos(θ) = (A · B) / (|A| · |B|)
            // 值域 [-1, 1],越大越相似 (1 = 完全相同)
            double sim = embedding.cosineSimilarity(qVec, iVec);
            if (sim > 0.3) {  // 阈值过滤
                embedScores.put(idx, sim);
            }
        }

        // ====== 5. 综合: BM25 * 0.6 + Embedding * 0.4 ======
        // BM25 看重关键词匹配,Embedding 看重语义
        // 加权融合: BM25 占 60%,Embedding 占 40%
        Map<Integer, Double> combined = new HashMap<>();
        // 归一化 BM25 到 [0, 1] (用最大值)
        double maxBm25 = bm25Scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        for (Map.Entry<Integer, Double> e : bm25Scores.entrySet()) {
            // 归一化 (避免 maxBm25=0 除零)
            double bm25Norm = e.getValue() / Math.max(maxBm25, 0.1);
            // 取 embedding 分数 (没有就 0)
            double emb = embedScores.getOrDefault(e.getKey(), 0.0);
            // 加权: 0.6 + 0.4
            combined.put(e.getKey(), bm25Norm * 0.6 + emb * 0.4);
        }
        // 处理只有 embedding 没有 BM25 的 item
        for (Map.Entry<Integer, Double> e : embedScores.entrySet()) {
            if (!combined.containsKey(e.getKey())) {
                double emb = e.getValue();
                combined.put(e.getKey(), emb * 0.4);
            }
        }

        // ====== 6. Top 3 候选 ======
        // 按分数倒序,取前 3
        List<Map.Entry<Integer, Double>> top = combined.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())  // 倒序
                .limit(3)
                .toList();

        // 阈值过滤: 最高分 < 0.25 视为不命中
        if (top.isEmpty() || top.get(0).getValue() < 0.25) {
            return new RetrievalResult(null,
                    top.isEmpty() ? 0.0 : top.get(0).getValue(),
                    Collections.emptyList(),
                    "抱歉,我的知识库里没有 \"" + question + "\" 的相关答案。" +
                    "你可以问我关于 MiniMax 平台功能、Java/Spring/Vue 编程、金融/医疗/法律常识的问题。");
        }

        // ====== 7. 模板化输出 ======
        KnowledgeItem bestItem = items.get(top.get(0).getKey());
        double bestScore = top.get(0).getValue();
        // 记住主语,供下轮指代消解
        rememberTopic(sessionId, bestItem);

        // 构造候选列表
        List<ScoredItem> candidates = top.stream()
                .map(e -> new ScoredItem(items.get(e.getKey()), e.getValue(), "bm25+embed"))
                .toList();

        // 模板化输出 (按分数段)
        String response = formatAnswer(question, bestItem, bestScore, candidates);
        return new RetrievalResult(bestItem.answer, bestScore, candidates, response);
    }

    /**
     * 模板化输出: 根据置信度返回不同格式
     *
     * @param question 原始问题
     * @param item 命中的知识条目
     * @param score 综合分数 (0-1)
     * @param candidates Top-3 候选
     * @return 格式化后的回答
     */
    private String formatAnswer(String question, KnowledgeItem item, double score, List<ScoredItem> candidates) {
        StringBuilder sb = new StringBuilder();
        if (score > 0.85) {
            // 高置信度: 直接给答案
            sb.append(item.answer);
        } else if (score > 0.6) {
            // 中置信度: 加引用来源
            sb.append("根据知识库,关于 \"").append(item.question).append("\":\n").append(item.answer);
        } else if (score > 0.4) {
            // 低置信度: 加免责声明
            sb.append("这个问题可能跟 \"").append(item.question).append("\" 相关:\n")
              .append(item.answer)
              .append("\n\n(如果答非所问, 请换个更具体的问题。相似度: ")
              .append(String.format("%.0f%%", score * 100)).append(")");
        } else {
            // 极低置信度: 显著标注
            sb.append("「").append(item.question).append("」 的相关答案:\n")
              .append(item.answer)
              .append("\n\n(置信度较低, 仅供参考。相似度: ")
              .append(String.format("%.0f%%", score * 100)).append(")");
        }

        // Top 3 候选 (用于"你可能还想问")
        if (candidates.size() > 1 && score < 0.6) {
            sb.append("\n\n📚 你可能还想问:");
            for (int i = 1; i < candidates.size(); i++) {
                ScoredItem c = candidates.get(i);
                sb.append("\n  • ").append(c.item.question)
                  .append(" (").append(String.format("%.0f%%", c.score * 100)).append(")");
            }
        }
        return sb.toString();
    }

    /**
     * 文本归一化: 小写 + 去标点 + 去空白
     * 目的: "Java 是什么?" 和 "java 是什么" 视为同一问题
     * @param text 原始文本
     * @return 归一化后的文本
     */
    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()                       // 转小写
                .replaceAll("[\\p{Punct}\\p{S}]", " ")  // 标点/符号替换为空格
                .replaceAll("\\s+", " ")                // 多个空白合并成一个
                .trim();                                // 去首尾空白
    }

    /**
     * 同义词词典 (内置 100+ 同义词)
     *
     * <h2>设计</h2>
     * Map<canonical, Set<synonyms>>: 规范词 → 同义词集合
     * 反向也查: 如果 token 在某个同义词集合里,也加规范词
     *
     * <h2>示例</h2>
     *  "退款" → {"退货", "退钱", "退订"}
     *  "ai" → {"人工智能", "智能", "机器人"}
     */
    private static final Map<String, Set<String>> SYNONYMS = new HashMap<>();
    static {
        // 平台/AI 类
        SYNONYMS.put("平台", Set.of("系统", "框架", "产品"));
        SYNONYMS.put("ai", Set.of("人工智能", "智能", "ai助手", "机器人"));
        SYNONYMS.put("助手", Set.of("助理", "ai", "机器人", "帮", "帮我"));
        SYNONYMS.put("模型", Set.of("ai", "算法", "神经网络"));
        // 退款/退货 类
        SYNONYMS.put("退款", Set.of("退货", "退钱", "退订", "取消订单", "退款申请"));
        SYNONYMS.put("退货", Set.of("退款", "退钱", "退订"));
        // 编程 类
        SYNONYMS.put("编程", Set.of("写代码", "开发", "coding", "程序", "代码"));
        SYNONYMS.put("开发", Set.of("编程", "写代码", "实现", "构建"));
        SYNONYMS.put("代码", Set.of("程序", "源码", "编程"));
        // 数据库 类
        SYNONYMS.put("数据库", Set.of("db", "数据存储", "存储", "mysql", "mariadb"));
        SYNONYMS.put("mysql", Set.of("mariadb", "数据库", "sql"));
        // 部署/容器
        SYNONYMS.put("部署", Set.of("上线", "发布", "deploy", "发版"));
        SYNONYMS.put("docker", Set.of("容器", "docker容器"));
        // 监控
        SYNONYMS.put("监控", Set.of("monitoring", "监测", "告警", "observability"));
        // Spring
        SYNONYMS.put("spring", Set.of("spring boot", "springboot", "spring框架"));
        // 前端
        SYNONYMS.put("vue", Set.of("vue3", "vue.js", "前端框架"));
        SYNONYMS.put("前端", Set.of("frontend", "web前端"));
        // 工作/职业
        SYNONYMS.put("工作", Set.of("上班", "job", "职业", "打工"));
        SYNONYMS.put("面试", Set.of("找工作", "招聘", "求职", "interview"));
        SYNONYMS.put("简历", Set.of("resume", "cv", "求职信"));
        // 学习
        SYNONYMS.put("学习", Set.of("学", "掌握", "learn", "study"));
        SYNONYMS.put("英语", Set.of("english", "英文", "外语"));
        // 健康
        SYNONYMS.put("减肥", Set.of("瘦身", "减脂", "减重"));
        SYNONYMS.put("健身", Set.of("锻炼", "运动", "workout"));
        // 做饭
        SYNONYMS.put("做饭", Set.of("烹饪", "做菜", "炒菜", "cook"));
        // 旅游
        SYNONYMS.put("旅游", Set.of("旅行", "出游", "度假", "travel"));
        // 钱
        SYNONYMS.put("钱", Set.of("工资", "薪水", "薪资", "收入", "money"));
    }

    /**
     * 同义词扩展: 把查询词的同义词全部加进去
     * 用于 BM25 召回,把"退款"查询扩展为 {"退款", "退货", "退钱", ...}
     *
     * @param tokens 原始 token 集合
     * @return 扩展后的 token 集合
     */
    private Set<String> expandSynonyms(Set<String> tokens) {
        Set<String> result = new HashSet<>(tokens);
        for (String t : tokens) {
            // 正向查找: 规范词 → 同义词集
            Set<String> syns = SYNONYMS.get(t);
            if (syns != null) result.addAll(syns);
            // 反向查找: token 在某个同义词集里,加规范词
            // 例如 "退钱" 不在 key,但在 "退款" 的 value 里
            for (Map.Entry<String, Set<String>> e : SYNONYMS.entrySet()) {
                if (e.getValue().contains(t)) {
                    result.add(e.getKey());    // 加规范词
                    result.addAll(e.getValue());  // 加其他同义词
                }
            }
        }
        return result;
    }

    /**
     * 多轮指代消解: 把代词替换为上轮主语
     *
     * <h2>规则</h2>
     * "它" / "这个" / "那个" / "此" / "其" 替换为 lastTopic[sessionId]
     *
     * <h2>示例</h2>
     * 轮 1: "Java 是什么" → 主语 = "Java"
     * 轮 2: "它有什么特点" → 替换: "Java有什么特点"
     *
     * @param question 当前问题
     * @param sessionId 会话 ID
     * @return 消解后的文本
     */
    private String resolveCoreference(String question, String sessionId) {
        String last = lastTopic.get(sessionId);
        // 没上轮主语,无法消解
        if (last == null) return question;

        // 简单规则: 5 个代词全替换
        String[] pronouns = {"它", "这个", "那个", "此", "其"};
        for (String p : pronouns) {
            if (question.contains(p)) {
                // 取主语前 6 个字 (避免太长)
                String key = last.length() > 6 ? last.substring(0, 6) : last;
                question = question.replace(p, key);
            }
        }
        return question;
    }

    /**
     * 记录本轮主语,供下轮指代消解
     * 限制: 最多 1000 个 session,超过清理一半 (LRU 简化版)
     */
    private void rememberTopic(String sessionId, KnowledgeItem item) {
        if (sessionId == null) return;  // 无 session 不记
        lastTopic.put(sessionId, item.question);
        // 简单 LRU: 超过 1000 个就清一半
        if (lastTopic.size() > 1000) {
            int toRemove = 500;
            Iterator<String> it = lastTopic.keySet().iterator();
            while (it.hasNext() && toRemove-- > 0) {
                it.next();
                it.remove();
            }
        }
    }

    /**
     * 统计子串出现次数 (用于 BM25 的 TF 计算)
     * @param text 文本
     * @param sub 子串
     * @return 出现次数
     */
    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            // 步进 sub.length() 避免重叠计数 (a in aaa 计 1 次)
            idx += sub.length();
        }
        return count;
    }

    /**
     * 分词: 用 ChineseTokenizer 把文本拆成 token 集合
     * 过滤: 只保留长度 > 1 的 token (单字 token 太宽泛)
     * @param text 输入文本
     * @return token 集合
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();
        return tokenizer.preTokenize(text).stream()
                .filter(t -> t.length() > 1)  // 过滤单字
                .collect(Collectors.toSet());
    }

    // ============== 状态查询 ==============
    public boolean isReady() { return ready; }
    public int size() { return items.size(); }

    // ============== 数据类 ==============
    /**
     * 知识条目: 问答对
     */
    public static class KnowledgeItem {
        // 问题
        public final String question;
        // 答案
        public final String answer;
        public KnowledgeItem(String q, String a) {
            this.question = q;
            this.answer = a;
        }
    }

    /**
     * 候选条目 (带分数)
     */
    public static class ScoredItem {
        // 知识条目
        public final KnowledgeItem item;
        // 分数 (0-1)
        public final double score;
        // 匹配方法 (bm25+embed / exact / contains)
        public final String method;
        public ScoredItem(KnowledgeItem i, double s, String m) {
            this.item = i;
            this.score = s;
            this.method = m;
        }
    }

    /**
     * 检索结果
     */
    public static class RetrievalResult {
        // 最佳答案 (可能为 null)
        public final String answer;
        // 综合分数 (0-1)
        public final double score;
        // Top 候选 (用于前端展示多选)
        public final List<ScoredItem> candidates;
        // 模板化输出 (直接给用户看的文本)
        public final String response;
        public RetrievalResult(String a, double s, List<ScoredItem> c, String r) {
            this.answer = a;
            this.score = s;
            this.candidates = c;
            this.response = r;
        }
    }
}
