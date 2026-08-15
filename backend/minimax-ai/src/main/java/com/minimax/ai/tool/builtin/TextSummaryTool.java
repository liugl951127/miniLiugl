package com.minimax.ai.tool.builtin;

import com.minimax.ai.entity.AiTool;
import com.minimax.ai.tool.AiToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文本摘要/情感分析/实体识别/关键词提取 (V2.8.3)
 * 自研实现, 不依赖外部 LLM/NLP 库
 */
@Slf4j
@Component
public class TextSummaryTool implements AiToolExecutor {

    @Override
    public String getCode() { return "text.analyze"; }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        long start = System.currentTimeMillis();
        try {
            String text = (String) input.get("text");
            if (text == null || text.isBlank()) {
                return Map.of("success", false, "message", "text 不能为空");
            }
            String task = (String) input.getOrDefault("task", "all");
            int topK = ((Number) input.getOrDefault("topK", 5)).intValue();
            int maxSummarySentences = ((Number) input.getOrDefault("maxSentences", 3)).intValue();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("charCount", text.length());
            if (task.equals("all") || task.equals("summary")) result.put("summary", summarize(text, maxSummarySentences));
            if (task.equals("all") || task.equals("sentiment")) result.put("sentiment", analyzeSentiment(text));
            if (task.equals("all") || task.equals("entities")) result.put("entities", extractEntities(text));
            if (task.equals("all") || task.equals("keywords")) result.put("keywords", extractKeywords(text, topK));
            result.put("costMs", System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("TextSummaryTool failed", e);
            return Map.of("success", false, "message", "分析失败: " + e.getMessage());
        }
    }

    public String summarize(String text, int maxSentences) {
        if (text == null) return "";
        String[] sentences = text.split("[。!?;\\n]+");
        List<String> valid = new ArrayList<>();
        for (String s : sentences) { s = s.trim(); if (s.length() >= 5) valid.add(s); }
        if (valid.size() <= maxSentences) return String.join("。 ", valid) + "。";
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String s : valid) {
            for (String word : s.replaceAll("[\\s,，。!?;:\"''()\\[\\]{}【】]", " ").split("\\s+")) {
                if (word.length() >= 2) wordFreq.merge(word, 1, Integer::sum);
            }
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        for (int i = 0; i < valid.size(); i++) {
            double score = 0;
            for (String word : valid.get(i).split("\\s+")) {
                if (word.length() >= 2) score += wordFreq.getOrDefault(word, 0);
            }
            score *= (1.0 + 1.0 / (i + 1));
            scores.put(valid.get(i), score);
        }
        List<String> topK = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(maxSentences).map(Map.Entry::getKey).collect(Collectors.toList());
        List<String> ordered = new ArrayList<>(valid);
        ordered.retainAll(topK);
        Collections.sort(ordered, Comparator.comparingInt(valid::indexOf));
        return String.join("。 ", ordered) + "。";
    }

    public Map<String, Object> analyzeSentiment(String text) {
        Set<String> positive = Set.of("好", "棒", "优秀", "完美", "喜欢", "爱", "开心", "快乐", "幸福", "满意",
                "good", "great", "excellent", "perfect", "love", "happy", "best", "amazing");
        Set<String> negative = Set.of("差", "坏", "糟糕", "失败", "讨厌", "恨", "伤心", "难过", "失望", "生气",
                "bad", "terrible", "awful", "hate", "sad", "angry", "worst", "horrible");
        int pos = 0, neg = 0;
        String lower = text.toLowerCase();
        for (String p : positive) { int idx = 0; while ((idx = lower.indexOf(p.toLowerCase(), idx)) != -1) { pos++; idx += p.length(); } }
        for (String n : negative) { int idx = 0; while ((idx = lower.indexOf(n.toLowerCase(), idx)) != -1) { neg++; idx += n.length(); } }
        String label; double score;
        if (pos == 0 && neg == 0) { label = "NEUTRAL"; score = 0.5; }
        else if (pos > neg) { label = "POSITIVE"; score = 0.5 + 0.5 * (pos - neg) / (pos + neg); }
        else if (neg > pos) { label = "NEGATIVE"; score = 0.5 - 0.5 * (neg - pos) / (pos + neg); }
        else { label = "NEUTRAL"; score = 0.5; }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("label", label);
        r.put("score", Math.round(score * 1000.0) / 1000.0);
        r.put("positiveCount", pos);
        r.put("negativeCount", neg);
        return r;
    }

    public Map<String, List<String>> extractEntities(String text) {
        Map<String, List<String>> entities = new LinkedHashMap<>();
        entities.put("EMAIL", matchAll(text, Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")));
        entities.put("URL", matchAll(text, Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")));
        entities.put("MOBILE", matchAll(text, Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)")));
        entities.put("ID_CARD", matchAll(text, Pattern.compile("(?<!\\d)[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx](?!\\d)")));
        entities.put("IPV4", matchAll(text, Pattern.compile("(?<!\\d)(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(?!\\d)")));
        entities.put("MONEY", matchAll(text, Pattern.compile("[¥$€]\\s*\\d+(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:,\\d{3})*(?:\\.\\d+)?\\s*[元块钱]")));
        entities.put("DATE", matchAll(text, Pattern.compile("\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}|\\d{4}年\\d{1,2}月")));
        entities.put("TIME", matchAll(text, Pattern.compile("\\d{1,2}:\\d{2}(?::\\d{2})?")));
        entities.put("PERCENT", matchAll(text, Pattern.compile("\\d+(?:\\.\\d+)?%")));
        return entities;
    }

    private List<String> matchAll(String text, Pattern p) {
        Matcher m = p.matcher(text);
        Set<String> set = new LinkedHashSet<>();
        while (m.find()) set.add(m.group());
        return new ArrayList<>(set);
    }

    public List<String> extractKeywords(String text, int topN) {
        if (text == null) return List.of();
        Set<String> STOP = Set.of("的", "了", "是", "在", "和", "与", "the", "a", "an", "is", "are", "to", "of", "and");
        Map<String, Integer> freq = new HashMap<>();
        StringBuilder cn = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) { cn.append(c); }
            else {
                if (cn.length() > 0) {
                    for (int i = 0; i < cn.length() - 1; i++) freq.merge(cn.substring(i, i + 2), 1, Integer::sum);
                    cn.setLength(0);
                }
            }
        }
        if (cn.length() > 1) for (int i = 0; i < cn.length() - 1; i++) freq.merge(cn.substring(i, i + 2), 1, Integer::sum);
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() >= 3) freq.merge(word, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .filter(e -> e.getKey().length() >= 2 && !STOP.contains(e.getKey()))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
