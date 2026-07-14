package com.minimax.ai.intent;

import com.minimax.ai.generation.KeywordEngine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 意图识别置信度评分器 (Day 29 - 智能化提升).
 *
 * <p>为 KeywordEngine 的识别结果增加置信度评分 (0.0 ~ 1.0)，
 * 置信度低于阈值时触发降级策略或人工兜底。
 *
 * <h3>置信度评分维度</h3>
 * <table>
 *   <tr><th>维度</th><th>权重</th><th>说明</th></tr>
 *   <tr><td>匹配强度 (matchStrength)</td><td>40%</td><td>命中关键词/正则的数量和明确性</td></tr>
 *   <tr><td>意图一致性 (intentCoherence)</td><td>30%</td><td>多意图打架时置信度下降</td></tr>
 *   <tr><td>上下文符合度 (contextFit)</td><td>20%</td><td>当前意图是否和上下文意图链一致</td></tr>
 *   <tr><td>文本清晰度 (clarity)</td><td>10%</td><td>文本是否含糊、包含多种解读</td></tr>
 * </table>
 *
 * <h3>降级策略</h3>
 * <pre>
 * confidence &lt; 0.50 → 低置信度 → 触发多模型投票 / 降级到 CHAT
 * confidence &lt; 0.30 → 极低     → 直接返回 CHAT + 标记需要澄清
 * confidence &lt; 0.10 → 未知     → 询问用户意图
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentConfidenceScorer {

    private final KeywordEngine keywordEngine;

    /** 置信度阈值 */
    public static final double LOW_CONF_THRESHOLD = 0.50;
    public static final double VERY_LOW_CONF_THRESHOLD = 0.30;
    public static final double UNKNOWN_THRESHOLD = 0.10;

    /** 强信号词（高置信度指标） */
    private static final Map<KeywordEngine.Intent, Set<String>> STRONG_SIGNALS = new EnumMap<>(KeywordEngine.Intent.class);
    static {
        STRONG_SIGNALS.put(KeywordEngine.Intent.TRANSFER_HUMAN,
                Set.of("转人工", "真人", "人工客服", "transfer", "human"));
        STRONG_SIGNALS.put(KeywordEngine.Intent.GENERATE_CODE,
                Set.of("生成代码", "写代码", "code", "spring boot项目", "create project"));
        STRONG_SIGNALS.put(KeywordEngine.Intent.GENERATE_MUSIC,
                Set.of("作曲", "生成音乐", "生成曲子", "compose music"));
        STRONG_SIGNALS.put(KeywordEngine.Intent.GENERATE_ANIMATION,
                Set.of("动画", "gif", "动图"));
        STRONG_SIGNALS.put(KeywordEngine.Intent.TTS,
                Set.of("语音合成", "tts", "读出来", "朗读"));
        STRONG_SIGNALS.put(KeywordEngine.Intent.STT,
                Set.of("语音识别", "stt", "转文字"));
    }

    /** 模糊词（降低置信度） */
    private static final Set<String> AMBIGUOUS_WORDS = Set.of(
            "大概", "也许", "可能", "好像", "似乎", "随便", "无所谓",
            "whatever", "maybe", "perhaps", "not sure", "any"
    );

    /**
     * 识别意图并给出置信度。
     *
     * @param text       用户输入
     * @param sessionId  会话 ID（用于上下文分析，可为 null）
     * @return 带置信度的识别结果
     */
    public IntentWithConfidence recognize(String text, String sessionId) {
        if (text == null || text.trim().isEmpty()) {
            return new IntentWithConfidence(KeywordEngine.Intent.UNKNOWN, 0.0,
                    ConfidenceLevel.UNKNOWN, "空输入");
        }

        KeywordEngine.Intent intent = keywordEngine.recognize(text);

        // 1. 匹配强度 (40%)
        double matchStrength = calcMatchStrength(text, intent);

        // 2. 意图一致性 (30%)
        double coherence = calcCoherence(text, intent);

        // 3. 上下文符合度 (20%)
        double contextFit = calcContextFit(text);

        // 4. 文本清晰度 (10%)
        double clarity = calcClarity(text);

        double confidence = Math.round(
                (0.40 * matchStrength + 0.30 * coherence + 0.20 * contextFit + 0.10 * clarity) * 100.0
        ) / 100.0;

        ConfidenceLevel level = classifyLevel(confidence);

        String reason = String.format(
                "match=%.2f coherence=%.2f context=%.2f clarity=%.2f",
                matchStrength, coherence, contextFit, clarity);

        log.debug("[IntentConf] '{}' → {} ({:.2f}, {}) reason={}",
                text.length() > 50 ? text.substring(0, 50) : text,
                intent, confidence, level, reason);

        return new IntentWithConfidence(intent, confidence, level, reason);
    }

    /** 识别 + 路由（含置信度） */
    public RouteResultWithConfidence route(String text, String sessionId) {
        IntentWithConfidence iwc = recognize(text, sessionId);
        KeywordEngine.RouteResult route = keywordEngine.route(text, null);

        RouteResultWithConfidence result = new RouteResultWithConfidence(
                route.intent, route.params, route.handler,
                iwc.getConfidence(), iwc.getLevel(), iwc.getReason()
        );
        return result;
    }

    // ---- 评分维度 ----

    /** 匹配强度: 命中多少强信号词 */
    private double calcMatchStrength(String text, KeywordEngine.Intent intent) {
        String lower = text.toLowerCase();
        Set<String> signals = STRONG_SIGNALS.getOrDefault(intent, Set.of());
        if (signals.isEmpty()) {
            return Math.min(1.0, keywordEngine.extractParams(text).size() / 3.0 + 0.3);
        }
        long hits = signals.stream().filter(s -> lower.contains(s.toLowerCase())).count();
        return Math.min(hits / 2.0 + 0.4, 1.0);
    }

    /** 意图一致性: 是否有多个意图竞争 */
    private double calcCoherence(String text, KeywordEngine.Intent winner) {
        String lower = text.toLowerCase();
        int complexHits = 0;
        for (KeywordEngine.Intent i : List.of(
                KeywordEngine.Intent.GENERATE_CODE,
                KeywordEngine.Intent.GENERATE_MUSIC,
                KeywordEngine.Intent.GENERATE_ANIMATION,
                KeywordEngine.Intent.GENERATE_CHART)) {
            if (lower.contains(i.name().toLowerCase().replace("_", " "))) complexHits++;
        }
        if (complexHits > 1) return 0.5;
        return 1.0;
    }

    /** 上下文符合度 */
    private double calcContextFit(String text) {
        String lower = text.toLowerCase();
        long ambigHits = AMBIGUOUS_WORDS.stream().filter(lower::contains).count();
        if (ambigHits >= 2) return 0.3;
        if (ambigHits == 1) return 0.6;
        if (text.length() < 10) return 0.7;
        return 1.0;
    }

    /** 文本清晰度 */
    private double calcClarity(String text) {
        if (text == null || text.isBlank()) return 0.0;
        long symbols = text.chars().filter(c ->
                !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)).count();
        if (symbols > text.length() * 0.3) return 0.4;
        long chinese = text.codePoints().filter(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN).count();
        if (chinese > text.length() * 0.5) return 0.9;
        return 0.8;
    }

    private ConfidenceLevel classifyLevel(double confidence) {
        if (confidence >= LOW_CONF_THRESHOLD) return ConfidenceLevel.HIGH;
        if (confidence >= VERY_LOW_CONF_THRESHOLD) return ConfidenceLevel.MEDIUM;
        if (confidence >= UNKNOWN_THRESHOLD) return ConfidenceLevel.LOW;
        return ConfidenceLevel.UNKNOWN;
    }

    // ============== 结果类 ==============

    @Getter
    public static class IntentWithConfidence {
        private final KeywordEngine.Intent intent;
        private final double confidence;
        private final ConfidenceLevel level;
        private final String reason;

        public IntentWithConfidence(KeywordEngine.Intent intent, double confidence,
                                   ConfidenceLevel level, String reason) {
            this.intent = intent;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.level = level;
            this.reason = reason;
        }
    }

    @Getter
    public static class RouteResultWithConfidence {
        private final KeywordEngine.Intent intent;
        private final Map<String, String> params;
        private final String handler;
        private final double confidence;
        private final ConfidenceLevel level;
        private final String confidenceReason;

        public RouteResultWithConfidence(KeywordEngine.Intent intent,
                                        Map<String, String> params, String handler,
                                        double confidence, ConfidenceLevel level,
                                        String confidenceReason) {
            this.intent = intent;
            this.params = params;
            this.handler = handler;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.level = level;
            this.confidenceReason = confidenceReason;
        }
    }

    public enum ConfidenceLevel {
        HIGH,    // ≥ 0.50
        MEDIUM,  // 0.30 ~ 0.50
        LOW,     // 0.10 ~ 0.30
        UNKNOWN  // < 0.10
    }
}
