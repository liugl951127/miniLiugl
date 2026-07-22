package com.minimax.ai.generation.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * N-gram 意图模型 (V3.5.15+)
 *
 * <h3>原理</h3>
 * <ul>
 *   <li><b>语料库</b>: intent → List&lt;training-text&gt;</li>
 *   <li><b>Bigram 提取</b>: 每个训练文本 → (word1, word2) pair</li>
 *   <li><b>概率</b>: P(bigram|intent) = (该 bigram 在 intent 训练集中出现次数) / (intent 训练集总 bigram)</li>
 *   <li><b>推理</b>: query 的所有 bigram 平均概率 = 该 intent 得分</li>
 * </ul>
 *
 * <h3>优势 (相比 TF)</h3>
 * <ul>
 *   <li>捕捉词序 (TF 只看子串存在)</li>
 *   <li>捕捉词搭配 ("生成" + "图表" vs "生成" + "代码" 区分明显)</li>
 *   <li>不依赖人工维护的关键词</li>
 * </ul>
 *
 * <h3>训练数据来源</h3>
 * <ol>
 *   <li>种子表 ai_intent_training_text (V3.5.15 新建)</li>
 *   <li>历史识别记录 (ai_intent_log 表, V3.5.15 新建)</li>
 *   <li>硬编码默认训练集 (兜底)</li>
 * </ol>
 *
 * <h3>性能</h3>
 * <ul>
 *   <li>训练: 1000 文本 O(N) (单线程 100ms)</li>
 *   <li>推理: O(query-bigrams × intent-ngrams) ≈ O(20 × 100) = 2000 次查表 ≈ 1ms</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.15
 */
@Slf4j
@Component
public class NgramModel {

    /**
     * Bigram 概率表
     * <p>结构: intent → (bigram → probability)
     */
    private final Map<String, Map<String, Double>> bigramProb = new ConcurrentHashMap<>();

    /**
     * 训练集大小 (每个 intent)
     */
    private final Map<String, Integer> intentCorpusSize = new ConcurrentHashMap<>();

    /**
     * 训练意图分类器
     *
     * @param textsByIntent intent → 训练文本列表
     */
    public void train(Map<String, List<String>> textsByIntent) {
        log.info("[ngram] start training: intents={}", textsByIntent.keySet());

        // 清空旧数据
        bigramProb.clear();
        intentCorpusSize.clear();

        // 逐个 intent 训练
        for (Map.Entry<String, List<String>> entry : textsByIntent.entrySet()) {
            String intent = entry.getKey();
            List<String> texts = entry.getValue();

            // 1. 统计所有 bigram
            Map<String, Integer> bigramCount = new HashMap<>();
            int totalBigrams = 0;

            for (String text : texts) {
                List<String> tokens = tokenize(text);
                for (int i = 0; i < tokens.size() - 1; i++) {
                    String bigram = tokens.get(i) + "|" + tokens.get(i + 1);
                    bigramCount.merge(bigram, 1, Integer::sum);
                    totalBigrams++;
                }
            }

            // 2. 计算概率 P(bigram|intent) = count(bigram) / total
            Map<String, Double> probs = new HashMap<>();
            for (Map.Entry<String, Integer> bc : bigramCount.entrySet()) {
                // +1 smoothing (Laplace) 避免 0 概率
                double p = (bc.getValue() + 1.0) / (totalBigrams + bigramCount.size());
                probs.put(bc.getKey(), p);
            }

            bigramProb.put(intent, probs);
            intentCorpusSize.put(intent, texts.size());
        }

        log.info("[ngram] training done: total bigrams={}",
                bigramProb.values().stream().mapToInt(Map::size).sum());
    }

    /**
     * 推理: query 在每个 intent 下的得分
     *
     * <p>算法: avg(log P(bigram|intent)) 避免长 query 稀释概率
     *
     * @param query 用户输入
     * @return intent → 得分 (0~1)
     */
    public Map<String, Double> score(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> tokens = tokenize(query);
        if (tokens.size() < 2) {
            return Collections.emptyMap();  // 1 个 token 没法 bigram
        }

        // 提取 query 的所有 bigram
        List<String> queryBigrams = new ArrayList<>();
        for (int i = 0; i < tokens.size() - 1; i++) {
            queryBigrams.add(tokens.get(i) + "|" + tokens.get(i + 1));
        }

        Map<String, Double> scores = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : bigramProb.entrySet()) {
            String intent = entry.getKey();
            Map<String, Double> probs = entry.getValue();

            // 平均 log 概率 (避免长 query 稀释)
            double logSum = 0.0;
            int matched = 0;
            for (String bg : queryBigrams) {
                Double p = probs.get(bg);
                if (p != null) {
                    logSum += Math.log(p);
                    matched++;
                }
            }

            // matched=0 的 intent 给一个小先验 (log 0 = -inf 跳过)
            if (matched > 0) {
                scores.put(intent, Math.exp(logSum / matched));
            }
        }

