package com.minimax.ai.knowledge;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V5.4+ 知识检索器 (KnowledgeRetriever)
 *
 * 设计思路: 不再 "生成" (Transformer 2 层学不到东西), 改 "检索" -
 *   1. 启动时把训练语料按问答对切分 + 向量化
 *   2. 用户提问时, 4 级匹配: 精确/包含/关键词/embedding
 *   3. 命中: 模板化输出 ("关于 XX, 答案是: ...")
 *   4. 未命中: 礼貌拒答 + 引导
 *
 * 准确率: 语料中存在的问答 100% 命中
 * 连贯性: 答案都是语料原文, 100% 连贯
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private final SimpleEmbedding embedding;
    private final ChineseTokenizer tokenizer;

    private final List<KnowledgeItem> items = new ArrayList<>();
    private final Map<String, double[]> questionVecCache = new HashMap<>();
    private boolean ready = false;

    @PostConstruct
    public void init() {
        try {
            loadFromClasspath("data/training-data.txt");
        } catch (Exception e) {
            log.warn("KnowledgeRetriever 加载语料失败: {}", e.getMessage());
        }
        if (items.isEmpty()) {
            loadBuiltinKnowledge();
        }
        precomputeVectors();
        ready = true;
        log.info("KnowledgeRetriever 就绪: {} 个知识条目, 维度 {}", items.size(), embedding.getDimension());
    }

    private void loadFromClasspath(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("训练语料 {} 不存在, 用内置兜底", path);
            return;
        }
        try (var in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().toList();
            parseCorpus(lines);
        }
    }

    private void parseCorpus(List<String> lines) {
        int n = lines.size();
        for (int i = 0; i < n; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//") || line.startsWith("=")) {
                continue;
            }
            String question = line;
            String answer = null;
            for (int j = i + 1; j < n; j++) {
                String next = lines.get(j).trim();
                if (next.isEmpty() || next.startsWith("#") || next.startsWith("//") || next.startsWith("=")) {
                    continue;
                }
                answer = next;
                i = j;
                break;
            }
            if (answer != null && !answer.equals(question)) {
                items.add(new KnowledgeItem(question, answer));
            }
        }
    }

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

    private void precomputeVectors() {
        for (KnowledgeItem item : items) {
            String key = item.question.toLowerCase().trim();
            if (!questionVecCache.containsKey(key)) {
                questionVecCache.put(key, embedding.embed(item.question));
            }
        }
    }

    public RetrievalResult retrieve(String question) {
        if (!ready || question == null || question.trim().isEmpty()) {
            return new RetrievalResult(null, 0.0, "抱歉,我没听清你的问题,能再说一次吗?");
        }

        String q = question.trim().toLowerCase();

        // 1. 精确匹配
        for (KnowledgeItem item : items) {
            if (item.question.toLowerCase().trim().equals(q)) {
                return new RetrievalResult(item.answer, 1.0, item.answer);
            }
        }

        // 2. 包含匹配
        for (KnowledgeItem item : items) {
            String iq = item.question.toLowerCase().trim();
            if (q.contains(iq) || iq.contains(q)) {
                return new RetrievalResult(item.answer, 0.95,
                        "「" + item.question + "」 的答案是: " + item.answer);
            }
        }

        // 3. 关键词重叠
        Set<String> qWords = tokenize(q);
        if (qWords.isEmpty()) {
            return new RetrievalResult(null, 0.0, "抱歉,你的问题我不太理解,能换个问法吗?");
        }
        KnowledgeItem bestKeyword = null;
        double bestKeywordScore = 0;
        for (KnowledgeItem item : items) {
            Set<String> iqWords = tokenize(item.question.toLowerCase().trim());
            if (iqWords.isEmpty()) continue;
            int overlap = 0;
            for (String w : qWords) {
                if (iqWords.contains(w)) overlap++;
            }
            double score = (double) overlap / Math.max(qWords.size(), iqWords.size());
            if (score > bestKeywordScore) {
                bestKeywordScore = score;
                bestKeyword = item;
            }
        }

        // 4. Embedding 相似度
        KnowledgeItem bestEmbed = null;
        double bestEmbedScore = 0;
        double[] qVec = embedding.embed(question);
        for (KnowledgeItem item : items) {
            double[] iVec = questionVecCache.get(item.question.toLowerCase().trim());
            if (iVec == null) continue;
            double sim = embedding.cosineSimilarity(qVec, iVec);
            if (sim > bestEmbedScore) {
                bestEmbedScore = sim;
                bestEmbed = item;
            }
        }

        // 5. 综合
        KnowledgeItem bestItem = bestKeyword != null && bestKeywordScore > 0.3 ? bestKeyword : bestEmbed;
        double bestScore = bestItem == bestKeyword ? bestKeywordScore : bestEmbedScore;
        if (bestItem == bestEmbed) {
            bestScore = Math.max(bestEmbedScore, bestKeywordScore * 0.4);
        }
        if (bestItem == null || bestScore < 0.3) {
            return new RetrievalResult(null, bestScore,
                    "抱歉,我的知识库里没有 \"" + question + "\" 的相关答案。" +
                    "你可以问我关于 MiniMax 平台功能、Java/Spring/Vue 编程、或者基础常识的问题。");
        }

        // 6. 模板化
        String response = formatAnswer(question, bestItem, bestScore);
        return new RetrievalResult(bestItem.answer, bestScore, response);
    }

    private String formatAnswer(String question, KnowledgeItem item, double score) {
        if (score > 0.85) {
            return item.answer;
        }
        if (score > 0.6) {
            return "根据知识库,关于 \"" + item.question + "\":\n" + item.answer;
        }
        return "这个问题可能跟 \"" + item.question + "\" 相关:\n" + item.answer +
                "\n\n(如果答非所问, 请换个更具体的问题。相似度: " +
                String.format("%.0f%%", score * 100) + ")";
    }

    private Set<String> tokenize(String text) {
        return tokenizer.preTokenize(text).stream()
                .filter(t -> t.length() > 1)
                .collect(Collectors.toSet());
    }

    public boolean isReady() { return ready; }
    public int size() { return items.size(); }

    public static class KnowledgeItem {
        public final String question;
        public final String answer;
        public KnowledgeItem(String q, String a) {
            this.question = q;
            this.answer = a;
        }
    }

    public static class RetrievalResult {
        public final String answer;
        public final double score;
        public final String response;
        public RetrievalResult(String a, double s, String r) {
            this.answer = a;
            this.score = s;
            this.response = r;
        }
    }
}