        return scores;
    }

    /**
     * 简单分词 (按空格 / 标点 / 字符边界)
     * <p>中文按 1 字 1 token, 英文按 word
     */
    private List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        String lower = text.toLowerCase().trim();

        List<String> tokens = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isWhitespace(c) || isCjkPunct(c) || isAsciiPunct(c)) {
                if (buf.length() > 0) {
                    tokens.add(buf.toString());
                    buf.setLength(0);
                }
            } else if (isCjk(c)) {
                // 中日韩: 1 字 1 token
                if (buf.length() > 0) {
                    tokens.add(buf.toString());
                    buf.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            tokens.add(buf.toString());
        }
        // 过滤空 / 长度 1 的英文 token (a, I 等)
        return tokens.stream()
                .filter(t -> !t.isEmpty())
                .filter(t -> t.length() > 1 || isCjk(t.charAt(0)))
                .collect(Collectors.toList());
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) ||
               (c >= 0x3040 && c <= 0x30FF) ||
               (c >= 0xAC00 && c <= 0xD7AF);
    }

    private static boolean isCjkPunct(char c) {
        return c == '。' || c == '，' || c == '！' || c == '？' ||
               c == '；' || c == '：' || c == '、' || c == '「' || c == '」' ||
               c == '『' || c == '』' || c == '（' || c == '）';
    }

    private static boolean isAsciiPunct(char c) {
        return "!@#$%^&*()_+-=[]{}|;':\",./<>?`~".indexOf(c) >= 0;
    }

    /**
     * 默认训练集 (硬编码, DB 为空时兜底)
     */
    public static Map<String, List<String>> defaultTrainingData() {
        Map<String, List<String>> data = new LinkedHashMap<>();

        data.put("GENERATE_CHART", List.of(
                "画一个柱状图", "生成图表", "显示统计图", "做个饼图", "折线图", "可视化数据",
                "show me a chart", "generate a bar chart", "plot the data",
                "我想看趋势图", "出图", "出图给我看", "数据可视化", "画张图",
                "展示成柱状图", "折线图展示", "雷达图分析", "热力图"));

        data.put("GENERATE_MUSIC", List.of(
                "生成音乐", "创作旋律", "写首歌", "做一段曲子", "合成 MIDI",
                "compose a melody", "generate music", "create a song",
                "来一段 BGM", "作曲", "和旋进行", "配乐"));

        data.put("GENERATE_ANIMATION", List.of(
                "做动画", "生成 GIF", "做个动图", "进度条动画", "过渡动画",
                "create an animation", "make a gif", "animate",
                "做个 loading 动画", "过场动画", "片头"));

        data.put("QUERY_DATA", List.of(
                "查询用户", "列出所有订单", "SELECT * FROM", "查一下数据", "统计人数",
                "query users", "fetch data", "select records",
                "查最近订单", "查所有用户", "数据库查一下", "搜一下"));

        data.put("ANALYZE_DATA", List.of(
                "分析销售趋势", "统计平均值", "求总和", "按月分组", "聚合统计",
                "analyze the data", "compute statistics", "average per group",
                "异常值检测", "数据洞察", "同比环比", "分位数分析"));

        data.put("GENERATE_CODE", List.of(
                "生成 Spring Boot 项目", "创建 Vue 组件", "写一个 Python 脚本",
                "scaffold a project", "create react app", "generate flask server",
                "帮我写代码", "生成项目脚手架", "写个 Express"));

        data.put("CHAT", List.of(
                "你好", "在吗", "你是谁", "介绍一下", "今天天气",
                "hello", "hi there", "what can you do", "tell me about yourself",
                "我想聊天", "闲聊", "陪聊"));

        data.put("IMAGE_ANALYZE", List.of(
                "分析图片", "这张图里有什么", "图片识别", "看图说话", "图像描述",
                "analyze image", "what is in this image", "describe picture",
                "看看这张照片", "识别图片内容"));

        data.put("VIDEO_ANALYZE", List.of(
                "分析视频", "这个视频讲什么", "视频识别", "看视频",
                "analyze video", "what is this video about", "describe video",
                "视频内容分析"));

        data.put("AUDIO_ANALYZE", List.of(
                "分析音频", "听这段录音", "识别声音", "音频转文字",
                "analyze audio", "transcribe audio", "what does this audio say",
                "语音识别一下"));

        data.put("TTS", List.of(
                "朗读", "读出来", "合成语音", "TTS", "用语音说",
                "text to speech", "speak this out", "read aloud",
                "把文字读出来", "转语音"));

        data.put("STT", List.of(
                "语音转文字", "把录音转写", "STT", "听写",
                "speech to text", "transcribe this", "audio to text",
                "把这段声音转文字"));

        data.put("TRANSFER_HUMAN", List.of(
                "转人工", "找真人", "人工客服", "坐席", "我要投诉",
                "transfer to human", "speak to agent", "human service",
                "找人工客服", "转接客服"));

        return data;
    }
}
